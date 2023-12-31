package humanika.rafeki.james.data;

import me.mcofficer.esparser.DataNode;
import me.mcofficer.esparser.DataFile;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;

public class NodeLookups {
    private HashMap<String, DataFile> dataFiles = new HashMap<>();
    private HashMap<String, HashMap<String, NodeInfo>> typeNameNode = new HashMap<>();
    private HashMap<String, ArrayList<NodeInfo>> nameNode = new HashMap<>();
    private HashMap<String, ArrayList<NodeInfo>> hashNode = new HashMap<>();
    private HashMap<Integer, ArrayList<Government>> swizzleGovernment = new HashMap<>();
    private final static StringMetric metric = StringMetrics.needlemanWunch();

    NodeLookups() {

    }

    /* synchronized */ void addFile(String relativePath, DataFile file) {
        dataFiles.put(relativePath, file);
        for(DataNode node : file.getNodes()) {
            if(node.size() < 2)
                continue;
            String type = node.token(0);
            String name = node.token(1);
            NodeInfo info = new NodeInfo(node);
            addToTypeNameNode(type, name, info);
            addToNameNode(name, info);
            addToHashNode(info.getHashString(), info);
            if(type.equals("government"))
                addToSwizzleGovernment(node);
        }
    }

    class FloatNode {
        public final float score;
        public final NodeInfo node;
        FloatNode(float score, NodeInfo node) {
            this.score = score;
            this.node = node;
        }
        public static int compare(FloatNode lhs, FloatNode rhs) {
            return lhs.score > rhs.score ? -1 : lhs.score < rhs.score ? 1 : 0;
        }
    }

    private class ShrinkableList extends ArrayList<FloatNode> {
        public void shrink(int len) {
            if(len < size())
                removeRange(len, size());
        }
    }

    public Optional<List<NodeInfo>> fuzzyMatchNodeNames(String query, int maxSearch, Predicate<NodeInfo> condition) {
        int threshold = maxSearch > 0 ? 3 * maxSearch : 0;
        ShrinkableList work = new ShrinkableList();
        for(Map.Entry<String, ArrayList<NodeInfo>> entry : nameNode.entrySet()) {
            ArrayList<NodeInfo> list = entry.getValue();
            if(list.size() < 1)
                continue;
            NodeInfo info = list.get(list.size() - 1);
            if(condition.test(info)) {
                work.add(new FloatNode(metric.compare(query, info.getSearchString()), info));
                if(threshold > 0 && work.size() > threshold) {
                    work.sort(FloatNode::compare);
                    work.shrink(maxSearch);
                }
            }
        }

        if(work.size() < 1)
            return Optional.empty();

        work.sort(FloatNode::compare);
        ArrayList<NodeInfo> output = new ArrayList<>();
        int stop = work.size();
        if(maxSearch > 0 && maxSearch < stop)
            stop = maxSearch;
        for(int i = 0; i < stop; i++)
            output.add(work.get(i).node);
        return Optional.of(output);
    }

    public Optional<List<NodeInfo>> nodesWithHash(String hash) {
        ArrayList<NodeInfo> got = hashNode.get(hash);
        if(got == null || got.size() < 1)
            return Optional.empty();
        return Optional.of(got);
    }

    public Optional<List<Government>> governmentsWithSwizzle(int swizzle) {
        List<Government> got = swizzleGovernment.get(Integer.valueOf(swizzle));
        return got != null ? Optional.of(got) : Optional.empty();
    }

    private void addToTypeNameNode(String type, String name, NodeInfo info) {
        HashMap<String, NodeInfo> got = typeNameNode.get(type);
        if(got == null) {
            got = new HashMap<>();
            typeNameNode.put(type, got);
        }
        got.put(name, info);
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
