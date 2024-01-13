package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateFields;
import humanika.rafeki.james.James;
import humanika.rafeki.james.Utils;
import humanika.rafeki.james.data.JamesConfig;
import humanika.rafeki.james.utils.Mask;
import humanika.rafeki.james.utils.MaskException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class MaskImageCommand extends PrimitiveCommand {
    private static final String[] VAR_ARRAY = {"image1", "image2", "image3", "image4"};
    private static final List<String> VAR_LIST = Arrays.asList(VAR_ARRAY);
    private static final Logger LOGGER = LoggerFactory.getLogger(MaskImageCommand.class);

    @Override
    public boolean shouldDefer() {
        return false;
    }

    @Override
    public String getName() {
        return "mask";
    }

    @Override
    public String getFullName() {
        return "mask";
    }

    @Override
    public Optional<String> getJson() {
        return Optional.of("mask-command.json");
    }

    @Override
    public Mono<Void> handleChatCommand() {
        LOGGER.info("MaskImageCommand::handleChatCommand starting");
        try {
            Interaction interaction = data.getInteraction();
            MessageChannel channel = interaction.getChannel().block();
            if(channel instanceof TextChannel) {
                TextChannel textChannel = (TextChannel)channel;
                if(textChannel.isNsfw())
                    return getChatEvent().reply().withContent("I'm under 18 years of age and will not accept images in NSFW (age-restricted) channels. There's nothing wrong with having a beard at my age. Stop judging me.");
            }

            StringBuffer errors = new StringBuffer();
            List<Attachment> imageAttachments;

            StringBuffer description = new StringBuffer();
            ArrayList<Attachment> attachments = new ArrayList<>();;
            for(String var : VAR_LIST) {
                Optional<Attachment> maybeData = data.getAttachment(var);
                if(!maybeData.isPresent())
                    continue;
                attachments.add(maybeData.get());
            }

            description.append(describe(interaction, attachments.size()));

            ArrayList<MessageCreateFields.File> result = new ArrayList<>();
            ArrayList<EmbedCreateFields.Field> fields = new ArrayList<>();
            for(Attachment data : attachments) {
                String outputName = data.getFilename().replace("[.][^.]*\\z", ".png")
                    .replaceAll("[^a-zA-Z0-9._ -]+"," ").replaceAll("\\s+", "_");
                try {
                    processOneFile(outputName, data, errors, result, fields);
                } catch(IOException ioe) {
                    errors.append(data.getFilename() + ": unable to read file");
                    description.append("Unable to read file!\n");
                }
            }

            if(result.size() < 1) {
                if(errors.length() < 1)
                    errors.append("Please attach one or more images.");
                errors.insert(0, "## No valid images!\n");
                return getChatEvent().reply(errors.toString()).withEphemeral(data.isEphemeral());
            } else {
                if(errors.length() > 1)
                    description.append("### Errors\n").append(errors.toString());
                return getChatEvent().reply().withFiles(result).withEphemeral(data.isEphemeral())
                    .withContent(description.toString());
            }
        } catch(Exception e) {
            LOGGER.error("MaskImageCommand failed", e);
            throw e;
        }
    }

    private String getCommentary() {
        String babble = James.getState().jamesPhrase("JAMES::ping");
        return babble!=null ? babble : "*no commentary*";
    }

    private String describe(Interaction interaction, int imageCount) {
        StringBuilder builder = new StringBuilder();
        Optional<Member> member = interaction.getMember();
        boolean one = imageCount == 1;
        if(member.isPresent())
            builder.append(member.get().getMention()).append(" sent ");
        else
            builder.append("You sent" );
        builder.append(one ? "this image" : "these images")
            .append(" and asked me to make ")
            .append(one ? "a collison mask for it.\n" : "collision masks for them.\n");
        return builder.toString();
    }

    private void processOneFile(String outputName, Attachment data, StringBuffer errors,
                                ArrayList<MessageCreateFields.File> result,
                                ArrayList<EmbedCreateFields.Field> fields) throws IOException {
        JamesConfig config = James.getConfig();
        String filename = data.getFilename().replaceAll("`", "'");
        OptionalInt width = data.getWidth();
        OptionalInt height = data.getHeight();
        if(!width.isPresent() || !height.isPresent() || width.getAsInt() < 1 || height.getAsInt() < 1) {
            errors.append('`').append(filename).append("`: is not an image\n");
            return;
        }
        String urlString = data.getUrl();
        URL url = null;
        try {
            url = new URI(urlString).toURL();
        } catch(URISyntaxException urise) {
            errors.append('`').append(filename).append("`: bad URL from discord: " + urise.getMessage() + '\n');
            return;
        }
        BufferedImage image = ImageIO.read(url);
        Mask mask = null;
        LOGGER.info("generating mask");
        try {
            mask = new Mask(image);
        } catch(MaskException me) {
            LOGGER.error("infinite loop in mask generation", me);
            errors.append('`').append(filename).append("`: possible infinite loop in mask generation\n");
            return;
        } catch(Exception e) {
            LOGGER.error("mask generation failed", e);
            errors.append('`').append(filename).append("`: mask generation failed (\"" + e.getMessage() + "\")\n");
            return;
        }
        LOGGER.info("got a mask");
        mask.drawMask(image);
        StringBuffer description = new StringBuffer(100);
        InputStream swizzledStream = Utils.imageStream(image);
        if(data.isSpoiler())
            result.add(MessageCreateFields.FileSpoiler.of(outputName, swizzledStream));
        else
            result.add(MessageCreateFields.File.of(outputName, swizzledStream));
        fields.add(EmbedCreateFields.Field.of(outputName, description.toString(), true));
    }
}
