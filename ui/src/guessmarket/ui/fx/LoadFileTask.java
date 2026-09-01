package guessmarket.ui.fx;

import guessmarket.engine.api.GuessMarketEngine;

import javafx.concurrent.Task;

/**
 * Loads a file on a background thread so the window stays responsive.
 *
 * The real work is fast, so short pauses are inserted between the steps to make
 * the progress bar visible, as the exercise asks.
 */
public class LoadFileTask extends Task<Integer> {

    private static final long STEP_PAUSE_MILLIS = 400;

    private final GuessMarketEngine engine;
    private final String path;

    public LoadFileTask(GuessMarketEngine engine, String path) {
        this.engine = engine;
        this.path = path;
    }

    @Override
    protected Integer call() throws Exception {
        updateProgress(0, 4);
        updateMessage("Opening the file...");
        pause();

        updateProgress(1, 4);
        updateMessage("Reading the contents...");
        pause();

        updateProgress(2, 4);
        updateMessage("Checking the contents...");
        int events = engine.loadFile(path);

        updateProgress(3, 4);
        updateMessage("Building the system...");
        pause();

        updateProgress(4, 4);
        updateMessage("Done");
        return events;
    }

    private void pause() throws InterruptedException {
        Thread.sleep(STEP_PAUSE_MILLIS);
    }
}
