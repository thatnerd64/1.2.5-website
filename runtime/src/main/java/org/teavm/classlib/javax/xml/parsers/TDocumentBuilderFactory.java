package org.teavm.classlib.javax.xml.parsers;

/**
 * No XML parser in the browser build. Minecraft 1.2.5 only uses one to read the (long gone) online resource
 * index; failing here makes it fall back to installing sounds from .minecraft/resources.
 */
public abstract class TDocumentBuilderFactory {
    protected TDocumentBuilderFactory() {
    }

    public static TDocumentBuilderFactory newInstance() {
        return new TDocumentBuilderFactory() {
        };
    }

    public TDocumentBuilder newDocumentBuilder() throws TParserConfigurationException {
        throw new TParserConfigurationException("XML parsing is not available");
    }

    public void setNamespaceAware(boolean aware) {
    }

    public void setValidating(boolean validating) {
    }
}
