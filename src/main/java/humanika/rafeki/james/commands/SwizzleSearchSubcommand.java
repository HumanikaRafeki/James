package humanika.rafeki.james.commands;

import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateFields;
import humanika.rafeki.james.James;
import humanika.rafeki.james.Utils;
import humanika.rafeki.james.data.JamesConfig;
import humanika.rafeki.james.data.JamesState;
import humanika.rafeki.james.data.NodeInfo;
import humanika.rafeki.james.data.SearchResult;
import humanika.rafeki.james.utils.ImageSwizzler;
import humanika.rafeki.james.utils.SwizzleCollage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
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
            builder.append("\nUse \"table: hide\" to hide the search result table.");
    }

    @Override
    public void getButtonFlags(StringBuilder builder) {
        super.getButtonFlags(builder);
        if(!showTable.isPresent() || showTable.getAsInt() != 0)
            builder.append('T');
    }

    @Override
    public Mono<Void> handleChatCommand() {
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
    protected List<SearchResult> getMatches(String query, Optional<String> maybeType) {
        if(maybeType.isPresent()) {
            final String type = maybeType.get();
            if(type.equals("image"))
                return James.getState().fuzzyMatchImagePaths(query, QUERY_COUNT, name -> true);
            else if(type.equals("variant"))
                return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT, info -> info.isShipVariant() && info.hasImage());
            else
                return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT, info -> info.getType().equals(type) && info.hasImage());
        } else
            return James.getState().fuzzyMatchNodesAndImages(query, QUERY_COUNT, info -> info.hasImage(), name -> true);
    }

    @Override
    protected Mono<Void> generateResult(SearchResult found, boolean ephemeral, PrimitiveCommand subcommand) {
        String[] split = getButtonEvent().getCustomId().split(":", 4);
        final boolean doTable = split[1].contains("T");
        split = split[0].split(" ");
        final String swizzleHash = split[2];
        final byte[] swizzleBytes = decoder.decode(swizzleHash);
        final BitSet swizzleSet = BitSet.valueOf(swizzleBytes);
        final JamesState state = James.getState();
        final ArrayList<MessageCreateFields.File> attachments = new ArrayList<>();
        final StringBuffer description = new StringBuffer();
        final ArrayList<EmbedCreateFields.Field> fields = new ArrayList<>();
        final Optional<NodeInfo> maybeInfo = found.getNodeInfo();
        final Set<String> seen = new HashSet<>();

        if(maybeInfo.isPresent())
            generateNodeInfoResult(maybeInfo.get(), state, fields, swizzleSet, attachments, seen);

        for(Iterator<String> iter = found.getImageIterator(); iter.hasNext();) {
            final String image = iter.next();
            if(seen.contains(image))
                continue;
            seen.add(image);
            final Path path = state.getImagePath(image).orElse(null);
            processOneFile("```julia\n\"" + image + "\"\n", path, fields, swizzleSet, attachments);
        }

        EmbedCreateSpec embed = EmbedCreateSpec.create();

        if(seen.size() == 0)
            embed = embed.withTitle("No Images").withDescription("No images found!");
        else
            embed = embed.withTitle("Search Results").withDescription("Swizzled as you asked")
                .withFooter(EmbedCreateFields.Footer.of(getCommentary(), null)).withFields(fields);

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

    private void generateNodeInfoResult(NodeInfo info, JamesState state, List<EmbedCreateFields.Field> fields, BitSet swizzleSet, List<MessageCreateFields.File> attachments, Set<String> seen) {
        String sprite = info.getSprite().orElse(null);
        String thumbnail = info.getThumbnail().orElse(null);
        String weaponSprite = info.getWeaponSprite().orElse(null);

        Path spritePath = sprite == null ? null : state.getImagePath(sprite).orElse(null);
        Path thumbnailPath = thumbnail == null ? null : state.getImagePath(thumbnail).orElse(null);
        Path weaponSpritePath = weaponSprite == null ? null : state.getImagePath(weaponSprite).orElse(null);

        if(spritePath != null) {
            seen.add(sprite);
            processOneFile("```julia\nsprite `" + sprite + "`\n",
                           spritePath, fields, swizzleSet, attachments);
        }
        if(thumbnailPath != null && thumbnailPath != spritePath) {
            seen.add(thumbnail);
            processOneFile("```julia\nthumbnail `" + thumbnail + "`\n",
                           thumbnailPath, fields, swizzleSet, attachments);
        }
        if(weaponSpritePath != null && weaponSpritePath != spritePath && weaponSpritePath != thumbnailPath) {
            seen.add(weaponSprite);
            processOneFile("```julia\nweapon\n\tsprite `" + weaponSprite + "`\n",
                           weaponSpritePath, fields, swizzleSet, attachments);
        }
    }

    private String getCommentary() {
        String babble = James.getState().jamesPhrase("JAMES::ping");
        return babble!=null ? babble : "*no commentary*";
    }

    private void processOneFile(String heading, Path path, List<EmbedCreateFields.Field> fields,
                                BitSet swizzleSet, List<MessageCreateFields.File> attachments) {
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
