package guessmarket.engine.orderbook;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The resting orders for one option: what people will pay, and what they will
 * accept.
 *
 * Bids are kept best first, meaning the highest price, and asks best first
 * meaning the lowest. Orders at the same price keep the order they arrived in,
 * so the one that has waited longest is filled first.
 */
public class OrderBook implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Comparator<Order> BEST_BID_FIRST =
            Comparator.comparingDouble(Order::getPrice).reversed()
                    .thenComparingInt(Order::getSerial);

    private static final Comparator<Order> BEST_ASK_FIRST =
            Comparator.comparingDouble(Order::getPrice)
                    .thenComparingInt(Order::getSerial);

    private final String optionName;
    private final List<Order> bids = new ArrayList<>();
    private final List<Order> asks = new ArrayList<>();

    /** The price of the most recent trade in this option, or null if there has been none. */
    private Double lastTradePrice = null;

    public OrderBook(String optionName) {
        this.optionName = optionName;
    }

    public String getOptionName() {
        return optionName;
    }

    public List<Order> getBids() {
        return Collections.unmodifiableList(bids);
    }

    public List<Order> getAsks() {
        return Collections.unmodifiableList(asks);
    }

    public void add(Order order) {
        if (order.isFilled()) {
            return;
        }
        if (order.getSide() == OrderSide.BUY) {
            bids.add(order);
            bids.sort(BEST_BID_FIRST);
        } else {
            asks.add(order);
            asks.sort(BEST_ASK_FIRST);
        }
    }

    public void removeFilled() {
        bids.removeIf(Order::isFilled);
        asks.removeIf(Order::isFilled);
    }

    public void recordTrade(double price) {
        lastTradePrice = price;
    }

    /** The most recent traded price, or null if nothing has traded. */
    public Double getLast() {
        return lastTradePrice;
    }

    /** The highest price anyone is currently willing to pay. */
    public Double getBestBid() {
        return bids.isEmpty() ? null : bids.get(0).getPrice();
    }

    /** The lowest price anyone is currently willing to accept. */
    public Double getBestAsk() {
        return asks.isEmpty() ? null : asks.get(0).getPrice();
    }

    /** Halfway between the best bid and the best ask, when both exist. */
    public Double getMid() {
        Double bid = getBestBid();
        Double ask = getBestAsk();
        return (bid == null || ask == null) ? null : (bid + ask) / 2.0;
    }

    /** The gap between the best ask and the best bid, when both exist. */
    public Double getSpread() {
        Double bid = getBestBid();
        Double ask = getBestAsk();
        return (bid == null || ask == null) ? null : ask - bid;
    }

    /** Money tied up in this user's resting buy orders. */
    public double cashCommittedBy(String userName) {
        double total = 0;
        for (Order order : bids) {
            if (order.getUserName().equals(userName)) {
                total += order.getRemaining() * order.getPrice();
            }
        }
        return total;
    }

    /** Shares tied up in this user's resting sell orders. */
    public long sharesCommittedBy(String userName) {
        long total = 0;
        for (Order order : asks) {
            if (order.getUserName().equals(userName)) {
                total += order.getRemaining();
            }
        }
        return total;
    }
}
