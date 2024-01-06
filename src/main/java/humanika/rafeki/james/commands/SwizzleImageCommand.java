package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateFields;
import humanika.rafeki.james.James;
import humanika.rafeki.james.data.JamesConfig;
import humanika.rafeki.james.utils.ImageSwizzler;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import javax.imageio.ImageIO;
import reactor.core.publisher.Mono;

public class SwizzleImageCommand extends PrimitiveSlashCommand {
    private static final String[] VAR_ARRAY = {"image1", "image2", "image3", "image4"};
    private static final List<String> VAR_LIST = Arrays.asList(VAR_ARRAY);

    public SwizzleImageCommand() {}

    @Override
    public String getName() {
        return "swizzleimage";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        Interaction interaction = data.getInteraction();
        // FIXME: Don't block here.
        MessageChannel channel = interaction.getChannel().block();
        if(channel instanceof TextChannel) {
            TextChannel textChannel = (TextChannel)channel;
            if(textChannel.isNsfw())
                return getChatEvent().reply().withContent("I'm under 18 years of age and will not accept images in NSFW (age-restricted) channels. There's nothing wrong with having a beard at my age. Stop judging me.");
        }

        StringBuffer errors = new StringBuffer();
        List<Attachment> imageAttachments;

        boolean swizzleAll = data.getBooleanOrDefault("all", Boolean.FALSE).booleanValue();
        Optional<Long> swizzleLong = data.getLong("swizzle");
        OptionalInt swizzleInt = OptionalInt.empty();
        if(!swizzleAll) {
            if(swizzleLong.isPresent())
                swizzleInt = OptionalInt.of(swizzleLong.get().intValue());
        }
        ArrayList<MessageCreateFields.File> result = new ArrayList<>();

        for(String var : VAR_LIST) {
            Optional<Attachment> maybeData = data.getAttachment(var);
            if(!maybeData.isPresent())
                continue;
            Attachment data = maybeData.get();
            try {
                processOneFile(data, errors, result, swizzleInt, swizzleAll);
            } catch(IOException ioe) {
                errors.append(data.getFilename() + ": unable to read file");
            }
        }

        if(result.size() < 1) {
            if(errors.length() < 1)
                // Should never happen, but just in case of logic errors:
                errors.append("Please attach one or more images.");
            return getChatEvent().reply(errors.toString()).withEphemeral(data.isEphemeral());
        } else if(errors.length() > 0)
            return getChatEvent().reply(errors.toString()).withFiles(result)
                .withEphemeral(data.isEphemeral())
                .withContent(describe(swizzleLong, swizzleAll, interaction));
        else
            return getChatEvent().reply().withFiles(result).withEphemeral(data.isEphemeral())
                .withContent(describe(swizzleLong, swizzleAll, interaction));
    }

    private String describe(Optional<Long> swizzle, boolean swizzleAll, Interaction interaction) {
        StringBuilder builder = new StringBuilder();
        Optional<Member> member = interaction.getMember();
        if(member.isPresent())
            builder.append(member.get().getMention()).append(" sent this image");
        else
            builder.append("You sent this image");
        if(swizzleAll)
            builder.append(" and decadently asked me to recolor it with all swizzles.");
        else if(swizzle.isPresent())
            builder.append(" and asked me to recolor it for swizzle ").append(swizzle.get());
        else
            builder.append(" and asked me to recolor it for swizzles 1 through 6.");
        if(member.isPresent())
            builder.append(" If you don't like the image, it's their fault.");
        else
            builder.append(" If you don't like the image, it's your fault.");
        return builder.toString();
    }

    private void processOneFile(Attachment data, StringBuffer errors, ArrayList<MessageCreateFields.File> result, OptionalInt swizzleInt, boolean swizzleAll) throws IOException {
        JamesConfig config = James.getConfig();
        String filename = data.getFilename();
        OptionalInt width = data.getWidth();
        OptionalInt height = data.getHeight();
        if(!width.isPresent() || !height.isPresent() || width.getAsInt() < 1 || height.getAsInt() < 1) {
            errors.append(filename + ": is not an image\n");
            return;
        }
        int w = width.getAsInt();
        int h = height.getAsInt();
        if(swizzleAll && (w > config.maxAllSwizzleWidth || h > config.maxAllSwizzleHeight)) {
            errors.append(filename + ": when using all swizzles, image must be " + config.maxAllSwizzleWidth + "x" + config.maxAllSwizzleHeight + "px. or smaller");
            return;
        }
        if(w > config.maxSwizzleImageWidth || h > config.maxSwizzleImageHeight) {
            errors.append(filename + " is larger than " + config.maxSwizzleImageWidth + "x" + config.maxSwizzleImageHeight + "px.");
            return;
        }
        String urlString = data.getUrl();
        URL url = new URL(urlString);
        BufferedImage image = ImageIO.read(url);
        // ImageIO.write(image, "png", new File("/tmp/swizzles/0.png"));
        // for(int i = 0; i < 28; i++)
        //     ImageIO.write(new ImageSwizzler().swizzleToImage(image, OptionalInt.of(i)), "png", new File("/tmp/swizzles/"+(i+1)+".png"));
        int swizzle;
        if(swizzleAll)
            swizzle = ImageSwizzler.TWENTY_NINE_SWIZZLES;
        else if(swizzleInt.isPresent())
            swizzle = swizzleInt.getAsInt();
        else
            swizzle = ImageSwizzler.ALL_OLD_SWIZZLES;
        InputStream swizzled = new ImageSwizzler().swizzle(image, swizzle);
        String outputName = filename.replace("\\.[^.]+$","") + ".png";
        if(data.isSpoiler())
            result.add(MessageCreateFields.FileSpoiler.of(outputName, swizzled));
        else
            result.add(MessageCreateFields.File.of(outputName, swizzled));
    }
}
