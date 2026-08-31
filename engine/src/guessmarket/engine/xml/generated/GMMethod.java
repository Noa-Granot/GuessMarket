package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * JAXB binding class for the exercise 2 schema.
 * The schema defines a choice, so exactly one of the two is present.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"gmlmsr", "gmOrderBook"})
@XmlRootElement(name = "GM-method")
public class GMMethod {

    @XmlElement(name = "GM-LMSR")
    protected GMLMSR gmlmsr;

    @XmlElement(name = "GM-order-book")
    protected GMOrderBook gmOrderBook;

    public GMLMSR getGMLMSR() { return gmlmsr; }
    public GMOrderBook getGMOrderBook() { return gmOrderBook; }
}
