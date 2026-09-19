package guessmarket.client.fx;

import guessmarket.client.net.HttpGuessMarketEngine;
import guessmarket.engine.api.EngineException;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * The login screen: a name, and nothing else.
 *
 * The exercise says explicitly not to add passwords or a sign up, so this asks
 * for a name and hands it to the server. A name already in use comes back as a
 * refusal with a sentence, which is shown as it is and the person tries again.
 *
 * The call to the server runs on a Task, off the JavaFX thread. A click must
 * never wait for the network on the thread that draws the window, because a
 * server that is slow or missing would freeze the whole program rather than
 * just this button. Only the two handlers touch the controls, and those run
 * back on the JavaFX thread.
 */
public class LoginPane extends VBox {

    private final HttpGuessMarketEngine engine;
    private final Consumer<String> onLoggedIn;

    private final TextField nameField = new TextField();
    private final Button loginButton = new Button("Enter the market");
    private final Label errorLabel = new Label();
    private final ProgressIndicator spinner = new ProgressIndicator();

    public LoginPane(HttpGuessMarketEngine engine, Consumer<String> onLoggedIn) {
        this.engine = engine;
        this.onLoggedIn = onLoggedIn;

        getStyleClass().add("login");
        setSpacing(14);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(40, 48, 44, 48));

        Label title = new Label("Guess Market");
        title.getStyleClass().add("login-title");

        Label subtitle = new Label("Choose a name. Everyone in the market must have a different one.");
        subtitle.getStyleClass().add("login-subtitle");
        subtitle.setWrapText(true);
        // Without this a wrapping label is allowed to shrink when the column
        // runs out of room, and a shrunk wrapping label truncates to one line
        // with an ellipsis instead of wrapping.
        subtitle.setMinHeight(Region.USE_PREF_SIZE);

        nameField.setPromptText("Your name");
        nameField.getStyleClass().add("login-field");
        // Enter does the same as the button, because typing a name and
        // reaching for the mouse is a small insult.
        nameField.setOnAction(event -> attemptLogin());

        loginButton.setDefaultButton(true);
        loginButton.getStyleClass().add("login-button");
        loginButton.setOnAction(event -> attemptLogin());

        errorLabel.getStyleClass().add("login-error");
        errorLabel.setWrapText(true);
        errorLabel.setMinHeight(Region.USE_PREF_SIZE);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        spinner.setPrefSize(18, 18);
        spinner.setVisible(false);
        spinner.setManaged(false);

        Label address = new Label(GuessMarketClientApp.serverAddress());
        address.getStyleClass().add("login-address");

        getChildren().addAll(title, subtitle, nameField, loginButton, spinner, errorLabel, address);

        // The window is not on screen yet while this constructor runs, so the
        // focus is asked for once the current pulse is over and there is
        // something to focus. The person can then just type.
        Platform.runLater(nameField::requestFocus);
    }

    private void attemptLogin() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Enter a name first.");
            return;
        }

        setBusy(true);

        Task<String> login = new Task<>() {
            @Override
            protected String call() {
                // Throws EngineException with the server's own sentence when
                // the name is taken or the server cannot be reached.
                engine.registerUser(name);
                return name;
            }
        };

        login.setOnSucceeded(event -> {
            setBusy(false);
            onLoggedIn.accept(login.getValue());
        });

        login.setOnFailed(event -> {
            setBusy(false);
            Throwable cause = login.getException();
            showError(messageFor(cause));
            nameField.selectAll();
            nameField.requestFocus();
        });

        Thread worker = new Thread(login, "login");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * An EngineException already carries a sentence meant to be read by the
     * person. Anything else is a surprise and says so rather than pretending.
     */
    private String messageFor(Throwable cause) {
        if (cause instanceof EngineException && cause.getMessage() != null) {
            return cause.getMessage();
        }
        if (cause == null) {
            return "The login did not go through, and no reason came back.";
        }
        return "Something went wrong talking to the server: " + cause;
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        nameField.setDisable(busy);
        spinner.setVisible(busy);
        spinner.setManaged(busy);
        if (busy) {
            hideError();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
