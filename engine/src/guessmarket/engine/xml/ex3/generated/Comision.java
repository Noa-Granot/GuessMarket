package guessmarket.engine.xml.ex3.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * JAXB binding class for the exercise 3 schema.
 * The element name follows the schema, misspelling included.
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
