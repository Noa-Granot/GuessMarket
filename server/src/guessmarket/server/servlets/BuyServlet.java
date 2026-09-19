package guessmarket.server.servlets;

import guessmarket.server.engine.ServerEngine;
import guessmarket.server.utils.Constants;
import guessmarket.server.utils.ServletUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Buying shares of an LMSR event.
 *
 * Who is buying comes from the session. A client that changed the parameter
 * would only be describing itself.
 */
@WebServlet(name = "BuyServlet", urlPatterns = "/buy")
public class BuyServlet extends GuessMarketServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        runAsUser(request, response, username -> {
            int eventId = ServletUtils.intParam(request, Constants.EVENT_ID);
            int optionIndex = ServletUtils.intParam(request, Constants.OPTION_INDEX);
            long quantity = ServletUtils.longParam(request, Constants.QUANTITY);
            Object receipt = ServerEngine.get().buy(eventId, username, optionIndex, quantity);
            ServerEngine.bumpVersion();
            ServletUtils.writeJson(response, receipt);
        });
    }
}
