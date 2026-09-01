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

public class SystemStateStore {

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
