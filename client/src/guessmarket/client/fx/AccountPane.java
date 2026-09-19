package guessmarket.client.fx;

import guessmarket.client.net.HttpGuessMarketEngine;
import guessmarket.engine.api.EngineException;
import guessmarket.engine.api.LoadException;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * The account screen, as far as it goes today: the load file button and the
 * path, which the sketch puts at the top of this screen.
 *
 * Uploading is the one action that changes the market for everybody, so it is
 * worth understanding what happens here. The file is read by the server, not
 * by this client: the path below is only shown so the person can see which
 * file they picked. The server accumulates it onto whatever is already there
 * and makes this user the market maker of every event in it.
 *
 * A refused file comes back as a LoadException carrying one line per problem,
 * exactly as it did in exercise 2 when the engine was in this process.
 */
public class AccountPane extends VBox {

    private final HttpGuessMarketEngine engine;
    private final Runnable onMarketChanged;

    private final Button loadButton = new Button("Load file");
    private final Label pathLabel = new Label("No file chosen");
    private final Label messageLabel = new Label();

    public AccountPane(HttpGuessMarketEngine engine, Runnable onMarketChanged) {
        this.engine = engine;
        this.onMarketChanged = onMarketChanged == null ? () -> { } : onMarketChanged;

        getStyleClass().add("account");
        setSpacing(10);
        setPadding(new Insets(14));

        loadButton.getStyleClass().add("load-button");
        loadButton.setOnAction(event -> chooseAndUpload());

        pathLabel.getStyleClass().add("path-label");
        pathLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pathLabel, javafx.scene.layout.Priority.ALWAYS);

        HBox loadBar = new HBox(10, loadButton, pathLabel);
        loadBar.setAlignment(Pos.CENTER_LEFT);
        loadBar.getStyleClass().add("load-bar");

        messageLabel.setWrapText(true);
        messageLabel.setMinHeight(Region.USE_PREF_SIZE);
        messageLabel.getStyleClass().add("load-message");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        Label todo = new Label("Your balance, the line for every movement in it, "
                + "the other users and loading funds go here.");
        todo.getStyleClass().add("placeholder-detail");
        todo.setWrapText(true);
        todo.setMinHeight(Region.USE_PREF_SIZE);

        getChildren().addAll(loadBar, messageLabel, todo);
    }

    private void chooseAndUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose an exercise 3 events file");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML files", "*.xml"));

        File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }

        pathLabel.setText(file.getAbsolutePath());
        upload(file);
    }

    private void upload(File file) {
        loadButton.setDisable(true);
        showMessage("Sending " + file.getName() + " to the server...", false);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return engine.upload(file);
            }
        };

        task.setOnSucceeded(event -> {
            loadButton.setDisable(false);
            showMessage(task.getValue(), false);
            // Everyone's events changed, starting with this client's own screen.
            onMarketChanged.run();
        });

        task.setOnFailed(event -> {
            loadButton.setDisable(false);
            showMessage(describe(task.getException()), true);
        });

        Thread worker = new Thread(task, "upload");
        worker.setDaemon(true);
        worker.start();
    }

    /** A rejected file has a sentence and then one line per problem. */
    private String describe(Throwable cause) {
        if (cause instanceof LoadException load) {
            StringBuilder text = new StringBuilder(load.getMessage());
            for (String problem : load.getProblems()) {
                text.append(System.lineSeparator()).append("- ").append(problem);
            }
            return text.toString();
        }
        if (cause instanceof EngineException && cause.getMessage() != null) {
            return cause.getMessage();
        }
        return cause == null ? "The upload failed." : String.valueOf(cause);
    }

    private void showMessage(String text, boolean bad) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().remove("load-message-bad");
        if (bad) {
            messageLabel.getStyleClass().add("load-message-bad");
        }
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}
