package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * JAXB binding class for the exercise 2 schema.
 * The schema calls this element "event"; it is named EventRef here so it does
 * not collide with the domain class of the same name.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
@XmlRootElement(name = "event")
public class EventRef {

    @XmlAttribute(name = "id", required = true)
    protected int id;

    public int getId() { return id; }
}
