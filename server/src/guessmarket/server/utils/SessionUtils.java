package guessmarket.server.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Who is making this request.
 *
 * The name is kept in the session so the client does not have to send it with
 * every call, and so one client cannot claim to be someone else just by
 * changing a parameter.
 */
public class SessionUtils {

    private SessionUtils() {
    }

    /** The logged in name, or null if this request has no session yet. */
    public static String getUsername(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object name = session.getAttribute(Constants.USERNAME_IN_SESSION);
        return name == null ? null : name.toString();
    }

    public static void setUsername(HttpServletRequest request, String username) {
        request.getSession(true).setAttribute(Constants.USERNAME_IN_SESSION, username);
    }

    public static void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
