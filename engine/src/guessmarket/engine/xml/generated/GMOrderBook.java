package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * JAXB binding class for the exercise 2 schema.
 * The attribute is spelled inital in the schema, not initial.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
@XmlRootElement(name = "GM-order-book")
public class GMOrderBook {

    @XmlAttribute(name = "inital", required = true)
    protected int inital;

    @XmlAttribute(name = "d", required = true)
    protected int d;

    @XmlAttribute(name = "allow-mint", required = true)
    protected String allowMint;

    public int getInital() { return inital; }
    public int getD() { return d; }
    public String getAllowMint() { return allowMint; }
}
