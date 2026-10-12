package xyz.daikirai.gui.opai;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;

/** One coordinate system for UI, HUD and the real OpenGL verification renderer. */
public final class OpaiCanvas {
    private long context;
    private boolean failed, frame;
    private float alpha = 1;
    private final Map<String, Integer> fonts = new HashMap<>();
    private final Map<String, byte[]> strings = new java.util.LinkedHashMap<String, byte[]>(512, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, byte[]> entry) { return size() > 1024; }
    };

    public boolean available() {
        if (failed) return false;
        if (context != 0) return true;
        try {
            NativeNanoVG.load(); context = NativeNanoVG.create();
            for (String font : new String[]{"googlesans-regular", "googlesans-medium", "googlesans-bold", "productsans-medium", "sourcesans3-semibold"}) {
                fonts.put(font, NativeNanoVG.font(context, font, NativeNanoVG.resource("fonts/" + font + ".ttf")));
            }
            return true;
        } catch (Exception | LinkageError error) {
            failed = true;
            if (context != 0) { NativeNanoVG.destroy(context); context = 0; }
            LogManager.getLogger("raven-opai").error("Opai renderer unavailable; keeping Raven visuals", error);
            return false;
        }
    }

    public boolean begin(float width, float height, int pixelWidth, int pixelHeight) {
        if (!available()) return false;
        if (frame) throw new IllegalStateException("Nested Opai frame");
        NativeNanoVG.begin(context, width, height, pixelWidth, pixelHeight);
        frame = true; alpha = 1; return true;
    }
    public void end() { if (frame) { try { NativeNanoVG.end(context); } finally { frame = false; } } }
    public void close() { end(); if (context != 0) { NativeNanoVG.destroy(context); context = 0; } }
    private void command(int op, int color, float... values) { NativeNanoVG.command(context, op, values, color); }
    public void save() { command(0, 0); }
    public void restore() { command(1, 0); }
    public void rounded(float x, float y, float w, float h, float radius, int color) {
        if (w > 0 && h > 0) command(2, color, x, y, w, h, Math.min(radius, Math.min(w, h) / 2));
    }
    public void rect(float x, float y, float w, float h, int color) { rounded(x, y, w, h, 0, color); }
    public void varying(float x, float y, float w, float h, float tl, float tr, float br, float bl, int color) {
        if (w > 0 && h > 0) command(3, color, x, y, w, h, tl, tr, br, bl);
    }
    public void panel(float x, float y, float w, float h, boolean header, int color) {
        varying(x, y, w, h, header ? 7 : 0, header ? 7 : 0, header ? 0 : 7, header ? 0 : 7, color);
    }
    public void clip(float x, float y, float w, float h, Runnable content) {
        if (w <= 0 || h <= 0) return;
        save(); try { command(4, 0, x, y, w, h); content.run(); } finally { restore(); }
    }
    public void opacity(float factor, Runnable content) {
        save(); float previous = alpha;
        try { alpha *= factor; command(5, 0, alpha); content.run(); }
        finally { alpha = previous; restore(); }
    }
    public void scale(float factor, float cx, float cy, Runnable content) {
        save(); try { translate(cx, cy); scale(factor, factor); translate(-cx, -cy); content.run(); } finally { restore(); }
    }
    public void translate(float x, float y) { command(6, 0, x, y); }
    public void scale(float x, float y) { command(7, 0, x, y); }
    public void rotate(float radians) { command(19, 0, radians); }
    public void shadow(float x, float y, float w, float h, float radius) { command(20, 0x65000000, x, y, w, h, radius); }
    public void ripple(float x, float y, float w, float h, float corner, float cx, float cy, float radius, int color) {
        command(21, color, x, y, w, h, corner, cx, cy, radius);
    }
    public void path() { command(8, 0); }
    public void move(float x, float y) { command(9, 0, x, y); }
    public void lineTo(float x, float y) { command(10, 0, x, y); }
    public void bezier(float x1, float y1, float x2, float y2, float x3, float y3) { command(11, 0, x1, y1, x2, y2, x3, y3); }
    public void quad(float x1, float y1, float x2, float y2) { command(12, 0, x1, y1, x2, y2); }
    public void circle(float x, float y, float r) { command(13, 0, x, y, r); }
    public void closePath() { command(14, 0); }
    public void fill(int color) { command(15, color); }
    public void stroke(float width, int color) { command(16, color, width); }
    public void arc(float x, float y, float r, float start, float end, boolean clockwise) { command(17, 0, x, y, r, start, end, clockwise ? 2 : 1); }
    public void hole() { command(18, 0); }
    public void roundedPath(float x, float y, float w, float h, float tl, float tr, float br, float bl) { command(22, 0, x, y, w, h, tl, tr, br, bl); }
    public void line(float x1, float y1, float x2, float y2, float width, int color) { path(); move(x1, y1); lineTo(x2, y2); stroke(width, color); }
    public void check(float x, float y, float size, int color) { path(); move(x, y + size * .5f); lineTo(x + size * .38f, y + size * .88f); lineTo(x + size, y); stroke(1, color); }
    public int image(int width, int height, byte[] rgba) { return image(width,height,rgba,true); }
    public int image(int width, int height, byte[] rgba,boolean nearest) { return NativeNanoVG.image(context,width,height,rgba,nearest); }
    public void deleteImage(int image) { NativeNanoVG.deleteImage(context, image); }
    public void image(int image, float x, float y, float w, float h, float radius, float opacity) { command(23, 0, x, y, w, h, radius, image, opacity); }

    private byte[] utf8(String text) { return strings.computeIfAbsent(text, t -> t.getBytes(StandardCharsets.UTF_8)); }
    public float measure(String text, float size, String font, float tracking) {
        return text.isEmpty() ? 0 : NativeNanoVG.text(context, utf8(text), 0, 0, size, fonts.get(font), 0, tracking, false,16);
    }
    public float measure(String text, float size) { return measure(text, size, "googlesans-medium", 0) + (text.isEmpty() ? 0 : .10f); }
    public String fit(String text, float size, float maximum, String font, float tracking) {
        if (measure(text, size, font, tracking) <= maximum) return text;
        if (measure("...", size, font, tracking) > maximum) return "";
        int end = text.length();
        while (end > 0 && measure(text.substring(0, end) + "...", size, font, tracking) > maximum) end = text.offsetByCodePoints(end, -1);
        return text.substring(0, end) + "...";
    }
    public void text(String text, float x, float cy, float size, float maximum, int color) {
        String fitted = fit(text, size, maximum, "googlesans-medium", 0);
        text(fitted, x, cy, size, "googlesans-medium", 0, color);
        text(fitted, x + .10f, cy, size, "googlesans-medium", 0, color);
    }
    public void plainText(String text, float x, float cy, float size, float maximum, int color) {
        text(fit(text, size, maximum, "googlesans-regular", 0), x, cy, size, "googlesans-regular", 0, color);
    }
    public void text(String text, float x, float cy, float size, String font, float tracking, int color) {
        if (!text.isEmpty()) NativeNanoVG.text(context, utf8(text), x, cy, size, fonts.get(font), color, tracking, true,16);
    }
    public void inkText(String text,float x,float cy,float size,String font,float tracking,int color) {
        if(!text.isEmpty()) NativeNanoVG.text(context,utf8(text),x,cy,size,fonts.get(font),color,tracking,true,64|128);
    }
    public void baselineText(String text,float x,float y,float size,String font,int color) {
        if(!text.isEmpty()) NativeNanoVG.text(context,utf8(text),x,y,size,fonts.get(font),color,0,true,64);
    }
}
