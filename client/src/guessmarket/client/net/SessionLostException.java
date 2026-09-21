package guessmarket.client.net;

import guessmarket.engine.api.EngineException;

/**
 * The server answered 401: it does not know this client's session.
 *
 * While the client is logged in, that only happens when the server was
 * restarted, because a restarted server starts with an empty market and
 * nobody in it. The right answer is to go back to the login screen, not to
 * keep showing a market that no longer exists.
 */
public class SessionLostException extends EngineException {

    public SessionLostException(String message) {
        super(message);
    }
}
