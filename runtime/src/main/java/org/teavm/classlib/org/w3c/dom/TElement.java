package org.teavm.classlib.org.w3c.dom;

public interface TElement extends TNode {
    String getAttribute(String name);

    String getTagName();

    TNodeList getElementsByTagName(String name);
}
