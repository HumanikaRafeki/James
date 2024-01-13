package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import humanika.rafeki.james.James;
import humanika.rafeki.james.Utils;
import humanika.rafeki.james.data.EndlessSky;
import humanika.rafeki.james.data.JamesState;
import humanika.rafeki.james.phrases.NewsDatabase;
import humanika.rafeki.james.phrases.PhraseDatabase;
import humanika.rafeki.james.phrases.PhraseLimits;
import humanika.rafeki.james.utils.KorathCipher;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.mcofficer.esparser.DataFile;
import me.mcofficer.esparser.DataNode;
import me.mcofficer.esparser.DataNodeStringLogger;
import reactor.core.publisher.Mono;

abstract class ParseCommand extends PrimitiveCommand {
    protected static final int MAX_STRING_LENGTH = 1000; // max embed field size is 1024
    protected static final Pattern REPITITION = Pattern.compile("\\A(\\d+)\\s+");

    protected abstract String invalidInputDescription();
    protected abstract List<String> getVarList();
    protected abstract String[] processInput(int count, PhraseDatabase phrases, NewsDatabase news, String entry, PhraseLimits limits);

    @Override
    public Mono<Void> handleChatCommand() {
        Interaction interaction = data.getInteraction();
        JamesState state = James.getState();
        PhraseLimits limits = state.getPhraseLimits();
        EndlessSky sky = state.getEndlessSky();
        PhraseDatabase phrases = sky.getPhrases();
        NewsDatabase news = sky.getNews();

        Optional<Attachment> maybeData = data.getAttachment("data");
        if(maybeData.isPresent()) {
            Attachment data = maybeData.get();
            List<Attachment> attachments = new ArrayList<Attachment>();
            attachments.add(data);
            news = new NewsDatabase(news);
            phrases = new PhraseDatabase(phrases);
            List<String> errors = readAttachments(attachments, phrases, news, limits);
            if(errors != null && errors.size() > 0) {
                StringBuilder builder = new StringBuilder();
                for(int i = 0; i < errors.size(); i++) {
                    if(i > 0)
                        builder.append('\n');
                    builder.append(errors.get(i));
                    if(builder.length() >= MAX_STRING_LENGTH)
                        break;
                }
                // "very long string" becomes "very long s..."
                if(builder.length() > MAX_STRING_LENGTH) {
                    builder.delete(MAX_STRING_LENGTH - 3, builder.length());
                    builder.append("...");
                }
                return getChatEvent().reply(builder.toString());
            }
        }
        String description = "";
        boolean ephemeral = data.isEphemeral();
        if(!ephemeral) {
            String mention = getChatEvent().getInteraction().getUser().getMention();
            description = "Requested by " + mention;
        }

        int maxRepetitions = James.getConfig().maxPhraseCommandRepetitions;

        List<EmbedCreateFields.Field> fields = new ArrayList<EmbedCreateFields.Field>();
        int linesRemaining = maxRepetitions;
        for(String var : getVarList()) {
            String entry = data.getStringOrDefault(var, "").trim().replace("\\s+"," ");
            if(entry.length() < 1)
                continue;
            Matcher matcher = REPITITION.matcher(entry);
            int count = 1;
            if(matcher.find() && matcher.end() < entry.length()) {
                count = Integer.parseInt(matcher.group(1), 10);
                if(count < 1)
                    count = 1;
                if(count > maxRepetitions)
                    count = maxRepetitions;
                if(count > linesRemaining)
                    count = linesRemaining;
                entry = entry.substring(matcher.end());
            }
            String[] result = processInput(count, phrases, news, entry, limits);
            fields.add(EmbedCreateFields.Field.of(result[0], result[1], false));
            if(count < 1)
                break;
            linesRemaining -= count;
        }

        if(fields.size() <= 0) {
            if(description.length() > 0)
                description += '\n';
            description += invalidInputDescription();
        }

        EmbedCreateSpec embed = EmbedCreateSpec.create().withFields(fields);
        if(description.length() > 0)
            embed = embed.withDescription(description);
        return getChatEvent().reply().withEmbeds(embed).withEphemeral(ephemeral);
    }

