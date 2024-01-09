package humanika.rafeki.james.commands;

import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateFields;
import humanika.rafeki.james.James;
import humanika.rafeki.james.Utils;
import humanika.rafeki.james.data.JamesConfig;
import humanika.rafeki.james.data.JamesState;
import humanika.rafeki.james.data.NodeInfo;
import humanika.rafeki.james.utils.ImageSwizzler;
import humanika.rafeki.james.utils.SwizzleCollage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import javax.imageio.ImageIO;
import reactor.core.publisher.Mono;

public class SwizzleSearchSubcommand extends NodeInfoCommand implements NodeInfoSubcommand {
    private final static Base64.Encoder encoder = Base64.getEncoder();
    private final static Base64.Decoder decoder = Base64.getDecoder();

    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String getFullName() {
        return "swizzle search";
    }

    private String swizzleString = null;
    private BitSet swizzleSet = null;
    private String swizzleHash = null;
    private String buttonDataName = null;
    private OptionalInt showTable = OptionalInt.empty();

    @Override
    public String getButtonDataName() {
        return buttonDataName;
    }

    @Override
    public Optional<InteractionEventHandler> findSubcommand() {
        return Optional.of(this);
    }

    @Override
    public void describeSearch(StringBuilder builder, Optional<String> maybeType, String query) {
        super.describeSearch(builder, maybeType, query);
        builder.append("\nSwizzle List: `").append(ImageSwizzler.describeSwizzleSet(swizzleSet)).append('`');
        if(!showTable.isPresent())
            builder.append("\nUse \"table: show\" to see which swizzle is where in each image.");
    }

    @Override
    public void getButtonFlags(StringBuilder builder) {
        super.getButtonFlags(builder);
        if(showTable.isPresent() && showTable.getAsInt() != 0) {
            System.out.println("TABLE PRESENT AND TRUE append T");
            builder.append('T');
        } else
            System.out.println("NO T NO TABLE " + showTable.isPresent());
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
        Optional<String> maybeShowTable = data.getString("table");
        if(maybeShowTable.isPresent())
            showTable = OptionalInt.of(maybeShowTable.get().equals("show") ? 1 : 0);
        swizzleString = data.getStringOrDefault("swizzles", "1-6");
        swizzleSet = null;
        try {
            swizzleSet = ImageSwizzler.bitSetForSwizzles(swizzleString);
        } catch(IllegalArgumentException iae) {
            return getChatEvent().reply(iae.toString()).withEphemeral(data.isEphemeral());            
        }
        swizzleHash = encoder.encodeToString(swizzleSet.toByteArray());
        buttonDataName = getFullName() + " " + swizzleHash;

        return super.handleChatCommand();
    }

