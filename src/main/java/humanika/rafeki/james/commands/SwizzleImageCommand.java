package humanika.rafeki.james.commands;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import javax.imageio.ImageIO;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;
import discord4j.core.object.entity.Attachment;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.MessageCreateFields;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import humanika.rafeki.james.James;
import humanika.rafeki.james.utils.ImageSwizzler;

public class SwizzleImageCommand extends SlashCommand {
    private static final String[] VAR_ARRAY = {"image1", "image2", "image3", "image4"};
    private static final List<String> VAR_LIST = Arrays.asList(VAR_ARRAY);

    public SwizzleImageCommand() {}

    @Override
    public String getName() {
        return "swizzleimage";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        Interaction interaction = event.getInteraction();
        if(!interaction.getGuildId().isPresent())
            return handleDirectMessage();

        MessageChannel channel = interaction.getChannel().block();
        if(channel instanceof TextChannel) {
            TextChannel textChannel = (TextChannel)channel;
            if(textChannel.isNsfw())
                return event.reply().withContent("I'm under 18 years of age and will not accept images in NSFW (age-restricted) channels. There's nothing wrong with having a beard at my age. Stop judging me.");
        }

        StringBuffer errors = new StringBuffer();
        List<Attachment> imageAttachments;

        Optional<Long> swizzleLong = getLong("swizzle");
        OptionalInt swizzleInt = OptionalInt.empty();
        if(swizzleLong.isPresent())
            swizzleInt = OptionalInt.of(swizzleLong.get().intValue());
        ArrayList<MessageCreateFields.File> result = new ArrayList<>();

        for(String var : VAR_LIST) {
            Optional<Attachment> maybeData = getAttachment(var);
            if(!maybeData.isPresent())
                continue;
            Attachment data = maybeData.get();
            try {
                processOneFile(data, errors, result, swizzleInt);
            } catch(IOException ioe) {
                errors.append(data.getFilename() + ": unable to read file");
            }
        }

        if(result.size() < 1) {
            if(errors.length() < 1)
                // Should never happen, but just in case of logic errors:
                errors.append("Please attach one or more images.");
            return event.reply(errors.toString()).withEphemeral(isEphemeral());
        } else if(errors.length() > 0)
            return event.reply(errors.toString()).withFiles(result).withEphemeral(isEphemeral()).withContent(describe(swizzleLong, interaction));
        else
            return event.reply().withFiles(result).withEphemeral(isEphemeral()).withContent(describe(swizzleLong, interaction));
    }

    private String describe(Optional<Long> swizzle, Interaction interaction) {
        StringBuilder builder = new StringBuilder();
        Optional<Member> member = interaction.getMember();
        if(member.isPresent())
            builder.append(member.get().getMention()).append(" sent this image");
        else
            builder.append("You sent this image");
        if(swizzle.isPresent())
            builder.append(" and asked me to recolor it for swizzle ").append(swizzle.get());
        else
            builder.append(" and asked me to recolor it for swizzles 1 through 6.");
        if(member.isPresent())
            builder.append(" If you don't like the image, it's their fault.");
        else
            builder.append(" If you don't like the image, it's your fault.");
        return builder.toString();
    }

    private void processOneFile(Attachment data, StringBuffer errors, ArrayList<MessageCreateFields.File> result, OptionalInt swizzle) throws IOException {
        int maxSwizzleImageSize = James.getConfig().maxSwizzleImageSize;
        String filename = data.getFilename();
        OptionalInt width = data.getWidth();
        OptionalInt height = data.getHeight();
        if(!width.isPresent() || !height.isPresent() || width.getAsInt() < 1 || height.getAsInt() < 1) {
            errors.append(filename + ": is not an image\n");
            return;
        }
        int w = width.getAsInt();
        int h = height.getAsInt();
        if(w > maxSwizzleImageSize || h > maxSwizzleImageSize) {
            errors.append(filename + " is larger than " + maxSwizzleImageSize + "px.");
            return;
        }
        String urlString = data.getUrl();
        URL url = new URL(urlString);
        BufferedImage image = ImageIO.read(url);
        // ImageIO.write(image, "png", new File("/tmp/swizzles/0.png"));
        // for(int i = 0; i < 28; i++)
        //     ImageIO.write(new ImageSwizzler().swizzleToImage(image, OptionalInt.of(i)), "png", new File("/tmp/swizzles/"+(i+1)+".png"));
        InputStream swizzled = new ImageSwizzler().swizzle(image, swizzle);
        String outputName = filename.replace("\\.[^.]+$","") + ".png";
        if(data.isSpoiler())
            result.add(MessageCreateFields.FileSpoiler.of(outputName, swizzled));
        else
            result.add(MessageCreateFields.File.of(outputName, swizzled));
    }
}
