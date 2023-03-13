package com.ashbysoft.wayland;

import java.nio.ByteBuffer;

public class Compositor extends WaylandObject {
    public static final int RQ_CREATE_SURFACE = 0;
    public static final int RQ_CREATE_REGION = 1;

    private Display _display;
    public Compositor(Display d) { _display = d; }

    public boolean createSurface(Surface s) {
        ByteBuffer b = newBuffer(12);
        b.putInt(getID());
        b.putInt(RQ_CREATE_SURFACE);
        b.putInt(s.getID());
        return _display.write(b);
    }
    public boolean createRegion(Region r) {
        ByteBuffer b = newBuffer(12);
        b.putInt(getID());
        b.putInt(RQ_CREATE_SURFACE);
        b.putInt(r.getID());
        return _display.write(b);
    }
}