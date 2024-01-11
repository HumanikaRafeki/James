package humanika.rafeki.james.data;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import me.mcofficer.esparser.DataNode;

public interface NodeDatabase {
    public Optional<List<NodeInfo>> selectNodesByName(String dataName, int maxSearch, Predicate<NodeInfo> condition);
    public Optional<NodeInfo> getFirstMatch(String dataName, Predicate<NodeInfo> condition);
    public List<SearchResult> fuzzyMatchNodeNames(String query, int maxSearch, Predicate<NodeInfo> condition);
}
