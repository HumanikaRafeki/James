package humanika.rafeki.james.phrases;

import java.util.Comparator;

public class Choice {
    public double weight = 1;
    public double accum = 1;
    public String word = null;
    public PhraseExpander expander = null;

    public Choice(double weight, String word, PhraseExpander expander) {
        this.weight = weight;
        this.accum = weight;
        this.word = word;
        this.expander = expander;
    }
}
