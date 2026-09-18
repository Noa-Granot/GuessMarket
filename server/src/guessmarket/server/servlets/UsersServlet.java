package guessmarket.server.servlets;

import guessmarket.engine.api.UserDto;
import guessmarket.server.engine.ServerEngine;
import guessmarket.server.utils.ServletUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Everyone in the system, which each person is allowed to see: their name,
 * their balance, and whether they are a market maker.
 *
 * This is one of the calls the clients poll, so it stays cheap.
 */
@WebServlet(name = "UsersServlet", urlPatterns = "/users")
public class UsersServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        List<UserDto> users = ServerEngine.get().listUsers();
        ServletUtils.writeJson(response, users);
    }
}
