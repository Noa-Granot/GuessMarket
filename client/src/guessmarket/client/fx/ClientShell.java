package guessmarket.client.fx;

import guessmarket.client.net.HttpGuessMarketEngine;
import guessmarket.client.net.ServerUnreachableException;
import guessmarket.client.net.SessionLostException;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

/**
 * What the window shows once somebody is logged in: the two screens from the
 * sketch, Events and Account, and a line saying who you are.
 *
 * Both tabs are empty on purpose. This class exists so that the login, the
 * session, the window and the logout can be proved to work before either screen
 * is written, and so the screens have somewhere to go when they are.
 *
 * There is no "acting as" control any more. In exercise 2 one window could
 * stand in for several people; here a person is whoever the session says they
 * are, and the only way to be somebody else is to be a different client.
 */
public class ClientShell extends BorderPane implements ConnectionWatch {

    private final HttpGuessMarketEngine engine;
    private final String userName;
    private final Runnable onLoggedOut;
    private final Consumer<String> onSessionLost;

    /**
     * The strip under the header that says the server is gone. Hidden while
     * the server answers; shown by the first poll that finds nobody there,
     * hidden again by the first one that gets through.
     */
    private final Label connectionBanner = new Label();

    /** Set once, so the two screens failing in the same tick leave only once. */
    private boolean leaving = false;

    private EventsController eventsController;
    private AccountPane accountPane;

    /**
     * The pull the exercise asks for.
     *
     * Once a second, both screens ask the server whether anything has changed.
     * Almost every tick the answer is 204 and nothing is redrawn, because the
     * engine hands back the copy it already had and the screens compare it by
     * identity. So a client that nobody is touching costs one empty request a
     * second, and a trade in another client appears here without anyone
     * clicking.
     */
    private static final double POLL_SECONDS = 1.0;
    private Timeline poll;

    public ClientShell(HttpGuessMarketEngine engine, String userName,
                       Runnable onLoggedOut, Consumer<String> onSessionLost) {
        this.engine = engine;
        this.userName = userName;
        this.onLoggedOut = onLoggedOut;
        this.onSessionLost = onSessionLost;

        getStyleClass().add("shell");
        setTop(new VBox(buildHeader(), buildBanner()));
        setCenter(buildTabs());
        if (eventsController != null) {
            eventsController.setConnectionWatch(this);
        }
        accountPane.setConnectionWatch(this);
        startPolling();
    }

    private Label buildBanner() {
        connectionBanner.getStyleClass().add("connection-banner");
        connectionBanner.setWrapText(true);
        connectionBanner.setMaxWidth(Double.MAX_VALUE);
        connectionBanner.setMinHeight(Region.USE_PREF_SIZE);
        connectionBanner.setPadding(new Insets(8, 14, 8, 14));
        connectionBanner.setVisible(false);
        connectionBanner.setManaged(false);
        return connectionBanner;
    }

    // ---------- what the polls report ----------

    @Override
    public void reached() {
        connectionBanner.setVisible(false);
        connectionBanner.setManaged(false);
    }

    /**
     * Only two failures are the window's business. Anything else is an
     * ordinary refusal and stays with the screen that asked.
     */
    @Override
    public void failed(Throwable cause) {
        if (cause instanceof SessionLostException) {
            sessionLost();
        } else if (cause instanceof ServerUnreachableException) {
            connectionBanner.setText("Lost contact with the server. "
                    + "Trying again every second; nothing you do will be sent until it answers.");
            connectionBanner.setVisible(true);
            connectionBanner.setManaged(true);
        }
    }

    /**
     * The server no longer knows this session. While logged in, that means it
     * was restarted, and a restarted server has an empty market with nobody
     * in it. Showing the old market would be showing something that is gone.
     */
    private void sessionLost() {
        if (leaving) {
            return;
        }
        leaving = true;
        if (poll != null) {
            poll.stop();
        }
        onSessionLost.accept("The server no longer knows you. It was most likely restarted, "
                + "and a restarted server starts with an empty market. Log in again.");
    }

