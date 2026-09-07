package guessmarket.engine.orderbook;

import java.io.Serializable;

/**
 * One instruction resting in a book, or being matched right now.
 *
 * Quantity is what was asked for and never changes; remaining falls as the
 * order is filled. An order with nothing remaining is taken out of the book.
 */
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int serial;
    private final String userName;
    private final OrderSide side;
    private final int optionIndex;
    private final double price;
    private final long quantity;

    private long remaining;

    public Order(int serial, String userName, OrderSide side,
                 int optionIndex, double price, long quantity) {
        this.serial = serial;
        this.userName = userName;
        this.side = side;
        this.optionIndex = optionIndex;
        this.price = price;
        this.quantity = quantity;
        this.remaining = quantity;
    }

    public int getSerial() { return serial; }
    public String getUserName() { return userName; }
    public OrderSide getSide() { return side; }
    public int getOptionIndex() { return optionIndex; }
    public double getPrice() { return price; }
    public long getQuantity() { return quantity; }
    public long getRemaining() { return remaining; }

    public boolean isFilled() {
        return remaining <= 0;
    }

    public void reduceBy(long filled) {
        if (filled > remaining) {
            throw new IllegalArgumentException("Cannot fill more than remains on order " + serial);
        }
        remaining -= filled;
    }
}
