package humanika.rafeki.james.data;

import me.mcofficer.esparser.DataNode;
import me.mcofficer.esparser.DataFile;

import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

public class NodeLookups {
    private HashMap<String, DataFile> dataFiles = new HashMap<>();
    private HashMap<String, HashMap<String, DataNode>> typeNameNode = new HashMap<>();
    private HashMap<String, ArrayList<DataNode>> nameNode = new HashMap<>();
    private HashMap<Integer, ArrayList<Government>> swizzleGovernment = new HashMap<>();

    NodeLookups() {

    }

    synchronized void addFile(String relativePath, DataFile file) {
        dataFiles.put(relativePath, file);
        for(DataNode node : file.getNodes()) {
            if(node.size() < 2)
                continue;
            String type = node.token(0);
            String name = node.token(1);

            addToTypeNameNode(type, name, node);
            addToNameNode(name, node);
            if(type.equals("government"))
                addToSwizzleGovernment(node);
        }
    }

    public Optional<List<Government>> governmentsWithSwizzle(int swizzle) {
        List<Government> got = swizzleGovernment.get(Integer.valueOf(swizzle));
        return got != null ? Optional.of(got) : Optional.empty();
    }

    private void addToTypeNameNode(String type, String name, DataNode node) {
        HashMap<String, DataNode> got = typeNameNode.get(type);
        if(got == null) {
            got = new HashMap<>();
            typeNameNode.put(type, got);
        }
        got.put(name, node);
    }
    private void addToNameNode(String name, DataNode node) {
        ArrayList<DataNode> got = nameNode.get(name);
        if(got == null) {
            got = new ArrayList<>();
            nameNode.put(name, got);
        }
        got.add(node);
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
