package com.ashbysoft.wayland;

import java.nio.ByteBuffer;

public class XdgSurface extends WaylandObject<XdgSurface.Listener> {

    public interface Listener {
        boolean xdgSurfaceConfigure(int serial);
    }
    public static final int RQ_DESTROY = 0;
    public static final int RQ_GET_TOPLEVEL = 1;
    public static final int RQ_GET_POPUP = 2;
    public static final int RQ_SET_WINDOW_GEOMETRY = 3;
    public static final int RQ_ACK_CONFIGURE = 4;
    public static final int EV_CONFIGURE = 0;

    public XdgSurface(Display d) { super(d); }
    public boolean handle(int oid, int op, int size, ByteBuffer b) {
        boolean rv = true;
        if (EV_CONFIGURE == op) {
            int serial = b.getInt();
            log(true, "configure:serial="+serial);
            for (Listener l : listeners())
                if (!l.xdgSurfaceConfigure(serial))
                    rv = false;
        }
        return rv;
    }

    public boolean destroy() {
        ByteBuffer b = newBuffer(8, RQ_DESTROY);
        log(false, "destroy");
        return _display.write(b);
    }
    public boolean getTopLevel(XdgToplevel t) {
        ByteBuffer b = newBuffer(12, RQ_GET_TOPLEVEL);
        b.putInt(t.getID());
        log(false, "getTopLevel->"+t.getID());
        return _display.write(b);
    }
    public boolean getPopup(XdgPopup u, XdgSurface x, Positioner p) {
        ByteBuffer b = newBuffer(20, RQ_GET_POPUP);
        b.putInt(u.getID());
        b.putInt(x.getID());
        b.putInt(p.getID());
        log(false, "getPopup->"+u.getID()+":xs="+x.getID()+" p="+p.getID());
        return _display.write(b);
    }
    public boolean setWindowGeometry(int x, int y, int w, int h) {
        ByteBuffer b = newBuffer(24, RQ_SET_WINDOW_GEOMETRY);
        b.putInt(x);
        b.putInt(y);
        b.putInt(w);
        b.putInt(h);
       log(false, "setWindowGeometry:x="+x+" y="+y+" w="+w+" h="+h);
         return _display.write(b);
    }
    public boolean ackConfigure(int serial) {
        ByteBuffer b = newBuffer(12, RQ_ACK_CONFIGURE);
        b.putInt(serial);
        log(false, "ackConfigure:serial="+serial);
        return _display.write(b);
    }
}