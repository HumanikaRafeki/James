package humanika.rafeki.james.phrases;

public class PhraseLimits {
    private int length;
    private int recursion;

    public PhraseLimits(int length, int recursion) {
        this.length = length;
        this.recursion = recursion;
    }

    public int getLength() {
        return length;
    }
    public int getRecursion() {
        return recursion;
    }

    boolean canRecurse(int depth) {
        return recursion > 0 ? depth < recursion : true;
    }

    boolean canExpandBy(int amount, StringBuilder buffer) {
        return this.length > 0 ? true : amount < length - buffer.length();
    }

    boolean appendRemaining(String toAppend, StringBuilder buffer) {
        if(length < 0) {
            buffer.append(toAppend);
            return true;
        }
        int buflen = buffer.length();
        if(buflen >= length)
            return false;
        if(toAppend.length() > 0) {
            int append = toAppend.length();
            int remain = length - buflen;
            if(append > remain)
                append = remain;
            if(append > 0)
                buffer.append(toAppend, 0, append);
        }
        return buffer.length() < length;
    }
}
