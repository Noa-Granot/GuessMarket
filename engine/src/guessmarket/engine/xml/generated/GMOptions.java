package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

/** GENERATED-STYLE BINDING CLASS -- see GuessMarket. */
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
