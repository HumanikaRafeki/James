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
import humanika.rafeki.james.Utils;
import humanika.rafeki.james.utils.ImageSwizzler;
import humanika.rafeki.james.utils.SwizzleCollage;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import javax.imageio.ImageIO;
import reactor.core.publisher.Mono;

public class SwizzleImageSubcommand extends PrimitiveCommand {
    private static final String[] VAR_ARRAY = {"image1", "image2", "image3", "image4"};
    private static final List<String> VAR_LIST = Arrays.asList(VAR_ARRAY);

    @Override
    public String getName() {
        return "image";
    }

    @Override
    public String getFullName() {
        return "swizzle image";
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

        String swizzleString = data.getStringOrDefault("swizzles", "1-6");
        BitSet swizzleSet = null;
        try {
            swizzleSet = ImageSwizzler.bitSetForSwizzles(swizzleString);
        } catch(IllegalArgumentException iae) {
            return getChatEvent().reply(iae.toString()).withEphemeral(data.isEphemeral());
        }

        StringBuffer description = new StringBuffer();
        description.append(describe(swizzleString, swizzleSet, interaction));
        ArrayList<MessageCreateFields.File> result = new ArrayList<>();
        for(String var : VAR_LIST) {
            Optional<Attachment> maybeData = data.getAttachment(var);
            if(!maybeData.isPresent())
                continue;
            Attachment data = maybeData.get();
            String outputName = data.getFilename().replace("[.][^.]*\\z", ".png")
                .replaceAll("[^a-zA-Z0-9._ -]+"," ").replaceAll("\\s+", "_");
            description.append("**File** *`" + outputName + "`*\n");
            try {
                processOneFile(outputName, data, errors, result, swizzleSet, description);
            } catch(IOException ioe) {
                errors.append(data.getFilename() + ": unable to read file");
                description.append("Unable to read file!\n");
            }
        }

        if(result.size() < 1) {
            if(errors.length() < 1)
                // Should never happen, but just in case of logic errors:
                errors.append("Please attach one or more images.");
            return getChatEvent().reply(errors.toString()).withEphemeral(data.isEphemeral());
        } else
            return getChatEvent().reply().withFiles(result).withEphemeral(data.isEphemeral())
                .withContent(description.toString());
    }

    private String describe(String swizzleString, BitSet swizzleSet, Interaction interaction) {
        StringBuilder builder = new StringBuilder();
        Optional<Member> member = interaction.getMember();
        if(member.isPresent())
            builder.append(member.get().getMention()).append(" sent this image");
        else
            builder.append("You sent this image");
        builder.append(" and asked me to recolor it.\nSwizzles: ").append(swizzleString).append('\n');
        return builder.toString();
    }

    private void processOneFile(String outputName, Attachment data, StringBuffer errors,
                                ArrayList<MessageCreateFields.File> result, BitSet swizzleSet,
                                StringBuffer description) throws IOException {
        JamesConfig config = James.getConfig();
        String filename = data.getFilename();
        OptionalInt width = data.getWidth();
        OptionalInt height = data.getHeight();
        if(!width.isPresent() || !height.isPresent() || width.getAsInt() < 1 || height.getAsInt() < 1) {
            errors.append(filename + ": is not an image\n");
            description.append("Not an image!\n");
            return;
        }
        String urlString = data.getUrl();
        URL url = new URL(urlString);
        BufferedImage image = ImageIO.read(url);
        // ImageIO.write(image, "png", new File("/tmp/swizzles/0.png"));
        // for(int i = 0; i < 28; i++)
        //     ImageIO.write(new ImageSwizzler().swizzleToImage(image, OptionalInt.of(i)), "png", new File("/tmp/swizzles/"+(i+1)+".png"));
        ImageSwizzler swizzler = null;
        BufferedImage swizzledImage = null;
        SwizzleCollage collage = null;
        try {
            swizzler = ImageSwizzler.swizzleImage(image, swizzleSet, ImageSwizzler.LARGEST_IMAGE_BOUNDS);
            swizzledImage = swizzler.getSwizzledImage();
            collage = swizzler.getCollage();
        } catch(IllegalArgumentException iae) {
            errors.append(iae.toString()).append('\n');
            description.append(iae.toString()).append('\n');
            return;
        }
        description.append("```julia\n").append(collage.asTable()).append("```\n");
        InputStream swizzledStream = Utils.imageStream(swizzledImage);
        if(data.isSpoiler())
            result.add(MessageCreateFields.FileSpoiler.of(outputName, swizzledStream));
        else
            result.add(MessageCreateFields.File.of(outputName, swizzledStream));
    }
}
