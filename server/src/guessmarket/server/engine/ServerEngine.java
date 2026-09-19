package guessmarket.server.engine;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.GuessMarketEngineImpl;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The one engine the whole server shares.
 *
 * Every servlet answers a different request, possibly on a different thread,
 * but they all have to be talking about the same market. This holds that single
 * instance and hands it out.
 *
 * It also holds the market's version number. Clients poll, and most polls find
 * nothing new; the version lets the server answer "nothing changed" instead of
 * sending the whole market several times a second. Anything that changes what
 * a client can see calls bumpVersion.
 *
 * Nothing is written to disk. When the server stops everything is gone, which
 * is what the exercise asks for.
 */
public final class ServerEngine {

    private static final GuessMarketEngine ENGINE = create();

    /** Starts at 1 so that a client holding 0 always gets a first answer. */
    private static final AtomicInteger VERSION = new AtomicInteger(1);

    private ServerEngine() {
    }

    private static GuessMarketEngine create() {
        GuessMarketEngine engine = new GuessMarketEngineImpl();
        // People log in before any file is uploaded, so there has to be a
        // system to put them in from the start.
        engine.ensureStarted();
        return engine;
    }

    public static GuessMarketEngine get() {
        return ENGINE;
    }

    public static int version() {
        return VERSION.get();
    }

    /** Called after anything that changes what the clients can see. */
    public static void bumpVersion() {
        VERSION.incrementAndGet();
    }
}
