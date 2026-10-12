// Adapted from Samsara 23aa1b7; GPL-3.0. Java 8 compatibility changes.
package xyz.daikirai.gui.opai;

import java.util.ArrayList;
import java.util.List;

// Jon_awa (2025-05-09), MIT.
public final class DynamicIslandState {
   public static final float TOP = 15;
   public static final float IDLE_TOP = 21;
   public static final float IDLE_HEIGHT = 23;
   public static final float ROW_HEIGHT = 36;
   public static final float SCAFFOLD_HEIGHT = 50;
   public static final float BREAKING_HEIGHT = 48;
   public static final float BREAKING_TITLE_FONT = 9;
   public static final float BREAKING_DETAIL_FONT = 8;
   public static final float SCAFFOLD_TEXT_X = 36;
   public static final float SCAFFOLD_RIGHT_PADDING = 8;
   public static final float RADIUS = 8.5f;
   public static final float TITLE_FONT = 10;
   public static final float DETAIL_FONT = 9;
   public static final float NOTICE_TEXT_X = 36;
   public static final float NOTICE_RIGHT_PADDING = 5.5f;
   public static final int MAX_NOTICES = 3;
   public static final long FADE_MS = 200;
   public static final long TOGGLE_MS = 800;
   private static final double MORPH_DECAY = 8.7;
   private static final double MORPH_FREQUENCY = 8;
   private final List<Notice> notices = new ArrayList<>();
   private final Morph width = new Morph();
   private final Morph height = new Morph();
   private final Morph position = new Morph();
   private boolean wasIdle = true;
   private boolean wasPanel;

   public enum Icon { TOGGLE, SUCCESS, WARNING, INFO, SCAFFOLD, BREAKING }

   @FunctionalInterface
   public interface TextWidth {
      float measure(String text, float size);

      default float measureBreaking(String text, float size) {
         return measure(text, size);
      }
   }

   public static final class Row {
      private final String title;
      private final String detail;
      private final String status;
      private final Icon icon;
      private final boolean enabled;
      private final float toggle;
      private final float progress;
      private final float y;
      private final float height;
      private final float opacity;
      public Row(String title, String detail, String status, Icon icon, boolean enabled,
                     float toggle, float progress, float y, float height, float opacity) {
         this.title = title;
         this.detail = detail;
         this.status = status;
         this.icon = icon;
         this.enabled = enabled;
         this.toggle = toggle;
         this.progress = progress;
         this.y = y;
         this.height = height;
         this.opacity = opacity;
      }
      public String title() { return this.title; }
      public String detail() { return this.detail; }
      public String status() { return this.status; }
      public Icon icon() { return this.icon; }
      public boolean enabled() { return this.enabled; }
      public float toggle() { return this.toggle; }
      public float progress() { return this.progress; }
      public float y() { return this.y; }
      public float height() { return this.height; }
      public float opacity() { return this.opacity; }
 }
   public static final class Frame {
      private final float width;
      private final float height;
      private final float idleOpacity;
      private final DynamicIslandStatus.Layout idle;
      private final List<Row> rows;
      private final float contentWidth;
      private final float top;
      private final float radius;
      public Frame(float width, float height, float idleOpacity, DynamicIslandStatus.Layout idle, List<Row> rows,
                       float contentWidth, float top, float radius) {
         this.width = width;
         this.height = height;
         this.idleOpacity = idleOpacity;
         this.idle = idle;
         this.rows = rows;
         this.contentWidth = contentWidth;
         this.top = top;
         this.radius = radius;
      }
      public float width() { return this.width; }
      public float height() { return this.height; }
      public float idleOpacity() { return this.idleOpacity; }
      public DynamicIslandStatus.Layout idle() { return this.idle; }
      public List<Row> rows() { return this.rows; }
      public float contentWidth() { return this.contentWidth; }
      public float top() { return this.top; }
      public float radius() { return this.radius; }
 }
   public static final class DetailLine {
      private final String prefix;
      private final String status;
      private final float statusOffset;
      public DetailLine(String prefix, String status, float statusOffset) {
         this.prefix = prefix;
         this.status = status;
         this.statusOffset = statusOffset;
      }
      public String prefix() { return this.prefix; }
      public String status() { return this.status; }
      public float statusOffset() { return this.statusOffset; }
 }
   public static final class Panel {
      private final float width;
      private final float height;
      private final float top;
      private final float radius;
      public Panel(float width, float height, float top, float radius) {
         this.width = width;
         this.height = height;
         this.top = top;
         this.radius = radius;
      }
      public float width() { return this.width; }
      public float height() { return this.height; }
      public float top() { return this.top; }
      public float radius() { return this.radius; }
 }

   public void post(String key, String title, String detail, String status, Icon icon,
                    boolean enabled, long now, long duration) {
      post(key, title, detail, status, icon, enabled, 0, now, duration);
   }

