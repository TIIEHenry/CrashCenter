package nota.android.crash.xp

import android.app.Application
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import nota.android.crash.log.hookSafeLog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import nota.android.crash.CrashHandler
import nota.android.crash.capture.CrashCapturePipeline
import nota.android.crash.xp.app.ModuleActivation

private const val PACKAGE_NAME = PrefManager.PACKAGE_NAME
private var xSharedPreferences: XSharedPreferences? = null

class XposedEntry : IXposedHookLoadPackage {

    private fun evaluatePackage(lpparam: XC_LoadPackage.LoadPackageParam): ScopeDecision {
        if (selfCheck(lpparam)) {
            return ScopeDecision(true, true, true)
        }

        if (xSharedPreferences == null) {
            xSharedPreferences = XSharedPreferences(PACKAGE_NAME, PrefManager.PREF_NAME)
        }
        xSharedPreferences?.reload()
        return ScopePolicy.evaluate(xSharedPreferences!!, lpparam)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val decision = evaluatePackage(lpparam)
        if (!decision.shouldHook) {
            return
        }

        hookSafeLog("catch package: ${lpparam.packageName}")
        XposedHelpers.findAndHookMethod(
            "android.app.Application",
            lpparam.classLoader,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    hookSafeLog("onCreate")
                    hookToGrabCrash(lpparam, param, decision)
                }
            }
        )
    }

    private fun hookToGrabCrash(
        lpparam: XC_LoadPackage.LoadPackageParam,
        param: XC_MethodHook.MethodHookParam,
        decision: ScopeDecision
    ) {
        val currentApplicationContext = param.thisObject as Application
        CrashHandler.insert { throwable, source ->
            CrashCapturePipeline.onException(
                currentApplicationContext,
                lpparam.packageName,
                lpparam.appInfo,
                throwable,
                source,
                decision
            )
        }
    }

    private fun selfCheck(lpparam: XC_LoadPackage.LoadPackageParam): Boolean {
        if (lpparam.packageName == PACKAGE_NAME) {
            Log.e("XposedEntry", "selfCheck:pkg: ${lpparam.packageName}")
            try {
                XposedHelpers.findAndHookMethod(
                    ModuleActivation::class.java.name,
                    lpparam.classLoader,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true)
                )
            } catch (e: Exception) {
                Log.e("XposedEntry", "selfCheckException: ${e.message}")
                hookSafeLog("XposedEntry", "selfCheckException", e)
            }
            return true
        }
        return false
    }
}
