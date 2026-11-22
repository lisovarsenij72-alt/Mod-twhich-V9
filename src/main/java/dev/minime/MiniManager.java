package dev.minime;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Чибики стримера (слоты) и зрителей. Все летают одной компанией рядом друг с другом,
 * живо (блуждают, делают рывки), смотрят на игрока. Физика: пружина и столкновения с блоками.
 */
public class MiniManager {
    public static final String STREAMER = "@streamer";

    private static class Mini {
        LivingEntity e;
        Vec3 vel = Vec3.ZERO;
        long respawnAt = 0;
        boolean cameFromAway = false;
        int awayTicks = 0;
        int id;
        String chatter;
        long lastMsg = System.currentTimeMillis();
        String model = "chibi", skin = "own";
        double scaleMul = 1.0;
        double lagFactor = 1.0;
        long spinUntil = 0;
        long nextDash = System.currentTimeMillis() + 2000;
        // slot: своя «реакция» вычисляется по id
    }

    public record Owner(String key, LivingEntity e) {}

    /** Снимок положения игрока (для задержки зрителей). */
    private record PState(Vec3 pos, Vec3 eye, Vec3 look, double yawDeg) {
        static PState of(LocalPlayer p) { return new PState(p.position(), p.getEyePosition(), p.getLookAngle(), p.getYRot()); }
    }

    private static final ArrayDeque<PState> HIST = new ArrayDeque<>();
    private static final Mini[] MINIS = new Mini[Config.MAX_SLOTS];
    private static final Map<String, Mini> CHATTERS = new LinkedHashMap<>();
    private static final Set<String> TRIED = ConcurrentHashMap.newKeySet();
    private static int nextId = -777100;
    private static volatile long lastMsg = System.currentTimeMillis();
    private static final int FAKE_ID = -777001;
    private static final Random RND = new Random();

    static {
        for (int i = 0; i < MINIS.length; i++) { MINIS[i] = new Mini(); MINIS[i].id = FAKE_ID - i; }
    }

    public static void onMessage() { lastMsg = System.currentTimeMillis(); }

    public static boolean isStreamer(String user) {
        Config c = MiniMeClient.config;
        return user.equalsIgnoreCase(c.channel) || (!c.twitchNick.isBlank() && user.equalsIgnoreCase(c.twitchNick));
    }

    private static Mini firstSlotMini() {
        for (Mini m : MINIS) if (m.e != null && !m.e.isRemoved()) return m;
        return null;
    }

    public static List<Owner> owners() {
        List<Owner> l = new ArrayList<>();
        Mini s = firstSlotMini();
        if (s != null) l.add(new Owner(STREAMER, s.e));
        for (Map.Entry<String, Mini> en : CHATTERS.entrySet()) {
            Mini m = en.getValue();
            if (m.e != null && !m.e.isRemoved()) l.add(new Owner(en.getKey(), m.e));
        }
        return l;
    }

    public static double baseHeight(LivingEntity e) { return e instanceof MiniChibiEntity ? 1.4 : 1.8; }

    public static boolean onAttack(Entity target) {
        for (Mini m : MINIS) if (m.e != null && m.e == target) { popMini(m); return true; }
        for (Mini m : new ArrayList<>(CHATTERS.values())) if (m.e != null && m.e == target) { popMini(m); return true; }
        return false;
    }

    // ---------------- для мемов ----------------

    public static void spinAll(long ms) {
        long until = System.currentTimeMillis() + ms;
        for (Mini m : MINIS) if (m.e != null) m.spinUntil = until;
        for (Mini m : CHATTERS.values()) if (m.e != null) m.spinUntil = until;
    }

    public static void hopAll(double power) {
        for (Mini m : MINIS) if (m.e != null) m.vel = m.vel.add(0, power, 0);
        for (Mini m : CHATTERS.values()) if (m.e != null) m.vel = m.vel.add(0, power, 0);
    }

