package org.teavm.classlib.org.w3c.dom;

public interface TNode {
    String getNodeName();

    String getNodeValue();

    String getTextContent();

    TNodeList getChildNodes();

    TNode getFirstChild();

    short getNodeType();
}
