package humanika.rafeki.james.data;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class NodeInfoSearchResult extends SearchResult {
    private NodeInfo info;

    NodeInfoSearchResult(float score, NodeInfo info) {
        super(score);
        this.info = info;
    }

    @Override
    public String getHashString() {
        return info.getHashString();
    }

    @Override
    public String getSearchString() {
        return info.getSearchString();
    }

    @Override
    public String getType() {
        return info.getType();
    }

    @Override
    public String getBestType() {
        return info.getBestType();
    }

    @Override
    public String getName() {
        return info.getDataName();
    }

    @Override
    public boolean isShipVariant() {
        return info.isShipVariant();
    }

    @Override
    public Optional<NodeInfo> getNodeInfo() {
        return Optional.of(info);
    }

    @Override
    public Iterator<String> getImageIterator() {
        return info.getImageIterator();
    }
}
