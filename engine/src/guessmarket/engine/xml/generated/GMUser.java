package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/** JAXB binding class for the exercise 2 schema. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"initialCash", "gmMareketMaker"})
@XmlRootElement(name = "GM-user")
public class GMUser {

    @XmlElement(name = "initial-cash")
    protected int initialCash;

    @XmlElement(name = "GM-mareket-maker")
    protected GMMareketMaker gmMareketMaker;

    @XmlAttribute(name = "name", required = true)
    protected String name;

    public int getInitialCash() { return initialCash; }
    public GMMareketMaker getGMMareketMaker() { return gmMareketMaker; }
    public String getName() { return name; }
}
