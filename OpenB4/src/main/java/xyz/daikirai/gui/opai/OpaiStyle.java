// Adapted from Samsara 23aa1b7; GPL-3.0. Java 8 compatibility changes.
package xyz.daikirai.gui.opai;

public final class OpaiStyle {
   public static final class Palette {
      private final int header;
      private final int body;
      private final int enabled;
      private final int enabledHover;
      private final int accent;
      private final int enabledText;
      private final int text;
      private final int hover;
      private final int field;
      private final int fieldLine;
      private final int track;
      private final int scrollbar;
      private final int selected;
      private final int toggleOutline;
      private final int tick;
      private final int hudProgress;
      private final int hudKnob;
      public Palette(int header, int body, int enabled, int enabledHover, int accent,
                         int enabledText, int text, int hover, int field, int fieldLine,
                         int track, int scrollbar, int selected, int toggleOutline, int tick,
                         int hudProgress, int hudKnob) {
         this.header = header;
         this.body = body;
         this.enabled = enabled;
         this.enabledHover = enabledHover;
         this.accent = accent;
         this.enabledText = enabledText;
         this.text = text;
         this.hover = hover;
         this.field = field;
         this.fieldLine = fieldLine;
         this.track = track;
         this.scrollbar = scrollbar;
         this.selected = selected;
         this.toggleOutline = toggleOutline;
         this.tick = tick;
         this.hudProgress = hudProgress;
         this.hudKnob = hudKnob;
      }
      public int header() { return this.header; }
      public int body() { return this.body; }
      public int enabled() { return this.enabled; }
      public int enabledHover() { return this.enabledHover; }
      public int accent() { return this.accent; }
      public int enabledText() { return this.enabledText; }
      public int text() { return this.text; }
      public int hover() { return this.hover; }
      public int field() { return this.field; }
      public int fieldLine() { return this.fieldLine; }
      public int track() { return this.track; }
      public int scrollbar() { return this.scrollbar; }
      public int selected() { return this.selected; }
      public int toggleOutline() { return this.toggleOutline; }
      public int tick() { return this.tick; }
      public int hudProgress() { return this.hudProgress; }
      public int hudKnob() { return this.hudKnob; }
 }

   static final float HEADER_TEXT_SIZE = 9.2f;
   static final float ROW_TEXT_SIZE = 8.3f;
   static final float TEXT_WEIGHT_OFFSET = 0.10f;

   public static final Palette LAVENDER = new Palette(
      0xFF1C1F29, 0xCC151315, 0xFFA8B0E7, 0xFFB0B8EF, 0xFFBBC3FF,
      0xFF172778, 0xFFE4E1E6, 0x0CE4E1E6, 0xFF2D2D38, 0xFFC4C0CC,
      0xFF484858, 0xCC636472, 0xFF414253, 0xFF938F99, 0xFF626786, 0xFF9CA5DA, 0xFF5B5F7C);
   public static final Palette LIGHT_PINK = new Palette(
      0xFF161110, 0xCC1A1413, 0xFFDF9D95, 0xFFE5A59D, 0xFFFFB4AA,
      0xFF5F150F, 0xFFEDE0DE, 0x0CEDE0DE, 0xFF2E2625, 0xFFCDB7B4,
      0xFF56413E, 0xCC69534D, 0xFF483A37, 0xFF9E8D8B, 0xFF85645D, 0xFFD19997, 0xFF7C5752);

   private OpaiStyle() {
   }

   public static Palette palette(String name) {
      return "Light Pink".equals(name) ? LIGHT_PINK : LAVENDER;
   }
}
