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

public class SayCommand extends ParseCommand {
    private static final String[] VAR_ARRAY = {"text"};
    private static final List<String> VAR_LIST = Arrays.asList(VAR_ARRAY);

    @Override
    public String getName() {
        return "say";
    }

    @Override
    protected String invalidInputDescription() {
        return "*No text provided!*";
    }

    @Override
    protected List<String> getVarList() {
        return VAR_LIST;
    }

    @Override
    protected String[] processInput(int count, PhraseDatabase phrases, NewsDatabase news, String entry, PhraseLimits limits) {
        if(count < 1) {
            String[] result = { "Error", "*Too many inputs! Use fewer phrases or repetitions.*" };
            return result;
        }
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < count; i++) {
            if(i > 0)
                builder.append('\n');
            phrases.expandText(entry, limits, builder);
        }
        // "very long string" becomes "very long s..."
        if(builder.length() > MAX_STRING_LENGTH) {
            builder.delete(MAX_STRING_LENGTH - 3, builder.length());
            builder.append("...");
        }
        String expanded = builder.toString().replaceAll("[@#<>|*_ \t]+"," ");
        String title = "Result";
        if(count > 1)
            title += " Repeated " + count + " Times";
        String[] result = { title, "`" + expanded.replace("`","'") + "`" };
        return result;
    }
}
