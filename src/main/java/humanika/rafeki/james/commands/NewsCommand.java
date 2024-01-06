package humanika.rafeki.james.commands;

import humanika.rafeki.james.James;
import humanika.rafeki.james.Utils;
import humanika.rafeki.james.data.EndlessSky;
import humanika.rafeki.james.data.JamesState;
import humanika.rafeki.james.phrases.NewsDatabase;
import humanika.rafeki.james.phrases.NewsStory;
import humanika.rafeki.james.phrases.PhraseDatabase;
import humanika.rafeki.james.phrases.PhraseLimits;
import humanika.rafeki.james.utils.KorathCipher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NewsCommand extends ParseCommand {
    private static final String[] VAR_ARRAY = {"news1", "news2", "news3", "news4"};
    private static final List<String> VAR_LIST = Arrays.asList(VAR_ARRAY);

    @Override
    public String getName() {
        return "news";
    }

    @Override
    protected String invalidInputDescription() {
        return "*No news provided!*";
    }

    @Override
    protected List<String> getVarList() {
        return VAR_LIST;
    }

    @Override
    protected String[] processInput(int count, PhraseDatabase phrases, NewsDatabase news, String entry, PhraseLimits limits) {
        if(count < 1) {
            String[] result = { "News \"" + entry + '"', "*Too many inputs! Use fewer news or repetitions.*" };
            return result;
        }
        String expanded = expandNews(count, phrases, news, entry, limits);
        if(expanded == null) {
            String[] result = { "News \"" + entry + '"', "*Phrase not found!*" };
            return result;
        } else {
            String title = "News \"" + entry + '"';
            if(count > 1)
                title += " Repeated " + count + " Times";
            String[] result = { title, "`" + expanded.replace("`","'") + "`" };
            return result;
        }
    }

    private String expandNews(int count, PhraseDatabase phrases, NewsDatabase news, String name, PhraseLimits limits) {
        StringBuilder builder = new StringBuilder();

        NewsStory gotten = news.getNews(name);
        if(gotten == null)
            return null;

        for(int repeat = 0; repeat < count && builder.length() < MAX_STRING_LENGTH; repeat++)
            builder.append(gotten.toString(phrases, limits)).append('\n');

        // "very long string" becomes "very long s..."
        if(builder.length() > MAX_STRING_LENGTH) {
            builder.delete(MAX_STRING_LENGTH - 3, builder.length());
            builder.append("...");
        }
        return builder.toString().replaceAll("[@#<>|*_ \t]+"," ");
    }
}
