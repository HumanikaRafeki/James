package humanika.rafeki.james.data;

import org.slf4j.Logger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import java.util.Optional;
import java.net.URI;
import java.awt.image.BufferedImage;
import org.eclipse.jgit.api.errors.GitAPIException;

import me.mcofficer.esparser.DataFile;

import humanika.rafeki.james.Utils;
import humanika.rafeki.james.James;
import humanika.rafeki.james.phrases.PhraseDatabase;
import humanika.rafeki.james.phrases.Phrase;
import humanika.rafeki.james.phrases.PhraseLimits;

public class JamesState implements AutoCloseable {
    private final ReentrantReadWriteLock modifying;
    private final PhraseLimits phraseLimits;
    private final URI botUri;
    private final Logger logger;

    // Objects that may be replaced at any time:
    private PhraseDatabase jamesPhrases = null;
    private EndlessSky endlessSky = null;

    public JamesState(JamesConfig config, Logger logger) {
        modifying = new ReentrantReadWriteLock();
        this.botUri = config.botRepo;
        this.phraseLimits = new PhraseLimits(config.maxExpandedPhraseLength, config.maxPhraseRecursionDepth);
        this.logger = logger;
        endlessSky = new EndlessSky(config);
    }

    public void close() {
        endlessSky.close();
        jamesPhrases.clear();
        endlessSky = null;
        jamesPhrases = null;
    }

    public URI getBotUri() {
        return botUri;
    }

    public PhraseLimits getPhraseLimits() {
        return phraseLimits;
    }

    public EndlessSky getEndlessSky() {
        return endlessSky;
    }

    public Optional<List<Government>> governmentsWithSwizzle(int swizzle) {
        return endlessSky.governmentsWithSwizzle(swizzle);
    }

    public Optional<List<NodeInfo>> fuzzyMatchNodeNames(String query, int maxSearch) {
        return endlessSky.fuzzyMatchNodeNames(query, maxSearch);
    }

    public String jamesPhrase(String name) {
        PhraseDatabase jamesPhrases = this.jamesPhrases;
        return jamesPhrases != null ? jamesPhrases.expand(name, phraseLimits) : null;
    }

    public String endlessSkyPhrase(String name) {
        return endlessSky.getPhrases().expand(name, phraseLimits);
    }

    public AutoCloseable use() throws InterruptedException {
        return new Reading(modifying.readLock());
    }

    private PhraseDatabase readJamesPhrases() throws IOException {
        List<String> jamesData = Utils.readResourceLines("james.txt");
        PhraseDatabase jamesPhrases = new PhraseDatabase();
        if(jamesData != null)
            jamesPhrases.addPhrases(new DataFile(jamesData).getNodes());
        return jamesPhrases;
    }

    public void update(JamesConfig config) throws GitAPIException, IOException, InterruptedException {
        // Do reentrant-safe things here:
        logger.info("Beginning James update.");
        logger.info("Reading james commentary...");
        PhraseDatabase jamesPhrases = readJamesPhrases();
        logger.info("Pulling endless-sky repository...");
        EndlessSky endlessSky = new EndlessSky(config);
        endlessSky.openOrPull();
        logger.info("Loading data from endless-sky repository...");
        endlessSky.reloadData();
        jamesPhrases.setParent(endlessSky.getPhrases());

        ReentrantReadWriteLock.WriteLock lock = modifying.writeLock();
        lock.lockInterruptibly();
        try {
            this.jamesPhrases = jamesPhrases;
            this.endlessSky = endlessSky;
        } finally {
            lock.unlock();
        }
        logger.info("James update is complete.");
    }

    private class Reading implements AutoCloseable {
        ReentrantReadWriteLock.ReadLock lock;
        Reading(ReentrantReadWriteLock.ReadLock lock) throws InterruptedException {
            this.lock = lock;
            this.lock.lockInterruptibly();
        }
        public void close() {
            lock.unlock();
        }
    };
};
