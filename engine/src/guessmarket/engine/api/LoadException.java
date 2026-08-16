package guessmarket.engine.api;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when a file cannot be loaded. Carries every problem found rather than
 * only the first, because a user fixing a broken file would otherwise have to
 * reload once per mistake.
 *
 * The UI catches this specifically so it can print the list; anything that only
 * catches EngineException still gets a sensible single-line message.
 */
public class LoadException extends EngineException {

    private final List<String> problems;

    public LoadException(String message, List<String> problems) {
        super(message);
        this.problems = List.copyOf(problems);
    }

    public LoadException(String message) {
        super(message);
        this.problems = Collections.emptyList();
    }

    public LoadException(String message, Throwable cause) {
        super(message, cause);
        this.problems = Collections.emptyList();
    }

    public List<String> getProblems() {
        return problems;
    }
}
