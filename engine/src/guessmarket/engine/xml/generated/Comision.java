package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * JAXB binding class for the exercise 2 schema.
 * The element is spelled comision in the schema, not commission.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"value"})
@XmlRootElement(name = "comision")
public class Comision {

    @XmlValue
    protected int value;

    @XmlAttribute(name = "type", required = true)
    protected String type;

    public int getValue() { return value; }
    public String getType() { return type; }
}
