package com.ashbysoft.swingland;

import com.ashbysoft.logger.Logger;
import java.util.HashMap;
import java.io.InputStream;
import java.io.IOException;

// Lazy loaded fonts - currently just fixed size bitmap fonts from internal resources
public class Font implements FontMetrics {
    public static final String FONT_PATH = "/fonts/";
    public static final String MONOSPACED = "MONOSPACED";

    private static final Logger _log = new Logger("[Font]:");
    private static final HashMap<String, Font> _fontCache = new HashMap<String, Font>();
    private String _name;
    private int _width;
    private int _height;
    private int _baseline;
    private int _leading;
    private int _glyphBytes;
    private int _offset;
    private int _count;
    private int _missing;
    private byte[] _buffer;

    protected Font(String name) { _name = name; }
    public String getFontName() { return _name; }
    public int getMissingGlyphCode() { return _missing; }
    public boolean canDisplay(char c) {
        return canDisplay((int)c);
    }
    public boolean canDisplay(int codePoint) {
        if (!ensureLoaded())
            return false;
        if ((codePoint - _offset) < _count)
            return true;
        return false;
    }
    // package-private bitmap renderer, not the formal scalable fonts API in the JDK..which is *huge*
    void renderString(Graphics g, String s, int x, int y) {
        int cx = 0;
        for (int i = 0; i < s.length(); i += 1) {
            int cp = s.codePointAt(i);
            if (!canDisplay(cp))
                cp = getMissingGlyphCode();
            cp -= _offset;
            int o = cp * _glyphBytes;
            int p = 7;
            for (int gy = 0; gy < _height; gy += 1) {
                for (int gx = 0; gx < _width; gx += 1) {
                    if ((_buffer[o] & (1 << p)) != 0)
                        g.setPixel(x+gx+cx, y+gy-_height);
                    p -= 1;
                    if (p < 0) {
                        p = 7;
                        o += 1;
                    }
                }
            }
            cx += _width;
        }
    }

    // FontMetrics API
    // NB: getAscent()+getDescent()+getLeading() == getHeight()
    public FontMetrics getFontMetrics() { return this; }
    public Font getFont() { return this; }
    public int getAscent() { return _height - _leading - _baseline; }
    public int getDescent() { return _baseline; }
    public int getHeight() { return _height; }
    public int getLeading() { return _leading; }
    public int stringWidth(String s) { return s.length() * _width; }

    // Lazy loader
    private synchronized boolean ensureLoaded() {
        if (_buffer != null)
            return true;
        try (InputStream in = getClass().getResourceAsStream(FONT_PATH + _name)) {
            // font header is four bytes, specifying: glyph dimensions (w x h), ASCII offset and glyph count (0=256)
            byte[] hdr = new byte[16];
            if (in.read(hdr) != 16)
                throw new IOException("font resource < 16 bytes");
            // font data is prefixed with a header containing..
            int gw = (int)hdr[0] & 0xff;    // glyph width
            int gh = (int)hdr[1] & 0xff;    // glyph height
            int gb = (int)hdr[2] & 0xff;    // glyph baseline position
            int gl = (int)hdr[3] & 0xff;    // glyph leading (interline gap)
            // character offset (into unicode list)
            int go = ((int)hdr[4] & 0xff) | (((int)hdr[5] & 0xff) << 8) | (((int)hdr[6] & 0xff) << 16) | (((int)hdr[7] & 0xff) << 24);
            // character count (in file)
            int gc = ((int)hdr[8] & 0xff) | (((int)hdr[9] & 0xff) << 8) | (((int)hdr[10] & 0xff) << 16) | (((int)hdr[11] & 0xff) << 24);
            // missing glyph code (unicode value)
            int mg = ((int)hdr[12] & 0xff) | (((int)hdr[13] & 0xff) << 8) | (((int)hdr[14] & 0xff) << 16) | (((int)hdr[15] & 0xff) << 24);
            // calculate byte size of a glyph, and load them into buffer
            int bpg = (gw * gh) / 8;
            byte[] buf = new byte[bpg * gc];
            if (in.read(buf) != (bpg * gc))
                throw new IOException("font resource shorter than declared size: ("+gw+"x"+gh+"x"+gc+")");
            // all good - stash info
            _width = gw;
            _height = gh;
            _baseline = gb;
            _leading = gl;
            _glyphBytes = bpg;
            _offset = go;
            _count = gc;
            _missing = mg;
            _buffer = buf;
            _log.info("lazy loaded: "+_name+": ("+gw+"x"+gh+"/"+gb+"/"+gl+"):"+go+"->"+(go+gc)+"/"+mg);
            return true;
        } catch (IOException e) {
            _log.error("unable to load font resource: "+FONT_PATH + _name + ": "+e.toString());
        }
        return false;
    }

    // Factory method
    public static Font getFont(String name) {
        // cached?
        synchronized (_fontCache) {
            if (!_fontCache.containsKey(name))
                _fontCache.put(name, new Font(name));
            return _fontCache.get(name);
        }
    }
}
