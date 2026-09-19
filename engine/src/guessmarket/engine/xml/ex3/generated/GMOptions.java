package guessmarket.engine.xml.ex3.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

/**
 * JAXB binding class for the exercise 3 schema.
 * The schema still says maxOccurs="2" while the exercise text asks for events
 * with more than two options. Nothing here validates against the schema, so a
 * file with three options parses; the loader decides whether to accept it.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"gmOption"})
@XmlRootElement(name = "GM-options")
public class GMOptions {

    @XmlElement(name = "GM-option", required = true)
    protected List<String> gmOption;

    public List<String> getGMOption() {
        if (gmOption == null) {
            gmOption = new ArrayList<>();
        }
        return this.gmOption;
    }
}
