package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

/**
 * JAXB binding class for the exercise 2 schema.
 * The element is spelled GM-mareket-maker in the schema, not market.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"event"})
@XmlRootElement(name = "GM-mareket-maker")
public class GMMareketMaker {

    @XmlElement(name = "event", required = true)
    protected List<EventRef> event;

    public List<EventRef> getEvent() {
        if (event == null) {
            event = new ArrayList<>();
        }
        return this.event;
    }
}
