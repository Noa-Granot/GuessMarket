package guessmarket.server.utils;

import com.google.gson.Gson;
import guessmarket.engine.api.EngineException;
import guessmarket.engine.api.LoadException;
import guessmarket.server.engine.ServerEngine;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * The small things every servlet needs: one Gson, a tidy way to answer, and
 * one place where an engine failure turns into an HTTP answer.
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
        response.setHeader(Constants.VERSION_HEADER, String.valueOf(ServerEngine.version()));
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
        response.setHeader(Constants.VERSION_HEADER, String.valueOf(ServerEngine.version()));
        response.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter out = response.getWriter()) {
            out.print(message);
            out.flush();
        }
    }

    /**
     * Tells a polling client that it already has the current version. The body
     * is empty on purpose: this is the answer most polls get, and it is the
     * whole point of sending the version at all.
     */
    public static void writeNotModified(HttpServletResponse response) {
        response.setHeader(Constants.VERSION_HEADER, String.valueOf(ServerEngine.version()));
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    /**
     * True when the client says it already holds the current version, so the
     * servlet can answer 204 instead of sending everything again.
     */
    public static boolean isUpToDate(HttpServletRequest request) {
        String raw = request.getParameter(Constants.SINCE);
        if (raw == null || raw.isBlank()) {
            return false;
        }
        try {
            return Integer.parseInt(raw.trim()) == ServerEngine.version();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * One place where an engine failure becomes an HTTP answer, so every
     * endpoint reports the same way.
     *
     * A LoadException carries a problem per line, because that is what the
     * person has to fix and the client shows it as it is.
     */
    public static void writeEngineError(HttpServletResponse response, EngineException e)
            throws IOException {
        StringBuilder message = new StringBuilder(e.getMessage() == null ? "" : e.getMessage());
        if (e instanceof LoadException load) {
            for (String problem : load.getProblems()) {
                message.append(System.lineSeparator()).append(problem);
            }
        }
        writeError(response, HttpServletResponse.SC_BAD_REQUEST, message.toString());
    }

    /**
     * Reads a whole number parameter.
     *
     * @throws BadRequestException with a sentence naming the parameter, which
     *                             the servlet turns into a 400.
     */
    public static int intParam(HttpServletRequest request, String name) {
        String raw = request.getParameter(name);
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("The request is missing " + name + ".");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("The " + name + " has to be a whole number.");
        }
    }

    public static long longParam(HttpServletRequest request, String name) {
        String raw = request.getParameter(name);
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("The request is missing " + name + ".");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("The " + name + " has to be a whole number.");
        }
    }

    public static double doubleParam(HttpServletRequest request, String name) {
        String raw = request.getParameter(name);
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("The request is missing " + name + ".");
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("The " + name + " has to be a number.");
        }
    }

    /** A parameter that is missing or not a number. Becomes a 400. */
    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }
}
