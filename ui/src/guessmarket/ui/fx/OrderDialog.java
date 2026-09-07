package guessmarket.ui.fx;

import guessmarket.engine.api.EventStateDto;
import guessmarket.engine.api.OptionStateDto;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;

/**
 * Asks for an order: which option, buy or sell, how many, and at what price.
 * The price ceiling is shown and checked here as well as in the engine.
 */
class OrderDialog extends Dialog<OrderDialog.Choice> {

    record Choice(int optionIndex, String side, long quantity, double price) {
    }

    private static final String BUY = "Buy";
    private static final String SELL = "Sell";

    private final ComboBox<String> optionBox = new ComboBox<>();
    private final ComboBox<String> sideBox = new ComboBox<>();
    private final TextField quantityField = new TextField();
    private final TextField priceField = new TextField();
    private final Label hint = new Label();

    private final double highestPrice;

    OrderDialog(EventStateDto state, String trader, int preselectedOption) {
        setTitle("Place an order");
        setHeaderText(trader + " trading in \"" + state.name() + "\"");

        double d = state.basePrice() == null ? 1.0 : state.basePrice();
        highestPrice = d - 0.01;

        List<OptionStateDto> options = state.options();
        for (OptionStateDto option : options) {
            optionBox.getItems().add(option.name());
        }
        optionBox.getSelectionModel().select(
                Math.max(0, Math.min(preselectedOption, options.size() - 1)));

        sideBox.getItems().addAll(BUY, SELL);
        sideBox.getSelectionModel().selectFirst();

        quantityField.setPromptText("whole number above zero");
        priceField.setPromptText(String.format("up to %.2f", highestPrice));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Option:"), 0, 0);
        grid.add(optionBox, 1, 0);
        grid.add(new Label("Action:"), 0, 1);
        grid.add(sideBox, 1, 1);
        grid.add(new Label("Shares:"), 0, 2);
        grid.add(quantityField, 1, 2);
        grid.add(new Label("Price each:"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(hint, 0, 4, 2, 1);
        getDialogPane().setContent(grid);

        ButtonType submit = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(submit, ButtonType.CANCEL);

        quantityField.textProperty().addListener((o, a, b) -> updateHint());
        priceField.textProperty().addListener((o, a, b) -> updateHint());
        updateHint();

        setResultConverter(button -> {
            if (button != submit) {
                return null;
            }
            Long quantity = parseQuantity();
            Double price = parsePrice();
            if (quantity == null || price == null) {
                return null;
            }
            return new Choice(optionBox.getSelectionModel().getSelectedIndex(),
                    sideBox.getSelectionModel().getSelectedItem(), quantity, price);
        });
    }

    private Long parseQuantity() {
        try {
            long value = Long.parseLong(quantityField.getText().trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parsePrice() {
        try {
            double value = Double.parseDouble(priceField.getText().trim());
            return (value > 0 && value <= highestPrice + 0.000001) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void updateHint() {
        Long quantity = parseQuantity();
        Double price = parsePrice();

        if (quantity == null) {
            hint.setText("Shares must be a whole number above zero.");
            return;
        }
        if (price == null) {
            hint.setText(String.format(
                    "The price must be above zero and no more than %.2f.", highestPrice));
            return;
        }
        hint.setText(String.format("Total: %d x %.2f = %.2f",
                quantity, price, quantity * price));
    }
}
