package xyz.daikirai.gui.opai;

import java.util.ArrayDeque;

/** Samsara's click-origin ink and press feedback, ported to Java 8 (GPL-3.0). */
final class OpaiFeedback {
    private final ArrayDeque<Pulse> pulses = new ArrayDeque<>();
    private final OpaiMotion hover = new OpaiMotion(0, 20);
    private double pressedAt = Double.NEGATIVE_INFINITY;
    void click(float x, float y, float w, float h, double now) {
        pressedAt = now;
        while (pulses.size() >= 3) pulses.removeFirst();
        pulses.addLast(new Pulse(OpaiMath.clamp(x / w, 0, 1), OpaiMath.clamp(y / h, 0, 1), now));
    }
    float hover(boolean hovered, double now) { return hover.approach(hovered ? 1 : 0, now); }
    float press(double now) {
        double age = now - pressedAt;
        return age < 0 || age >= 220 ? 0 : (float)(Math.sin(Math.PI * age / 220) * Math.exp(-age / 220));
    }
    void paint(OpaiCanvas c, float x, float y, float w, float h, float corner, int color, double now) {
        pulses.removeIf(p -> now - p.started >= 420);
        for (Pulse p : pulses) {
            float t = (float)OpaiMath.clamp((now - p.started) / 420, 0, 1);
            float cx = p.x * w, cy = p.y * h;
            float max = (float)Math.hypot(Math.max(cx, w - cx), Math.max(cy, h - cy));
            float growth = 1 - (float)Math.pow(1 - Math.min(1, t / .75f), 3);
            c.ripple(x, y, w, h, corner, x + cx, y + cy, Math.max(.5f, max * growth), OpaiMath.alpha(color, .22f * (1 - t) * (1 - t)));
        }
    }
    private static final class Pulse {
        final float x, y; final double started;
        Pulse(float x, float y, double started) { this.x=x; this.y=y; this.started=started; }
    }
}
