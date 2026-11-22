package dev.minime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/** Свои цвета ползунками. Пузыри и меню настраиваются отдельно. */
public class MiniColorScreen extends Screen {
    private final Screen parent;
    private final String[] labels = {"Пузырь: фон", "Пузырь: обводка", "Пузырь: текст", "Пузырь: ник", "Меню: подложка"};
    private final int[] defaults = {0xFFFFFF, 0x000000, 0x000000, 0x9146FF, 0xF4F4F4};
    private static int target = 0;

    public MiniColorScreen(Screen parent) {
        super(Component.literal("Цвета"));
        this.parent = parent;
    }

    private Config cfg() { return MiniMeClient.config; }

    private String get(int i) {
        return switch (i) { case 0 -> cfg().cBubbleFill; case 1 -> cfg().cBubbleBorder; case 2 -> cfg().cBubbleText; case 3 -> cfg().cBubbleName; default -> cfg().cPanel; };
    }

    private void set(int i, String v) {
        switch (i) { case 0 -> cfg().cBubbleFill = v; case 1 -> cfg().cBubbleBorder = v; case 2 -> cfg().cBubbleText = v; case 3 -> cfg().cBubbleName = v; default -> cfg().cPanel = v; }
        if (i < 4) cfg().bubbleStyle = "custom"; else cfg().menuStyle = "custom";   // сразу показываем свои цвета
    }

    private int color(int i) { return Config.rgb(get(i), defaults[i]); }

    private static class ChannelSlider extends AbstractSliderButton {
        private final String name;
        private final IntConsumer onChange;

        ChannelSlider(int x, int y, int w, int h, String name, int initial, IntConsumer onChange) {
            super(x, y, w, h, Component.empty(), initial / 255.0);
            this.name = name;
            this.onChange = onChange;
            updateMessage();
        }

        @Override protected void updateMessage() { setMessage(Component.literal(name + ": " + (int) Math.round(this.value * 255))); }
        @Override protected void applyValue() { onChange.accept((int) Math.round(this.value * 255)); }
    }

    private int y0() { return this.height / 2 - 105; }

    @Override
    protected void init() {
        int x0 = this.width / 2 - 150, y0 = y0();
        int cur = color(target);
        final int[] st = {(cur >> 16) & 255, (cur >> 8) & 255, cur & 255};

        addRenderableWidget(Button.builder(Component.literal("Что красим: " + labels[target] + "  ›"), b -> {
            target = (target + 1) % labels.length;
            this.rebuildWidgets();
        }).bounds(x0, y0, 300, 20).build());

        String[] names = {"Красный", "Зелёный", "Синий"};
        for (int i = 0; i < 3; i++) {
            final int ch = i;
            addRenderableWidget(new ChannelSlider(x0, y0 + 26 * (i + 1), 300, 20, names[i], st[i], v -> {
                st[ch] = v;
                set(target, String.format("%02X%02X%02X", st[0], st[1], st[2]));
            }));
        }

        int yb = y0 + 26 * 4 + 56;
        addRenderableWidget(Button.builder(Component.literal("Пузыри: " + Theme.styleName(cfg().bubbleStyle)), b -> {
            cfg().bubbleStyle = Theme.nextStyle(cfg().bubbleStyle);
            b.setMessage(Component.literal("Пузыри: " + Theme.styleName(cfg().bubbleStyle)));
        }).bounds(x0, yb, 148, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Меню: " + Theme.styleName(cfg().menuStyle)), b -> {
            cfg().menuStyle = Theme.nextStyle(cfg().menuStyle);
            b.setMessage(Component.literal("Меню: " + Theme.styleName(cfg().menuStyle)));
        }).bounds(x0 + 152, yb, 148, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
                .bounds(x0, yb + 26, 300, 20).build());
    }

    @Override
    public void onClose() {
        cfg().save();
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        Theme.Look menu = Theme.menu(cfg());
        int y0 = y0();
        Theme.box(g, this.width / 2 - 165, y0 - 34, 330, 26 * 4 + 56 + 26 + 20 + 44, menu, 1f);
        super.extractRenderState(g, mouseX, mouseY, partial);
        g.centeredText(this.font, Component.literal("Цвета: двигайте ползунки"), this.width / 2, y0 - 24, 0xFF000000 | menu.text());

        // предпросмотр пузыря
        Theme.Look b = Theme.bubble(cfg());
        int bx = this.width / 2 - 70, by = y0 + 26 * 4 + 4, bw = 140, bh = 40;
        Theme.box(g, bx, by, bw, bh, b, 1f);
        g.text(this.font, "Streamer", bx + 5, by + 5, Theme.nameColor(b, 1f), b.glass());
        g.text(this.font, "Привет! Как дела?", bx + 5, by + 20, Theme.textColor(b, 1f), b.glass());
    }
}