   public void postScaffold(String detail, float progress, long now) {
      this.notices.removeIf(item -> !item.key.equals("scaffold"));
      post("scaffold", "Scaffold Toggled", detail, "", Icon.SCAFFOLD, true,
         OpaiMath.clamp(progress, 0, 1), now, 500);
   }

   public void postBreaking(String blockName, float progress, long now) {
      String title = "Breaking " + blockName;
      // A new defense block retargets the same shell, without carrying the old block's width.
      this.notices.removeIf(item -> !item.key.equals("bed-aura") || !title.equals(item.title));
      float clamped = OpaiMath.clamp(progress, 0, 1);
      post("bed-aura", title, "Break Progress: " + (int)(clamped * 100) + "%", "", Icon.BREAKING, true,
         clamped, now, 500);
   }

   public void remove(String key) {
      this.notices.removeIf(item -> item.key.equals(key));
   }

   private void post(String key, String title, String detail, String status, Icon icon,
                     boolean enabled, float progress, long now, long duration) {
      this.notices.removeIf(item -> now >= item.expiresAt);
      Notice notice = this.notices.stream().filter(item -> item.key.equals(key)).findFirst().orElse(null);
      if (notice == null) {
         notice = new Notice(key, now);
         this.notices.add(notice);
      }
      notice.title = title;
      notice.detail = detail;
      notice.status = status;
      notice.icon = icon;
      notice.enabled = enabled;
      notice.progress = progress;
      notice.expiresAt = now + duration;
      notice.toggle.target(enabled ? 1 : 0, now);
      while (this.notices.size() > MAX_NOTICES) {
         this.notices.remove(0);
      }
   }

   public Frame frame(long now, float viewportWidth, DynamicIslandStatus status, TextWidth measure, boolean reducedMotion) {
      return frame(now, viewportWidth, status, measure, reducedMotion, null);
   }

   public Frame frame(long now, float viewportWidth, DynamicIslandStatus status, TextWidth measure,
                      boolean reducedMotion, Panel panel) {
      float available = Math.max(1, viewportWidth - 16);
      DynamicIslandStatus.Layout idle = status.layout(measure, available);
      float idleWidth = Math.min(available, idle.width());
      List<Row> rows = new ArrayList<>();
      float occupied = 0;
      float largestWidth = 0;
      this.notices.removeIf(notice -> now >= notice.expiresAt);
      for (Notice notice : this.notices) {
         if (panel != null) break;
         boolean progressPanel = notice.icon == Icon.SCAFFOLD || notice.icon == Icon.BREAKING;
         float fullHeight = notice.icon == Icon.BREAKING ? BREAKING_HEIGHT : progressPanel ? SCAFFOLD_HEIGHT : ROW_HEIGHT;
         rows.add(new Row(notice.title, notice.detail, notice.status, notice.icon, notice.enabled,
            reducedMotion ? (notice.enabled ? 1 : 0) : notice.toggle.value(now), notice.progress,
            occupied, fullHeight, 1));
         float textWidth = notice.icon == Icon.BREAKING
            ? Math.max(measure.measureBreaking(notice.title, BREAKING_TITLE_FONT),
               measure.measureBreaking("Break Progress: 100%", BREAKING_DETAIL_FONT))
            : Math.max(measure.measure(notice.title, TITLE_FONT),
               measure.measure(notice.detail, DETAIL_FONT) + measure.measure(notice.status, DETAIL_FONT)
               + (notice.status.isEmpty() ? 0 : measure.measure("!", DETAIL_FONT)));
         // Mining reserves all three percentage digits before the first frame.
         float contentWidth = progressPanel
            ? SCAFFOLD_TEXT_X + textWidth + SCAFFOLD_RIGHT_PADDING
            : NOTICE_TEXT_X + textWidth + NOTICE_RIGHT_PADDING;
         notice.contentWidth = Math.max(notice.contentWidth, contentWidth);
         largestWidth = Math.max(largestWidth, Math.min(available, notice.contentWidth));
         occupied += fullHeight;
      }
      boolean isIdle = panel == null && rows.isEmpty();
      float targetWidth = panel != null ? Math.min(available, panel.width()) : isIdle ? idleWidth : largestWidth;
      float targetHeight = panel != null ? panel.height() : OpaiMath.clamp(occupied, IDLE_HEIGHT, MAX_NOTICES * ROW_HEIGHT);
      // Fixed row coordinates keep the title, status and punctuation together during every frame.
      double impulse = isIdle != this.wasIdle || (panel != null) != this.wasPanel ? (isIdle ? 5.5 : 7.25) : 0;
      float animatedWidth = OpaiMath.clamp(this.width.value(targetWidth, now, reducedMotion, impulse), 1, available);
      float animatedHeight = Math.max(1, this.height.value(targetHeight, now, reducedMotion, impulse));
      boolean scaffold = rows.stream().anyMatch(row -> row.icon() == Icon.SCAFFOLD);
      float top = this.position.value(panel != null ? panel.top() : scaffold ? TOP : IDLE_TOP, now, reducedMotion, impulse);
      // Round the shrinking shell into a capsule when returning to idle.
      float radius = isIdle ? animatedHeight / 2 : panel != null ? panel.radius() : RADIUS;
      this.wasIdle = isIdle;
      this.wasPanel = panel != null;
      return new Frame(animatedWidth, animatedHeight, isIdle ? 1 : 0, idle, java.util.Collections.unmodifiableList(new ArrayList<>(rows)), targetWidth, top, radius);
   }

