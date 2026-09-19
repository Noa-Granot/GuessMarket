package guessmarket.engine.xml.ex3.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * JAXB binding class for the exercise 3 schema.
 *
 * The initial stock attribute has been seen spelled both "initial" and
 * "inital" in the material handed out, so both are bound. They are Integer
 * rather than int so that a missing attribute reads as absent instead of as
 * zero, which validation needs to tell apart.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
@XmlRootElement(name = "GM-order-book")
public class GMOrderBook {

    @XmlAttribute(name = "initial")
    protected Integer initial;

    @XmlAttribute(name = "inital")
    protected Integer inital;

    @XmlAttribute(name = "d", required = true)
    protected int d;

    @XmlAttribute(name = "allow-mint", required = true)
    protected String allowMint;

    /** True when the file carried the attribute under either spelling. */
    public boolean hasInitialShares() { return initial != null || inital != null; }

    /** The initial stock, or 0 when the attribute is missing. */
    public int getInitialShares() {
        if (initial != null) {
            return initial;
        }
        return inital == null ? 0 : inital;
    }

    public int getD() { return d; }
    public String getAllowMint() { return allowMint; }
}
