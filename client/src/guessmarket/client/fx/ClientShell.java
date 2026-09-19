package guessmarket.client.fx;

import guessmarket.client.net.HttpGuessMarketEngine;

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

import java.io.IOException;
import java.net.URL;

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
public class ClientShell extends BorderPane {

    private final HttpGuessMarketEngine engine;
    private final String userName;
    private final Runnable onLoggedOut;

    private EventsController eventsController;

    public ClientShell(HttpGuessMarketEngine engine, String userName, Runnable onLoggedOut) {
        this.engine = engine;
        this.userName = userName;
        this.onLoggedOut = onLoggedOut;

        getStyleClass().add("shell");
        setTop(buildHeader());
        setCenter(buildTabs());
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
        Tab account = new Tab("Account", new AccountPane(engine, this::refreshEvents));
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
