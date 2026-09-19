package guessmarket.server.servlets;

import guessmarket.server.engine.ServerEngine;
import guessmarket.server.utils.Constants;
import guessmarket.server.utils.ServletUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Everything about one event: its options and prices, its order books, its
 * trades, who holds what, and its price history.
 *
 * This is the second thing the clients poll, once the person has an event
 * selected, so it takes the same version parameter as the events list.
 */
@WebServlet(name = "EventServlet", urlPatterns = "/event")
public class EventServlet extends GuessMarketServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        runGuarded(response, () -> {
            if (ServletUtils.isUpToDate(request)) {
                ServletUtils.writeNotModified(response);
                return;
            }
            int eventId = ServletUtils.intParam(request, Constants.EVENT_ID);
            ServletUtils.writeJson(response, ServerEngine.get().eventState(eventId));
        });
    }
}
