package nota.android.crash.xp;

import android.app.Application;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import nota.android.crash.CrashHandler;
import nota.android.crash.capture.CrashCapturePipeline;
import nota.android.crash.xp.app.ModuleActivation;


public class XposedEntry implements IXposedHookLoadPackage {
    private static final String PACKAGE_NAME = PrefManager.PACKAGE_NAME;
    private static XSharedPreferences sXSharedPreferences = null;

    private ScopeDecision evaluatePackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (selfCheck(lpparam)) {
            return new ScopeDecision(true, true, true);
        }

        if (sXSharedPreferences == null) {
            sXSharedPreferences = new XSharedPreferences(PACKAGE_NAME, PrefManager.PREF_NAME);
        }
        sXSharedPreferences.reload();
        return ScopePolicy.evaluate(sXSharedPreferences, lpparam);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        ScopeDecision decision = evaluatePackage(lpparam);
        if (!decision.getShouldHook()) {
            return;
        }

        XposedBridge.log("catch package: " + lpparam.packageName);
        XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                XposedBridge.log("onCreate");
                hookToGrabCrash(lpparam, param, decision);
            }
        });


    }

    private void hookToGrabCrash(
            XC_LoadPackage.LoadPackageParam lpparam,
            XC_MethodHook.MethodHookParam param,
            ScopeDecision decision) {
        Application currentApplicationContext = (Application) param.thisObject;
        CrashHandler.insert((throwable, source) -> CrashCapturePipeline.onException(
                currentApplicationContext,
                lpparam.packageName,
                lpparam.appInfo,
                throwable,
                source,
                decision));
    }

    private boolean selfCheck(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals(PACKAGE_NAME)) {
            Log.e("XposedEntry", "selfCheck:pkg: " + lpparam.packageName);
            try {
                XposedHelpers.findAndHookMethod(
                        ModuleActivation.class.getName(),
                        lpparam.classLoader,
                        "isModuleActive",
                        XC_MethodReplacement.returnConstant(true));
            } catch (Exception e) {
                Log.e("XposedEntry", "selfCheckException: " + e.getMessage());
                XposedBridge.log(e);
            }
            return true;
        }
        return false;
    }
}
