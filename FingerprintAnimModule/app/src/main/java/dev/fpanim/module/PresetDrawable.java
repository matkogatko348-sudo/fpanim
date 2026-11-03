package dev.fpanim.module;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

/** Original, self-contained animated presets drawn with Canvas. */
public class PresetDrawable extends Drawable implements Animatable {
    private static final long PERIOD_MS = 1400;

    private final int style;
    private final int color;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final long start = SystemClock.uptimeMillis();
    private boolean running = true;

    public PresetDrawable(int style, int color) {
        this.style = style;
        this.color = color;
    }

    private static int withAlpha(int c, int a) {
        return (c & 0x00FFFFFF) | (Math.max(0, Math.min(255, a)) << 24);
    }

    private static int shift(int c, float deg) {
        float[] hsv = new float[3];
        Color.colorToHSV(c, hsv);
        hsv[0] = (hsv[0] + deg + 360f) % 360f;
        return Color.HSVToColor(hsv);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        float cx = b.exactCenterX();
        float cy = b.exactCenterY();
        float r = Math.min(b.width(), b.height()) / 2f;
        float t = ((SystemClock.uptimeMillis() - start) % PERIOD_MS) / (float) PERIOD_MS;

        paint.setShader(null);
        switch (style) {
            case Prefs.GLOW:
                drawGlow(canvas, cx, cy, r, t);
                break;
            case Prefs.AURORA:
                drawAurora(canvas, cx, cy, r, t);
                break;
            default:
                drawRipple(canvas, cx, cy, r, t);
                break;
        }
        if (running && isVisible()) invalidateSelf();
    }

    private void drawRipple(Canvas c, float cx, float cy, float r, float t) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, r * 0.05f));
        for (int i = 0; i < 3; i++) {
            float p = (t + i / 3f) % 1f;
            paint.setColor(color);
            paint.setAlpha((int) (255 * (1f - p) * (1f - p)));
            c.drawCircle(cx, cy, r * (0.25f + 0.75f * p), paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setAlpha(200);
        c.drawCircle(cx, cy, r * 0.08f, paint);
    }

    private void drawGlow(Canvas c, float cx, float cy, float r, float t) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(2 * Math.PI * t);
        float rad = Math.max(1f, r * (0.55f + 0.45f * pulse));
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(cx, cy, rad,
                new int[]{withAlpha(color, 210), withAlpha(color, 90), withAlpha(color, 0)},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, rad, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, r * 0.03f));
        paint.setColor(color);
        paint.setAlpha((int) (120 + 100 * pulse));
        c.drawCircle(cx, cy, r * 0.5f, paint);
    }

    private void drawAurora(Canvas c, float cx, float cy, float r, float t) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(2 * Math.PI * t);
        int c0 = withAlpha(color, 230);
        int c1 = withAlpha(shift(color, 70f), 230);
        int c2 = withAlpha(shift(color, -70f), 230);
        c.save();
        c.rotate(t * 360f, cx, cy);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(3f, r * 0.14f));
        paint.setShader(new SweepGradient(cx, cy, new int[]{c0, c1, c2, c0}, null));
        paint.setAlpha((int) (170 + 85 * pulse));
        c.drawCircle(cx, cy, r * 0.8f, paint);
        c.restore();
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setAlpha((int) (40 + 40 * pulse));
        c.drawCircle(cx, cy, r * 0.6f, paint);
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (visible && changed) invalidateSelf();
        return changed;
    }

    @Override public void start() { running = true; invalidateSelf(); }
    @Override public void stop() { running = false; }
    @Override public boolean isRunning() { return running; }
}
