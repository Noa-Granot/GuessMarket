package guessmarket.server.servlets;

import guessmarket.server.engine.ServerEngine;
import guessmarket.server.utils.Constants;
import guessmarket.server.utils.ServletUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Opening an event for trading.
 *
 * Only its market maker may do this and only if he can pay the opening cost.
 * Both rules live in the engine; the servlet only says who is asking.
 */
@WebServlet(name = "OpenServlet", urlPatterns = "/open")
public class OpenServlet extends GuessMarketServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        runAsUser(request, response, username -> {
            int eventId = ServletUtils.intParam(request, Constants.EVENT_ID);
            Object receipt = ServerEngine.get().openEvent(eventId, username);
            ServerEngine.bumpVersion();
            ServletUtils.writeJson(response, receipt);
        });
    }
}
