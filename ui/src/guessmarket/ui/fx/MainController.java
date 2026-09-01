package guessmarket.ui.fx;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.LoadException;

import javafx.fxml  .FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
        setTabsEnabled(false);
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
        if (eventsViewController != null) {
            eventsViewController.setEngine(engine);
        }
        if (usersViewController != null) {
            usersViewController.setEngine(engine);
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
        if (eventsViewController != null) {
            eventsViewController.refresh();
        }
        if (usersViewController != null) {
            usersViewController.refresh();
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
