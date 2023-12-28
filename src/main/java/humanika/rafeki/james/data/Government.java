package humanika.rafeki.james.data;

import me.mcofficer.esparser.DataNode;

public class Government {
    public final String name;
    public final String displayName;
    public final int swizzle;

    public Government(DataNode node) {
        name = node.size() > 1 ? node.token(1) : "Unspecified";
        String displayName = name;
        int swizzle = 0;
        for(DataNode child : node.getChildren()) {
            if(child.size() > 1) {
                if(child.token(0).equals("swizzle") && child.isNumberAt(1))
                    swizzle = Integer.parseInt(child.token(1), 10);
                else if(child.token(0).equals("display name"))
                    displayName = child.token(1);
            }
        }
        this.swizzle = swizzle;
        this.displayName = displayName;
    }
}