    /** Частицы у чибика автора (или у первого). */
    public static void burstOwner(String user, String fx, int n) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Mini m = isStreamer(user) ? firstSlotMini() : CHATTERS.get(user.toLowerCase());
        if (m == null || m.e == null) m = firstSlotMini();
        if (m == null || m.e == null) return;
        burst(mc.level, m.e.position().add(0, 0.5 * baseHeight(m.e) * MiniMeClient.config.scale, 0), n, fx);
    }

    // ---------------- сообщения чата ----------------

    public static void onChat(String user, String text) {
        Config cfg = MiniMeClient.config;
        Minecraft mc = Minecraft.getInstance();
        if (!cfg.enabled || mc.level == null) return;
        long now = System.currentTimeMillis();
        Mini m;
        if (isStreamer(user)) m = firstSlotMini();
        else m = cfg.chatterChibis ? touchChatter(user, cfg, mc) : null;
        Memes.handle(user, text);
        if (m == null) return;
        m.lastMsg = now;
        m.vel = m.vel.add(0, 0.3, 0);

        String t = text.trim().toLowerCase();
        if (cfg.chatCommands) {
            if (t.startsWith("!pop")) { popMini(m); return; }
            if (t.startsWith("!spin")) m.spinUntil = now + 2000;
            if (t.startsWith("!hop")) m.vel = m.vel.add(0, 0.9, 0);
        }
        if (cfg.chatHearts && m.e != null && (t.contains("<3") || t.contains("❤") || t.contains("люблю"))) {
            burst(mc.level, m.e.position().add(0, 0.5 * baseHeight(m.e) * cfg.scale, 0), 12, "hearts");
            Sfx.effect("hearts");
        }
    }

    private static Mini touchChatter(String user, Config cfg, Minecraft mc) {
        if (cfg.maxChatters <= 0) return null;
        String key = user.toLowerCase();
        Mini m = CHATTERS.get(key);
        if (m == null) {
            while (CHATTERS.size() >= cfg.maxChatters) {
                String oldest = null; long best = Long.MAX_VALUE;
                for (Map.Entry<String, Mini> en : CHATTERS.entrySet())
                    if (en.getValue().lastMsg < best) { best = en.getValue().lastMsg; oldest = en.getKey(); }
                if (oldest == null) break;
                removeEntity(CHATTERS.remove(oldest));
            }
            m = new Mini();
            m.id = nextId--;
            m.chatter = key;
            int h = Math.abs(key.hashCode());
            String[] builtin = {"steve", "alex", "girl"};
            m.skin = builtin[h % 3];
            m.model = ((h / 3) % 4 == 0) ? "fox" : "chibi";
            m.scaleMul = 0.85 + (h % 31) / 100.0;
            m.lagFactor = 0.6 + (h % 80) / 100.0;                    // у каждого своя «реакция»
            for (String sk : cfg.allSkins()) if (sk.equalsIgnoreCase(key)) { m.skin = sk; m.model = "chibi"; }
            CHATTERS.put(key, m);
            if (cfg.chatterFetchSkins && key.matches("[a-z0-9_]{3,16}") && TRIED.add(key)) {
                final Mini fm = m;
                SkinFetcher.fetchQuiet(key, real -> mc.execute(() -> {
                    if (real != null) { fm.skin = real; fm.model = "chibi"; }
                }));
            }
        }
        return m;
    }

    // ---------------- основной тик ----------------

    private static PState delayed(int ticks) {
        if (HIST.isEmpty()) return null;
        int i = 0;
        for (PState s : HIST) if (i++ >= ticks) return s;
        return HIST.peekLast();
    }

    public static void tick(Minecraft mc) {
        Config cfg = MiniMeClient.config;
        ClientLevel level = mc.level;
        LocalPlayer p = mc.player;
        if (level != null) tickBursts(level);
        if (level == null || p == null || !cfg.enabled) { removeAll(); HIST.clear(); return; }

        PState now0 = PState.of(p);
        HIST.addFirst(now0);
        while (HIST.size() > 200) HIST.removeLast();

        long now = System.currentTimeMillis();
        boolean idle = cfg.autoAway && now - lastMsg > cfg.idleSeconds * 1000L;
        int ns = 0;
        for (Config.Slot s : cfg.slots) if (s.enabled) ns++;
        double sp = 0.45 + 0.6 * cfg.scale;

        int k = 0;
        for (int i = 0; i < MINIS.length; i++) {
            Config.Slot s = cfg.slots.get(i);
            Mini m = MINIS[i];
            if (!s.enabled) { removeEntity(m); continue; }
            m.model = s.model; m.skin = s.skin;
            if (now >= m.respawnAt && !(idle && m.e == null)) {
                double lateral = (k - (ns - 1) / 2.0) * sp;
                double lf = 0.7 + ((i * 37) % 60) / 100.0;                       // у каждого слота своя реакция
                PState st = delayed((int) Math.round(cfg.slotLag * 20 * lf));
                if (st == null) st = now0;
                Vec3 tgt = idle ? awayTarget(now0, k, Math.max(ns, 1)) : target(st, cfg, m, lateral, k % 2 * 0.12);
                boolean ok = drive(m, tgt, idle, k, mc, cfg, level, p, now);
                if (!ok) m.cameFromAway = true;
            }
            k++;
        }

        // зрители: рядом со всеми чибиками, с индивидуальной задержкой
        if (!cfg.chatterChibis) { for (Mini m : CHATTERS.values()) removeEntity(m); CHATTERS.clear(); return; }
        double half = Math.max(ns - 1, 0) / 2.0;
        int c = 0;
        for (Iterator<Map.Entry<String, Mini>> it = CHATTERS.entrySet().iterator(); it.hasNext(); ) {
            Mini m = it.next().getValue();
            boolean quiet = now - m.lastMsg > cfg.chatterSeconds * 1000L;
            if (quiet && m.e == null) { it.remove(); continue; }
            int lag = (int) Math.round(cfg.chatterLag * 20 * m.lagFactor);
            PState st = delayed(lag);
            if (st == null) st = now0;
            double side = (c % 2 == 0) ? 1 : -1;
            double lateral = side * (half + (c / 2 + 1)) * sp;
            Vec3 tgt = quiet ? awayTarget(now0, c + 3, 8) : target(st, cfg, m, lateral, -0.1 + (c % 3) * 0.08);
            boolean ok = drive(m, tgt, quiet, c, mc, cfg, level, p, now);
            if (quiet && !ok) { it.remove(); continue; }
            c++;
        }
    }

    private static boolean drive(Mini m, Vec3 tgt, boolean away, int k, Minecraft mc, Config cfg, ClientLevel level, LocalPlayer p, long now) {
        boolean chibi = (m.model.equals("chibi") || m.model.equals("fox")) && MiniMeMain.CHIBI != null;
        if (m.e != null && (m.e.level() != level || m.e.isRemoved())) m.e = null;
        else if (m.e != null && (m.e instanceof MiniChibiEntity) != chibi) removeEntity(m);

        if (m.e == null) {
            m.e = chibi ? new MiniChibiEntity(MiniMeMain.CHIBI, level)
                        : new MiniEntity(level, new GameProfile(UUID.randomUUID(), "Mini"));
            m.e.setId(m.id);
            m.e.setNoGravity(true);
            m.e.setSilent(true);
            m.e.noPhysics = false;
            Vec3 start = m.cameFromAway ? awayTarget(PState.of(p), k, 8) : tgt;
            m.e.setPos(start.x, start.y, start.z);
            level.addEntity(m.e);
            m.vel = Vec3.ZERO;
            if (!m.cameFromAway) burst(level, start.add(0, 0.3 * cfg.scale, 0), 12, cfg.popEffect);
            m.cameFromAway = false;
            m.awayTicks = 0;
        }

        if (m.e instanceof MiniChibiEntity ce) {
            ce.fox = m.model.equals("fox");
            ce.texture = SkinManager.skinFor(mc, m.skin).body().texturePath();
        } else if (m.e instanceof MiniEntity pl) {
            pl.skinName = m.skin;
        }

        AttributeInstance sc = m.e.getAttribute(Attributes.SCALE);
        double want = cfg.scale * m.scaleMul;
        if (sc != null && Math.abs(sc.getBaseValue() - want) > 1e-4) sc.setBaseValue(want);

        Vec3 here = m.e.position();
        if (away) {
            m.awayTicks++;
            if (here.distanceTo(p.position()) > 14 || m.awayTicks > 120) { removeEntity(m); return false; }
        } else {
            m.awayTicks = 0;
            if (now > m.nextDash) {                                   // время от времени резкий рывок
                m.vel = m.vel.add((RND.nextDouble() - 0.5) * 0.5, (RND.nextDouble() - 0.3) * 0.3, (RND.nextDouble() - 0.5) * 0.5);
                m.nextDash = now + 2500 + RND.nextInt(4500);
            }
        }
        if (here.distanceTo(tgt) > 60) {
            m.e.setPos(tgt.x, tgt.y, tgt.z);
            m.vel = Vec3.ZERO;
            here = tgt;
        }
        m.vel = m.vel.scale(0.8).add(tgt.subtract(here).scale(away ? 0.03 : 0.05));
        double spd = m.vel.length();
        if (spd > 1.0) m.vel = m.vel.scale(1.0 / spd);
        m.e.move(MoverType.SELF, m.vel);

        // смотрит на игрока: поворот корпуса, головы и наклон вверх/вниз
        double dx = p.getX() - m.e.getX(), dz = p.getZ() - m.e.getZ();
        double dy = p.getEyeY() - m.e.getEyeY();
        float yawT = (float) (Mth.atan2(dz, dx) * 57.29578) - 90f;
        float pitchT = Mth.clamp((float) (-Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 57.29578), -45f, 45f);
        float yaw = Mth.rotLerp(0.4f, m.e.getYRot(), yawT);
        if (now < m.spinUntil) yaw = yawT + (now % 600) / 600f * 360f;       // крутится (!spin, мемы)
        m.e.setYRot(yaw);
        m.e.setYHeadRot(yaw);
        m.e.setYBodyRot(yaw);
        m.e.setXRot(Mth.lerp(0.4f, m.e.getXRot(), pitchT));
        return true;
    }

    private static Vec3 flatForward(double yawDeg) {
        double yaw = Math.toRadians(yawDeg);
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    /** Точка компании + боковое смещение + «блуждание», чтобы чибики летали, а не висели. */
    private static Vec3 target(PState s, Config cfg, Mini m, double lateral, double zig) {
        double t = System.currentTimeMillis() / 1000.0;
        double ph = m.id * 1.7;
        double d = cfg.distance;
        Vec3 fwd = flatForward(s.yawDeg());
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
        Vec3 base;
        switch (cfg.position) {
            case "side":
                base = s.pos().add(fwd.scale(0.4 * d)).add(right.scale(0.45 * d)).add(0, 1.2, 0);
                break;
            case "back":
                base = s.pos().add(fwd.scale(-0.6 * d)).add(0, 1.5, 0);
                break;
            default:
                base = s.eye().add(s.look().scale(d)).add(0, 0.15, 0);
        }
        double a = 0.25 + 0.6 * cfg.scale;                       // размах блуждания
        double wx = (Math.sin(t * 0.9 + ph) * 0.8 + Math.sin(t * 1.7 + ph * 2) * 0.25) * a;
        double wy = (Math.sin(t * 1.3 + ph * 0.5) * 0.6 + Math.sin(t * 2.3 + ph) * 0.2) * a;
        double wz = Math.cos(t * 0.7 + ph * 1.3) * a;
        return base.add(right.scale(lateral + wx)).add(fwd.scale(wz)).add(0, zig + wy, 0);
    }

    private static Vec3 awayTarget(PState s, int k, int n) {
        Vec3 fwd = flatForward(s.yawDeg());
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
        return s.pos().add(fwd.scale(-14)).add(right.scale((k - (n - 1) / 2.0) * 3)).add(0, 9, 0);
    }

    private static void popMini(Mini m) {
        Minecraft mc = Minecraft.getInstance();
        if (m.e == null || mc.level == null) return;
        Config cfg = MiniMeClient.config;
        Vec3 c = m.e.position().add(0, 0.5 * baseHeight(m.e) * cfg.scale, 0);
        burst(mc.level, c, 64, cfg.popEffect);
        Sfx.effect(cfg.popEffect);
        removeEntity(m);
        m.cameFromAway = false;
        if (m.chatter != null) CHATTERS.remove(m.chatter);
        else m.respawnAt = System.currentTimeMillis() + cfg.respawnSeconds * 1000L;
    }

    public static void popFirst() {
        Mini m = firstSlotMini();
        if (m != null) popMini(m);
    }

    /** Ключи и названия эффектов (для меню). */
    public static final String[] FX = {"poof", "snow", "cherry", "sand", "souls", "hearts", "flame", "stars", "notes", "magic",
            "portal", "totem", "firework", "sparks", "glow", "crit", "joy", "witch", "angry", "ink", "gold", "diamond", "emerald"};
    public static final String[] FXN = {"дым", "снег", "сакура", "песок", "души", "сердца", "огонь", "звёзды", "ноты", "магия",
            "портал", "тотем", "салют", "искры", "свечение", "крит", "радость", "ведьма", "злость", "чернила", "золото", "алмазы", "изумруды"};

    private static ParticleOptions effectOf(String effect) {
        switch (effect) {
            case "snow": return ParticleTypes.SNOWFLAKE;
            case "cherry": return ParticleTypes.CHERRY_LEAVES;
            case "sand": return ParticleTypes.WHITE_ASH;          // лёгкая светлая пыль, не падает кучей
            case "souls": return ParticleTypes.SOUL;
            case "hearts": return ParticleTypes.HEART;
            case "flame": return ParticleTypes.FLAME;
            case "stars": return ParticleTypes.END_ROD;
            case "notes": return ParticleTypes.NOTE;
            case "magic": return ParticleTypes.ENCHANT;
            case "portal": return ParticleTypes.PORTAL;
            case "totem": return ParticleTypes.TOTEM_OF_UNDYING;
            case "firework": return ParticleTypes.FIREWORK;
            case "sparks": return ParticleTypes.ELECTRIC_SPARK;
            case "glow": return ParticleTypes.GLOW;
            case "crit": return ParticleTypes.CRIT;
            case "joy": return ParticleTypes.HAPPY_VILLAGER;
            case "witch": return ParticleTypes.WITCH;
            case "angry": return ParticleTypes.ANGRY_VILLAGER;
            case "ink": return ParticleTypes.SQUID_INK;
            case "gold": return ParticleTypes.WAX_ON;             // золотые искры
            case "diamond": return ParticleTypes.SCRAPE;          // бирюзово-белые искры
            case "emerald": return ParticleTypes.COMPOSTER;       // зелёные искры
            default: return ParticleTypes.POOF;
        }
    }

    private static double speedOf(String fx) {
        switch (fx) {
            case "firework": return 0.30;
            case "sparks": return 0.25;
            case "stars": return 0.22;
            case "hearts": return 0.10;
            case "souls": return 0.15;
            case "snow": case "cherry": return 0.06;
            default: return 0.20;
        }
    }

    /** Эффекты, которые сами падают: их разбрасываем по сфере, а не из одной точки. */
    private static boolean heavy(String fx) {
        return fx.equals("snow") || fx.equals("cherry") || fx.equals("totem");
    }

    private static class Burst {
        Vec3 c; String fx; int total; int step = 0; final int steps = 8;
        Burst(Vec3 c, String fx, int total) { this.c = c; this.fx = fx; this.total = total; }
    }
    private static final List<Burst> BURSTS = new ArrayList<>();

    /** Ставит залп в очередь: частицы вылетают во все стороны волнами в течение ~0.4 с. */
    private static void burst(ClientLevel level, Vec3 c, int n, String effect) {
        BURSTS.add(new Burst(c, effect, n));
    }

    private static void tickBursts(ClientLevel level) {
        for (Iterator<Burst> it = BURSTS.iterator(); it.hasNext(); ) {
            Burst b = it.next();
            emit(level, b);
            if (++b.step >= b.steps) it.remove();
        }
    }

    private static void emit(ClientLevel level, Burst b) {
        ParticleOptions fx = effectOf(b.fx);
        int per = Math.max(1, b.total / b.steps);
        double speed = speedOf(b.fx);
        double maxR = heavy(b.fx) ? 0.9 : 0.2;
        double r = maxR * (b.step + 1) / b.steps;
        for (int i = 0; i < per; i++) {
            // равномерное случайное направление по сфере
            double ux = RND.nextGaussian(), uy = RND.nextGaussian(), uz = RND.nextGaussian();
            double len = Math.sqrt(ux * ux + uy * uy + uz * uz);
            if (len < 1e-4) { ux = 0; uy = 1; uz = 0; len = 1; }
            ux /= len; uy /= len; uz /= len;
            double rr = r * (0.4 + 0.6 * RND.nextDouble());
            double vx = ux * speed, vy = uy * speed, vz = uz * speed;
            if ("notes".equals(b.fx)) { vx = RND.nextDouble(); vy = 0; vz = 0; }   // у нот первая компонента — цвет
            level.addParticle(fx, b.c.x + ux * rr, b.c.y + uy * rr, b.c.z + uz * rr, vx, vy, vz);
        }
    }

    private static void removeEntity(Mini m) {
        if (m != null && m.e != null) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) level.removeEntity(m.e.getId(), Entity.RemovalReason.DISCARDED);
            m.e = null;
            m.vel = Vec3.ZERO;
        }
    }

    public static void removeAll() {
        for (Mini m : MINIS) removeEntity(m);
        for (Mini m : CHATTERS.values()) removeEntity(m);
    }
}
