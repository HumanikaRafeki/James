package humanika.rafeki.james.data;

import me.mcofficer.esparser.DataNode;
import me.mcofficer.esparser.DataFile;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import humanika.rafeki.james.phrases.PhraseDatabase;
import humanika.rafeki.james.phrases.NewsDatabase;

/** Clones the Endless Sky repository. Provides access to its data. */
public class EndlessSky implements AutoCloseable {
    private JamesConfig config;
    private File workingCopy;
    private Git repo;
    private PhraseDatabase phrases = null;
    private NewsDatabase news = null;
    private HashMap dataFiles = null;

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
        dataFiles = null;
    }

    public PhraseDatabase getPhrases() {
        return phrases;
    }

    public NewsDatabase getNews() {
        return news;
    }

    public synchronized void repoAction(int action) throws GitAPIException, IOException {
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

    private synchronized void readGameData() throws IOException {
        Path data = new File(workingCopy, "data").toPath();
        PhraseDatabase phrases = new PhraseDatabase();
        NewsDatabase news = new NewsDatabase();
        HashMap dataFiles = new HashMap();
        List<Path> paths = Files.walk(data).filter(path->path.toString().endsWith(".txt")).collect(Collectors.toList());
        for(Path path : paths) {
            String relative = data.relativize(path).toString();
            List<String> lines = Files.readAllLines(path);
            DataFile read = new DataFile(lines);
            dataFiles.put(relative, read);
            phrases.addPhrases(read.getNodes());
            news.addNews(read.getNodes());
        }
        this.phrases = phrases;
        this.news = news;
        this.dataFiles = dataFiles;
    }
}
