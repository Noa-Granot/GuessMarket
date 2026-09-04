package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

/** JAXB binding class for the exercise 2 schema. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"event"})
@XmlRootElement(name = "GM-market-maker")
public class GMMarketMaker {

    @XmlElement(name = "event", required = true)
    protected List<EventRef> event;

    public List<EventRef> getEvent() {
        if (event == null) {
            event = new ArrayList<>();
        }
        return this.event;
    }
}
