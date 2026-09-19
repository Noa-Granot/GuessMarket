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
 * Putting money into your own account.
 *
 * Whose account is taken from the session rather than from a parameter, so
 * nobody can top up somebody else's balance.
 */
@WebServlet(name = "FundsServlet", urlPatterns = "/funds")
public class FundsServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String username = SessionUtils.getUsername(request);
        if (username == null) {
            ServletUtils.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Log in first.");
            return;
        }

        String raw = request.getParameter(Constants.AMOUNT);
        double amount;
        try {
            amount = Double.parseDouble(raw == null ? "" : raw.trim());
        } catch (NumberFormatException e) {
            ServletUtils.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "The amount has to be a number.");
            return;
        }

        try {
            double balance = ServerEngine.get().addFunds(username, amount);
            // Other people see this balance in the users list, so the market
            // has changed as far as their polling is concerned.
            ServerEngine.bumpVersion();
            ServletUtils.writeJson(response, balance);
        } catch (EngineException e) {
            ServletUtils.writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
}
