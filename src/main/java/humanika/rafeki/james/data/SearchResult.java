package humanika.rafeki.james.data;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public abstract class SearchResult {
    private float score;

    protected SearchResult(float score) {
        this.score = score;
    }

    public final float getScore() {
        return score;
    }

    public static int lessThan(SearchResult lhs, SearchResult rhs) {
        return lhs.score > rhs.score ? -1 : lhs.score < rhs.score ? 1 : 0;
    }

    public static int greaterThan(SearchResult lhs, SearchResult rhs) {
        return lhs.score > rhs.score ? 1 : lhs.score < rhs.score ? -1 : 0;
    }

    public abstract String getHashString();
    public abstract String getSearchString();
    public abstract String getType();
    public abstract String getBestType();
    public abstract String getName();
    public abstract boolean isShipVariant();
    public abstract Optional<NodeInfo> getNodeInfo();
    public abstract Iterator<String> getImageIterator();

    public static SearchResult of(float score, NodeInfo info) throws IllegalArgumentException {
        return new NodeInfoSearchResult(score, info);
    }

    public static SearchResult of(float score, String searchString, String hashString, String image) throws IllegalArgumentException {
        return new ImageSearchResult(score, searchString, hashString, image);
    }
}
