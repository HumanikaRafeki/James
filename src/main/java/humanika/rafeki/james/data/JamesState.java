package humanika.rafeki.james.data;

import java.io.IOException;
import java.util.function.Function;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    public JamesState(PhraseLimits limits) {
        modifying = new ReentrantReadWriteLock();
        this.limits = limits;

        jamesTxt = null;
    }

    public String jamesPhrase(String name) {
        PhraseDatabase jamesPhrases = this.jamesPhrases;
        return jamesPhrases != null ? jamesPhrases.expand(name, phraseLimits) : null;
    }

    public Reading use() throws InterruptedException {
        return new Reading();
    }

    public void update() throws IOException {
        String jamesData = Utils.readResourceString("james.txt");
        PhraseDatabase jamesPhrases = new PhraseDatabase();
        if(jamesData != null)
            jamesPhrases.load(new DataFile(jamesData));

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

    public class Reading implements AutoClosable {
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
