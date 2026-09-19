package guessmarket.engine.xml.ex3.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/** JAXB binding class for the exercise 3 schema. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"b"})
@XmlRootElement(name = "GM-LMSR")
public class GMLMSR {

    @XmlElement(required = true)
    protected int b;

    public int getB() { return b; }
}
