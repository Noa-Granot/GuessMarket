package guessmarket.engine.xml.ex3.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * JAXB binding class for the exercise 3 schema.
 *
 * Two differences from exercise 2:
 *
 *   - there is no id element any more, so the server numbers the events itself.
 *   - the commission element has been seen spelled both "commission" and
 *     "comision" in the material handed out. Both are bound, and getComision
 *     returns whichever one the file used, so either version loads. Binding
 *     only one of them would make every event arrive with a null commission
 *     and fail validation with a misleading message.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"description", "commission", "comision", "gmOptions", "gmMethod"})
@XmlRootElement(name = "GM-event")
public class GMEvent {

    @XmlElement(required = true)
    protected String description;

    @XmlElement(name = "commission")
    protected Comision commission;

    @XmlElement(name = "comision")
    protected Comision comision;

    @XmlElement(name = "GM-options", required = true)
    protected GMOptions gmOptions;

    @XmlElement(name = "GM-method", required = true)
    protected GMMethod gmMethod;

    @XmlAttribute(name = "name", required = true)
    protected String name;

    public String getDescription() { return description; }

    /** Whichever spelling the file used, or null if it carried neither. */
    public Comision getComision() { return commission != null ? commission : comision; }

    public GMOptions getGMOptions() { return gmOptions; }
    public GMMethod getGMMethod() { return gmMethod; }
    public String getName() { return name; }
}
