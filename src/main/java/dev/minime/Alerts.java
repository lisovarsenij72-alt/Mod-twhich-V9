package dev.minime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.regex.Pattern;

/** Звоночек при упоминании и звук «67». */
public class Alerts {
    private static final SoundEvent BELL = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("minecraft", "block.bell.use"));
    private static final SoundEvent SIX_SEVEN = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("minime", "six_seven"));
    public static final Pattern NUM67 = Pattern.compile("(?<![\\p{L}\\p{N}])67(?![\\p{L}\\p{N}])");
    private static Pattern mention;
    private static String mentionKey = "";
    private static long last67 = 0, lastBell = 0;

    private static synchronized Pattern mentionPattern(Config c) {
        String key = String.join("|", c.mentionWords);
        if (!key.equals(mentionKey)) {
            mentionKey = key;
            if (c.mentionWords.isEmpty()) mention = null;
            else {
                StringBuilder sb = new StringBuilder("(?iu)(?<![\\p{L}\\p{N}_])(?:");
                for (int i = 0; i < c.mentionWords.size(); i++) {
                    if (i > 0) sb.append('|');
                    sb.append(Pattern.quote(c.mentionWords.get(i).trim()));
                }
                sb.append(")(?![\\p{L}\\p{N}_])");
                mention = Pattern.compile(sb.toString());
            }
        }
        return mention;
    }

    public static void onMessage(String user, String text) {
        Config c = MiniMeClient.config;
        if (c == null) return;
        long now = System.currentTimeMillis();
        if (c.sound67 && NUM67.matcher(text).find() && now - last67 > 2500) {
            last67 = now;
            play(SIX_SEVEN, 1.0f, c.sound67Volume);
        }
        Pattern p = mentionPattern(c);
        if (c.mentionSound && p != null && p.matcher(text).find() && now - lastBell > 1500) {
            lastBell = now;
            play(BELL, 1.4f, 1.0f);
        }
    }

    private static void play(SoundEvent e, float pitch, float volume) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.getSoundManager().play(SimpleSoundInstance.forUI(e, pitch, volume)));
    }
}
