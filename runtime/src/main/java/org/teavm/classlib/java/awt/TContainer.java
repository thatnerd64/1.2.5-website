package org.teavm.classlib.java.awt;

import java.util.ArrayList;
import java.util.List;

public class TContainer extends TComponent {
    private final List<TComponent> components = new ArrayList<>();
    private TLayoutManager layout;

    public TContainer() {
    }

    public TComponent add(TComponent c) {
        addImpl(c);
        return c;
    }

    public TComponent add(String name, TComponent c) {
        addImpl(c);
        return c;
    }

    public void add(TComponent c, Object constraints, int index) {
        addImpl(c);
    }

    public TComponent add(TComponent c, int index) {
        addImpl(c);
        return c;
    }

    public void add(TComponent c, Object constraints) {
        addImpl(c);
    }

    protected void addImpl(TComponent c) {
        if (c.parent != null) {
            c.parent.remove(c);
        }
        c.parent = this;
        components.add(c);
        if (c.getWidth() == 0 && c.getHeight() == 0) {
            c.setSize(getWidth(), getHeight());
        }
    }

    public void remove(TComponent c) {
        if (components.remove(c)) {
            c.parent = null;
        }
    }

    public void remove(int index) {
        remove(components.get(index));
    }

    public void removeAll() {
        for (TComponent c : new ArrayList<>(components)) {
            remove(c);
        }
    }

    public int getComponentCount() {
        return components.size();
    }

    public TComponent getComponent(int i) {
        return components.get(i);
    }

    public TComponent[] getComponents() {
        return components.toArray(new TComponent[0]);
    }

    public void setLayout(TLayoutManager layout) {
        this.layout = layout;
    }

    public TLayoutManager getLayout() {
        return layout;
    }

    public TInsets getInsets() {
        return new TInsets(0, 0, 0, 0);
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        for (TComponent c : components) {
            c.setSize(width, height);
        }
    }
}
