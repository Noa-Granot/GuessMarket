package guessmarket.server.servlets;

import guessmarket.server.engine.ServerEngine;
import guessmarket.server.utils.ServletUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The account screen: this person's balance, the events they are in or run,
 * and their balance over time.
 *
 * Whose account is taken from the session, so this endpoint can only ever
 * describe the person asking. What everyone is allowed to see about everyone
 * else is a shorter list, and that is /users.
 */
@WebServlet(name = "UserStateServlet", urlPatterns = "/user-state")
public class UserStateServlet extends GuessMarketServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        runAsUser(request, response, username -> {
            if (ServletUtils.isUpToDate(request)) {
                ServletUtils.writeNotModified(response);
                return;
            }
            ServletUtils.writeJson(response, ServerEngine.get().userState(username));
        });
    }
}
