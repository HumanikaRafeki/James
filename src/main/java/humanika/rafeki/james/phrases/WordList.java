package humanika.rafeki.james.phrases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import me.mcofficer.esparser.DataNode;

public class WordList implements PhraseExpander {

    private static final Pattern phrasePattern = Pattern.compile("[$][{][^\\}]*[}]", Pattern.DOTALL);

    protected ArrayList<Choice> choices;
    private double accumulatedWeight = 1;

    public WordList(DataNode node) {
        this(node, true);
    }

    public WordList(DataNode node, boolean allowPhraseReferences) {
        choices = new ArrayList<Choice>();
        for(DataNode child : node.getChildren()) {
            addWord(child, allowPhraseReferences);
        }
        accumulateWeights();
    }

    @Override
    public void expand(StringBuilder result, PhraseProvider phrases, Set<String> touched, PhraseLimits limits) {
        if(choices.size() < 1)
            return;

        Choice chosen = randomChoice();

        if(chosen == null)
            return;
        else if(chosen.expander != null)
            chosen.expander.expand(result, phrases, touched, limits);
        else if(chosen.word != null) {
            limits.appendRemaining(chosen.word, result);
        }
    }

    protected Choice randomChoice() {
        if(choices.size() == 0)
            return null;
        else if(choices.size() == 1)
            return choices.get(0);
        double accum = Math.random() * accumulatedWeight;
        int first = 0, after = choices.size() - 1;
        Choice chosen = choices.get(after);
        if(accum >= chosen.accum)
            return chosen;
        while(after > first + 1) {
            int middle = (first + after) / 2;
            chosen = choices.get(middle);
            double middleAccum = chosen.accum;
            if(accum < middleAccum)
                after = middle;
            else
                first = middle;
        }
        return choices.get(first);
    }

    protected void accumulateWeights() {
        double accum = 0;
        for(Choice choice : choices) {
            choice.accum = accum;
            // Replace bad weights with 1
            if(!(choice.weight > 1e-5) || !(choice.weight < 1e+12))
                choice.weight = 1;
            accum += choice.weight;
        }
        accumulatedWeight = accum;
    }

    protected void addWord(DataNode node, boolean allowPhraseReferences) {
        if(node.size() < 1)
            return;
        double weight = 1;
        if(node.size() > 1 && node.isNumberAt(1))
            weight = Double.valueOf(node.token(1));
        choices.add(asChoice(weight, node.token(0), allowPhraseReferences));
    }

    private Choice asChoice(double weight, String token, boolean allowPhraseReferences) {
        if(allowPhraseReferences && token.indexOf("${") >= 0)
            return new Choice(weight, null, new WordExpander(token));
        else
            return new Choice(weight, token, null);
    }
}
