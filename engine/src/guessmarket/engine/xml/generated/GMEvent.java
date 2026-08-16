package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/** GENERATED-STYLE BINDING CLASS -- see GuessMarket. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"id", "description", "comision", "gmOptions", "gmMethod"})
@XmlRootElement(name = "GM-event")
public class GMEvent {

    protected int id;

    @XmlElement(required = true)
    protected String description;

    @XmlElement(required = true)
    protected Comision comision;

    @XmlElement(name = "GM-options", required = true)
    protected GMOptions gmOptions;

    @XmlElement(name = "GM-method", required = true)
    protected GMMethod gmMethod;

    @XmlAttribute(name = "name", required = true)
    protected String name;

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Comision getComision() {
        return comision;
    }

    public GMOptions getGMOptions() {
        return gmOptions;
    }

    public GMMethod getGMMethod() {
        return gmMethod;
    }

    public String getName() {
        return name;
    }
}
