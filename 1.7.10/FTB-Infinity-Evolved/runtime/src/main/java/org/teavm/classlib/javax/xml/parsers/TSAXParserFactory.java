package org.teavm.classlib.javax.xml.parsers;

/** javax.xml.parsers.SAXParserFactory for {@link TSAXParser}. */
public class TSAXParserFactory {
    private boolean namespaceAware;
    private boolean validating;

    protected TSAXParserFactory() {
    }

    public static TSAXParserFactory newInstance() {
        return new TSAXParserFactory();
    }

    public TSAXParser newSAXParser() throws TParserConfigurationException {
        return new TSAXParser();
    }

    public void setNamespaceAware(boolean awareness) {
        namespaceAware = awareness;
    }

    public boolean isNamespaceAware() {
        return namespaceAware;
    }

    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    public boolean isValidating() {
        return validating;
    }

    public void setFeature(String name, boolean value) {
    }

    public boolean getFeature(String name) {
        return false;
    }
}
