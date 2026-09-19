package guessmarket.server.servlets;

import guessmarket.engine.api.EventDto;
import guessmarket.server.engine.ServerEngine;
import guessmarket.server.utils.ServletUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Every event in the market, which everybody is allowed to see.
 *
 * The clients poll this, so it answers in one of two ways. A client that sends
 * the version it already holds and is still current gets 204 and an empty
 * body; anyone else gets the whole list. Either answer carries the current
 * version in the X-Market-Version header, which is what the client sends back
 * next time.
 */
@WebServlet(name = "EventsServlet", urlPatterns = "/events")
public class EventsServlet extends GuessMarketServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        runGuarded(response, () -> {
            if (ServletUtils.isUpToDate(request)) {
                ServletUtils.writeNotModified(response);
                return;
            }
            List<EventDto> events = ServerEngine.get().listEvents();
            ServletUtils.writeJson(response, events);
        });
    }
}
