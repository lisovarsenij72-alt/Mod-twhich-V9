package dev.minime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Чибики: вкл/выкл, модель и скин каждого + скачивание скина по нику. */
public class MiniSlotsScreen extends Screen {
    private final Screen parent;
    private EditBox nickBox;
    private String status = "";

    public MiniSlotsScreen(Screen parent) {
        super(Component.literal("Чибики"));
        this.parent = parent;
    }

    private Config cfg() { return MiniMeClient.config; }

    private int y0() { return this.height / 2 - 100; }

    @Override
    protected void init() {
        int x0 = this.width / 2 - 150, y0 = y0();
        for (int i = 0; i < Config.MAX_SLOTS; i++) {
            final int idx = i;
            Config.Slot s = cfg().slots.get(i);
            int y = y0 + i * 24;
            addRenderableWidget(Button.builder(Component.literal("#" + (i + 1) + " " + (s.enabled ? "вкл" : "выкл")), b -> {
                s.enabled = !s.enabled;
                b.setMessage(Component.literal("#" + (idx + 1) + " " + (s.enabled ? "вкл" : "выкл")));
            }).bounds(x0, y, 64, 20).build());
            addRenderableWidget(Button.builder(Component.literal(s.model), b -> {
                List<String> all = cfg().allModels();
                s.model = all.get((all.indexOf(s.model) + 1) % all.size());
                b.setMessage(Component.literal(s.model));
            }).bounds(x0 + 68, y, 76, 20).build());
            addRenderableWidget(Button.builder(Component.literal("скин: " + s.skin), b -> {
                List<String> all = cfg().allSkins();
                s.skin = all.get((all.indexOf(s.skin) + 1) % all.size());
                b.setMessage(Component.literal("скин: " + s.skin));
            }).bounds(x0 + 148, y, 152, 20).build());
        }

        int yb = y0 + Config.MAX_SLOTS * 24 + 6;
        nickBox = new EditBox(this.font, x0, yb, 190, 20, Component.literal("Ник игрока Minecraft"));
        nickBox.setMaxLength(16);
        addRenderableWidget(nickBox);
        addRenderableWidget(Button.builder(Component.literal("Скачать скин"), b ->
                SkinFetcher.fetch(nickBox.getValue(), msg -> Minecraft.getInstance().execute(() -> {
                    status = msg;
                    if (msg.startsWith("Скин ")) this.rebuildWidgets();
                }))).bounds(x0 + 194, yb, 106, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Возврат после удара: " + cfg().respawnSeconds + " с"), b -> {
            cfg().respawnSeconds = cfg().respawnSeconds >= 10 ? 2 : cfg().respawnSeconds + 2;
            b.setMessage(Component.literal("Возврат после удара: " + cfg().respawnSeconds + " с"));
        }).bounds(x0, yb + 40, 190, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Тест: лопнуть"), b -> MiniManager.popFirst())
                .bounds(x0 + 194, yb + 40, 106, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
                .bounds(x0, yb + 66, 300, 20).build());
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
        Theme.box(g, this.width / 2 - 165, y0 - 30, 330, Config.MAX_SLOTS * 24 + 132, look, 1f);
        super.extractRenderState(g, mouseX, mouseY, partial);
        int txt = 0xFF000000 | look.text();
        g.centeredText(this.font, Component.literal("Чибики"), this.width / 2, y0 - 20, txt);
        g.text(this.font, "Ник Minecraft для скачивания скина:", this.width / 2 - 150, y0 + Config.MAX_SLOTS * 24 - 6, txt, false);
        if (!status.isEmpty()) g.text(this.font, status, this.width / 2 - 150, y0 + Config.MAX_SLOTS * 24 + 30, txt, false);
    }
}
