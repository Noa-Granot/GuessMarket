package guessmarket.engine.model;

/**
 * When the event's commission is charged. Maps onto the "type" attribute of the
 * comision element in the XML file.
 */
public enum CommissionType {
    ON_PURCHASE("on-purchase", "Charged on every purchase"),
    ON_CLOSE("on-close", "Charged when the event closes");

    private final String xmlValue;
    private final String display;

    CommissionType(String xmlValue, String display) {
        this.xmlValue = xmlValue;
        this.display = display;
    }

    public String getXmlValue() {
        return xmlValue;
    }

    public String getDisplay() {
        return display;
    }

    public static CommissionType fromXml(String value) {
        if (value != null) {
            String trimmed = value.trim();
            for (CommissionType type : values()) {
                if (type.xmlValue.equalsIgnoreCase(trimmed)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Unknown commission type: " + value);
    }
}
