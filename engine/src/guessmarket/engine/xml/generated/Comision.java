package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * GENERATED-STYLE BINDING CLASS -- see GuessMarket.
 *
 * Note the spelling: the schema element really is "comision", not "commission".
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"value"})
@XmlRootElement(name = "comision")
public class Comision {

    @XmlValue
    protected int value;

    @XmlAttribute(name = "type", required = true)
    protected String type;

    public int getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
