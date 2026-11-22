package dev.minime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/** Главное меню (клавиша O). */
public class MiniScreen extends Screen {
    private static final float[] SIZES = {0.15f, 0.2f, 0.3f, 0.4f, 0.5f, 0.75f, 1.0f};
    private static final float[] DIST = {1.0f, 1.5f, 2.2f, 3.0f, 4.0f, 5.0f};
    private static final int[] SECS = {4, 8, 12, 20};
    private static final int[] IDLE = {5, 10, 20, 40};
    private static final String[] POS = {"front", "side", "back"};
    private EditBox channelBox;

    public MiniScreen() { super(Component.literal("Mini Me")); }

    private Config cfg() { return MiniMeClient.config; }

    private void add(int x, int y, int w, Supplier<String> label, Runnable onClick) {
        addRenderableWidget(Button.builder(Component.literal(label.get()), b -> {
            onClick.run();
            b.setMessage(Component.literal(label.get()));
        }).bounds(x, y, w, 20).build());
    }

    private static int nearest(float[] arr, float v) {
        int best = 0;
        for (int k = 0; k < arr.length; k++) if (Math.abs(arr[k] - v) < Math.abs(arr[best] - v)) best = k;
        return best;
    }

    private int fxIndex() {
        for (int k = 0; k < MiniManager.FX.length; k++) if (MiniManager.FX[k].equals(cfg().popEffect)) return k;
        return 0;
    }

    private static String onOff(boolean b) { return b ? "вкл" : "выкл"; }

    @Override
    protected void init() {
        int colW = 150, gap = 21;
        int xl = this.width / 2 - colW - 4, xr = this.width / 2 + 4;
        int y = this.height / 2 - 112;

        add(xl, y, colW, () -> "Размер: " + cfg().scale, () -> cfg().scale = SIZES[(nearest(SIZES, cfg().scale) + 1) % SIZES.length]);
        add(xl, y + gap, colW, () -> "Дистанция: " + cfg().distance, () -> cfg().distance = DIST[(nearest(DIST, cfg().distance) + 1) % DIST.length]);
        add(xl, y + gap * 2, colW, () -> "Позиция: " + switch (cfg().position) { case "side" -> "сбоку"; case "back" -> "сзади"; default -> "спереди"; }, () -> {
            int i = 0;
            for (int k = 0; k < POS.length; k++) if (POS[k].equals(cfg().position)) i = k;
            cfg().position = POS[(i + 1) % POS.length];
        });
        add(xl, y + gap * 3, colW, () -> "Все чибики: " + onOff(cfg().enabled), () -> cfg().enabled = !cfg().enabled);
        add(xl, y + gap * 4, colW, () -> "Улетают в тишине: " + (cfg().autoAway ? "да" : "нет"), () -> cfg().autoAway = !cfg().autoAway);
        add(xl, y + gap * 5, colW, () -> "Пузыри: " + Theme.styleName(cfg().bubbleStyle), () -> cfg().bubbleStyle = Theme.nextStyle(cfg().bubbleStyle));
        add(xl, y + gap * 6, colW, () -> "Меню: " + Theme.styleName(cfg().menuStyle), () -> cfg().menuStyle = Theme.nextStyle(cfg().menuStyle));

        add(xr, y, colW, () -> "Пузыри, сек: " + cfg().bubbleSeconds, () -> {
            int i = 0;
            for (int k = 0; k < SECS.length; k++) if (SECS[k] == cfg().bubbleSeconds) i = k;
            cfg().bubbleSeconds = SECS[(i + 1) % SECS.length];
        });
        add(xr, y + gap, colW, () -> "Тишина до ухода: " + cfg().idleSeconds + " с", () -> {
            int i = 0;
            for (int k = 0; k < IDLE.length; k++) if (IDLE[k] == cfg().idleSeconds) i = k;
            cfg().idleSeconds = IDLE[(i + 1) % IDLE.length];
        });
        add(xr, y + gap * 2, colW, () -> "Лопается: " + MiniManager.FXN[fxIndex()], () -> { cfg().popEffect = MiniManager.FX[(fxIndex() + 1) % MiniManager.FX.length]; this.rebuildWidgets(); });

        add(xr, y + gap * 3, colW, () -> "Звук эффекта: " + Sfx.shortName(cfg().effectSounds.get(cfg().popEffect)), () -> {
            String cur = cfg().effectSounds.getOrDefault(cfg().popEffect, "off");
            int i = Sfx.PRESETS.indexOf(cur);
            String next = Sfx.PRESETS.get((i + 1) % Sfx.PRESETS.size());
            cfg().effectSounds.put(cfg().popEffect, next);
            Sfx.play(next, 1.0f, cfg().effectVolume);
        });
        addRenderableWidget(Button.builder(Component.literal("Чат и звуки…"), b -> {
            cfg().save();
            Minecraft.getInstance().setScreenAndShow(new MiniChatScreen(this));
        }).bounds(xr, y + gap * 4, colW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Чибики и скины…"), b -> {
            cfg().save();
            Minecraft.getInstance().setScreenAndShow(new MiniSlotsScreen(this));
        }).bounds(xl, y + gap * 7, colW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Цвета…"), b -> {
            cfg().save();
            Minecraft.getInstance().setScreenAndShow(new MiniColorScreen(this));
        }).bounds(xr, y + gap * 7, colW, 20).build());

        int fullW = colW * 2 + 8;
        channelBox = new EditBox(this.font, xl, y + gap * 8 + 2, fullW - 84, 20, Component.literal("Канал Twitch"));
        channelBox.setMaxLength(40);
        channelBox.setValue(cfg().channel);
        addRenderableWidget(channelBox);
        addRenderableWidget(Button.builder(Component.literal("Connect"), b -> {
            cfg().channel = channelBox.getValue().trim();
            cfg().save();
            MiniMeClient.twitch.connect(cfg().channel, cfg().twitchNick, cfg().oauthToken);
        }).bounds(xl + fullW - 80, y + gap * 8 + 2, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
                .bounds(xl, y + gap * 9 + 2, fullW, 20).build());
    }

    @Override
    public void onClose() {
        cfg().save();
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        Theme.Look look = Theme.menu(cfg());
        Theme.box(g, this.width / 2 - 165, this.height / 2 - 138, 330, 262, look, 1f);
        super.extractRenderState(g, mouseX, mouseY, partial);
        g.centeredText(this.font, Component.literal("Mini Me — настройки"), this.width / 2, this.height / 2 - 128, 0xFF000000 | look.text());
    }
}
