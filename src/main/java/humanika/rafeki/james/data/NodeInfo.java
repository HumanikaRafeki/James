package humanika.rafeki.james.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.mcofficer.esparser.DataNode;
import org.simmetrics.StringMetric;

public class NodeInfo {
    private final static Set<String> skipTypes;
    private final static MessageDigest hasher;
    private final static Base64.Encoder encoder;

    static {
        skipTypes = new HashSet<>();
        skipTypes.add("phrase");
        skipTypes.add("news");
        skipTypes.add("star");
        skipTypes.add("conversation");
        skipTypes.add("event");
        skipTypes.add("fleet");
        skipTypes.add("landing message");
        hasher = makeHasher();
        encoder = Base64.getEncoder();
    }

    private static final MessageDigest makeHasher() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException nsae) {
            // Should never happen because SHA-256 is required by Java standard.
            return null;
        }
    }

    private Optional<String> sprite;
    private Optional<String> weaponSprite;
    private Optional<String> thumbnail;
    private final Optional<List<DataNode>> description;
    private final Optional<List<DataNode>> spaceport;
    private final List<String> images = new ArrayList<>();

    /** For Ship variants, "base" is the name of the template ship and "dataName" is the variant name */
    private final Optional<String> base;
    private final Optional<String> displayName;
    private final Optional<String> effectRefType;
    private final Optional<String> effectRefDataName;

    /** True iff this is a ship variant (ship "base name" "variant name") */
    private final boolean shipVariantFlag;
    private final String type;
    private final Optional<String> subtype;
    private final String searchString;

    /** This is node.token(1) except Ship variants, for  which it is token(2) */
    private final String dataName;

    private final String name;
    private final String base64Hash;
    private final DataNode node;

    private Optional<NodeInfo> baseNode;
    private Optional<NodeInfo> effectNode;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String className = this.getClass().getCanonicalName();
        if(className == null)
            className = "NodeInfo";
        builder.append(className).append('[')
            .append("type=").append(type).append("; dataName=`").append(dataName)
            .append("`; name=`").append(name).append('`');
        if(subtype.isPresent())
            builder.append("; subtype=`").append(subtype.get()).append('`');
        if(displayName.isPresent())
            builder.append("; displayName=`").append(displayName.get()).append('`');
        if(base.isPresent())
            builder.append("; base=`").append(base.get()).append('`');
        if(shipVariantFlag)
            builder.append("; shipVariant=true");
        if(sprite.isPresent())
            builder.append("; sprite=`").append(sprite.get()).append('`');
        if(weaponSprite.isPresent())
            builder.append("; weaponSprite=`").append(weaponSprite.get()).append('`');
        if(thumbnail.isPresent())
            builder.append("; thumbnail=`").append(thumbnail.get()).append('`');
        if(description.isPresent())
            builder.append("; description=[").append(description.get().size()).append(" strings]");
        if(spaceport.isPresent())
            builder.append("; spaceport=[").append(spaceport.get().size()).append(" strings]");
        builder.append("; hash=").append(base64Hash);
        builder.append("; query=`").append(searchString).append('`');
        return builder.append(']').toString();
    }

    NodeInfo(DataNode node) {
        this.node = node;

        String type = node.token(0);

        String name = null;
        String base = null;
        String displayName = null;
        String subtype = null;
        String dataName = null;
        ArrayList<DataNode> description = null;
        ArrayList<DataNode> spaceport = null;

        String sprite = null;
        String thumbnail = null;
        String effectRefType = null;
        String effectRefDataName = null;
        String weaponSprite = null;

        if(type.equals("ship") && node.size() > 2) {
            base = node.token(1);
            dataName = node.token(2);
            subtype = "variant";
        } else
            dataName = node.token(1);

        name = dataName;

        if(type.equals("tip"))
            description = node.getChildren();

        if(type.equals("star"))
            sprite = dataName;

        if(type.equals("mission"))
            subtype = "spaceport";

        // For types that may have more information, check child nodes.
        if(!skipTypes.contains(type)) {
            for(DataNode child : node.getChildren()) {
                if(child.size() == 1) {
                    switch(child.token(0)) {
                    case "assisting":
                    case "boarding":
                    case "outfitter":
                    case "job":
                    case "shipyard":
                    case "landing":
                        if(type.equals("mission"))
                            subtype = child.token(0);
                        break;
                    case "weapon":
                        for(DataNode grand : child.getChildren())
                            if(grand.size() >= 2 && grand.token(0).equals("sprite"))
                                weaponSprite = grand.token(1);
                        break;
                    }
                } else if(child.size() >= 2) {
                    switch(child.token(0)) {
                    case "environmental effect":
                        effectRefType = "effect";
                        effectRefDataName = child.token(1);
                        break;
                    case "description":
                        if(description == null)
                            description = new ArrayList<>();
                        description.add(child);
                        break;
                    case "spaceport":
                        if(spaceport == null)
                            spaceport = new ArrayList<>();
                        spaceport.add(child);
                        break;
                    case "display name":
                        displayName = child.token(1);
                        break;
                    case "name":
                        name = child.token(1);
                        break;
                    case "landscape":
                    case "sprite":
                        sprite = child.token(1);
                        break;
                    case "thumbnail":
                        thumbnail = child.token(1);
                        break;
                    }
                }
            }
        }

        if(sprite != null)
            images.add(sprite);
        if(weaponSprite != null && sprite != weaponSprite)
            images.add(weaponSprite);
        if(thumbnail != null && thumbnail != sprite && thumbnail != weaponSprite)
            images.add(thumbnail);
        this.sprite = Optional.ofNullable(sprite);
        this.weaponSprite = Optional.ofNullable(weaponSprite);
        this.thumbnail = Optional.ofNullable(thumbnail);
        this.description = Optional.ofNullable(description);
        this.spaceport = Optional.ofNullable(spaceport);
        this.base = Optional.ofNullable(base);
        this.displayName = Optional.ofNullable(displayName);
        this.effectRefType = Optional.ofNullable(effectRefType);
        this.effectRefDataName = Optional.ofNullable(effectRefDataName);
        this.type = type;
        this.dataName = dataName;
        this.name = name;
        this.subtype = Optional.ofNullable(subtype);
        this.shipVariantFlag = type.equals("ship") && base != null;

        StringBuilder builder = new StringBuilder();
        if(this.shipVariantFlag)
            builder.append(base).append(' ').append(dataName);
        else
            builder.append(dataName);
        this.searchString = builder.toString().toLowerCase().replaceAll("[^0-9a-zA-Z-]+", " ").strip();

        builder.delete(0, builder.length());
        builder.append("type=").append(type).append(";subtype=").append(subtype).append(";dataName=").append(dataName).append(";base=").append(base);
        this.base64Hash = generateBase64Hash(builder.toString());

        baseNode = Optional.empty();
        effectNode = Optional.empty();        
    }

    void postLoad(NodeDatabase nodes) {
        if(effectRefType.isPresent() && effectRefDataName.isPresent()) {
            String type = effectRefType.get();
            Optional<NodeInfo> ref = nodes.getFirstMatch(effectRefDataName.get(), info -> info.getType().equals(type));
            ref.ifPresent(node -> {
                effectNode = Optional.of(node);
                node.getImageIterator().forEachRemaining(image -> {
                    if(image != null && !this.images.contains(image))
                        this.images.add(image);
                });
            });
        }

        base.ifPresent(baseName -> {
            baseNode = nodes.getFirstMatch(baseName, info -> info.getType().equals("ship"));
            if(baseNode.isPresent()) {
                NodeInfo bn = baseNode.get();
                if(!sprite.isPresent())
                    sprite = bn.getSprite();
                if(!thumbnail.isPresent())
                    thumbnail = bn.getThumbnail();
            }
        });
    }

    private static /* synchronized */ String generateBase64Hash(String content) {
        return encoder.encodeToString(hasher.digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    public DataNode getDataNode() {
        return node;
    }

    public boolean isShipVariant() {
        return shipVariantFlag;
    }

    public String getHashString() {
        return base64Hash;
    }

    public String getSearchString() {
        return searchString;
    }

    public Optional<String> getSprite() {
        return sprite;
    }

    public Optional<String> getThumbnail() {
        return thumbnail;
    }

    public Optional<String> getWeaponSprite() {
        return weaponSprite;
    }

    public Optional<String> getBestImage() {
        if(sprite.isPresent())
            return sprite;
        else if(thumbnail.isPresent())
            return thumbnail;
        else if(weaponSprite.isPresent())
            return weaponSprite;
        else if(images.size() > 0)
            return Optional.of(images.get(0));
        return Optional.empty();
    }

    public Optional<String> getBestThumbnail() {
        if(thumbnail.isPresent())
            return thumbnail;
        else if(sprite.isPresent())
            return sprite;
        else if(weaponSprite.isPresent())
            return weaponSprite;
        return Optional.empty();
    }

    public String getBestType() {
        if(type.equals("ship") && base.isPresent())
            return "variant";
        else
            return type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDataName() {
        return dataName;
    }

    public String getDisplayName() {
        return displayName.isPresent() ? displayName.get() : name;
    }

    public boolean hasDescription() {
        return description.isPresent() || baseNode.isPresent() && baseNode.get().description.isPresent();
    }

    public boolean hasSpaceport() {
        return spaceport.isPresent();
    }

    public boolean hasImage() {
        return images.size() > 0;
    }

    public Optional<List<DataNode>> getDescription() {
        if(description.isPresent())
            return description;
        else if(baseNode.isPresent())
            return baseNode.get().description;
        return Optional.empty();
    }

    public Optional<List<DataNode>> getSpaceport() {
        return spaceport;
    }

    public Iterator<String> getImageIterator() {
        return images.iterator();
    }
}
