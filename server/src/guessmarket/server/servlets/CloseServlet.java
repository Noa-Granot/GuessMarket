package guessmarket.server.servlets;

import guessmarket.server.engine.ServerEngine;
import guessmarket.server.utils.Constants;
import guessmarket.server.utils.ServletUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Closing an event and deciding which option won.
 *
 * Only the market maker may do this. Everybody holding the winning option is
 * paid, so this is the call that changes the most accounts at once.
 */
@WebServlet(name = "CloseServlet", urlPatterns = "/close")
public class CloseServlet extends GuessMarketServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        runAsUser(request, response, username -> {
            int eventId = ServletUtils.intParam(request, Constants.EVENT_ID);
            int winningOptionIndex = ServletUtils.intParam(request, Constants.OPTION_INDEX);
            Object receipt = ServerEngine.get().closeEvent(eventId, username, winningOptionIndex);
            ServerEngine.bumpVersion();
            ServletUtils.writeJson(response, receipt);
        });
    }
}
