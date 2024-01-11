package humanika.rafeki.james.data;

import me.mcofficer.esparser.DataNode;
import me.mcofficer.esparser.DataFile;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.awt.image.BufferedImage;
import java.util.function.Predicate;


import humanika.rafeki.james.phrases.PhraseDatabase;
import humanika.rafeki.james.phrases.NewsDatabase;

/** Clones the Endless Sky repository. Provides access to its data. */
public class EndlessSky implements AutoCloseable {
    private JamesConfig config;
    private File workingCopy;
    private Git repo;
    private PhraseDatabase phrases = null;
    private NewsDatabase news = null;
    private NodeLookups lookups = null;
    private ImageCatalog images = null;

    private final static int OPEN_ACTION = 1;
    private final static int PULL_ACTION = 2;

    public EndlessSky(JamesConfig config) {
        this.config = config;
        this.workingCopy = new File(config.workArea, "endless-sky");
        this.repo = null;
    }

    public void openOrPull() throws GitAPIException, IOException {
        if(repo != null)
            repoAction(PULL_ACTION);
        else
            repoAction(OPEN_ACTION);
    }

    public void reloadData() throws GitAPIException, IOException {
        readGameData();
    }

    public void close() {
        Git repo = this.repo;
        this.repo = null;
        repo.close();
        phrases = null;
        news = null;
        lookups = null;
        images = null;
    }

    public Optional<List<Government>> governmentsWithSwizzle(int swizzle) {
        if(lookups != null)
            return lookups.governmentsWithSwizzle(swizzle);
        return Optional.empty();
    }

    public List<SearchResult> fuzzyMatchNodesAndImages(String query, int maxSearch, Predicate<NodeInfo> nodeCondition, Predicate<String> imageCondition) {
        List<SearchResult> nodes = fuzzyMatchNodeNames(query, maxSearch, nodeCondition);
        List<SearchResult> images = fuzzyMatchImagePaths(query, maxSearch, imageCondition);

        if(nodes.size() == 0)
            return images;
        else if(images.size() == 0)
            return nodes;

        // Add the two maxSearch from the two lists.
        List<SearchResult> result = new ArrayList<>();
        for(int i = 0, n = 0, side = 99; side != 0;) {
            // side: -1 = add image; +1 = add node; 0 = nothing more to add
            // i: index within images of first image not yet added
            // n: index within nodes of first node not yet added
            if(result.size() >= maxSearch)
                break;
            side = 0;
            if(i < images.size()) {
                if(n < nodes.size())
                    side = images.get(i).getScore() < nodes.get(n).getScore() ? 1 : -1;
                else
                    side = -1;
            } else if(n < nodes.size())
                side = 1;
            if(side == 1) {
                result.add(nodes.get(n));
                n++;
            } else if(side == -1) {
                result.add(images.get(i));
                i++;
            }
        }
        return result;
    }

    public Optional<SearchResult> dummyResultWithHash(String hash) {
        Optional<SearchResult> result = lookups.dummyResultWithHash(hash);
        return result.isPresent() ? result : images.dummyResultWithHash(hash);
    }

    public List<SearchResult> fuzzyMatchNodeNames(String query, int maxSearch, Predicate<NodeInfo> condition) {
        return lookups.fuzzyMatchNodeNames(query, maxSearch, condition);
    }

    public Optional<List<NodeInfo>> selectNodesByName(String dataName, int maxSearch, Predicate<NodeInfo> condition) {
        return lookups.selectNodesByName(dataName, maxSearch, condition);
    }

    public Optional<List<NodeInfo>> nodesWithHash(String hash) {
        return lookups.nodesWithHash(hash);
    }

    public List<SearchResult> fuzzyMatchImagePaths(String query, int maxSearch, Predicate<String> condition) {
        return images.fuzzyMatchRelativePaths(query, maxSearch, condition);
    }

    public Optional<String> imageWithHash(String hash) {
        return images.imageWithHash(hash);
    }

    public Optional<Path> getImagePath(String name) {
        return images.getImagePath(name);
    }

    public Optional<Path> getImageRelativePath(String name) {
        return images.getImageRelativePath(name);
    }

    public Optional<BufferedImage> loadImage(String name) throws IOException {
        if(images != null)
            return images.loadImage(name);
        return Optional.empty();
    }

    public PhraseDatabase getPhrases() {
        return phrases;
    }

    public NewsDatabase getNews() {
        return news;
    }

    public /* synchronized */ void repoAction(int action) throws GitAPIException, IOException {
        if(action == OPEN_ACTION) {
            if(workingCopy.exists()) {
                Git repo = Git.open(workingCopy);
                repo.pull().call();
                this.repo = repo;
            } else
                this.repo = Git.cloneRepository().setURI(config.endlessSkyRepo.toString()).setDirectory(workingCopy).setCloneAllBranches(false).call();
        } else if(action == PULL_ACTION)
            repo.pull().call();
    }

    private /* synchronized */ void readGameData() throws IOException {
        Path data = new File(workingCopy, "data").toPath();
        PhraseDatabase phrases = new PhraseDatabase();
        NewsDatabase news = new NewsDatabase();
        HashMap dataFiles = new HashMap();
        NodeLookups lookups = new NodeLookups();
        ImageCatalog images = new ImageCatalog(new File(workingCopy, "images").toPath());
        List<Path> paths = Files.walk(data).filter(path->path.toString().endsWith(".txt")).collect(Collectors.toList());
        for(Path path : paths) {
            String relative = data.relativize(path).toString();
            List<String> lines = Files.readAllLines(path);
            DataFile read = new DataFile(lines);
            phrases.addPhrases(read.getNodes());
            news.addNews(read.getNodes());
            lookups.addFile(relative, read);
        }
        lookups.postLoad();
        this.phrases = phrases;
        this.news = news;
        this.lookups = lookups;
        this.images = images;
    }
}
