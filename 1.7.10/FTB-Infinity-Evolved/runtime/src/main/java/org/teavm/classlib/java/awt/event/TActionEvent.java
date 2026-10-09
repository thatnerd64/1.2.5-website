package org.teavm.classlib.java.awt.event;

/** java.awt.event.ActionEvent (Realms' download progress fires one). */
public class TActionEvent extends TAWTEvent {
    public static final int ACTION_PERFORMED = 1001;
    private final String command;

    public TActionEvent(Object source, int id, String command) {
        super(source, id);
        this.command = command;
    }

    public String getActionCommand() {
        return command;
    }
}
