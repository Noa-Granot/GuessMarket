package guessmarket.server.servlets;

import guessmarket.server.engine.ServerEngine;
import guessmarket.server.utils.Constants;
import guessmarket.server.utils.ServletUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Submitting an order to an order book event.
 *
 * Whatever matches is matched at once and whatever is left rests in the book,
 * so one call can change several people's holdings. That is why the version is
 * bumped here even when the order rests untouched: the book itself changed.
 */
@WebServlet(name = "OrderServlet", urlPatterns = "/order")
public class OrderServlet extends GuessMarketServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        runAsUser(request, response, username -> {
            int eventId = ServletUtils.intParam(request, Constants.EVENT_ID);
            int optionIndex = ServletUtils.intParam(request, Constants.OPTION_INDEX);
            long quantity = ServletUtils.longParam(request, Constants.QUANTITY);
            double price = ServletUtils.doubleParam(request, Constants.PRICE);

            String side = request.getParameter(Constants.SIDE);
            if (side == null || side.isBlank()) {
                throw new ServletUtils.BadRequestException(
                        "The request is missing " + Constants.SIDE + ". It must be Buy or Sell.");
            }

            Object receipt = ServerEngine.get()
                    .placeOrder(eventId, username, optionIndex, side.trim(), quantity, price);
            ServerEngine.bumpVersion();
            ServletUtils.writeJson(response, receipt);
        });
    }
}
