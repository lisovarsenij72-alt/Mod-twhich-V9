package dev.minime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Мемы по словам из чата: большая надпись, частицы, звук и реакция чибиков. */
public class Memes {
    enum Act { NONE, SPIN_ALL, HOP_ALL }

    record Meme(String key, Pattern p, String text, int color, String fx, String sound, Act act) {}

    private static Pattern w(String words) {
        return Pattern.compile("(?iu)(?<![\\p{L}\\p{N}_])(?:" + words + ")(?![\\p{L}\\p{N}_])");
    }

    static final List<Meme> LIST = List.of(
            new Meme("42", w("42"), "42 БРАТУХА", 0xFFD93B, "stars", "minime:meme_42", Act.SPIN_ALL),
            new Meme("mogged", w("mogged|mog|могд|моггед|могнул"), "MOGGED", 0xFF4D4D, "sparks", "minime:meme_mogged", Act.HOP_ALL));

    public static Map<String, String> defaultSounds() {
        Map<String, String> m = new LinkedHashMap<>();
        for (Meme x : LIST) m.put(x.key(), x.sound());
        return m;
    }

    private static long lastAny = 0;

    /** Главный поток. */
    public static void handle(String user, String text) {
        Config c = MiniMeClient.config;
        if (c == null || !c.memes) return;
        long now = System.currentTimeMillis();
        if (now - lastAny < 2500) return;
        for (Meme m : LIST) {
            if (!m.p().matcher(text).find()) continue;
            lastAny = now;
            MiniHud.addPopup(m.text(), m.color());
            MiniManager.burstOwner(user, m.fx(), 24);
            Sfx.play(c.memeSounds.getOrDefault(m.key(), m.sound()), 1.0f, 1.0f);
            if (m.act() == Act.SPIN_ALL) MiniManager.spinAll(2200);
            else if (m.act() == Act.HOP_ALL) MiniManager.hopAll(0.6);
            return;
        }
    }
}
