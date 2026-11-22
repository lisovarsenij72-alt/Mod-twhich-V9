package dev.minime;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Внешний вид пузырей и меню (стили независимы). Стекло — полупрозрачная панель с бликами. */
public final class Theme {
    public record Look(int fill, int fillA, int border, int borderA, int text, int name, boolean glass) {}

    public static Look bubble(Config c) {
        switch (c.bubbleStyle) {
            case "dark": return new Look(0x161616, 255, 0xFFFFFF, 255, 0xFFFFFF, 0xB388FF, false);
            case "light": return new Look(0xFFFFFF, 255, 0x000000, 255, 0x000000, 0x9146FF, false);
            case "custom": return new Look(Config.rgb(c.cBubbleFill, 0xFFFFFF), 255, Config.rgb(c.cBubbleBorder, 0),
                    255, Config.rgb(c.cBubbleText, 0), Config.rgb(c.cBubbleName, 0x9146FF), false);
            default: return new Look(0xFFFFFF, 70, 0xFFFFFF, 160, 0xFFFFFF, 0xD2C2FF, true);
        }
    }

    public static Look menu(Config c) {
        switch (c.menuStyle) {
            case "dark": return new Look(0x0B0B0B, 0xDD, 0x444444, 255, 0xFFFFFF, 0xFFFFFF, false);
            case "light": return new Look(0xF4F4F4, 0xDD, 0xAAAAAA, 255, 0x111111, 0x111111, false);
            case "custom": {
                int p = Config.rgb(c.cPanel, 0xF4F4F4);
                return new Look(p, 0xDD, p, 0, Config.contrast(p) & 0xFFFFFF, Config.contrast(p) & 0xFFFFFF, false);
            }
            default: return new Look(0xFFFFFF, 56, 0xFFFFFF, 150, 0xFFFFFF, 0xFFFFFF, true);
        }
    }

    public static String styleName(String s) {
        return switch (s) { case "dark" -> "чёрный"; case "light" -> "белый"; case "custom" -> "свой цвет"; default -> "стекло"; };
    }

    public static String nextStyle(String s) {
        return switch (s) { case "glass" -> "light"; case "light" -> "dark"; case "dark" -> "custom"; default -> "glass"; };
    }

    private static int argb(int a, int rgb) { return ((Math.max(0, Math.min(255, a))) << 24) | (rgb & 0xFFFFFF); }

    /** Прямоугольник со срезанными углами: заливка + рамка (+ блики для стекла). */
    public static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, Look l, float alpha) {
        int fa = argb((int) (l.fillA() * alpha), l.fill());
        int ba = argb((int) (l.borderA() * alpha), l.border());
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fa);
        if (l.borderA() > 0) {
            g.fill(x + 1, y, x + w - 1, y + 1, ba);
            g.fill(x + 1, y + h - 1, x + w - 1, y + h, ba);
            g.fill(x, y + 1, x + 1, y + h - 1, ba);
            g.fill(x + w - 1, y + 1, x + w, y + h - 1, ba);
        }
        if (l.glass()) {
            g.fill(x + 2, y + 2, x + w - 2, y + 2 + Math.max(2, h / 3), argb((int) (48 * alpha), 0xFFFFFF));   // верхний блик
            g.fill(x + 2, y + h - 4, x + w - 2, y + h - 2, argb((int) (26 * alpha), 0xFFFFFF));               // нижний отсвет
            g.fill(x + 3, y + 3, x + 6, y + 4, argb((int) (150 * alpha), 0xFFFFFF));                          // искорка
        }
    }

    public static int textColor(Look l, float alpha) { return argb((int) (255 * alpha), l.text()); }
    public static int nameColor(Look l, float alpha) { return argb((int) (255 * alpha), l.name()); }
}
