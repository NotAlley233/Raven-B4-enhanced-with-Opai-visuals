// Adapted from Samsara 23aa1b7; GPL-3.0. Java 8 compatibility changes.
package xyz.daikirai.gui.opai;

import java.util.ArrayList;
import java.util.List;

public final class DynamicIslandStatus {
      private final String username;
      private final String server;
      private final int ping;
      private final int fps;
      public DynamicIslandStatus(String username, String server, int ping, int fps) {
         this.username = username;
         this.server = server;
         this.ping = ping;
         this.fps = fps;
      }
      public String username() { return this.username; }
      public String server() { return this.server; }
      public int ping() { return this.ping; }
      public int fps() { return this.fps; }

   public static final float FONT_SIZE = 8;
   public static final float INSET = 9;
   public static final float RIGHT_INSET = 12.5f;
   public static final float ICON_SIZE = 10.5f;
   public static final float ICON_GAP = 3.5f;
   public static final int FOREGROUND = 0xFFE4E1E6;
   public static final int LAVENDER = 0xFFBBC3FF;
   public static final int ONLINE = 0xFF80C810;
   public static final int PING_ORANGE = 0xFFC87D28;
   public static final int PING_RED = 0xFFCA3E2E;

   public enum Symbol { CHROME, USER, LINK, REFRESH, BED }
   public static final class Part {
      private final String text;
      private final Symbol icon;
      private final int color;
      private final float x;
      private final float width;
      public Part(String text, Symbol icon, int color, float x, float width) {
         this.text = text;
         this.icon = icon;
         this.color = color;
         this.x = x;
         this.width = width;
      }
      public String text() { return this.text; }
      public Symbol icon() { return this.icon; }
      public int color() { return this.color; }
      public float x() { return this.x; }
      public float width() { return this.width; }
 }
   public static final class Layout {
      private final List<Part> parts;
      private final float width;
      public Layout(List<Part> parts, float width) {
         this.parts = parts;
         this.width = width;
      }
      public List<Part> parts() { return this.parts; }
      public float width() { return this.width; }
 }

   public boolean singleplayer() {
      return this.server == null;
   }

   public Layout layout(DynamicIslandState.TextWidth measure, float available) {
      List<Part> parts = new ArrayList<>();
      // Shorten only genuinely oversized names/addresses, never because the shell is animating.
      float fixed = INSET + RIGHT_INSET + 4 * (ICON_SIZE + ICON_GAP)
         + 3 * (2 + measure.measure("  ·  ", FONT_SIZE)) + measure.measure("Opai", FONT_SIZE)
         + measure.measure(Math.max(0, this.fps) + " fps", FONT_SIZE)
         + (singleplayer() ? 0 : measure.measure(pingText() + " to ", FONT_SIZE));
      String connection = singleplayer() ? "singleplayer" : this.server;
      float remaining = Math.max(0, available - fixed);
      float userWidth = measure.measure(this.username, FONT_SIZE);
      float connectionWidth = measure.measure(connection, FONT_SIZE);
      float userBudget = userWidth + connectionWidth <= remaining ? userWidth
         : Math.min(userWidth, Math.max(remaining - connectionWidth, remaining * .4f));
      String user = DynamicIslandState.fit(this.username, FONT_SIZE, userBudget, measure);
      connection = DynamicIslandState.fit(connection, FONT_SIZE,
         Math.max(0, remaining - measure.measure(user, FONT_SIZE)), measure);
      float x = INSET;
      x = icon(parts, Symbol.CHROME, LAVENDER, x);
      x = label(parts, "Opai", LAVENDER, x, measure);
      x = separator(parts, x, measure);
      x = icon(parts, Symbol.USER, FOREGROUND, x);
      x = label(parts, user, FOREGROUND, x, measure);
      x = separator(parts, x, measure);
      x = icon(parts, Symbol.LINK, singleplayer() ? FOREGROUND : pingColor(), x);
      if (!singleplayer()) {
         x = label(parts, pingText(), pingColor(), x, measure);
         x = label(parts, " to ", FOREGROUND, x, measure);
      }
      x = label(parts, connection, FOREGROUND, x, measure);
      x = separator(parts, x, measure);
      x = icon(parts, Symbol.REFRESH, FOREGROUND, x);
      x = label(parts, Math.max(0, this.fps) + " fps", FOREGROUND, x, measure);
      return new Layout(java.util.Collections.unmodifiableList(new ArrayList<>(parts)), x + RIGHT_INSET);
   }

   private String pingText() {
      return this.ping < 0 ? "...ms" : this.ping + "ms";
   }

   private int pingColor() {
      return this.ping < 0 ? FOREGROUND : this.ping < 100 ? ONLINE
         : this.ping <= 400 ? PING_ORANGE : PING_RED;
   }

   private static float icon(List<Part> parts, Symbol icon, int color, float x) {
      parts.add(new Part("", icon, color, x, ICON_SIZE));
      return x + ICON_SIZE + ICON_GAP;
   }

   private static float label(List<Part> parts, String text, int color, float x,
                              DynamicIslandState.TextWidth measure) {
      float width = measure.measure(text, FONT_SIZE);
      parts.add(new Part(text, null, color, x, width));
      return x + width;
   }

   private static float separator(List<Part> parts, float x, DynamicIslandState.TextWidth measure) {
      return label(parts, "  ·  ", FOREGROUND, x + 1.25f, measure) + .75f;
   }
}
