package dev.fpanim.module;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private SharedPreferences sp;
    private RadioGroup group;
    private EditText colorEt, dirEt, patEt, fpsEt;
    private CheckBox enabledCb, debugCb;
    private ImageView preview;
    private TextView previewNote;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @SuppressWarnings({"deprecation", "WorldReadableFiles"})
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        boolean shareable = true;
        try {
            sp = getSharedPreferences(Prefs.NAME, MODE_WORLD_READABLE);
        } catch (SecurityException e) {
            shareable = false;
            sp = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        TextView status = new TextView(this);
        status.setText(shareable
                ? "Module storage OK. Enable this module in LSPosed, scope SystemUI and the "
                  + "Samsung Biometrics app, then soft reboot."
                : "WARNING: LSPosed is not active for this app, so settings cannot reach the "
                  + "hooks. Enable the module in LSPosed first, then reopen this screen.");
        status.setPadding(0, 0, 0, dp(12));
        root.addView(status);

        enabledCb = new CheckBox(this);
        enabledCb.setText("Enable replacement");
        enabledCb.setChecked(sp.getBoolean(Prefs.K_ENABLED, true));
        root.addView(enabledCb);

        root.addView(label("Animation style"));
        group = new RadioGroup(this);
        String[] names = {"Ripple rings", "Glow burst", "Aurora sweep", "Custom folder"};
        int cur = sp.getInt(Prefs.K_STYLE, Prefs.RIPPLE);
        for (int i = 0; i < names.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(100 + i);
            rb.setText(names[i]);
            group.addView(rb);
        }
        group.check(100 + cur);
        root.addView(group);

        preview = new ImageView(this);
        preview.setBackground(new ColorDrawable(Color.BLACK));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(180), dp(180));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = dp(8);
        root.addView(preview, lp);
        previewNote = new TextView(this);
        previewNote.setGravity(Gravity.CENTER);
        root.addView(previewNote);

        root.addView(label("Preset color (hex)"));
        colorEt = edit(sp.getString(Prefs.K_COLOR, Prefs.DEFAULT_COLOR));
        root.addView(colorEt);

        root.addView(label("Custom folder (animation.json or PNG/WebP frames)"));
        dirEt = edit(sp.getString(Prefs.K_DIR, Prefs.DEFAULT_DIR));
        root.addView(dirEt);

        root.addView(label("Frame rate for PNG/WebP frames"));
        fpsEt = edit(sp.getString(Prefs.K_FPS, "30"));
        fpsEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(fpsEt);

        root.addView(label("Resource name pattern (regex)"));
        patEt = edit(sp.getString(Prefs.K_PATTERN, Prefs.DEFAULT_PATTERN));
        root.addView(patEt);

        debugCb = new CheckBox(this);
        debugCb.setText("Log fingerprint-related resource names (LSPosed logs, tag FPAnim)");
        debugCb.setChecked(sp.getBoolean(Prefs.K_DEBUG, true));
        root.addView(debugCb);

        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        });
        root.addView(save);

        Button reset = new Button(this);
        reset.setText("Reset pattern to default");
        reset.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { patEt.setText(Prefs.DEFAULT_PATTERN); }
        });
        root.addView(reset);

        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup g, int id) { updatePreview(); }
        });
        updatePreview();
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setPadding(0, dp(14), 0, dp(2));
        return t;
    }

    private EditText edit(String value) {
        EditText e = new EditText(this);
        e.setText(value);
        e.setSingleLine(true);
        return e;
    }

    private int selectedStyle() {
        return Math.max(0, group.getCheckedRadioButtonId() - 100);
    }

    private void updatePreview() {
        int style = selectedStyle();
        if (style == Prefs.CUSTOM) {
            preview.setImageDrawable(null);
            previewNote.setText("Custom animations are not previewed here.");
            return;
        }
        int color;
        try {
            color = Color.parseColor(colorEt.getText().toString().trim());
        } catch (Throwable t) {
            color = Color.parseColor(Prefs.DEFAULT_COLOR);
        }
        preview.setImageDrawable(new PresetDrawable(style, color));
        previewNote.setText("Preview");
    }

    private void save() {
        boolean ok = sp.edit()
                .putBoolean(Prefs.K_ENABLED, enabledCb.isChecked())
                .putInt(Prefs.K_STYLE, selectedStyle())
                .putString(Prefs.K_COLOR, colorEt.getText().toString().trim())
                .putString(Prefs.K_DIR, dirEt.getText().toString().trim())
                .putString(Prefs.K_FPS, fpsEt.getText().toString().trim())
                .putString(Prefs.K_PATTERN, patEt.getText().toString().trim())
                .putBoolean(Prefs.K_DEBUG, debugCb.isChecked())
                .commit();
        updatePreview();
        Toast.makeText(this, ok ? "Saved. Soft reboot to apply." : "Save failed",
                Toast.LENGTH_LONG).show();
    }
}
