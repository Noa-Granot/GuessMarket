package guessmarket.ui.fx;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.LoadException;
import guessmarket.engine.api.NewEventSpec;
import guessmarket.engine.api.UserDto;

import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/**
 * The top area of the window: choosing a file, loading it on a background
 * thread, and showing the progress. Also holds the two tabs.
 */
public class MainController {

    @FXML private Button loadButton;
    @FXML private ComboBox<String> actingAsBox;
    @FXML private Button createEventButton;
    @FXML private ComboBox<String> skinBox;
    @FXML private CheckBox animationsBox;
    @FXML private Label filePathLabel;
    @FXML private ProgressBar loadProgress;
    @FXML private Label loadMessage;
    @FXML private TabPane tabPane;
    @FXML private Tab eventsTab;
    @FXML private Tab usersTab;

    @FXML private EventsController eventsViewController;
    @FXML private UsersController usersViewController;

    private GuessMarketEngine engine;
    private Stage stage;

    @FXML
    private void initialize() {
        loadProgress.setProgress(0);
        loadMessage.setText("");
        filePathLabel.setText("No file loaded");
        actingAsBox.setDisable(true);
        actingAsBox.setPromptText("no users");
        actingAsBox.valueProperty().addListener((obs, was, now) -> {
            if (eventsViewController != null) {
                eventsViewController.setActingUser(now);
            }
        });
        createEventButton.setDisable(true);
        setUpSkins();
        setUpAnimations();
        setTabsEnabled(false);
    }

    /**
     * BONUS. Switching skin swaps one stylesheet for another. The default is
     * the plain one, so the program looks unchanged until the person chooses.
     */
    private void setUpSkins() {
        for (Skin skin : Skin.values()) {
            skinBox.getItems().add(skin.getDisplay());
        }
        skinBox.getSelectionModel().select(Skin.DEFAULT.getDisplay());
        skinBox.valueProperty().addListener((obs, was, now) -> applySkin(Skin.byDisplay(now)));
    }

    private void applySkin(Skin skin) {
        if (stage == null || stage.getScene() == null) {
            return;
        }
        stage.getScene().getStylesheets().clear();
        java.net.URL sheet = getClass().getResource(skin.getStylesheet());
        if (sheet != null) {
            stage.getScene().getStylesheets().add(sheet.toExternalForm());
        }
    }

    /** BONUS. Animations start switched off, so they cannot slow anyone down. */
    private void setUpAnimations() {
        animationsBox.setSelected(false);
        Animations.setEnabled(false);
        animationsBox.selectedProperty().addListener(
                (obs, was, now) -> Animations.setEnabled(now));
    }

    /** BONUS. Creating an event, with the acting user as its market maker. */
    @FXML
    private void onCreateEvent() {
        String creator = actingAsBox.getValue();
        if (engine == null || !engine.isLoaded() || creator == null) {
            return;
        }

        Optional<NewEventSpec> spec = new NewEventDialog(creator).showAndWait();
        if (spec.isEmpty()) {
            return;
        }

        try {
            int id = engine.createEvent(creator, spec.get());
            refreshViews();
            Alert done = new Alert(Alert.AlertType.INFORMATION);
            done.setTitle("Event created");
            done.setHeaderText("Event " + id + " created");
            done.setContentText(creator + " is its market maker. It is not started until "
                    + creator + " opens it.");
            done.showAndWait();
        } catch (guessmarket.engine.api.EngineException e) {
            Animations.shake(createEventButton);
            Alert problem = new Alert(Alert.AlertType.WARNING);
            problem.setTitle("The event was not created");
            problem.setHeaderText("The event was not created");
            problem.setContentText(e.getMessage());
            problem.showAndWait();
        }
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
        if (eventsViewController != null) {
            eventsViewController.setEngine(engine);
        }
        if (usersViewController != null) {
            usersViewController.setEngine(engine);
        }
        if (eventsViewController != null) {
            // The users screen has to follow whatever the events screen changes.
            eventsViewController.setOnSystemChanged(this::refreshUsersOnly);
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * The exercise requires a file chooser dialog, so there is deliberately no
     * text field for typing a path.
     */
    @FXML
    private void onLoadFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a Guess Market events file");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML files", "*.xml"));

        File chosen = chooser.showOpenDialog(stage);
        if (chosen == null) {
            return;
        }
        startLoad(chosen);
    }

    private void startLoad(File file) {
        LoadFileTask task = new LoadFileTask(engine, file.getAbsolutePath());

        loadButton.setDisable(true);
        setTabsEnabled(false);
        unbindProgress();
        loadProgress.progressProperty().bind(task.progressProperty());
        loadMessage.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            unbindProgress();
            loadProgress.setProgress(1);
            int count = task.getValue();
            filePathLabel.setText(file.getAbsolutePath());
            loadMessage.setText("Loaded " + count + (count == 1 ? " event" : " events"));
            loadButton.setDisable(false);
            setTabsEnabled(true);
            refreshViews();
        });

        task.setOnFailed(e -> {
            unbindProgress();
            loadProgress.setProgress(0);
            loadMessage.setText("The file was not loaded");
            loadButton.setDisable(false);
            // Whatever was loaded before is still in place, so the tabs stay usable.
            setTabsEnabled(engine != null && engine.isLoaded());
            showLoadFailure(task.getException());
        });

        Thread thread = new Thread(task, "guess-market-file-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshViews() {
        refreshActingUsers();
        if (eventsViewController != null) {
            eventsViewController.refresh();
        }
        refreshUsersOnly();
    }

    private void refreshUsersOnly() {
        if (usersViewController != null) {
            usersViewController.refresh();
        }
    }

    /** Rebuilds the list of users and selects the first one. */
    private void refreshActingUsers() {
        String keep = actingAsBox.getValue();
        actingAsBox.getItems().clear();
        if (engine == null || !engine.isLoaded()) {
            actingAsBox.setDisable(true);
            createEventButton.setDisable(true);
            return;
        }
        for (UserDto user : engine.listUsers()) {
            actingAsBox.getItems().add(user.name());
        }
        actingAsBox.setDisable(actingAsBox.getItems().isEmpty());
        createEventButton.setDisable(actingAsBox.getItems().isEmpty());
        if (!actingAsBox.getItems().isEmpty()) {
            actingAsBox.getSelectionModel().selectFirst();
        }
    }


    private void unbindProgress() {
        loadProgress.progressProperty().unbind();
        loadMessage.textProperty().unbind();
    }

    /** A failed load lists every problem at once, in a scrollable area. */
    private void showLoadFailure(Throwable error) {
        String detail = error == null ? "" : String.valueOf(error.getMessage());
        List<String> problems = (error instanceof LoadException load)
                ? load.getProblems()
                : List.of();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle("Load failed");
        alert.setHeaderText("The file was not loaded.");
        alert.setContentText(detail);

        if (!problems.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (String problem : problems) {
                text.append("- ").append(problem).append(System.lineSeparator());
            }
            TextArea area = new TextArea(text.toString());
            area.setEditable(false);
            area.setWrapText(true);
            area.setPrefRowCount(Math.min(12, problems.size() + 2));
            alert.getDialogPane().setExpandableContent(area);
            alert.getDialogPane().setExpanded(true);
        }

        alert.showAndWait();
    }

    private void setTabsEnabled(boolean enabled) {
        if (eventsTab != null) {
            eventsTab.setDisable(!enabled);
        }
        if (usersTab != null) {
            usersTab.setDisable(!enabled);
        }
    }
}
