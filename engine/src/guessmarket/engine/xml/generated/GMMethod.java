package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/** GENERATED-STYLE BINDING CLASS -- see GuessMarket. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"gmlmsr"})
@XmlRootElement(name = "GM-method")
public class GMMethod {

    @XmlElement(name = "GM-LMSR", required = true)
    protected GMLMSR gmlmsr;

    public GMLMSR getGMLMSR() {
        return gmlmsr;
    }
}
