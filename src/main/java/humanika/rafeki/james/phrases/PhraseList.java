package humanika.rafeki.james.phrases;

import java.util.Set;

import me.mcofficer.esparser.DataNode;

class PhraseList extends WordList {
    public PhraseList(DataNode node) {
        super(node, false);
    }

    @Override
    public void expand(StringBuilder result, PhraseProvider phrases, Set<String> touched, PhraseLimits limits) {
        if(choices.size() < 1)
            return;

        Choice chosen = randomChoice();
        PhraseExpander expander = phrases.getExpander(chosen.word);
        if(expander == null)
            return;
        expander.expand(result, phrases, touched, limits);
    }
};
