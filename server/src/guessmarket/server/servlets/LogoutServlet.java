package guessmarket.server.servlets;

import guessmarket.server.utils.ServletUtils;
import guessmarket.server.utils.SessionUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Forgets who this session was.
 *
 * The user stays in the market with their balance and holdings, because the
 * exercise has no notion of removing a user. This only ends the session.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = "/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        SessionUtils.clear(request);
        ServletUtils.writeText(response, "logged out");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        doGet(request, response);
    }
}
