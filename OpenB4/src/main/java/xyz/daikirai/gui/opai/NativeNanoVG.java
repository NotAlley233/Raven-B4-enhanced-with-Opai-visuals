package xyz.daikirai.gui.opai;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** JNI bridge to NanoVG GL2. It shares LWJGL 2's current context without loading LWJGL 3. */
public final class NativeNanoVG {
    private static boolean loaded;
    private NativeNanoVG() { }

    public static synchronized void load() throws Exception {
        if (loaded) return;
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        String arch = System.getProperty("os.arch", "");
        if (!os.startsWith("windows") || !(arch.equals("amd64") || arch.equals("x86_64"))) {
            throw new IllegalStateException("The bundled Opai renderer supports Windows x64; Raven remains available.");
        }
        Path directory = Files.createTempDirectory("raven-nanovg-");
        Path library = directory.resolve("raven-nanovg.dll");
        Files.write(library, resource("native/windows-x86_64/raven-nanovg.dll"));
        directory.toFile().deleteOnExit(); library.toFile().deleteOnExit();
        System.load(library.toAbsolutePath().toString());
        loaded = true;
    }

    static byte[] resource(String name) throws Exception {
        try (InputStream stream = NativeNanoVG.class.getResourceAsStream("/assets/raven/opai/" + name)) {
            if (stream == null) throw new IllegalStateException("Missing Opai resource: " + name);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] block = new byte[8192]; int count;
            while ((count = stream.read(block)) >= 0) bytes.write(block, 0, count);
            return bytes.toByteArray();
        }
    }

    static native long create();
    static native void destroy(long handle);
    static native int font(long handle, String name, byte[] data);
    static native void begin(long handle, float width, float height, int pixelWidth, int pixelHeight);
    static native void end(long handle);
    static native void command(long handle, int operation, float[] arguments, int color);
    static native float text(long handle, byte[] utf8, float x, float y, float size, int font, int color, float tracking, boolean draw, int alignment);
    static native int image(long handle, int width, int height, byte[] rgba, boolean nearest);
    static native void deleteImage(long handle, int image);
    static native void testContext(int width, int height);
    static native void clearTestFrame();
    static native byte[] pixels(int width, int height);
    static native int glError();
    static native String driver();
    static native int[] state();
    static native void closeTestContext();
}