   public static float radius(Frame frame) {
      return frame.radius();
   }

   public static float top(Frame frame) {
      return frame.top();
   }

   public void clear() {
      this.notices.clear();
      this.width.clear();
      this.height.clear();
      this.position.clear();
      this.wasIdle = true;
      this.wasPanel = false;
   }

   /** Analytical damped spring: identical at any frame rate, with velocity retained on interruption. */
   private static final class Morph {
      private double current;
      private double target;
      private double velocity;
      private long sampled = -1;

      float value(float destination, long now, boolean reducedMotion, double impulse) {
         if (this.sampled < 0 || reducedMotion) {
            this.current = this.target = destination;
            this.velocity = 0;
         } else {
            double seconds = Math.max(0, now - this.sampled) / 1000.0;
            double displacement = this.current - this.target;
            double sineCoefficient = (this.velocity + MORPH_DECAY * displacement) / MORPH_FREQUENCY;
            double cosine = Math.cos(MORPH_FREQUENCY * seconds);
            double sine = Math.sin(MORPH_FREQUENCY * seconds);
            double decay = Math.exp(-MORPH_DECAY * seconds);
            double offset = displacement * cosine + sineCoefficient * sine;
            this.current = this.target + decay * offset;
            this.velocity = decay * (-MORPH_DECAY * offset
               + MORPH_FREQUENCY * (-displacement * sine + sineCoefficient * cosine));
            if (destination != this.target) {
               this.target = destination;
               // Start promptly from rest; a reversal retains the spring's actual velocity.
               if (Math.abs(this.velocity) < Math.max(.1, Math.abs(destination - this.current) * .1)) {
                  this.velocity = (destination - this.current) * impulse;
               }
            }
            if (Math.abs(this.current - this.target) < .01 && Math.abs(this.velocity) < .1) {
               this.current = this.target;
               this.velocity = 0;
            }
         }
         this.sampled = now;
         return (float)this.current;
      }

      void clear() {
         this.sampled = -1;
         this.current = this.target = this.velocity = 0;
      }
   }

   public static String fit(String text, float size, float width, TextWidth measure) {
      if (width <= 0) {
         return "";
      }
      // Adding/subtracting the insets can lose a few float ULPs. Do not ellipsize a fitting line.
      if (measure.measure(text, size) <= width + .01f) {
         return text;
      }
      if (measure.measure("...", size) > width) {
         return "";
      }
      int end = text.length();
      while (end > 0 && measure.measure(text.substring(0, end) + "...", size) > width) {
         end = text.offsetByCodePoints(end, -1);
      }
      return text.substring(0, end) + "...";
   }

   public static DetailLine detailLine(String detail, String status, float width, TextWidth measure) {
      String suffix = fit(status, DETAIL_FONT, width, measure);
      float remaining = Math.max(0, width - measure.measure(suffix, DETAIL_FONT));
      boolean truncated = measure.measure(detail, DETAIL_FONT) > remaining + .01f;
      float gap = truncated && !suffix.isEmpty() ? measure.measure(" ", DETAIL_FONT) : 0;
      String prefix = fit(detail, DETAIL_FONT, Math.max(0, remaining - gap), measure);
      if (!prefix.isEmpty() && gap > 0) {
         prefix += " ";
      }
      return new DetailLine(prefix, suffix, measure.measure(prefix, DETAIL_FONT));
   }

   private static final class Notice {
      final String key;
      final Transition toggle;
      String title;
      String detail;
      String status;
      Icon icon;
      boolean enabled;
      float progress;
      float contentWidth;
      long expiresAt;

      Notice(String key, long now) {
         this.key = key;
         this.toggle = new Transition(now);
      }
   }

   private static final class Transition {
      float from;
      float to;
      long started;

      Transition(long now) {
         this.started = now;
      }

      void target(float destination, long now) {
         if (destination != this.to) {
            this.from = value(now);
            this.to = destination;
            this.started = now;
         }
      }

      float value(long now) {
         float progress = OpaiMath.clamp((now - this.started) / (float)FADE_MS, 0, 1);
         float eased = this.to < this.from ? progress
            : progress >= 1 ? 1 : 1 - (float)Math.pow(2, -10 * progress);
         return this.from + (this.to - this.from) * eased;
      }
   }
}
