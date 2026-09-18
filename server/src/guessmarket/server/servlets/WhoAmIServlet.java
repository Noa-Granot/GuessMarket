package guessmarket.server.servlets;

import guessmarket.server.utils.ServletUtils;
import guessmarket.server.utils.SessionUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Who the server thinks this client is.
 *
 * Useful from Postman to see whether a session survived, and the client uses
 * it to decide whether to show the login screen.
 */
@WebServlet(name = "WhoAmIServlet", urlPatterns = "/whoami")
public class WhoAmIServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String username = SessionUtils.getUsername(request);
        if (username == null) {
            ServletUtils.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Not logged in.");
            return;
        }
        ServletUtils.writeText(response, username);
    }
}
