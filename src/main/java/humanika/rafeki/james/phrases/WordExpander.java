package humanika.rafeki.james.phrases;

import java.util.Set;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.mcofficer.esparser.DataNode;

class WordExpander implements PhraseExpander {

    public static Pattern phraseFinder = Pattern.compile("(.*?)[$][{]([^\\}]*)[}]", Pattern.DOTALL);
    public static int maxAllowedMatches = 30;

    private ArrayList<Entry> content;

    public WordExpander(String token) {
        content = new ArrayList<Entry>();
        Matcher matcher = phraseFinder.matcher(token);

        // Optimize for most common case: no ${} in string:
        if(!matcher.find()) {
            content.add(new Entry(token, false));
            return;
        }

        int last = 0;
        do {
            String before = matcher.group(1);
            String var = matcher.group(2);
            if(before != null && before.length() > 0)
                content.add(new Entry(before, false));
            if(var != null && var.length() > 0)
                content.add(new Entry(var, true));
            last = matcher.end();
        } while(matcher.find(last));

        if(last < token.length())
            content.add(new Entry(token.substring(last), false));
    }

    @Override
    public void expand(StringBuilder result, PhraseProvider phrases, Set<String> touched, PhraseLimits limits) {
        for(Entry entry : content) {
            if(entry.isPhrase) {
                PhraseExpander phrase = phrases.getExpander(entry.token);
                if(phrase == null)
                    continue;
                phrase.expand(result, phrases, touched, limits);
            } else
                limits.appendRemaining(entry.token, result);
        }
    }

    private class Entry {
        String token;
        boolean isPhrase;
        public Entry(String token, boolean isPhrase) {
            this.token = token;
            this.isPhrase = isPhrase;
        }
    }
}
