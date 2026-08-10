package guessmarket.engine.api;

/**
 * The single exception type the engine throws outward. The UI catches this one
 * type and prints its message, so the engine controls the wording of every
 * error the user sees while the UI controls how it is displayed.
 */
public class EngineException extends RuntimeException {

    public EngineException(String message) {
        super(message);
    }

    public EngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
