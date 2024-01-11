package humanika.rafeki.james.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import me.mcofficer.esparser.DataFile;
import me.mcofficer.esparser.DataNode;
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;

public class NodeLookups implements NodeDatabase {
    private Map<String, DataFile> dataFiles = new HashMap<>();
    private Map<String, ArrayList<NodeInfo>> nameNode = new HashMap<>();
    private Set<DataNode> allTopLevelNodes = new HashSet<>();
    private Set<NodeInfo> allTopNodeInfo = new HashSet<>();
    private Map<String, ArrayList<NodeInfo>> hashNode = new HashMap<>();
    private Map<Integer, ArrayList<Government>> swizzleGovernment = new HashMap<>();
    private final static StringMetric metric = StringMetrics.needlemanWunch();

    /* synchronized */ void addFile(String relativePath, DataFile file) {
        dataFiles.put(relativePath, file);
        for(DataNode node : file.getNodes()) {
            if(allTopLevelNodes.contains(node))
                continue;
            allTopLevelNodes.add(node);
            if(node.size() < 2)
                continue;
            NodeInfo info = new NodeInfo(node);
            allTopNodeInfo.add(info);
            String type = info.getType();
            String name = info.getDataName();
            addToNameNode(name, info);
            addToHashNode(info.getHashString(), info);
            if(type.equals("government"))
                addToSwizzleGovernment(node);
        }
    }

    public void postLoad() {
        for(NodeInfo node : allTopNodeInfo)
            node.postLoad(this);
    }

    public Optional<List<NodeInfo>> selectNodesByName(String dataName, int maxSearch, Predicate<NodeInfo> condition) {
        ArrayList<NodeInfo> infos = nameNode.get(dataName);
        if(infos == null || infos.size() < 1)
            return Optional.empty();
        ArrayList<NodeInfo> results = new ArrayList<>(infos.size());
        for(NodeInfo info : infos) {
            if(condition.test(info))
                results.add(info);
            if(results.size() >= maxSearch)
                return Optional.empty();
        }
        return Optional.of(results);
    }

    public Optional<NodeInfo> getFirstMatch(String dataName, Predicate<NodeInfo> condition) {
        ArrayList<NodeInfo> infos = nameNode.get(dataName);
        for(NodeInfo info : infos)
            if(condition.test(info))
                return Optional.of(info);
        return Optional.empty();
    }

    public List<SearchResult> fuzzyMatchNodeNames(String query, int maxSearch, Predicate<NodeInfo> condition) {
        int threshold = maxSearch > 0 ? 3 * maxSearch : 0;
        ShrinkableArrayList<SearchResult> work = new ShrinkableArrayList<>();
        for(Map.Entry<String, ArrayList<NodeInfo>> entry : nameNode.entrySet()) {
            ArrayList<NodeInfo> list = entry.getValue();
            if(list.size() > 0) {
                NodeInfo info = list.get(list.size() - 1);
                if(condition.test(info)) {
                    float score = metric.compare(query, info.getSearchString());
                    work.add(SearchResult.of(score, info));
                    if(threshold > 0 && work.size() >= threshold) {
                        work.sort(SearchResult::lessThan);
                        work.shrink(maxSearch);
                    }
                }
            }
        }
        if(work.size() < 1)
            return Collections.emptyList();
        if(work.size() > 1)
            work.sort(SearchResult::lessThan);
        if(maxSearch > 0 && work.size() > maxSearch)
            work.shrink(maxSearch);
        return Collections.unmodifiableList(work);
    }

    public Optional<List<NodeInfo>> nodesWithHash(String hash) {
        ArrayList<NodeInfo> got = hashNode.get(hash);
        if(got == null || got.size() < 1)
            return Optional.empty();
        return Optional.of(got);
    }

    public Optional<SearchResult> dummyResultWithHash(String hash) {
        ArrayList<NodeInfo> got = hashNode.get(hash);
        if(got == null || got.size() < 1)
            return Optional.empty();
        return Optional.of(SearchResult.of(0, got.get(got.size() - 1)));
    }

    public Optional<List<Government>> governmentsWithSwizzle(int swizzle) {
        List<Government> got = swizzleGovernment.get(Integer.valueOf(swizzle));
        return got != null ? Optional.of(got) : Optional.empty();
    }

    private void addToNameNode(String name, NodeInfo info) {
        ArrayList<NodeInfo> got = nameNode.get(name);
        if(got == null) {
            got = new ArrayList<>();
            nameNode.put(name, got);
        }
        got.add(info);
    }

    private void addToHashNode(String hash, NodeInfo info) {
        ArrayList<NodeInfo> got = hashNode.get(hash);
        if(got == null) {
            got = new ArrayList<>();
            hashNode.put(hash, got);
        }
        got.add(info);
    }

    private void addToSwizzleGovernment(DataNode node) {
        Government gov = new Government(node);
        Integer swizzle = Integer.valueOf(gov.swizzle);
        ArrayList<Government> governments = swizzleGovernment.get(swizzle);
        if(governments == null) {
            governments = new ArrayList<>();
            swizzleGovernment.put(swizzle, governments);
        }
        governments.add(gov);
    }
}
