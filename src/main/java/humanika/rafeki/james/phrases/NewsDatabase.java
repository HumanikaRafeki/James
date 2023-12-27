package humanika.rafeki.james.phrases;

import me.mcofficer.esparser.DataNode;

import java.util.List;
import java.util.HashMap;

public class NewsDatabase {
    NewsDatabase parent;
    HashMap<String, NewsStory> news;

    public NewsDatabase() {
        this(null);
    }

    public NewsDatabase(NewsDatabase parent) {
        this.parent = parent;
        news = new HashMap<String, NewsStory>();
    }

    public void clear() {
        news.clear();
    }

    public void addNews(List<DataNode> data) {
        for(DataNode node : data)
            if(node.size() > 1 && node.token(0).equals("news"))
                news.put(node.token(1), new NewsStory(node));
    }

    public NewsStory getNews(String name) {
        NewsStory result = news.getOrDefault(name, null);
        if(result == null && parent != null)
            result = parent.getNews(name);
        return result;
    }
};
