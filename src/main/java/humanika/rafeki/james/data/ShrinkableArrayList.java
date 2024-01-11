package humanika.rafeki.james.data;

import java.util.ArrayList;

public class ShrinkableArrayList<T> extends ArrayList<T> {
    public void shrink(int len) {
        if(len < size())
            removeRange(len, size());
    }
}
