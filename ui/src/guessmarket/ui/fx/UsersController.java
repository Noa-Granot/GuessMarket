package guessmarket.ui.fx;

import guessmarket.engine.api.EventDto;
import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.UserDto;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The users screen: the list of users on the left and the details of the
 * selected user on the right, following the supplied sketch.
 */
public class UsersController {

    @FXML private TableView<UserRow> usersTable;
    @FXML private TableColumn<UserRow, String> userNameColumn;
    @FXML private TableColumn<UserRow, Number> userBalanceColumn;
    @FXML private TableColumn<UserRow, String> userRoleColumn;

    @FXML private Label userTitle;
    @FXML private Label userBalanceLabel;
    @FXML private VBox participationBox;
    @FXML private VBox userEventDetailBox;

    private final ObservableList<UserRow> rows = FXCollections.observableArrayList();

    private GuessMarketEngine engine;

    @FXML
    private void initialize() {
        userNameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        userRoleColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole()));
        userBalanceColumn.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getBalance()));
        userBalanceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "" : String.format("%.2f", value.doubleValue()));
            }
        });

        usersTable.setItems(rows);
        usersTable.setPlaceholder(new Label("No users to show."));
        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, was, now) -> showDetails(now));

        clearDetails();
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
    }

    public void refresh() {
        rows.clear();
        clearDetails();

        if (engine == null || !engine.isLoaded()) {
            return;
        }

        List<UserRow> built = new ArrayList<>();
        for (UserDto user : engine.listUsers()) {
            built.add(new UserRow(
                    user.name(),
                    user.balance(),
                    user.isMarketMaker() ? "Market maker" : "Player",
                    user.marketMakerForEventIds()));
        }
        rows.setAll(built);
    }

    private void clearDetails() {
        userTitle.setText("Select a user to see their details");
        userBalanceLabel.setText("");
        participationBox.getChildren().clear();
        userEventDetailBox.getChildren().clear();
    }

    private void showDetails(UserRow row) {
        clearDetails();

        if (row == null || engine == null || !engine.isLoaded()) {
            return;
        }

        userTitle.setText(row.getName());
        userBalanceLabel.setText(String.format("Account balance: %.2f", row.getBalance()));

        if (row.getMarketMakerFor().isEmpty()) {
            participationBox.getChildren().add(
                    new Label("Not the market maker of any event."));
        } else {
            for (int eventId : row.getMarketMakerFor()) {
                String name = nameOfEvent(eventId);
                participationBox.getChildren().add(
                        new Label("Market maker of event " + eventId + " - " + name));
            }
        }

        userEventDetailBox.getChildren().add(new Label(
                "Trading details appear once users can trade, in the next stage of the exercise."));
    }

    private String nameOfEvent(int eventId) {
        for (EventDto event : engine.listEvents()) {
            if (event.id() == eventId) {
                return event.name();
            }
        }
        return "unknown event";
    }
}
