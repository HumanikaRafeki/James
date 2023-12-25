package humanika.rafeki.james.phrases;

import me.mcofficer.esparser.DataNode;

import java.util.HashSet;

public class NewsStory {
    Phrase name;
    Phrase message;

    NewsStory(DataNode node) {
        name = null;
        message = null;
        for(DataNode child : node.getChildren())
            if(child.size() < 1)
                continue;
            else if(child.token(0).equals("name"))
                name = new Phrase(child);
            else if(child.token(0).equals("message"))
                message = new Phrase(child);
    }

    public Phrase getName() {
        return name;
    }

    public Phrase getMessage() {
        return message;
    }

    public String toString(PhraseDatabase phrases, PhraseLimits limits) {
        HashSet<String> touched = new HashSet<String>();
        StringBuilder result = new StringBuilder();
        if(name != null)
            name.expand(result, phrases, touched, limits);
        else
            limits.appendRemaining("(no name)", result);
        limits.appendRemaining(": ", result);
        if(message != null)
            message.expand(result, phrases, touched, limits);
        else
            limits.appendRemaining("(no message)", result);
        return result.toString();
    }
};
