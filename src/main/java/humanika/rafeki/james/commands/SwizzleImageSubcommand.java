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
import humanika.rafeki.james.utils.ImageSwizzler;
import humanika.rafeki.james.utils.SwizzleCollage;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
        Optional<String> maybeTable = data.getString("table");
        boolean doTable = maybeTable.isPresent() && maybeTable.get().equals("show");
        BitSet swizzleSet = null;
        try {
            swizzleSet = ImageSwizzler.bitSetForSwizzles(swizzleString);
        } catch(IllegalArgumentException iae) {
            return getChatEvent().reply(iae.toString()).withEphemeral(data.isEphemeral());
        }

        StringBuffer description = new StringBuffer();
        ArrayList<Attachment> attachments = new ArrayList<>();;
        for(String var : VAR_LIST) {
            Optional<Attachment> maybeData = data.getAttachment(var);
            if(!maybeData.isPresent())
                continue;
            attachments.add(maybeData.get());
        }

        description.append(describe(maybeTable, swizzleSet, interaction, attachments.size()));

        ArrayList<MessageCreateFields.File> result = new ArrayList<>();
        ArrayList<EmbedCreateFields.Field> fields = new ArrayList<>();
        for(Attachment data : attachments) {
            String outputName = data.getFilename().replace("[.][^.]*\\z", ".png")
                .replaceAll("[^a-zA-Z0-9._ -]+"," ").replaceAll("\\s+", "_");
            try {
                processOneFile(outputName, data, errors, result, swizzleSet, fields);
            } catch(IOException ioe) {
                errors.append(data.getFilename() + ": unable to read file");
                description.append("Unable to read file!\n");
            } catch(URISyntaxException urise) {
                errors.append(data.getFilename() + ": invalid uri: " + urise.getMessage());
                description.append("Bad URL from Discord: " + urise.getMessage() + "\n");
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
            ArrayList<EmbedCreateSpec> embeds = new ArrayList<>();
            if(doTable)
                embeds.add(EmbedCreateSpec.create().withTitle("Swizzled Images").withFields(fields)
                           .withFooter(EmbedCreateFields.Footer.of(getCommentary(), null)));
            return getChatEvent().reply().withFiles(result).withEphemeral(data.isEphemeral())
                .withContent(description.toString()).withEmbeds(embeds);
        }
    }

    private String getCommentary() {
        String babble = James.getState().jamesPhrase("JAMES::ping");
        return babble!=null ? babble : "*no commentary*";
    }

    private String describe(Optional<String> maybeTable, BitSet swizzleSet, Interaction interaction, int imageCount) {
        StringBuilder builder = new StringBuilder();
        Optional<Member> member = interaction.getMember();
        if(member.isPresent())
            builder.append(member.get().getMention()).append(" sent ");
        else
            builder.append("You sent" );
        if(imageCount == 1)
            builder.append("this image");
        else
            builder.append("these images");
        builder.append(" and asked me to recolor with:\n*Swizzle List* `")
            .append(ImageSwizzler.describeSwizzleSet(swizzleSet)).append("`\n");
        if(!maybeTable.isPresent())
            builder.append("Use \"table: show\" to see which swizzle is where in each image.\n");
        return builder.toString();
    }

    private void processOneFile(String outputName, Attachment data, StringBuffer errors,
                                ArrayList<MessageCreateFields.File> result, BitSet swizzleSet,
                                ArrayList<EmbedCreateFields.Field> fields) throws IOException, URISyntaxException {
        JamesConfig config = James.getConfig();
        String filename = data.getFilename().replaceAll("`", "'");
        OptionalInt width = data.getWidth();
        OptionalInt height = data.getHeight();
        if(!width.isPresent() || !height.isPresent() || width.getAsInt() < 1 || height.getAsInt() < 1) {
            errors.append('`').append(filename).append("`: is not an image\n");
            return;
        }
        String urlString = data.getUrl();
        URL url = new URI(urlString).toURL();
        BufferedImage image = ImageIO.read(url);
        // for(int i = 0; i <= 28; i++) {
        //     BitSet set = new BitSet();
        //     set.set(i);
        //     ImageIO.write(ImageSwizzler.swizzleImage(image, set, ImageSwizzler.NICE_IMAGE_BOUNDS).getSwizzledImage(),
        //                   "png", new File("/tmp/swazzles/"+i+".png"));
        // }
        ImageSwizzler swizzler = null;
        BufferedImage swizzledImage = null;
        SwizzleCollage collage = null;
        try {
            swizzler = ImageSwizzler.swizzleImage(image, swizzleSet, ImageSwizzler.LARGEST_IMAGE_BOUNDS);
            swizzledImage = swizzler.getSwizzledImage();
            collage = swizzler.getCollage();
        } catch(IllegalArgumentException iae) {
            errors.append('`').append(filename).append('`').append(iae.getMessage()).append('\n');
            return;
        }
        StringBuffer description = new StringBuffer(100);
        description.append("```julia\n").append(collage.asTable()).append("```\n");
        InputStream swizzledStream = Utils.imageStream(swizzledImage);
        if(data.isSpoiler())
            result.add(MessageCreateFields.FileSpoiler.of(outputName, swizzledStream));
        else
            result.add(MessageCreateFields.File.of(outputName, swizzledStream));
        fields.add(EmbedCreateFields.Field.of(outputName, description.toString(), true));
    }
}
