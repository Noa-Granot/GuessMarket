package guessmarket.server.servlets;

import guessmarket.engine.api.EngineException;
import guessmarket.server.engine.ServerEngine;
import guessmarket.server.utils.Constants;
import guessmarket.server.utils.ServletUtils;
import guessmarket.server.utils.SessionUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Logging in, which is only a name.
 *
 * The exercise says not to add passwords or a sign up, so this registers the
 * name and remembers it in the session. A name already in use is refused with
 * a sentence the client can show as it is, and the person tries again.
 */
@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handle(request, response);
    }

    /** GET is allowed too, so the whole thing can be tried from a browser bar. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handle(request, response);
    }

    private void handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String alreadyLoggedIn = SessionUtils.getUsername(request);
        if (alreadyLoggedIn != null) {
            ServletUtils.writeText(response, alreadyLoggedIn);
            return;
        }

        String raw = request.getParameter(Constants.USERNAME);
        if (raw == null || raw.isBlank()) {
            ServletUtils.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Please enter a user name.");
            return;
        }

        String username = raw.trim();
        try {
            // registerUser is synchronised in the engine, so two people sending
            // the same name at the same moment cannot both get in.
            ServerEngine.get().registerUser(username);
        } catch (EngineException e) {
            ServletUtils.writeError(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
            return;
        }

        SessionUtils.setUsername(request, username);
        ServletUtils.writeText(response, username);
    }
}
