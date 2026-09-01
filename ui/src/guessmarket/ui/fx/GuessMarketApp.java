package guessmarket.ui.fx;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.GuessMarketEngineImpl;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Entry point of the graphical application. Creates the engine and hands it to
 * the main controller, so this is the only place the concrete engine class is
 * named.
 */
public class GuessMarketApp extends Application {

    private static final String MAIN_FXML = "app.fxml";
    private static final String STYLESHEET = "app.css";

    @Override
    public void start(Stage stage) throws Exception {
        URL fxml = getClass().getResource(MAIN_FXML);
        if (fxml == null) {
            throw new IllegalStateException(
                    "app.fxml was not found next to GuessMarketApp.class. "
                            + "Check that the fxml files are copied into the build output.");
        }

        FXMLLoader loader = new FXMLLoader(fxml);
        Parent root = loader.load();

        MainController controller = loader.getController();
        GuessMarketEngine engine = new GuessMarketEngineImpl();
        controller.setEngine(engine);
        controller.setStage(stage);

        Scene scene = new Scene(root, 1180, 720);
        URL css = getClass().getResource(STYLESHEET);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("Guess Market");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
