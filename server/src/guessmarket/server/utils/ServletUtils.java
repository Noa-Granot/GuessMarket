package guessmarket.server.utils;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * The small things every servlet needs: one Gson, and a tidy way to answer.
 *
 * Gson is thread safe once built, so one instance is shared rather than a new
 * one per request.
 */
public class ServletUtils {

    private static final Gson GSON = new Gson();

    private ServletUtils() {
    }

    public static Gson gson() {
        return GSON;
    }

    /** Answers with the object as JSON and a 200. */
    public static void writeJson(HttpServletResponse response, Object body) throws IOException {
        response.setContentType(Constants.JSON);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter out = response.getWriter()) {
            out.print(GSON.toJson(body));
            out.flush();
        }
    }

    /**
     * Answers with a plain sentence and an error code.
     *
     * The message is meant to be shown to the person as it is, so it says what
     * went wrong rather than naming an exception.
     */
    public static void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setContentType(Constants.TEXT);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        try (PrintWriter out = response.getWriter()) {
            out.print(message);
            out.flush();
        }
    }

    public static void writeText(HttpServletResponse response, String message) throws IOException {
        response.setContentType(Constants.TEXT);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter out = response.getWriter()) {
            out.print(message);
            out.flush();
        }
    }
}
