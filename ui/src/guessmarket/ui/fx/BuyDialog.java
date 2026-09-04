package guessmarket.ui.fx;

import guessmarket.engine.api.EngineException;
import guessmarket.engine.api.EventStateDto;
import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.OptionStateDto;
import guessmarket.engine.api.QuoteDto;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;

/**
 * Asks which option to buy and how many shares, and shows what it would cost
 * before anything is bought.
 */
class BuyDialog extends Dialog<BuyDialog.Choice> {

    /** What the user chose. Option index starts at 0, as the engine expects. */
    record Choice(int optionIndex, long quantity) {
    }

    private final ComboBox<String> optionBox = new ComboBox<>();
    private final TextField quantityField = new TextField();
    private final Label quoteLabel = new Label();

    BuyDialog(GuessMarketEngine engine, EventStateDto state, String buyer) {
        setTitle("Buy shares");
        setHeaderText(buyer + " buying in \"" + state.name() + "\"");

        List<OptionStateDto> options = state.options();
        for (OptionStateDto option : options) {
            optionBox.getItems().add(option.name());
        }
        optionBox.getSelectionModel().selectFirst();
        quantityField.setPromptText("whole number above zero");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Option:"), 0, 0);
        grid.add(optionBox, 1, 0);
        grid.add(new Label("Shares:"), 0, 1);
        grid.add(quantityField, 1, 1);
        grid.add(new Label("Cost:"), 0, 2);
        grid.add(quoteLabel, 1, 2);
        getDialogPane().setContent(grid);

        ButtonType buy = new ButtonType("Buy", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(buy, ButtonType.CANCEL);

        quantityField.textProperty().addListener((obs, was, now) -> updateQuote(engine, state));
        optionBox.valueProperty().addListener((obs, was, now) -> updateQuote(engine, state));
        updateQuote(engine, state);

        setResultConverter(button -> {
            if (button != buy) {
                return null;
            }
            Long quantity = parseQuantity();
            if (quantity == null) {
                return null;
            }
            return new Choice(optionBox.getSelectionModel().getSelectedIndex(), quantity);
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

    /** Shows the live price, or why the entry is not usable yet. */
    private void updateQuote(GuessMarketEngine engine, EventStateDto state) {
        Long quantity = parseQuantity();
        if (quantity == null) {
            quoteLabel.setText("Enter a whole number above zero.");
            return;
        }
        try {
            QuoteDto quote = engine.quote(state.id(),
                    optionBox.getSelectionModel().getSelectedIndex(), quantity);
            quoteLabel.setText(String.format(
                    "shares %.2f  +  commission %.2f  =  %.2f",
                    quote.shareCost(), quote.commission(), quote.total()));
        } catch (EngineException e) {
            quoteLabel.setText(e.getMessage());
        }
    }
}
