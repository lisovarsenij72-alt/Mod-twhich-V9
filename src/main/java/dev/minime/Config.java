package dev.minime;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;

public class Config {
    public static final int MAX_SLOTS = 5;

    /** Один чибик: включён ли, какая модель, какой скин. */
    public static class Slot {
        public boolean enabled;
        public String model = "chibi";   // chibi | fox | player
        public String skin = "own";      // own | steve | alex | girl | имя файла из config/minime/skins
        public Slot() {}
        public Slot(boolean enabled) { this.enabled = enabled; }
    }

    public String channel = "your_channel";
    public String twitchNick = "";
    public String oauthToken = "";
    public List<Slot> slots = defaultSlots();

    public float scale = 0.3f;
    public float distance = 2.2f;
    public String position = "front";         // front | side | back
    public boolean autoAway = true;
    public int idleSeconds = 20;
    public int respawnSeconds = 4;
    public String popEffect = "poof";
    public int bubbleSeconds = 8;
    public int maxBubbles = 4;
    public boolean enabled = true;

    // стили: у пузырей и у меню — НЕЗАВИСИМЫЕ. glass | light | dark | custom
    public String bubbleStyle = "glass";
    public String menuStyle = "glass";
    public String cBubbleFill = "FFFFFF";
    public String cBubbleBorder = "000000";
    public String cBubbleText = "000000";
    public String cBubbleName = "9146FF";
    public String cPanel = "F4F4F4";

    public boolean ttsEnabled = true;
    public boolean ttsReadNames = true;
    public float ttsVolume = 1.0f;

    public float sound67Volume = 1.0f;

    // чибики зрителей: у каждого, кто пишет в чат, свой персонаж
    public boolean chatterChibis = true;
    public int maxChatters = 5;
    public int chatterSeconds = 45;
    public boolean chatterFetchSkins = true;  // пробовать скачать скин по нику зрителя
    public boolean colorNicks = true;         // цветные ники в пузырях
    public boolean chatCommands = true;       // !pop !spin !hop
    public boolean chatHearts = true;         // сердечки на <3 / люблю

    public float slotLag = 0.4f;              // задержка чибиков стримера (сек)
    public float chatterLag = 0.8f;           // задержка зрителей (сек): они следуют с отставанием
    public boolean memes = true;              // мемы по словам из чата
    public Map<String, String> memeSounds = Memes.defaultSounds();
    public boolean effectSoundsOn = true;     // звуки эффектов частиц
    public float effectVolume = 1.0f;
    public Map<String, String> effectSounds = defaultEffectSounds();

    public boolean mentionSound = true;       // звоночек при упоминании
    public boolean sound67 = true;            // звук на «67»
    public List<String> mentionWords = new ArrayList<>(List.of("вик", "вика", "вики", "weak", "слабость", "виктория"));

    static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("minime");
    static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("minime.json");
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static Map<String, String> defaultEffectSounds() {
        Map<String, String> m = new LinkedHashMap<>();
        String[][] d = {{"poof", "entity.puffer_fish.blow_out"}, {"snow", "block.snow.break"}, {"cherry", "block.cherry_leaves.break"},
                {"sand", "block.sand.break"}, {"souls", "entity.ghast.scream"}, {"hearts", "entity.cat.purr"}, {"flame", "item.firecharge.use"},
                {"stars", "block.amethyst_block.chime"}, {"notes", "block.note_block.harp"}, {"magic", "block.enchantment_table.use"},
                {"portal", "block.portal.trigger"}, {"totem", "item.totem.use"}, {"firework", "entity.firework_rocket.blast"},
                {"sparks", "item.trident.thunder"}, {"glow", "entity.glow_squid.ambient"}, {"crit", "entity.player.attack.crit"},
                {"joy", "entity.villager.celebrate"}, {"witch", "entity.witch.celebrate"}, {"angry", "entity.villager.no"},
                {"ink", "entity.squid.squirt"}, {"gold", "entity.experience_orb.pickup"}, {"diamond", "entity.player.levelup"},
                {"emerald", "entity.villager.trade"}};
        for (String[] e : d) m.put(e[0], e[1]);
        return m;
    }

    static List<Slot> defaultSlots() {
        List<Slot> l = new ArrayList<>();
        for (int i = 0; i < MAX_SLOTS; i++) l.add(new Slot(i == 0));
        return l;
    }

    public static Config load() {
        Config c = null;
        try {
            Files.createDirectories(DIR.resolve("skins"));
            if (Files.exists(FILE)) c = GSON.fromJson(Files.readString(FILE), Config.class);
        } catch (Exception e) { e.printStackTrace(); }
        if (c == null) c = new Config();
        c.normalize();
        c.save();
        return c;
    }

    /** Приводит конфиг в порядок (старые версии, ошибки в файле). */
    public void normalize() {
        if (slots == null) slots = defaultSlots();
        while (slots.size() < MAX_SLOTS) slots.add(new Slot(false));
        while (slots.size() > MAX_SLOTS) slots.remove(slots.size() - 1);
        List<String> skins = allSkins();
        for (Slot s : slots) {
            if (s == null) continue;
            if (!allModels().contains(s.model)) s.model = "chibi";
            if (!skins.contains(s.skin)) s.skin = "own";
        }
        if (mentionWords == null) mentionWords = new ArrayList<>();
        if (effectSounds == null) effectSounds = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : defaultEffectSounds().entrySet()) effectSounds.putIfAbsent(e.getKey(), e.getValue());
        if (memeSounds == null) memeSounds = new LinkedHashMap<>();
        memeSounds.keySet().retainAll(Memes.defaultSounds().keySet());                       // убираем удалённые мемы
        memeSounds.putAll(Memes.defaultSounds());                                             // звуки 42 и mogged — наши
        if (!List.of("glass", "light", "dark", "custom").contains(bubbleStyle)) bubbleStyle = "glass";
        if (!List.of("glass", "light", "dark", "custom").contains(menuStyle)) menuStyle = "glass";
    }

    public void save() {
        try { Files.writeString(FILE, GSON.toJson(this)); } catch (Exception e) { e.printStackTrace(); }
    }

    public List<String> allModels() { return List.of("chibi", "fox", "player"); }

    /** own, steve, alex, girl + все PNG из config/minime/skins */
    public List<String> allSkins() {
        List<String> l = new ArrayList<>(List.of("own", "steve", "alex", "girl"));
        try (var s = Files.list(DIR.resolve("skins"))) {
            s.map(p -> p.getFileName().toString()).filter(n -> n.toLowerCase().endsWith(".png"))
             .map(n -> n.substring(0, n.length() - 4)).sorted().forEach(l::add);
        } catch (Exception ignored) {}
        return l;
    }

    public static int rgb(String hex, int def) {
        try {
            String h = hex.trim().replace("#", "");
            return h.length() == 6 ? Integer.parseInt(h, 16) : def;
        } catch (Exception e) { return def; }
    }

    public static int contrast(int rgb) {
        double l = 0.299 * ((rgb >> 16) & 255) + 0.587 * ((rgb >> 8) & 255) + 0.114 * (rgb & 255);
        return l > 140 ? 0xFF111111 : 0xFFFFFFFF;
    }
}
