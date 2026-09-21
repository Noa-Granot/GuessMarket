package guessmarket.client.net;

import guessmarket.engine.api.EngineException;

/**
 * Nothing answered at all: Tomcat is down, or the address is wrong.
 *
 * Kept apart from an ordinary EngineException so the window can tell "the
 * server said no" from "there is no server", and say so in a banner instead of
 * in whichever small label the failed request happened to belong to.
 */
public class ServerUnreachableException extends EngineException {

    public ServerUnreachableException(String message, Throwable cause) {
        super(message, cause);
    }
}
