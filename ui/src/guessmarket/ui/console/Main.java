package guessmarket.ui.console;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.GuessMarketEngineImpl;

/**
 * Entry point. The only thing it does is pick an engine implementation and hand
 * it to the UI -- which is what keeps the UI dependent on the interface rather
 * than on the implementation.
 */
public class Main {

    public static void main(String[] args) {
        GuessMarketEngine engine = new GuessMarketEngineImpl();
        new ConsoleUi(engine).run();
    }
}
