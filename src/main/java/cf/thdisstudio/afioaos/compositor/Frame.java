package cf.thdisstudio.afioaos.compositor;

import com.sun.jna.NativeLong;
import com.sun.jna.platform.unix.X11;

import static cf.thdisstudio.afioaos.compositor.WindowManager.x11;
import static cf.thdisstudio.afioaos.compositor.WindowManager.x11c;

public class Frame {
    private X11.Window frame;
    private X11.Window titlebar;

    private final X11.Display display;
    private final X11.Window root;
    private final X11.Window client;

    private int x;
    private int y;
    private int width;
    private int height;

    public Frame(X11.Display display, X11.Window root, X11.Window client) {
        this.display = display;
        this.root = root;
        this.client = client;
        init();
    }

    private void init() {
        X11.XWindowAttributes attrs = new X11.XWindowAttributes();
        x11.XGetWindowAttributes(display, client, attrs);

        setLocation(attrs.x, attrs.y, attrs.width, attrs.height+25);

        createFrame();
        createTitlebar(attrs);
    }

    private void createFrame() {
        frame = x11.XCreateSimpleWindow(
                display,
                root,
                x,
                y,
                width,
                height,
                3,
                0x00feff,
                0x00feff
        );
        x11.XSelectInput(display, frame, new NativeLong(X11.SubstructureRedirectMask | X11.SubstructureNotifyMask));
        x11c.XReparentWindow(display, client, frame, 0, 25);
        x11c.XMoveWindow(display, client, 0, 25);
        x11.XMapWindow(display, frame);
    }

    private void createTitlebar(X11.XWindowAttributes attrs) {
        titlebar = x11.XCreateSimpleWindow(display, frame, 0, 0, 3, 25, 0, 0, 0xffffff);
        x11.XMapWindow(display, titlebar);
        x11c.XGrabButton(display, X11.Button1, X11.Button1Mask, titlebar, false, X11.ButtonPressMask | X11.ButtonMotionMask | X11.ButtonReleaseMask, X11.GrabModeAsync, X11.GrabModeAsync, null, null);
        x11.XMapWindow(display, x11.XCreateSimpleWindow(display, frame, attrs.width-20, 3, 15, 15, 1, 0x000000, 0xffffff));
    }

    public void move(int x, int y) {
        x11c.XMoveWindow(display, frame, this.x + x, this.y + y);
        this.x = this.x + x;
        this.y = this.y + y;
    }

    private void setLocation(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public X11.Window getFrame(){
        return frame;
    }

    public void destroy() {
        x11.XUnmapWindow(display, frame);
        x11c.XReparentWindow(display, client, root, 0, 0);
        x11.XDestroyWindow(display, frame);
    }
}
