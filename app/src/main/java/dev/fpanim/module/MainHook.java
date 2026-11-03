package dev.fpanim.module;

import android.content.res.Resources;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "[FPAnim] ";
    private static final Set<String> SEEN = Collections.synchronizedSet(new HashSet<String>());

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        String pkg = lp.packageName;
        if (!"com.android.systemui".equals(pkg)
                && !"com.samsung.android.biometrics.app.setting".equals(pkg)) {
            return;
        }
        XposedBridge.log(TAG + "loaded in " + pkg);
        try {
            hookResources(pkg);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "resource hook failed: " + t);
        }
        try {
            hookLottie(lp.classLoader, pkg);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "lottie hook failed: " + t);
        }
    }

    private static String entryName(Resources res, int id) {
        try {
            return res.getResourceEntryName(id);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isTarget(String pkg, String name) {
        if (Prefs.debug()
                && name.matches("(?i).*(finger|udfps|fp_|biometric).*")
                && SEEN.add(name)) {
            XposedBridge.log(TAG + pkg + " saw resource: " + name);
        }
        return Prefs.pattern().matcher(name).matches();
    }

    private void hookResources(final String pkg) {
        Class<?> impl = XposedHelpers.findClass("android.content.res.ResourcesImpl", null);
        XposedBridge.hookAllMethods(impl, "loadDrawable", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (param.args.length < 3
                            || !(param.args[0] instanceof Resources)
                            || !(param.args[2] instanceof Integer)) return;
                    if (!Prefs.enabled()) return;
                    int id = (Integer) param.args[2];
                    if (id == 0) return;
                    String name = entryName((Resources) param.args[0], id);
                    if (name == null || !isTarget(pkg, name)) return;
                    Drawable d = Replacements.build();
                    if (d != null) {
                        if (Prefs.debug()) XposedBridge.log(TAG + "replaced drawable: " + name);
                        param.setResult(d);
                    }
                } catch (Throwable t) {
                    XposedBridge.log(TAG + t);
                }
            }
        });
        XposedBridge.log(TAG + "hooked ResourcesImpl.loadDrawable in " + pkg);
    }

    private void hookLottie(ClassLoader cl, final String pkg) {
        Class<?> lav = XposedHelpers.findClassIfExists("com.airbnb.lottie.LottieAnimationView", cl);
        if (lav == null) {
            XposedBridge.log(TAG + "no Lottie in " + pkg + " (ok, resource hook only)");
            return;
        }
        XposedBridge.hookAllMethods(lav, "setAnimation", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (param.args.length != 1 || !(param.args[0] instanceof Integer)) return;
                    if (!Prefs.enabled()) return;
                    View v = (View) param.thisObject;
                    String name = entryName(v.getResources(), (Integer) param.args[0]);
                    if (name == null || !isTarget(pkg, name)) return;

                    String json = Replacements.lottieJson();
                    if (json != null) {
                        XposedHelpers.callMethod(v, "setAnimationFromJson", json, name);
                        param.setResult(null);
                        XposedBridge.log(TAG + "replaced Lottie: " + name);
                        return;
                    }
                    Drawable d = Replacements.build();
                    if (d != null) {
                        XposedHelpers.callMethod(v, "setImageDrawable", d);
                        if (d instanceof Animatable) ((Animatable) d).start();
                        param.setResult(null);
                        XposedBridge.log(TAG + "replaced Lottie with drawable: " + name);
                    }
                } catch (Throwable t) {
                    XposedBridge.log(TAG + t);
                }
            }
        });
        XposedBridge.log(TAG + "hooked LottieAnimationView.setAnimation in " + pkg);
    }
}
