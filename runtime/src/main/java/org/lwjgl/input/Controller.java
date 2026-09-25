package org.lwjgl.input;

public interface Controller {
    String getName();

    int getIndex();

    int getButtonCount();

    int getAxisCount();

    boolean isButtonPressed(int index);

    float getAxisValue(int index);

    void poll();
}
