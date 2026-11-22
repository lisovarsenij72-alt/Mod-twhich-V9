package dev.minime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/** Чат и звуки: озвучка, звонок, «67», чибики зрителей, приколы. */
public class MiniChatScreen extends Screen {
    private final Screen parent;
    private static final float[] VOL = {0.3f, 0.6f, 1.0f};
    private static final int[] MAXC = {0, 2, 3, 5, 8};
    private static final int[] SECS = {15, 30, 45, 90, 180};
    private static final float[] LAG = {0f, 0.3f, 0.6f, 0.8f, 1.2f, 2.0f, 3.0f};
    private static final float[] SLAG = {0f, 0.2f, 0.4f, 0.8f, 1.2f, 2.0f};
    private static final float[] EVOL = {0.3f, 0.6f, 1.0f};

    public MiniChatScreen(Screen parent) {
        super(Component.literal("Чат и звуки"));
        this.parent = parent;
    }

    private Config cfg() { return MiniMeClient.config; }

    private void add(int x, int y, int w, Supplier<String> label, Runnable onClick) {
        addRenderableWidget(Button.builder(Component.literal(label.get()), b -> {
            onClick.run();
            b.setMessage(Component.literal(label.get()));
        }).bounds(x, y, w, 20).build());
    }

    private static String yn(boolean b) { return b ? "да" : "нет"; }

    private int y0() { return this.height / 2 - 115; }

    @Override
    protected void init() {
        int colW = 150, gap = 22;
        int xl = this.width / 2 - colW - 4, xr = this.width / 2 + 4, y = y0();

        // звуки
        add(xl, y, colW, () -> "Озвучка: " + (cfg().ttsEnabled ? "вкл" : "выкл"), () -> cfg().ttsEnabled = !cfg().ttsEnabled);
        add(xl, y + gap, colW, () -> "Читать ники: " + yn(cfg().ttsReadNames), () -> cfg().ttsReadNames = !cfg().ttsReadNames);
        add(xl, y + gap * 2, colW, () -> "Звонок (упомин.): " + (cfg().mentionSound ? "вкл" : "выкл"), () -> cfg().mentionSound = !cfg().mentionSound);
        add(xl, y + gap * 3, colW, () -> "Звук «67»: " + (cfg().sound67 ? "вкл" : "выкл"), () -> cfg().sound67 = !cfg().sound67);
        add(xl, y + gap * 4, colW, () -> "Громкость «67»: " + Math.round(cfg().sound67Volume * 100) + "%", () -> {
            int i = 0;
            for (int k = 0; k < VOL.length; k++) if (Math.abs(VOL[k] - cfg().sound67Volume) < 0.05f) i = k;
            cfg().sound67Volume = VOL[(i + 1) % VOL.length];
        });

        // зрители и приколы
        add(xr, y, colW, () -> "Чибики зрителей: " + (cfg().chatterChibis ? "вкл" : "выкл"), () -> cfg().chatterChibis = !cfg().chatterChibis);
        add(xr, y + gap, colW, () -> "Макс. зрителей: " + cfg().maxChatters, () -> {
            int i = 0;
            for (int k = 0; k < MAXC.length; k++) if (MAXC[k] == cfg().maxChatters) i = k;
            cfg().maxChatters = MAXC[(i + 1) % MAXC.length];
        });
        add(xr, y + gap * 2, colW, () -> "Скин зрителя по нику: " + yn(cfg().chatterFetchSkins), () -> cfg().chatterFetchSkins = !cfg().chatterFetchSkins);
        add(xr, y + gap * 3, colW, () -> "Исчезает через: " + cfg().chatterSeconds + " с", () -> {
            int i = 0;
            for (int k = 0; k < SECS.length; k++) if (SECS[k] == cfg().chatterSeconds) i = k;
            cfg().chatterSeconds = SECS[(i + 1) % SECS.length];
        });
        add(xr, y + gap * 4, colW, () -> "Цветные ники: " + yn(cfg().colorNicks), () -> cfg().colorNicks = !cfg().colorNicks);
        add(xl, y + gap * 5, colW, () -> "Команды !pop !spin !hop: " + (cfg().chatCommands ? "вкл" : "выкл"), () -> cfg().chatCommands = !cfg().chatCommands);
        add(xr, y + gap * 5, colW, () -> "Сердечки на <3: " + yn(cfg().chatHearts), () -> cfg().chatHearts = !cfg().chatHearts);

        add(xl, y + gap * 6, colW, () -> "Мемы (42, mogged…): " + (cfg().memes ? "вкл" : "выкл"), () -> cfg().memes = !cfg().memes);
        add(xr, y + gap * 6, colW, () -> "Задержка зрителей: " + cfg().chatterLag + " с", () -> {
            int i = 0;
            for (int k = 0; k < LAG.length; k++) if (Math.abs(LAG[k] - cfg().chatterLag) < 0.05f) i = k;
            cfg().chatterLag = LAG[(i + 1) % LAG.length];
        });
        add(xl, y + gap * 8, colW * 2 + 8, () -> "Задержка своих чибиков: " + cfg().slotLag + " с", () -> {
            int i = 0;
            for (int k = 0; k < SLAG.length; k++) if (Math.abs(SLAG[k] - cfg().slotLag) < 0.05f) i = k;
            cfg().slotLag = SLAG[(i + 1) % SLAG.length];
        });
        add(xl, y + gap * 7, colW, () -> "Звуки эффектов: " + (cfg().effectSoundsOn ? "вкл" : "выкл"), () -> cfg().effectSoundsOn = !cfg().effectSoundsOn);
        add(xr, y + gap * 7, colW, () -> "Громкость эффектов: " + Math.round(cfg().effectVolume * 100) + "%", () -> {
            int i = 0;
            for (int k = 0; k < EVOL.length; k++) if (Math.abs(EVOL[k] - cfg().effectVolume) < 0.05f) i = k;
            cfg().effectVolume = EVOL[(i + 1) % EVOL.length];
        });

        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
                .bounds(xl, y + gap * 9 + 8, colW * 2 + 8, 20).build());
    }

    @Override
    public void onClose() {
        cfg().save();
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        Theme.Look look = Theme.menu(cfg());
        int y0 = y0();
        Theme.box(g, this.width / 2 - 165, y0 - 32, 330, 22 * 9 + 8 + 20 + 52, look, 1f);
        super.extractRenderState(g, mouseX, mouseY, partial);
        g.centeredText(this.font, Component.literal("Чат и звуки"), this.width / 2, y0 - 22, 0xFF000000 | look.text());
    }
}
