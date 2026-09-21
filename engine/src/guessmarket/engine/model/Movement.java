package guessmarket.engine.model;

import java.io.Serializable;

/**
 * One change to an account, and why it happened.
 *
 * Exercise 3 asks for every line a balance has moved through, including money
 * arriving because of somebody else's trade. A balance on its own cannot say
 * that, so each movement carries the reason it was made for.
 *
 * @param serial       counts the movements of this account, starting at 1
 * @param reason       what caused it, written to be read by a person
 * @param change       positive for money in, negative for money out
 * @param balanceAfter the balance once it had been applied
 */
public record Movement(int serial, String reason, double change, double balanceAfter)
        implements Serializable {

    private static final long serialVersionUID = 1L;
}
