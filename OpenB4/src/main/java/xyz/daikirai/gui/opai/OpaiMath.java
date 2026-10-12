package xyz.daikirai.gui.opai;

public final class OpaiMath {
    private OpaiMath() { }
    public static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    public static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    public static int alpha(int color, float factor) { return (color & 0xFFFFFF) | Math.round((color >>> 24) * clamp(factor, 0, 1)) << 24; }
    public static int mix(int from, int to, float amount) {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int a = (from >>> shift) & 255, b = (to >>> shift) & 255;
            result |= Math.round(a + (b - a) * clamp(amount, 0, 1)) << shift;
        }
        return result;
    }
    public static String display(String name) { return name.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2").replaceAll("([a-z0-9])([A-Z])", "$1 $2"); }
}
