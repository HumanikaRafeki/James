package humanika.rafeki.james.phrases;

public class Choice implements Comparable<Choice> {
    public final double weight;
    public double accum;
    public final String word;
    public final PhraseExpander expander;

    public Choice(double weight, String word, PhraseExpander expander) {
        this.weight = weight;
        this.accum = weight;
        this.word = word;
        this.expander = expander;
    }

    public Choice(double weight) {
        this.weight = weight;
        this.accum = weight;
        this.word = null;
        this.expander = null;
    }

    @Override
    public int compareTo(Choice other) {
        Choice b = (Choice)other;
        if(accum < b.accum)
            return -1;
        else if(accum > b.accum)
            return 1;
        return 0;
    }
}
