package guessmarket.ui.fx;

import guessmarket.engine.api.EventRoleDto;
import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.HoldingDto;
import guessmarket.engine.api.PointDto;
import guessmarket.engine.api.UserDto;
import guessmarket.engine.api.UserStateDto;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
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
    @FXML private LineChart<Number, Number> balanceChart;

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

    /** Reloads from the engine, keeping the selected user if they are still there. */
    public void refresh() {
        String keep = null;
        UserRow selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            keep = selected.getName();
        }

        rows.clear();
        if (engine == null || !engine.isLoaded()) {
            clearDetails();
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

        if (keep != null) {
            for (UserRow row : rows) {
                if (row.getName().equals(keep)) {
                    usersTable.getSelectionModel().select(row);
                    return;
                }
            }
        }
        clearDetails();
    }

    /** BONUS: this user's balance after every change. */
    private void showBalanceChart(UserStateDto state) {
        balanceChart.getData().clear();
        XYChart.Series<Number, Number> line = new XYChart.Series<>();
        line.setName(state.name());
        for (PointDto point : state.balanceHistory().points()) {
            line.getData().add(new XYChart.Data<>(point.step(), point.value()));
        }
        balanceChart.getData().add(line);
    }

    private void clearDetails() {
        userTitle.setText("Select a user to see their details");
        userBalanceLabel.setText("");
        participationBox.getChildren().clear();
        userEventDetailBox.getChildren().clear();
        balanceChart.getData().clear();
    }

    private void showDetails(UserRow row) {
        clearDetails();
        if (row == null || engine == null || !engine.isLoaded()) {
            return;
        }

        UserStateDto state = engine.userState(row.getName());
        userTitle.setText(state.name());
        userBalanceLabel.setText(String.format("Account balance: %.2f", state.balance()));
        showBalanceChart(state);
        Animations.fadeIn(userTitle);

        if (state.events().isEmpty()) {
            participationBox.getChildren().add(
                    new Label("Not involved in any event yet."));
            return;
        }

        for (EventRoleDto role : state.events()) {
            String label = role.eventName() + "  (" + role.statusDisplay() + ")";
            if (role.isMarketMaker()) {
                label += "  ·  market maker";
            }
            Label heading = new Label(label);
            heading.getStyleClass().add("section-title");
            participationBox.getChildren().add(heading);

            if (role.holdings().isEmpty()) {
                participationBox.getChildren().add(new Label("   holds no shares"));
            } else {
                for (HoldingDto holding : role.holdings()) {
                    participationBox.getChildren().add(new Label(String.format(
                            "   %d shares of %s", holding.shares(), holding.optionName())));
                }
            }
        }
    }
}
