package humanika.rafeki.james.data;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class ImageSearchResult extends SearchResult {
    /** A string to use for fuzzy matching */
    private String searchString;

    /** Relative path of the image */
    private String image;

    /** A unique hash of the relative path */
    private String hashString;

    ImageSearchResult(float score, String searchString, String hashString, String image) {
        super(score);
        this.searchString = searchString;
        this.image = image;
        this.hashString = hashString;
    }

    @Override
    public String getHashString() {
        return hashString;
    }

    @Override
    public String getSearchString() {
        return searchString;
    }

    @Override
    public String getType() {
        return "image";
    }

    @Override
    public String getBestType() {
        return "image";
    }

    @Override
    public String getName() {
        return image;
    }

    @Override
    public boolean isShipVariant() {
        return false;
    }

    @Override
    public Optional<NodeInfo> getNodeInfo() {
        return Optional.empty();
    }

    @Override
    public Iterator<String> getImageIterator() {
        return new SingleStringIterator(image);
    }
}
