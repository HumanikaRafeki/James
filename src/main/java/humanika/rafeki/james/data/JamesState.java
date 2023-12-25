package humanika.rafeki.james.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import me.mcofficer.esparser.DataFile;

import humanika.rafeki.james.Utils;
import humanika.rafeki.james.phrases.PhraseDatabase;
import humanika.rafeki.james.phrases.Phrase;
import humanika.rafeki.james.phrases.PhraseLimits;

public class JamesState {
    private final ReentrantReadWriteLock modifying;
    private final PhraseLimits phraseLimits;

    // Objects that may be replaced at any time:
    private PhraseDatabase jamesPhrases;

    public JamesState(PhraseLimits phraseLimits) {
        modifying = new ReentrantReadWriteLock();
        this.phraseLimits = phraseLimits;

        jamesPhrases = null;
    }

    public String jamesPhrase(String name) {
        PhraseDatabase jamesPhrases = this.jamesPhrases;
        return jamesPhrases != null ? jamesPhrases.expand(name, phraseLimits) : null;
    }

    public Reading use() throws InterruptedException {
        return new Reading(modifying.readLock());
    }

    public void update() throws IOException, InterruptedException {
        List<String> jamesData = Utils.readResourceLines("james.txt");
        PhraseDatabase jamesPhrases = new PhraseDatabase();
        if(jamesData != null)
            jamesPhrases.addPhrases(new DataFile(jamesData).getNodes());

        // FIXME: Read other things here.

        ReentrantReadWriteLock.WriteLock lock = modifying.writeLock();
        lock.lockInterruptibly();
        try {
            this.jamesPhrases = jamesPhrases;
            // FIXME: Write other things here.
        } finally {
            lock.unlock();
        }
    }

    public class Reading implements AutoCloseable {
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
