package org.teavm.classlib.org.w3c.dom;

public interface TDocument extends TNode {
    TElement getDocumentElement();

    TNodeList getElementsByTagName(String name);
}
