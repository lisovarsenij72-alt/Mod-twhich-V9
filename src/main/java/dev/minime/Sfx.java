package dev.minime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Проигрывание звуков по id (например minecraft:block.cherry_leaves.break). */
public class Sfx {
    private static final Map<String, SoundEvent> CACHE = new HashMap<>();

    /** Звуки для кнопки «Звук эффекта» в меню. */
    public static final List<String> PRESETS = List.of("off",
            "entity.experience_orb.pickup", "entity.player.levelup", "block.note_block.bell", "block.note_block.harp",
            "block.amethyst_block.chime", "entity.villager.celebrate", "entity.ghast.scream", "entity.witch.celebrate",
            "block.cherry_leaves.break", "block.snow.break", "block.sand.break", "entity.firework_rocket.blast",
            "item.totem.use", "entity.puffer_fish.blow_out", "entity.cat.purr", "block.enchantment_table.use",
            "entity.squid.squirt", "item.firecharge.use", "entity.iron_golem.damage", "entity.villager.no");

    public static void play(String id, float pitch, float vol) {
        if (id == null || id.isBlank() || id.equals("off")) return;
        String full = id.contains(":") ? id : "minecraft:" + id;
        Identifier rl = Identifier.tryParse(full);
        if (rl == null) return;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            SoundEvent ev = CACHE.computeIfAbsent(full, k -> SoundEvent.createVariableRangeEvent(rl));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, Math.max(0f, Math.min(1f, vol))));
        });
    }

    /** Звук выбранного эффекта частиц. */
    public static void effect(String fx) {
        Config c = MiniMeClient.config;
        if (c == null || !c.effectSoundsOn) return;
        play(c.effectSounds.get(fx), 1.0f, c.effectVolume);
    }

    public static String shortName(String id) {
        if (id == null || id.equals("off") || id.isBlank()) return "выкл";
        String s = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        s = s.replace('.', ' ').replace('_', ' ');
        return s.length() > 18 ? s.substring(0, 18) : s;
    }
}
