package humanika.rafeki.james.commands;

import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateFields;
import humanika.rafeki.james.James;
import humanika.rafeki.james.data.JamesState;
import humanika.rafeki.james.data.NodeInfo;
import humanika.rafeki.james.data.SearchResult;
import humanika.rafeki.james.utils.AddParagraphFields;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.mcofficer.esparser.DataNode;
import reactor.core.publisher.Mono;

class ShowSubcommand extends PrimitiveCommand implements NodeInfoSubcommand {
    private static final int MAX_CHARS_PER_FIELD = 1000; // actual limit is 1024
    private static final int MAX_PRIVATE_LINES = 300;
    private static final int MAX_PUBLIC_LINES = 60;
    boolean showData;
    boolean showImages;

    @Override
    public String getName() {
        if(showData) {
            if(showImages)
                return "both";
            else
                return "data";
        } else
            return "image";
    }

    @Override
    public String getFullName() {
        if(showData) {
            if(showImages)
                return "show both";
            else
                return "show data";
        } else
            return "show image";
    }

    @Override
    public String getButtonDataName() {
        return getFullName();
    }

    ShowSubcommand showing(boolean data, boolean images) {
        try {
            ShowSubcommand cloned = (ShowSubcommand)clone();
            cloned.showData = data;
            cloned.showImages = images;
            return cloned;
        } catch(CloneNotSupportedException cnse) {
            // Should never happen
            return null;
        }
    }

    protected Mono<Void> generateResult(SearchResult found, boolean ephemeral) {
        List<EmbedCreateSpec> embeds = new ArrayList<>();
        ArrayList<MessageCreateFields.File> attachmentField = new ArrayList<>();
        InputStream attachmentStream = null;

        try {
            Optional<NodeInfo> maybeInfo = found.getNodeInfo();
            if(showData && maybeInfo.isPresent()) {
                NodeInfo info = maybeInfo.get();
                DataNode node = info.getDataNode();
                List<String> allLines = node.getLines();
                List<String> someLines = allLines;
                int maxLines = ephemeral ? MAX_PRIVATE_LINES : MAX_PUBLIC_LINES;
                if(allLines.size() > maxLines)
                    someLines = allLines.subList(0, maxLines);
                List<EmbedCreateFields.Field> fields = new ArrayList<>();
                AddParagraphFields fieldAdder = new AddParagraphFields(5, true);
                boolean finished = fieldAdder.add("", fields, someLines.stream(), MAX_CHARS_PER_FIELD);
                if(!finished || someLines != allLines) {
                    String attachmentContents = String.join("", someLines);
                    byte[] bytes = attachmentContents.getBytes(StandardCharsets.UTF_8);
                    attachmentStream = new ByteArrayInputStream(bytes);
                    StringBuilder builder = new StringBuilder(200);
                    builder.append(info.getType()).append('_').append(info.getDataName());
                    String filename = builder.toString().replaceAll("[^a-zA-Z0-9_-]+","_")+".txt";
                    attachmentField.add(MessageCreateFields.File.of(filename, attachmentStream));
                    builder.delete(0, builder.length());
                    builder.append("I don't want to type that much. I've attached the file \"")
                        .append(filename).append("\" with the full text. You may have to scroll up to find it.");
                    if(!ephemeral && allLines.size() < MAX_PRIVATE_LINES)
                        builder.append(" I'll print the whole thing");
                    else if(!ephemeral && MAX_PRIVATE_LINES > MAX_PUBLIC_LINES)
                        builder.append(" I'll print up to ").append(MAX_PRIVATE_LINES).append(" lines");
                    builder.append(" if you try again and set option `hidden` to `hide`");
                    fields.add(EmbedCreateFields.Field.of("Too much data!", builder.toString(), false));
                }
                embeds.add(EmbedCreateSpec.create().withFields(fields)
                           .withTitle(info.getBestType() + ' ' + info.getName()));
            }
            
            if(showImages) {
                if(!embedImages(found, embeds, true))
                    embeds.add(EmbedCreateSpec.create().withDescription("No images found.").withTitle("No Images"));
            }

            return getButtonEvent().getReply().flatMap(reply -> getButtonEvent().editReply().withEmbeds(embeds).withComponents().withFiles(attachmentField)).then();
        } catch(Exception e) {
            try {
                attachmentStream.close();
            } catch(IOException ioe) {}
            throw e;
        }
    }

    private boolean embedImages(SearchResult result, List<EmbedCreateSpec> embeds, boolean recurse) {
        Set<String> seen = new HashSet<>();
        Optional<NodeInfo> maybeInfo = result.getNodeInfo();
        JamesState state = James.getState();
        if(maybeInfo.isPresent())
            embedNodeImages(state, maybeInfo.get(), embeds, recurse, seen);
        for(Iterator<String> iter = result.getImageIterator(); iter.hasNext();) {
            String image = iter.next();
            if(seen.contains(image))
                continue;
            seen.add(image);
            state.getImageRawUrl(image).ifPresent(path ->
                embeds.add(EmbedCreateSpec.create().withImage(path).withTitle("Image")
                          .withDescription("```julia\n\"" + image + "\"\n```")));
        }
        return seen.size() > 0;
    }

    private void embedNodeImages(JamesState state, NodeInfo info, List<EmbedCreateSpec> embeds, boolean recurse, Set<String> seen) {
        String sprite = info.getSprite().orElse(null);
        String thumbnail = info.getThumbnail().orElse(null);
        String weaponSprite = info.getWeaponSprite().orElse(null);
        if(sprite != null) {
            seen.add(sprite);
            state.getImageRawUrl(sprite).ifPresent(path ->
                embeds.add(EmbedCreateSpec.create().withImage(path).withTitle("Sprite")
                           .withDescription("```julia\nsprite \"" + sprite + "\"\n```")));
        }
        if(thumbnail != null && thumbnail != sprite) {
            seen.add(thumbnail);
            state.getImageRawUrl(thumbnail).ifPresent(path ->
                embeds.add(EmbedCreateSpec.create().withImage(path).withTitle("Thumbnail")
                           .withDescription("```julia\nthumbnail \"" + thumbnail + "\"\n```")));
        }
        if(weaponSprite != null && weaponSprite != thumbnail && weaponSprite != sprite) {
            seen.add(weaponSprite);
            state.getImageRawUrl(weaponSprite).ifPresent(path ->
                embeds.add(EmbedCreateSpec.create().withImage(path).withTitle("Weapon Sprite")
                           .withDescription("```julia\nweapon\n\tsprite \"" + weaponSprite + "\"\n```")));
        }
    }
}
