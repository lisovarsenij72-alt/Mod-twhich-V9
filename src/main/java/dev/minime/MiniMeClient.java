package dev.minime;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class MiniMeClient implements ClientModInitializer {
    public static Config config;
    public static TwitchChat twitch;
    public static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("minime", "companion");

    @Override
    public void onInitializeClient() {
        config = Config.load();

        if (MiniMeMain.CHIBI != null) {
            try { EntityRenderers.register(MiniMeMain.CHIBI, MiniChibiRenderer::new); }
            catch (Throwable t) { t.printStackTrace(); }
        }
        twitch = new TwitchChat((nick, text) -> { MiniHud.addMessage(nick, text); Tts.speak(nick, text); Alerts.onMessage(nick, text); MiniManager.onMessage(); Minecraft.getInstance().execute(() -> MiniManager.onChat(nick, text)); },
                st -> Minecraft.getInstance().execute(() -> msg(Minecraft.getInstance(), st)));
        twitch.connect(config.channel, config.twitchNick, config.oauthToken);

        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("minime", "main"));
        KeyMapping menu = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.minime.menu", GLFW.GLFW_KEY_O, cat));
        KeyMapping cycle = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.minime.cycle", GLFW.GLFW_KEY_M, cat));
        KeyMapping skinKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.minime.skin", GLFW.GLFW_KEY_K, cat));
        KeyMapping toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.minime.toggle", GLFW.GLFW_KEY_N, cat));
        KeyMapping reconnect = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.minime.reconnect", GLFW.GLFW_KEY_J, cat));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menu.consumeClick()) {
                mc.setScreenAndShow(new MiniScreen());
            }
            while (cycle.consumeClick()) {
                Config.Slot s0 = config.slots.get(0);
                List<String> all = config.allModels();
                s0.model = all.get((all.indexOf(s0.model) + 1) % all.size());
                config.save();
                msg(mc, "Модель #1: " + s0.model);
            }
            while (skinKey.consumeClick()) {
                Config.Slot s0 = config.slots.get(0);
                List<String> all = config.allSkins();
                s0.skin = all.get((all.indexOf(s0.skin) + 1) % all.size());
                config.save();
                msg(mc, "Скин #1: " + s0.skin);
            }
            while (toggle.consumeClick()) {
                config.enabled = !config.enabled; config.save();
                msg(mc, config.enabled ? "Чибики включены" : "Чибики выключены");
            }
            while (reconnect.consumeClick()) {
                config = Config.load();
                SkinManager.clear();
                twitch.connect(config.channel, config.twitchNick, config.oauthToken);
            }
            MiniManager.tick(mc);
        });

        // удар по мини: он лопается на частицы (пакет атаки на сервер не уходит)
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (entity != null && MiniManager.onAttack(entity)) return InteractionResult.SUCCESS;
            return InteractionResult.PASS;
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, HUD_ID, new MiniHud()::draw);
    }

    private static void msg(Minecraft mc, String s) {
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal("[Mini] " + s));
    }
}
