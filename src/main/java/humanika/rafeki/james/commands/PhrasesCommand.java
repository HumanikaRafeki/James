package humanika.rafeki.james.commands;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import humanika.rafeki.james.James;
import humanika.rafeki.james.Utils;
import humanika.rafeki.james.utils.KorathCipher;
import humanika.rafeki.james.data.EndlessSky;
import humanika.rafeki.james.data.JamesState;
import humanika.rafeki.james.phrases.PhraseDatabase;
import humanika.rafeki.james.phrases.PhraseLimits;
import humanika.rafeki.james.phrases.NewsDatabase;
import humanika.rafeki.james.phrases.Phrase;

public class PhrasesCommand extends ParseCommand {
    private static final String[] VAR_ARRAY = {"phrase1", "phrase2", "phrase3", "phrase4"};
    private static final List<String> VAR_LIST = Arrays.asList(VAR_ARRAY);

    @Override
    public String getName() {
        return "phrases";
    }

    @Override
    protected String invalidInputDescription() {
        return "*No phrases provided!*";
    }

    @Override
    protected List<String> getVarList() {
        return VAR_LIST;
    }

    @Override
    protected String[] processInput(int count, PhraseDatabase phrases, NewsDatabase news, String entry, PhraseLimits limits) {
        String expanded = expandPhrases(count, phrases, entry, limits);
        if(expanded == null) {
            String[] result = { "Phrase \"" + entry + '"', "*Phrase not found!*" };
            return result;
        } else {
            String title = "Phrase \"" + entry + '"';
            if(count > 1)
                title += " Repeated " + count + " Times";
            String[] result = { title, "`" + expanded.replace("`","'") + "`" };
            return result;
        }
    }

    private String expandPhrases(int count, PhraseDatabase phrases, String phrase, PhraseLimits limits) {
        StringBuilder builder = new StringBuilder();

        Phrase gotten = phrases.get(phrase);
        if(gotten == null)
            return null;

        for(int repeat = 0; repeat < count && builder.length() < MAX_STRING_LENGTH; repeat++)
            builder.append(gotten.expand(phrases, limits)).append('\n');

        // "very long string" becomes "very long s..."
        if(builder.length() > MAX_STRING_LENGTH) {
            builder.delete(MAX_STRING_LENGTH - 3, builder.length());
            builder.append("...");
        }
        return builder.toString().replaceAll("[@#<>|*_ \t]+"," ");
    }
}
