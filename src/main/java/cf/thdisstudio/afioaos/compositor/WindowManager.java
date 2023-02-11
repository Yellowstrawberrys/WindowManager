package cf.thdisstudio.afioaos.compositor;

import com.sun.jna.NativeLong;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import javax.management.RuntimeErrorException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class WindowManager extends Thread {

    boolean isInterrupt = false;

    public static X11 x11;
    public static X11c x11c;
    private final X11.Display display;
    private final X11.Window root;
    private final Map<X11.Window, Frame> clients = new HashMap<>();

    public WindowManager(String displayName) {
        x11 = X11.INSTANCE;
        x11c = X11c.INSTANCE;
        if(displayName == null || displayName.isBlank() || displayName.isEmpty()) {
            throw new NullPointerException("Display name cannot be null");
        }
        this.display = x11.XOpenDisplay(displayName);
        if(display == null) throw new NullPointerException("XOpenDisplay returned null");
        this.root = x11.XDefaultRootWindow(display);
    }

    public void run() {
        x11c.XGrabServer(display);

        X11.WindowByReference rRoot = new X11.WindowByReference(), rParent = new X11.WindowByReference();
        PointerByReference windows = new PointerByReference();
        IntByReference windowsSize = new IntByReference();

        x11.XQueryTree(display, root, rRoot, rParent, windows, windowsSize);

        if(!rRoot.getValue().equals(root)) throw new RuntimeException("Returned root should be equal as root");

        int[] ids = windows.getValue().getIntArray(0, windowsSize.getValue());
        for(int id : ids) {
            frameWindow(new X11.Window(id), true);
        }
        x11.XFree(windows.getValue());
        x11c.XUngrabServer(display);

        detectWindowManager();

        while (!isInterrupt) {
            X11.XEvent event = new X11.XEvent();
            x11.XNextEvent(display, event);
            System.out.println(event.type);

            switch (event.type) {
                case X11.CreateNotify -> onCreateNotify();
                case X11.ConfigureRequest -> onConfigureRequest(event.xconfigurerequest);
                case X11.ConfigureNotify -> onConfigureNotify(event.xconfigure);
                case X11.MapRequest -> onMapRequest(event.xmaprequest);
                case X11.ReparentNotify -> onReparentNotify(event.xreparent);
                case X11.MapNotify -> onMapNotify(event.xmap);
                case X11.UnmapNotify -> onUnmapNotify(event.xunmap);
                case X11.DestroyNotify -> onDestroyNotify(event.xdestroywindow);

                case X11.ButtonPress -> onButtonPress(event.xbutton);
            }
        }
    }

    private void onCreateNotify() {}

    private void onConfigureRequest(X11.XConfigureRequestEvent event) {
        X11c.XWindowChanges changes = new X11c.XWindowChanges();

        changes.x = event.x;
        changes.y = event.y;
        changes.width = event.width;
        changes.height = event.height;
        changes.border_width = event.border_width;
        changes.sibling = event.above;
        changes.stack_mode = event.detail;

        if(clients.containsKey(event.window)) {
            final X11.Window frame = clients.get(event.window).getFrame();
            x11c.XConfigureWindow(display, frame, event.value_mask.intValue(), changes);
        }

        x11c.XConfigureWindow(display, event.window, event.value_mask.intValue(), changes);
    }

    private void onConfigureNotify(X11.XConfigureEvent event) {}

    private void onMapRequest(X11.XMapRequestEvent event) {
        frameWindow(event.window, false);
        x11.XMapWindow(display, event.window);
    }

    private void onReparentNotify(X11.XReparentEvent event) {}

    private void onMapNotify(X11.XMapEvent event) {}

    private void onUnmapNotify(X11.XUnmapEvent event) {
        if(!clients.containsKey(event.window) || event.event.equals(root)) return;

        unframeWindow(event.window);
    }

    private void onDestroyNotify(X11.XDestroyWindowEvent event) {}

    private void onButtonPress(X11.XButtonEvent event) {
        System.out.println("FUCK: "+event.button);
        switch (event.button) {
            case X11.Button1 -> {
                clients.get(event.window).move(event.x_root - event.x, event.y_root - event.y);
                break;
            }
        }
    }

    private void detectWindowManager() {
        AtomicBoolean detected = new AtomicBoolean(false);
        x11.XSetErrorHandler((display, errorEvent) -> {
            if(errorEvent.type == X11.BadAccess) {
                detected.set(true);
            }
            return 0;
        });
        x11.XSelectInput(display, root, new NativeLong(X11.SubstructureRedirectMask | X11.SubstructureNotifyMask));
        x11.XSync(display, false);
        if(detected.get()) {
            throw new RuntimeErrorException(new Error("Another Window Manager is already running."));
        }
    }

    final int borderWidth = 3;
    final int borderColor = 0xff0000;
    final int backgroundColor = 0x0000ff;

    private void frameWindow(X11.Window window, boolean wasItCreatedBefore) {
        if(clients.containsKey(window)) return;
        Frame frame = new Frame(display, root, window);
        clients.put(window, frame);
    }

    private void unframeWindow(X11.Window window) {
        Frame frame = clients.get(window);
        frame.destroy();
        clients.remove(frame);
    }

    @Override
    public void interrupt() {
        super.interrupt();
        isInterrupt = true;
        x11.XCloseDisplay(display);
    }
}
