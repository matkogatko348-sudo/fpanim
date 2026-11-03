package dev.fpanim.module;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

/** Builds the replacement drawable (preset or custom frames) or Lottie JSON. */
final class Replacements {
    private static final int MAX_FRAMES = 150;
    private static String cachedKey;
    private static List<Bitmap> frames;
    private static boolean warnedMissing;

    private Replacements() {}

    static Drawable build() {
        int s = Prefs.style();
        if (s == Prefs.CUSTOM) return frameDrawable();
        return new PresetDrawable(s, Prefs.color());
    }

    /** Returns animation.json from the custom folder, or null. Only used in custom mode. */
    static String lottieJson() {
        if (Prefs.style() != Prefs.CUSTOM) return null;
        try {
            File f = new File(Prefs.dir(), "animation.json");
            if (!f.isFile()) return null;
            try (FileInputStream in = new FileInputStream(f)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return out.toString("UTF-8");
            }
        } catch (Throwable t) {
            XposedBridge.log("[FPAnim] cannot read animation.json: " + t);
            return null;
        }
    }

    private static synchronized Drawable frameDrawable() {
        try {
            File dir = new File(Prefs.dir());
            File[] files = dir.listFiles((d, n) -> {
                String l = n.toLowerCase();
                return l.endsWith(".png") || l.endsWith(".webp");
            });
            if (files == null || files.length == 0) {
                if (!warnedMissing) {
                    XposedBridge.log("[FPAnim] no PNG/WebP frames readable in " + dir
                            + " (missing folder or SELinux denial?)");
                    warnedMissing = true;
                }
                return null;
            }
            Arrays.sort(files);
            String key = dir.getPath() + "|" + files.length + "|" + files[0].lastModified();
            if (!key.equals(cachedKey)) {
                List<Bitmap> list = new ArrayList<>();
                for (int i = 0; i < Math.min(files.length, MAX_FRAMES); i++) {
                    Bitmap bm = BitmapFactory.decodeFile(files[i].getPath());
                    if (bm != null) list.add(bm);
                }
                frames = list;
                cachedKey = key;
                XposedBridge.log("[FPAnim] loaded " + list.size() + " frames from " + dir);
            }
            if (frames == null || frames.isEmpty()) return null;
            AnimationDrawable ad = new AnimationDrawable();
            int dur = 1000 / Prefs.fps();
            for (Bitmap bm : frames) {
                ad.addFrame(new BitmapDrawable(Resources.getSystem(), bm), dur);
            }
            ad.setOneShot(false);
            ad.start();
            return ad;
        } catch (Throwable t) {
            XposedBridge.log("[FPAnim] frame load failed: " + t);
            return null;
        }
    }
}