    private List<String> readAttachments(List<Attachment> attachments, PhraseDatabase phrases, NewsDatabase news, PhraseLimits limits) {
        if(attachments.size() > 0) {
            List<String> errors = validateAttachmentList(attachments);
            if(errors != null && errors.size() > 0)
                return errors;
            errors = readFromAttachments(attachments, phrases, news);
            if(errors != null && errors.size() > 0)
                return errors;
        }
        return null;
    }

    private List<String> readFromAttachments(List<Attachment> attachments, PhraseDatabase phrases, NewsDatabase news) {
        DataNodeStringLogger logger = new DataNodeStringLogger();
        List<String> errors = new ArrayList<String>();

        for(Attachment a : attachments) {
            try {
                DataFile file = null;
                try {
                    file = readAttachment(a, logger);
                } catch(URISyntaxException urise) {
                    errors.add(a.getFilename() + ": bad URL from discord");
                    continue;
                }
                if(file == null) {
                    errors.add(a.getFilename() + ": could not read" );
                    continue;
                }
                phrases.addPhrases(file.getNodes());
                news.addNews(file.getNodes());
                logger.stopLogging();
                logger.freeResources();
            } catch(IOException exc) {
                errors.add(a.getFilename() + ": could not read: " + exc);
                return errors;
            }
        }

        String parserErrors = logger.toString();
        if(parserErrors.length() > 1000)
            parserErrors = parserErrors.substring(0,1000);
        if(parserErrors.length() > 0)
            errors.add(parserErrors);
        return errors;
    }

    private List<String> validateAttachmentList(List<Attachment> attachments) {
        int maxPhraseAttachmentSize = James.getConfig().maxPhraseAttachmentSize;
        int acceptable = 0;
        List<String> errors = new ArrayList<String>();
        errors.add(String.format("Please attach one or more text files. They must be less than %.1f kiB total.\n", maxPhraseAttachmentSize/1024.0));
        int start = errors.size();
        long size = 0;
        for(Attachment a : attachments)
            if(validateAttachment(a, errors)) {
                acceptable++;
                size += a.getSize();
            }
        
        if(size > maxPhraseAttachmentSize)
            errors.add("Total file size is too large: "
                       + String.format("%.1f", size/1024.0) + " > "
                       + String.format("%.1f", maxPhraseAttachmentSize/1024.0) + "kiB");
        if(errors.size() > 1)
            return errors;
        else
            return null;
    }

    private boolean validateAttachment(Attachment a, List<String> errors) {
        if(a.getHeight().isPresent()) {
            errors.add(a.getFilename() + ": is not a text file\n");
            return false;
        }
        Optional<String> maybeContent = a.getContentType();
        if(!maybeContent.isPresent()) {
            errors.add(a.getFilename() + ": has no content type");
            return false;
        }
        String type = maybeContent.get();
        int maxPhraseAttachmentSize = James.getConfig().maxPhraseAttachmentSize;
        if(!type.startsWith("text/plain"))
            errors.add(a.getFilename() + ": is not a text file (content type \""
                       + type + "\" expected \"text/plain charset=utf-8\"\n");
        else if(type.indexOf("charset=utf-8") < 0)
            errors.add(a.getFilename() + ": is not a text file (content type \""
                       + type + "\" expected \"text/plain charset=utf-8\"\n");
        else if(a.getSize() > maxPhraseAttachmentSize)
            errors.add(a.getFilename() + ": is too large: "
                       + String.format("%.1f", a.getSize()/1024.0) + " > "
                       + String.format("%.1f", maxPhraseAttachmentSize/1024.0) + " kiB\n");
        else if(a.getSize() == 0)
            errors.add(a.getFilename() + ": is empty\n");
        else
            return true;
        return false;
    }

    private DataFile readAttachment(Attachment a, DataNodeStringLogger logger) throws IOException, URISyntaxException {
        String result = Utils.downloadAsString(new URI(a.getUrl()).toURL());
        String[] lines = result.split("(?<=\\R)");
        return new DataFile(Arrays.asList(lines), logger);
    }
}
