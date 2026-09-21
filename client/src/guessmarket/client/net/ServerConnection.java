package guessmarket.client.net;

import guessmarket.engine.api.EngineException;
import guessmarket.engine.api.LoadException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The plumbing between this client and the server. One per running client.
 *
 * Everything above this class talks about events and users; this is the only
 * place that knows about URLs, parameters and status codes.
 *
 * Two things here are worth understanding before anything else:
 *
 * 1. The cookie manager. The server decides who you are from the session, and
 *    the session travels in a cookie. An HTTP client that does not keep
 *    cookies gets a brand new session on every call, so the login appears to
 *    work and then every other call answers "Log in first." That one line in
 *    the constructor is what makes the whole identity scheme work.
 *
 * 2. The version. Every answer carries the market's version in a header. A
 *    caller that already holds the current version sends it back and the
 *    server replies 204 with nothing in it, which is what most polls get.
 */
public class ServerConnection {

    /**
     * Where the server is. The exercise says a client may assume localhost and
     * the war name it was submitted with. It can still be overridden from the
     * command line with -Dguessmarket.server=... without rebuilding.
     */
    private static final String DEFAULT_BASE = "http://localhost:8080/GuessMarket";

    private final String base;
    private final HttpClient http;

    /** The newest version any answer has carried. */
    private volatile int version = 0;

    public ServerConnection() {
        this(System.getProperty("guessmarket.server", DEFAULT_BASE));
    }

    public ServerConnection(String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;

        CookieManager cookies = new CookieManager();
        cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        this.http = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public int version() {
        return version;
    }

    /** Where this client is talking to, for anything that wants to show it. */
    public String base() {
        return base;
    }

    /** The address assumed when nothing overrides it. */
    public static String defaultBase() {
        return DEFAULT_BASE;
    }

    // ---------- the three shapes of request this client makes ----------

    /** A GET that always wants an answer. */
    public String get(String path, Map<String, Object> params) {
        return send(request(path, params).GET().build()).body();
    }

    /**
     * A GET that is happy to be told nothing has changed.
     *
     * @param since the version the caller already holds, or null to ask for
     *              the answer whatever the version
     * @return the body, or null when the server said 204 because the caller is
     *         already up to date
     */
    public String getIfChanged(String path, Map<String, Object> params, Integer since) {
        Map<String, Object> all = new LinkedHashMap<>(params);
        if (since != null) {
            all.put("since", since);
        }
        HttpResponse<String> response = send(request(path, all).GET().build());
        return response.statusCode() == 204 ? null : response.body();
    }

    /** A POST whose parameters go in the query string, as the servlets read them. */
    public String post(String path, Map<String, Object> params) {
        return send(request(path, params)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build()).body();
    }

    /**
     * Sends a file the way the exercise asks for: one POST, multipart, with the
     * file in a part the servlet reads by name.
     *
     * The body is built here rather than taken from a library so the client
     * needs no jar of its own. The shape is the one the HTTP standard
     * describes: a boundary that appears in no part, each part introduced by
     * its headers and a blank line, and a final boundary with two extra
     * dashes.
     */
    public String postFile(String path, String partName, File file) {
        try {
            return postMultipart(path, partName, file.getName(), Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new EngineException("The file could not be read: " + e.getMessage(), e);
        }
    }

    public String postMultipart(String path, String partName, String fileName, byte[] content) {
        String boundary = "GuessMarketBoundary" + System.nanoTime();
        byte[] body = multipartBody(boundary, partName, fileName, content);

        HttpRequest request = request(path, Map.of())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return send(request).body();
    }

    static byte[] multipartBody(String boundary, String partName, String fileName, byte[] content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"" + partName
                    + "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            out.write("Content-Type: text/xml\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(content);
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new EngineException("The upload could not be prepared: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    public static byte[] readAll(InputStream in) {
        try {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new EngineException("The file could not be read: " + e.getMessage(), e);
        }
    }

    // ---------- sending, and what a failure means ----------

    private HttpRequest.Builder request(String path, Map<String, Object> params) {
        return HttpRequest.newBuilder(URI.create(base + path + query(params)))
                .timeout(Duration.ofSeconds(30));
    }

    private String query(Map<String, Object> params) {
        if (params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("?");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (sb.length() > 1) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private HttpResponse<String> send(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // A refused connection has no message of its own, so none is added.
            String detail = e.getMessage() == null ? "" : " (" + e.getMessage() + ")";
            throw new ServerUnreachableException("The server could not be reached at " + base
                    + ". Check that Tomcat is running." + detail, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EngineException("The request was interrupted.", e);
        }

        rememberVersion(response);

        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response;
        }
        if (status == 401) {
            String body = response.body() == null ? "" : response.body().strip();
            throw new SessionLostException(body.isEmpty() ? "Log in first." : body);
        }
        throw asException(response.body());
    }

    private void rememberVersion(HttpResponse<String> response) {
        response.headers().firstValue("X-Market-Version").ifPresent(value -> {
            try {
                version = Integer.parseInt(value.trim());
            } catch (NumberFormatException ignored) {
                // A server that did not send a usable version leaves the old
                // one in place, which only costs a full answer next time.
            }
        });
    }

    /**
     * The server sends a failure as plain text: the sentence first, then one
     * line per problem when a file was rejected. That is rebuilt here into the
     * same LoadException the screens already know how to display.
     */
    private EngineException asException(String body) {
        String text = body == null ? "" : body.strip();
        if (text.isEmpty()) {
            return new EngineException("The server refused the request without saying why.");
        }
        List<String> lines = new ArrayList<>(List.of(text.split("\\R")));
        String first = lines.remove(0);
        if (lines.isEmpty()) {
            return new EngineException(first);
        }
        return new LoadException(first, lines);
    }
}
