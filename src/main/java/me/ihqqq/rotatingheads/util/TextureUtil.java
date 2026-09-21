package me.ihqqq.rotatingheads.util;

import me.ihqqq.rotatingheads.exception.MessageException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextureUtil {
    private static final String TEXTURE_HOST = "textures.minecraft.net";
    private static final String TEXTURE_BASE = "http://" + TEXTURE_HOST + "/texture/";
    private static final Pattern HASH = Pattern.compile("[0-9a-fA-F]{20,64}");
    private static final Pattern URL_FIELD =
            Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    private TextureUtil() {
    }

    public static String normalize(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) {
            return "";
        }
        if (HASH.matcher(value).matches()) {
            return encode(TEXTURE_BASE + value.toLowerCase());
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return encode(requireTextureUrl(value));
        }
        String url = urlFromBase64(value);
        if (url == null) {
            throw new MessageException("validation.texture");
        }
        return value;
    }

    public static String urlFromBase64(String base64) {
        try {
            String json = new String(Base64.getDecoder().decode(base64.trim()),
                    StandardCharsets.UTF_8);
            Matcher matcher = URL_FIELD.matcher(json);
            if (!matcher.find()) {
                return null;
            }
            return requireTextureUrl(matcher.group(1));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String requireTextureUrl(String url) {
        final String host;
        try {
            host = URI.create(url).getHost();
        } catch (IllegalArgumentException exception) {
            throw new MessageException("validation.texture");
        }
        if (host == null || !host.equalsIgnoreCase(TEXTURE_HOST)) {
            throw new MessageException("validation.texture");
        }
        return url;
    }

    static String encode(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}