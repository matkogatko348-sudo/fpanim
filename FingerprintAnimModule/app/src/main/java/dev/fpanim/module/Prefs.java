package dev.fpanim.module;

import android.graphics.Color;
import android.os.SystemClock;

import java.util.regex.Pattern;

import de.robv.android.xposed.XSharedPreferences;

/** Hook-side reader for the settings saved by SettingsActivity. */
public final class Prefs {
    public static final String PKG = "dev.fpanim.module";
    public static final String NAME = "settings";

    public static final String K_ENABLED = "enabled";
    public static final String K_STYLE = "style";
    public static final String K_COLOR = "color";
    public static final String K_DIR = "dir";
    public static final String K_PATTERN = "pattern";
    public static final String K_FPS = "fps";
    public static final String K_DEBUG = "debug";

    public static final int RIPPLE = 0, GLOW = 1, AURORA = 2, CUSTOM = 3;

    public static final String DEFAULT_COLOR = "#00E5FF";
    public static final String DEFAULT_DIR = "/sdcard/FingerprintAnim";
    public static final String DEFAULT_PATTERN =
            "(?i).*(fingerprint|udfps|finger_|fp_).*(anim|ripple|effect|lottie|scan|touch).*";

    private static XSharedPreferences xp;
    private static long lastReload;
    private static String patternSrc;
    private static Pattern pattern;

    private Prefs() {}

    private static synchronized XSharedPreferences p() {
        long now = SystemClock.uptimeMillis();
        if (xp == null) {
            xp = new XSharedPreferences(PKG, NAME);
            xp.makeWorldReadable();
            lastReload = now;
        } else if (now - lastReload > 2000) {
            xp.reload();
            lastReload = now;
        }
        return xp;
    }

    public static boolean enabled() { return p().getBoolean(K_ENABLED, true); }
    public static boolean debug() { return p().getBoolean(K_DEBUG, true); }
    public static int style() { return p().getInt(K_STYLE, RIPPLE); }
    public static String dir() { return p().getString(K_DIR, DEFAULT_DIR); }

    public static int fps() {
        try {
            int v = Integer.parseInt(p().getString(K_FPS, "30").trim());
            return Math.max(1, Math.min(120, v));
        } catch (Throwable t) {
            return 30;
        }
    }

    public static int color() {
        try {
            return Color.parseColor(p().getString(K_COLOR, DEFAULT_COLOR).trim());
        } catch (Throwable t) {
            return Color.parseColor(DEFAULT_COLOR);
        }
    }

    public static synchronized Pattern pattern() {
        String src = p().getString(K_PATTERN, DEFAULT_PATTERN);
        if (pattern == null || !src.equals(patternSrc)) {
            try {
                pattern = Pattern.compile(src);
            } catch (Throwable t) {
                pattern = Pattern.compile(DEFAULT_PATTERN);
            }
            patternSrc = src;
        }
        return pattern;
    }
}
