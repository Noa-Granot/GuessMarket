package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/** JAXB binding class for the exercise 2 schema. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
@XmlRootElement(name = "Guess-Market")
public class GuessMarket {

    @XmlElement(name = "GM-events", required = true)
    protected GMEvents gmEvents;

    @XmlElement(name = "GM-users", required = true)
    protected GMUsers gmUsers;

    public GMEvents getGMEvents() { return gmEvents; }
    public GMUsers getGMUsers() { return gmUsers; }
}
