package cf.thdisstudio.afioaos.compositor;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.unix.X11;

import java.awt.*;

public interface X11c extends Library {
    X11c INSTANCE = Native.load("X11", X11c.class);

    public int XConfigureWindow(X11.Display display, X11.Window window, int value_mask, XWindowChanges changes);
    public int XReparentWindow(X11.Display display, X11.Window window, X11.Window parent, int x, int y);
    public int XGrabServer(X11.Display display);
    public int XUngrabServer(X11.Display display);
    public int XSetWindowBackgroundPixmap(X11.Display display, X11.Window window, X11.Pixmap pixmap);
    public int XMoveWindow(X11.Display display, X11.Window window, int x, int y);
    public int XGrabButton(X11.Display display, int button, int modifiers, X11.Window grab_window, boolean owner_events, int event_mask, int pointer_mode, int keyboard_mode, X11.Window confine_to, Cursor cursor);

    public class XWindowChanges extends Structure
    {
        public int x;
        public int y;
        public int width;
        public int height;
        public int border_width;
        public X11.Window sibling;
        public int stack_mode;
    }
}
