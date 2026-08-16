package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

/** GENERATED-STYLE BINDING CLASS -- see GuessMarket. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"gmEvent"})
@XmlRootElement(name = "GM-events")
public class GMEvents {

    @XmlElement(name = "GM-event", required = true)
    protected List<GMEvent> gmEvent;

    public List<GMEvent> getGMEvent() {
        if (gmEvent == null) {
            gmEvent = new ArrayList<>();
        }
        return this.gmEvent;
    }
}