    @Override
    protected Optional<List<NodeInfo>> getMatches(String query, Optional<String> maybeType) {
        if(maybeType.isPresent()) {
            final String type = maybeType.get();
            if(type.equals("variant"))
                return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT, info -> info.isShipVariant() && info.hasImage());
            else
                return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT, info -> info.getType().equals(type) && !info.isShipVariant() && info.hasImage());
        } else
            return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT, info -> !info.isShipVariant()  && info.hasImage());
    }

    @Override
    protected Mono<Void> generateResult(List<NodeInfo> found, boolean ephemeral, PrimitiveCommand subcommand) {
        if(found.size() < 1)
            return Mono.empty();

        String[] split = getButtonEvent().getCustomId().split(":", 4);
        boolean doTable = split[1].contains("T");
        split = split[0].split(" ");
        String swizzleHash = split[2];
        byte[] swizzleBytes = decoder.decode(swizzleHash);
        BitSet swizzleSet = BitSet.valueOf(swizzleBytes);
        JamesState state = James.getState();
        NodeInfo info = found.get(found.size() - 1);
        ArrayList<MessageCreateFields.File> attachments = new ArrayList<>();
        StringBuffer description = new StringBuffer();

        String sprite = info.getSprite().orElse(null);
        String thumbnail = info.getThumbnail().orElse(null);
        String weaponSprite = info.getWeaponSprite().orElse(null);

        Path spritePath = sprite == null ? null : state.getImagePath(sprite).orElse(null);
        Path thumbnailPath = thumbnail == null ? null : state.getImagePath(thumbnail).orElse(null);
        Path weaponSpritePath = weaponSprite == null ? null : state.getImagePath(weaponSprite).orElse(null);

        EmbedCreateSpec embed = EmbedCreateSpec.create();

        if(spritePath == null && thumbnailPath == null && weaponSpritePath == null)
            embed = embed.withTitle("No Images").withDescription("No images found!");
        else {
            embed = embed.withTitle("Search Results").withDescription("Swizzled as you asked")
                .withFooter(EmbedCreateFields.Footer.of(getCommentary(), null));
            ArrayList<EmbedCreateFields.Field> fields = new ArrayList<>();

            if(spritePath != null)
                processOneFile("```julia\nsprite `" + sprite + "`\n",
                               spritePath, fields, swizzleSet, attachments);
            if(thumbnailPath != null && thumbnailPath != spritePath)
                processOneFile("```julia\nthumbnail `" + thumbnail + "`\n",
                               thumbnailPath, fields, swizzleSet, attachments);
            if(weaponSpritePath != null && weaponSpritePath != spritePath && weaponSpritePath != thumbnailPath)
                processOneFile("```julia\nweapon\n\tsprite `" + thumbnail + "`\n",
                               weaponSpritePath, fields, swizzleSet, attachments);

            embed = embed.withFields(fields);
        }

        final EmbedCreateSpec finalEmbed = embed;

        if(doTable)
            return getButtonEvent()
                .getReply()
                .flatMap(reply ->
                         getButtonEvent()
                         .editReply()
                         .withComponents()
                         .withEmbeds(finalEmbed)
                         .withFiles(attachments)
                         ).then();
        else
            return getButtonEvent()
                .getReply()
                .flatMap(reply ->
                         getButtonEvent()
                         .editReply()
                         .withComponents()
                         .withFiles(attachments)
                         ).then();
    }

    private String getCommentary() {
        String babble = James.getState().jamesPhrase("JAMES::ping");
        return babble!=null ? babble : "*no commentary*";
    }

    private void processOneFile(String heading, Path path, ArrayList<EmbedCreateFields.Field> fields,
                                BitSet swizzleSet, ArrayList<MessageCreateFields.File> attachments) {
        StringBuffer description = new StringBuffer(100);
        description.append(heading);
        JamesConfig config = James.getConfig();
        BufferedImage image;
        try {
            image = ImageIO.read(path.toFile());
        } catch(IOException ioe) {
            description.append("```\n").append(ioe.toString()).append('\n');
            return;
        }
        ImageSwizzler swizzler = null;
        BufferedImage swizzledImage = null;
        SwizzleCollage collage = null;
        try {
            swizzler = ImageSwizzler.swizzleImage(image, swizzleSet, ImageSwizzler.LARGEST_IMAGE_BOUNDS);
            swizzledImage = swizzler.getSwizzledImage();
            collage = swizzler.getCollage();
        } catch(IllegalArgumentException iae) {
            description.append("```\n").append(iae.toString()).append('\n');
            return;
        } catch(IOException ioe2) {
            description.append("```\n").append(ioe2.toString()).append('\n');
            return;
        }
        description.append('\n').append(collage.asTable()).append("```\n\n");
        InputStream swizzledStream = null;
        try {
            swizzledStream = Utils.imageStream(swizzledImage);
        } catch(IOException ioe3) {
            description.append(ioe3.toString()).append('\n');
            return;
        }
        String outputName = path.getName(path.getNameCount() - 1).toString();
        attachments.add(MessageCreateFields.File.of(outputName, swizzledStream));
        fields.add(EmbedCreateFields.Field.of(outputName, description.toString(), true));
    }
}
