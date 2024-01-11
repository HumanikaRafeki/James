package humanika.rafeki.james.data;

import java.util.Iterator;
import java.util.NoSuchElementException;

class SingleStringIterator implements Iterator<String> {
    private String value;
    public SingleStringIterator(String value) {
        this.value = value;
    }
    @Override
    public boolean hasNext() {
        return value != null;
    }
    @Override
    public String next() {
        String value = this.value;
        this.value = null;
        if(value == null)
            throw new NoSuchElementException();
        return value;
    }
}
