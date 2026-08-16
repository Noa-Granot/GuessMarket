package guessmarket.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * GENERATED-STYLE BINDING CLASS -- do not put logic here.
 *
 * This mirrors what xjc produces from the exercise 1 schema. Once the real
 * schema is downloaded, regenerate this whole package with xjc-run.bat and
 * delete these hand-written stand-ins. Nothing outside guessmarket.engine.xml
 * refers to them, so replacing them touches one file: XmlLoader.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"gmEvents"})
@XmlRootElement(name = "Guess-Market")
public class GuessMarket {

    @XmlElement(name = "GM-events", required = true)
    protected GMEvents gmEvents;

    public GMEvents getGMEvents() {
        return gmEvents;
    }

    public void setGMEvents(GMEvents value) {
        this.gmEvents = value;
    }
}
