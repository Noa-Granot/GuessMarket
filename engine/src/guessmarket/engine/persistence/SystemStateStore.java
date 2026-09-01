package guessmarket.engine.persistence;

import guessmarket.engine.api.EngineException;
import guessmarket.engine.model.MarketSystem;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

/**
 * BONUS: saves and restores the whole system, trade history included, so a
 * session can be picked up later.
 *
 * Java serialisation is used rather than a text format, because every class in
 * the model is already a plain object graph hanging off MarketSystem -- writing
 * it takes one call, and nothing has to be kept in step with a hand-written
 * format as the model grows in the next exercises.
 *
 * The exercise asks the user for a path WITHOUT an extension, so this class owns
 * the extension and appends it on the way in and out. The user never types it.
 */
public class SystemStateStore {

    /** Chosen here and nowhere else, so it can be changed in one line. */
    private static final String EXTENSION = ".gm";

    public void save(String pathWithoutExtension, MarketSystem system) {
        File file = resolve(pathWithoutExtension);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            throw new EngineException("There is no folder at: " + parent.getPath());
        }

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(system);
        } catch (IOException e) {
            throw new EngineException("The system could not be saved to " + file.getPath()
                    + ". " + reason(e), e);
        }
    }

    public MarketSystem load(String pathWithoutExtension) {
        File file = resolve(pathWithoutExtension);

        if (!file.exists()) {
            throw new EngineException("No saved system was found at: " + file.getPath());
        }
        if (file.isDirectory()) {
            throw new EngineException("That path is a folder, not a saved system: " + file.getPath());
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Object restored = in.readObject();
            if (!(restored instanceof MarketSystem system)) {
                throw new EngineException("The file at " + file.getPath()
                        + " is not a saved Guess Market system.");
            }
            return system;
        } catch (EOFException e) {
            throw new EngineException("The file at " + file.getPath()
                    + " is empty or incomplete, so it cannot be restored.", e);
        } catch (StreamCorruptedException e) {
            throw new EngineException("The file at " + file.getPath()
                    + " is not a saved Guess Market system, or has been damaged.", e);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new EngineException("The file at " + file.getPath()
                    + " was not written by this version of the program.", e);
        } catch (IOException e) {
            throw new EngineException("The saved system could not be read from " + file.getPath()
                    + ". " + reason(e), e);
        }
    }

    /** Appends the extension, and tolerates a user who typed it anyway. */
    private File resolve(String pathWithoutExtension) {
        if (pathWithoutExtension == null || pathWithoutExtension.isBlank()) {
            throw new EngineException("No path was entered.");
        }

        String path = pathWithoutExtension.trim();
        if (path.length() >= 2 && path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1).trim();
        }
        if (!path.toLowerCase().endsWith(EXTENSION)) {
            path = path + EXTENSION;
        }
        return new File(path);
    }

    private String reason(IOException e) {
        String message = e.getMessage();
        return (message == null || message.isBlank()) ? "" : "Details: " + message.trim();
    }
}
