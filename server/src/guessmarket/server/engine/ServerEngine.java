package guessmarket.server.engine;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.GuessMarketEngineImpl;

/**
 * The one engine the whole server shares.
 *
 * Every servlet answers a different request, possibly on a different thread,
 * but they all have to be talking about the same market. This holds that single
 * instance and hands it out.
 *
 * Nothing is written to disk. When the server stops everything is gone, which
 * is what the exercise asks for.
 */
public final class ServerEngine {

    private static final GuessMarketEngine ENGINE = create();

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
}
