package guessmarket.server.servlets;

import guessmarket.server.engine.ServerEngine;
import guessmarket.server.utils.Constants;
import guessmarket.server.utils.ServletUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;

/**
 * Uploading a file of events.
 *
 * The content is read straight out of the request and handed to the engine as
 * a stream. It is never written anywhere. The exercise says twice that the
 * marker's Tomcat has no permission to write and will crash if we try.
 *
 * fileSizeThreshold is set well above the size of these files on purpose:
 * below it Tomcat keeps the part in memory, above it Tomcat spills the part to
 * a temporary file of its own. The files in this exercise are a few kilobytes,
 * so they stay in memory and nothing reaches a disk.
 *
 * Files accumulate. Each upload adds its events to the market rather than
 * replacing what is there, and whoever uploaded becomes the market maker of
 * every event in their file.
 */
@WebServlet(name = "UploadServlet", urlPatterns = "/upload")
@MultipartConfig(fileSizeThreshold = 10 * 1024 * 1024,
                 maxFileSize = 10 * 1024 * 1024,
                 maxRequestSize = 10 * 1024 * 1024)
public class UploadServlet extends GuessMarketServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        runAsUser(request, response, username -> {
            try (InputStream content = contentOf(request)) {
                int added = ServerEngine.get().uploadFile(content, username);
                ServerEngine.bumpVersion();
                ServletUtils.writeText(response, added + (added == 1 ? " event" : " events")
                        + " added to the market.");
            } catch (ServletException e) {
                throw new ServletUtils.BadRequestException(
                        "The upload did not arrive as a file. " + e.getMessage());
            }
        });
    }

    /**
     * The file as the client sent it.
     *
     * A real client posts multipart, which is what the exercise asks for. A raw
     * body is accepted as well so that the endpoint can be tried from Postman
     * by pasting XML, without building a form.
     */
    private InputStream contentOf(HttpServletRequest request) throws IOException, ServletException {
        String type = request.getContentType();
        if (type != null && type.toLowerCase().startsWith("multipart/")) {
            Part part = request.getPart(Constants.FILE_PART);
            if (part == null) {
                throw new ServletUtils.BadRequestException(
                        "The upload has no part called \"" + Constants.FILE_PART + "\".");
            }
            return part.getInputStream();
        }
        return request.getInputStream();
    }
}
