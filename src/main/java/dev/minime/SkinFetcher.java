package dev.minime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.function.Consumer;

/** Скачивает скин игрока Minecraft по нику и кладёт в config/minime/skins. */
public class SkinFetcher {
    public static void fetch(String name, Consumer<String> status) {
        String n = name == null ? "" : name.trim();
        if (!n.matches("[A-Za-z0-9_]{1,16}")) { status.accept("Неверный ник"); return; }
        status.accept("Скачиваю скин " + n + "…");
        Thread t = new Thread(() -> {
            try {
                String real = download(n);
                status.accept("Скин " + real + " сохранён: выберите его кнопкой скина");
            } catch (Exception e) {
                status.accept("Ошибка: игрок не найден или нет интернета");
            }
        }, "minime-skin");
        t.setDaemon(true);
        t.start();
    }

    /** Тихая версия для зрителей: возвращает имя файла или null. */
    public static void fetchQuiet(String name, Consumer<String> done) {
        Thread t = new Thread(() -> {
            String real = null;
            try { real = download(name.trim()); } catch (Exception ignored) {}
            done.accept(real);
        }, "minime-skin-quiet");
        t.setDaemon(true);
        t.start();
    }

    private static String download(String n) throws Exception {
        HttpClient c = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(8)).build();
        JsonObject o1 = JsonParser.parseString(getString(c, "https://api.mojang.com/users/profiles/minecraft/" + n)).getAsJsonObject();
        String id = o1.get("id").getAsString();
        String real = o1.get("name").getAsString();
        JsonObject prof = JsonParser.parseString(getString(c, "https://sessionserver.mojang.com/session/minecraft/profile/" + id)).getAsJsonObject();
        String tex = null;
        JsonArray props = prof.getAsJsonArray("properties");
        for (JsonElement e : props) {
            JsonObject po = e.getAsJsonObject();
            if ("textures".equals(po.get("name").getAsString())) tex = po.get("value").getAsString();
        }
        if (tex == null) throw new IllegalStateException("no textures");
        JsonObject texJson = JsonParser.parseString(new String(Base64.getDecoder().decode(tex), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject skin = texJson.getAsJsonObject("textures").getAsJsonObject("SKIN");
        String url = skin.get("url").getAsString();
        boolean slim = skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")
                && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(getBytes(c, url)));
        if (img.getHeight() == 32) img = upgradeLegacy(img);
        Path dir = Config.DIR.resolve("skins");
        Files.createDirectories(dir);
        ImageIO.write(img, "png", dir.resolve(real + ".png").toFile());
        Path marker = dir.resolve(real + ".slim");
        if (slim) Files.writeString(marker, "slim"); else Files.deleteIfExists(marker);
        Minecraft.getInstance().execute(SkinManager::clear);
        return real;
    }

    private static String getString(HttpClient c, String url) throws Exception {
        HttpResponse<String> r = c.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new IllegalStateException("http " + r.statusCode());
        return r.body();
    }

    private static byte[] getBytes(HttpClient c, String url) throws Exception {
        HttpResponse<byte[]> r = c.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() != 200) throw new IllegalStateException("http " + r.statusCode());
        return r.body();
    }

    /** Старый формат 64x32 -> 64x64 (копируем левую руку и ногу зеркально). */
    private static BufferedImage upgradeLegacy(BufferedImage src) {
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        int[][] r = {
                {4, 16, 16, 32, 4, 4}, {8, 16, 16, 32, 4, 4}, {0, 20, 24, 32, 4, 12}, {4, 20, 16, 32, 4, 12}, {8, 20, 8, 32, 4, 12}, {12, 20, 16, 32, 4, 12},
                {44, 16, -8, 32, 4, 4}, {48, 16, -8, 32, 4, 4}, {40, 20, 0, 32, 4, 12}, {44, 20, -8, 32, 4, 12}, {48, 20, -16, 32, 4, 12}, {52, 20, -8, 32, 4, 12}};
        for (int[] a : r)
            for (int i = 0; i < a[4]; i++)
                for (int j = 0; j < a[5]; j++)
                    out.setRGB(a[0] + a[2] + (a[4] - 1 - i), a[1] + a[3] + j, src.getRGB(a[0] + i, a[1] + j));
        return out;
    }
}
