package guessmarket.server.servlets;

import guessmarket.server.engine.ServerEngine;
import guessmarket.server.utils.Constants;
import guessmarket.server.utils.ServletUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * What buying a number of shares would cost, before anyone commits to it.
 *
 * Nothing changes here, which is why it is a GET and why it does not touch the
 * version. The buy dialog calls it as the person types.
 */
@WebServlet(name = "QuoteServlet", urlPatterns = "/quote")
public class QuoteServlet extends GuessMarketServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        runGuarded(response, () -> {
            int eventId = ServletUtils.intParam(request, Constants.EVENT_ID);
            int optionIndex = ServletUtils.intParam(request, Constants.OPTION_INDEX);
            long quantity = ServletUtils.longParam(request, Constants.QUANTITY);
            ServletUtils.writeJson(response,
                    ServerEngine.get().quote(eventId, optionIndex, quantity));
        });
    }
}
