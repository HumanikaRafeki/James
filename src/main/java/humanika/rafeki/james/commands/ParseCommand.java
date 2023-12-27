package humanika.rafeki.james.commands;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateFields;

import humanika.rafeki.james.James;
import humanika.rafeki.james.Utils;
import humanika.rafeki.james.utils.KorathCipher;
import humanika.rafeki.james.data.EndlessSky;
import humanika.rafeki.james.data.JamesState;
import humanika.rafeki.james.phrases.PhraseDatabase;
import humanika.rafeki.james.phrases.PhraseLimits;
import humanika.rafeki.james.phrases.NewsDatabase;

abstract class ParseCommand extends SlashCommand {
    protected static final int MAX_STRING_LENGTH = 1000; // max embed field size is 1024
    protected static final Pattern REPITITION = Pattern.compile("\\A(\\d+)\\s+");

    protected abstract String invalidInputDescription();
    protected abstract List<String> getVarList();
    protected abstract String[] processInput(int count, PhraseDatabase phrases, NewsDatabase news, String entry, PhraseLimits limits);

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        if(!event.getInteraction().getGuildId().isPresent())
            return handleDirectMessage(event);

        String description = "";
        boolean ephemeral = isEphemeral(event);
        if(!ephemeral) {
            String mention = event.getInteraction().getUser().getMention();
            description = "Requested by " + mention;
        }

        JamesState state = James.getState();
        PhraseLimits limits = state.getPhraseLimits();
        EndlessSky sky = state.getEndlessSky();
        PhraseDatabase phrases = sky.getPhrases();
        NewsDatabase news = sky.getNews();
        int maxRepetitions = James.getConfig().maxPhraseCommandRepetitions;

        List<EmbedCreateFields.Field> fields = new ArrayList<EmbedCreateFields.Field>();
        int linesRemaining = maxRepetitions;
        for(String var : getVarList()) {
            String entry = getStringOrDefault(event, var, "").trim().replace("\\s+"," ");
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
        return event.reply().withEmbeds(embed).withEphemeral(ephemeral);
    }
}
