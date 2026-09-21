package guessmarket.client.fx;

import guessmarket.client.net.HttpGuessMarketEngine;
import guessmarket.client.net.ServerConnection;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * The exercise 3 client.
 *
 * One running copy of this program is one person. It holds one
 * HttpGuessMarketEngine, and that engine holds one session cookie, which is
 * what makes two clients on the same machine two different people rather than
 * the same one twice.
 *
 * The window shows the login screen first and swaps to the main screen once the
 * server has accepted a name. Both screens are built here in Java rather than
 * loaded from FXML, because they are small; the events screen will reuse the
 * FXML from exercise 2.
 */
public class GuessMarketClientApp extends Application {

    private static final String TITLE = "Guess Market";

    private final HttpGuessMarketEngine engine = new HttpGuessMarketEngine();

    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        stage.setTitle(TITLE);
        stage.setMinWidth(420);
        stage.setMinHeight(320);

        showLogin();
        stage.show();
    }

    /** The login screen, and what to do once a name has been accepted. */
    private void showLogin() {
        showLogin(null);
    }

    /** The login screen with a sentence saying why the person is back on it. */
    private void showLogin(String reason) {
        LoginPane login = new LoginPane(engine, this::showMarket);
        if (reason != null) {
            login.explain(reason);
        }
        setScene(login, 460, 460);
        stage.setTitle(TITLE);
    }

    /** The main screen, and what to do when the person logs out. */
    private void showMarket(String userName) {
        ClientShell shell = new ClientShell(engine, userName, this::showLogin, this::showLogin);
        setScene(shell, 1100, 700);
        stage.setTitle(TITLE + " - " + userName);
        stage.centerOnScreen();
    }

    private void setScene(javafx.scene.Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        URL css = getClass().getResource("client.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
    }

    /**
     * The server is left running when a client closes, which is what the
     * exercise asks for: the user stays in the market and the name stays taken.
     */
    @Override
    public void stop() {
        // Nothing to close. The connection holds no sockets of its own between
        // requests, and the session is the server's to forget.
    }

    public static void main(String[] args) {
        launch(args);
    }

    /** Where this client looks for the server, for the line on the login screen. */
    static String serverAddress() {
        return System.getProperty("guessmarket.server", ServerConnection.defaultBase());
    }
}
