package guessmarket.server.servlets;

import guessmarket.engine.api.EngineException;
import guessmarket.server.utils.ServletUtils;
import guessmarket.server.utils.SessionUtils;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * What every Guess Market endpoint does the same way.
 *
 * Three things were being repeated in every servlet: find out who is calling,
 * refuse the request if nobody is logged in, and turn an engine failure into a
 * sentence the client can show. They live here once.
 *
 * Identity always comes from the session and never from a parameter, so no
 * client can act as somebody else by editing a URL.
 */
public abstract class GuessMarketServlet extends HttpServlet {

    /** The body of an endpoint that needs a logged in user. */
    protected interface Action {
        void run(String username) throws IOException;
    }

    protected void runAsUser(HttpServletRequest request, HttpServletResponse response,
                             Action action) throws IOException {

        String username = SessionUtils.getUsername(request);
        if (username == null) {
            ServletUtils.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Log in first.");
            return;
        }
        runGuarded(response, () -> action.run(username));
    }

    /** The same failure handling, for an endpoint that anyone may call. */
    protected void runGuarded(HttpServletResponse response, IoAction action) throws IOException {
        try {
            action.run();
        } catch (ServletUtils.BadRequestException e) {
            ServletUtils.writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (EngineException e) {
            ServletUtils.writeEngineError(response, e);
        }
    }

    protected interface IoAction {
        void run() throws IOException;
    }
}
