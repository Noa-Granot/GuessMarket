package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/** JAXB binding class for the exercise 2 schema. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"id", "description", "commission", "gmOptions", "gmMethod"})
@XmlRootElement(name = "GM-event")
public class GMEvent {

    protected int id;

    @XmlElement(required = true)
    protected String description;

    @XmlElement(required = true)
    protected Commission commission;

    @XmlElement(name = "GM-options", required = true)
    protected GMOptions gmOptions;

    @XmlElement(name = "GM-method", required = true)
    protected GMMethod gmMethod;

    @XmlAttribute(name = "name", required = true)
    protected String name;

    public int getId() { return id; }
    public String getDescription() { return description; }
    public Commission getCommission() { return commission; }
    public GMOptions getGMOptions() { return gmOptions; }
    public GMMethod getGMMethod() { return gmMethod; }
    public String getName() { return name; }
}
