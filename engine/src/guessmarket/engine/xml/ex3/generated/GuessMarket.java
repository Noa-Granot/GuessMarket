package guessmarket.engine.xml.ex3.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * JAXB binding class for the exercise 3 schema.
 * The exercise 3 file carries events only. Users log in, so GM-users is gone.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"gmEvents"})
@XmlRootElement(name = "Guess-Market")
public class GuessMarket {

    @XmlElement(name = "GM-events", required = true)
    protected GMEvents gmEvents;

    public GMEvents getGMEvents() { return gmEvents; }
}
