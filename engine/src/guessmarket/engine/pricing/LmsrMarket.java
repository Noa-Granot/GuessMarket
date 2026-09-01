package guessmarket.engine.pricing;

import java.io.Serializable;

public class LmsrMarket implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int b;

    public LmsrMarket(int b) {
        if (b <= 0) {
            throw new IllegalArgumentException("Liquidity parameter b must be positive, got " + b);
        }
        this.b = b;
    }

    public int getB() {
        return b;
    }

    /**
     * The cost function C(q) = b * ln( sum of e^(qi/b) ).
     * This is the total amount of money the event's pot should hold given q.
     */
    public double cost(long[] quantities) {
        double max = Double.NEGATIVE_INFINITY;
        for (long q : quantities) {
            max = Math.max(max, q / (double) b);
        }
        double sum = 0.0;
        for (long q : quantities) {
            sum += Math.exp(q / (double) b - max);
        }
        return b * (max + Math.log(sum));
    }

    /**
     * The value of each option, between 0 and 1. The values always add up to 1.
     */
    public double[] prices(long[] quantities) {
        double max = Double.NEGATIVE_INFINITY;
        for (long q : quantities) {
            max = Math.max(max, q / (double) b);
        }
        double[] shifted = new double[quantities.length];
        double sum = 0.0;
        for (int i = 0; i < quantities.length; i++) {
            shifted[i] = Math.exp(quantities[i] / (double) b - max);
            sum += shifted[i];
        }
        for (int i = 0; i < shifted.length; i++) {
            shifted[i] /= sum;
        }
        return shifted;
    }

    /**
     * What it costs to buy `amount` shares of option `index`: the difference
     * between the pot after the purchase and the pot before it.
     */
    public double costOfBuying(long[] quantities, int index, long amount) {
        long[] after = quantities.clone();
        after[index] += amount;
        return cost(after) - cost(quantities);
    }

    /**
     * C(0, 0) -- the money that must already be in the pot before anyone trades.
     * This is the subsidy the market maker puts up to open the event.
     */
    public double initialSubsidy(int optionCount) {
        return cost(new long[optionCount]);
    }
}