    private HBox buildHeader() {
        Label title = new Label("Guess Market");
        title.getStyleClass().add("shell-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label who = new Label("Signed in as " + userName);
        who.getStyleClass().add("shell-who");

        Button logout = new Button("Log out");
        logout.getStyleClass().add("shell-logout");
        logout.setOnAction(event -> logout(logout));

        HBox header = new HBox(12, title, spacer, who, logout);
        header.getStyleClass().add("shell-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 14, 10, 14));
        return header;
    }

    private TabPane buildTabs() {
        Tab events = new Tab("Events", buildEventsScreen());
        events.setClosable(false);

        // Uploading changes what the events screen shows, so the two are
        // introduced to each other here rather than knowing about each other.
        accountPane = new AccountPane(engine, userName, this::refreshEvents);
        Tab account = new Tab("Account", accountPane);
        account.setClosable(false);

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(events, account);
        return tabs;
    }

    /**
     * Loads the events screen from its fxml and hands it the engine.
     *
     * If the fxml cannot be loaded the window still opens, with the reason on
     * screen, because a client that dies at startup tells you nothing about
     * why.
     */
    private javafx.scene.Node buildEventsScreen() {
        try {
            URL layout = getClass().getResource("events.fxml");
            if (layout == null) {
                return placeholder("The events screen could not be loaded.",
                        "events.fxml was not found next to EventsController. "
                                + "It has to be copied into the compiler output.");
            }
            FXMLLoader loader = new FXMLLoader(layout);
            javafx.scene.Node screen = loader.load();
            eventsController = loader.getController();
            eventsController.setEngine(engine);
            eventsController.setUserName(userName);
            eventsController.setOnMarketChanged(this::refreshAccount);
            // The first look at the market, before anybody touches anything.
            eventsController.refresh();
            return screen;
        } catch (IOException e) {
            return placeholder("The events screen could not be loaded.", String.valueOf(e));
        }
    }

    /** Called after anything that changes what the market looks like. */
    private void refreshEvents() {
        if (eventsController != null) {
            eventsController.refresh();
        }
    }

    /** Starts the pull. Called once the two screens exist. */
    private void startPolling() {
        poll = new Timeline(new KeyFrame(Duration.seconds(POLL_SECONDS), event -> {
            if (eventsController != null) {
                eventsController.pollTick();
            }
            if (accountPane != null) {
                accountPane.pollTick();
            }
        }));
        poll.setCycleCount(Animation.INDEFINITE);
        poll.play();
    }

    /** A trade changes the balance, so the account screen hears about it too. */
    private void refreshAccount() {
        if (accountPane != null) {
            accountPane.refresh();
        }
    }

    private StackPane placeholder(String heading, String detail) {
        Label headingLabel = new Label(heading);
        headingLabel.getStyleClass().add("placeholder-heading");

        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add("placeholder-detail");
        detailLabel.setWrapText(true);
        detailLabel.setMaxWidth(420);

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(8, headingLabel, detailLabel);
        box.setAlignment(Pos.CENTER);

        StackPane pane = new StackPane(box);
        pane.getStyleClass().add("placeholder");
        pane.setPadding(new Insets(40));
        return pane;
    }

    /**
     * Logging out ends the session but leaves the person in the market, which
     * is what the exercise asks for: their name stays taken and their balance
     * and holdings stay where they are.
     *
     * Like the login, this runs off the JavaFX thread. Unlike the login, a
     * failure is not worth stopping for — the session is going away from this
     * client's point of view either way.
     */
    private void logout(Button button) {
        button.setDisable(true);
        leaving = true;
        // Stop asking the server about a session that is about to end.
        if (poll != null) {
            poll.stop();
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                engine.logout();
                return null;
            }
        };
        task.setOnSucceeded(event -> onLoggedOut.run());
        task.setOnFailed(event -> onLoggedOut.run());

        Thread worker = new Thread(task, "logout");
        worker.setDaemon(true);
        worker.start();
    }
}
