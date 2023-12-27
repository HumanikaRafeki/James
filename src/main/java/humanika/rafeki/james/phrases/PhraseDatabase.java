package humanika.rafeki.james.phrases;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import me.mcofficer.esparser.DataNode;

public class PhraseDatabase implements PhraseProvider {

    PhraseDatabase parent;
    HashMap<String, Phrase> expanders;

    public PhraseDatabase(PhraseDatabase parent) {
        expanders = new HashMap<String, Phrase>();
        this.parent = parent;
    }

    public PhraseDatabase() {
        expanders = new HashMap<String, Phrase>();
        this.parent = null;
    }

    public PhraseDatabase getParent() {
        return parent;
    }

    public void setParent(PhraseDatabase parent) {
        this.parent = parent;
    }

    public void clear() {
        expanders.clear();
    }

    public void addPhrases(List<DataNode> data) {
        for(DataNode node : data)
            if(node.size() > 1 && node.getTokens().get(0).equals("phrase"))
                addPhrase(node);
    }

    public void addPhrase(DataNode node) {
        Phrase phrase = new Phrase(node);
        String name = phrase.getName();
        if(name != null && name.length() > 0)
            expanders.put(name, phrase);
        else
            node.printTrace("not a valid phrase node");
    }

    public String expand(String phraseName, PhraseLimits limits) {
        Phrase phrase = get(phraseName);
        if(phrase == null)
            return null;
        return phrase.expand(this, limits);
    }

    public void expandText(String text, PhraseLimits limits, StringBuilder result) {
        WordExpander expander = new WordExpander(text);
        expander.expand(result, this, new HashSet<String>(), limits);
    }

    public Phrase get(String phrase) {
        Phrase got = expanders.getOrDefault(phrase, null);
        if(got != null)
            return got;
        if(parent == null)
            return null;
        return parent.get(phrase);
    }

    @Override
    public PhraseExpander getExpander(String phrase) {
        return get(phrase);
    }
}
