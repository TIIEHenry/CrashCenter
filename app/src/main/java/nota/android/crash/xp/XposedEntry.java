package nota.android.crash.xp;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import nota.android.crash.CrashHandler;
import nota.android.crash.xp.app.ActivityMain;
import nota.android.crash.xp.app.R;


public class XposedEntry implements IXposedHookLoadPackage {
    private static final String PACKAGE_NAME = PrefManager.PACKAGE_NAME;
    private static final String CRASH_INFO_CLASS = "nota.android.crash.ActivityCrashInfo";
    private static XSharedPreferences sXSharedPreferences = null;
    private static boolean showNotify = false;

    private boolean shouldHandlePackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (selfCheck(lpparam)) {
            return true;
        }

        if (sXSharedPreferences == null) {
            sXSharedPreferences = new XSharedPreferences(PACKAGE_NAME, PrefManager.PREF_NAME);
        }
        sXSharedPreferences.reload();
        boolean scopeMode = sXSharedPreferences.getBoolean(PrefManager.PREF_SCOPE_MODE, false);
        Set<String> packagesDisabled = sXSharedPreferences.getStringSet(PrefManager.PREF_PACKAGE_LIST, new HashSet<>());
        boolean disabled = packagesDisabled.contains(lpparam.packageName);
        boolean ignore = lpparam.packageName.equals("android");
        boolean isXposedInstaller = lpparam.packageName.equals("de.robv.android.xposed.installer") || lpparam.packageName.equals("org.meowcat.edxposed.manager")|| lpparam.packageName.equals("org.lsposed.manager");
        if (lpparam.appInfo == null) {
            return false;
        }
        boolean isSystemApp = (lpparam.appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        boolean handleSystemApp = sXSharedPreferences.getBoolean(PrefManager.PREF_HANDLE_SYSTEM, false);
        if (scopeMode) {
            showNotify = true;
            return (!isSystemApp || handleSystemApp) && !(ignore ||isXposedInstaller ||disabled);
        } else {
            showNotify = !disabled;
            return true;
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (selfCheck(lpparam)) {
            showNotify = true;
        } else {
            if (!shouldHandlePackage(lpparam))
                return;
        }

        XposedBridge.log("catch package: " + lpparam.packageName);
        XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                XposedBridge.log("onCreate");
                hookToGrabCrash(lpparam, param);
            }
        });


    }

    private void hookToGrabCrash(XC_LoadPackage.LoadPackageParam lpparam, XC_MethodHook.MethodHookParam param) {
        Application currentApplicationContext = (Application) param.thisObject;
        CrashHandler.insert(throwable -> new Handler(currentApplicationContext.getMainLooper()).post(() -> {
            try {
                XposedBridge.log(throwable);
                if (showNotify) {
                    Toast.makeText(currentApplicationContext, getCrashTip(lpparam.packageName) + lpparam.appInfo.loadLabel(currentApplicationContext.getPackageManager()) + " " + throwable.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    showNotification(lpparam.packageName, currentApplicationContext, currentApplicationContext, lpparam.appInfo, throwable);
                }
            } catch (Throwable e) {
                System.exit(0);
                //                e.printStackTrace();
                XposedBridge.log(e.toString());
            }
        }));
    }

    private void showNotification(String pkgName, Context c, Application application, ApplicationInfo applicationInfo, Throwable throwable) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        ComponentName comp = new ComponentName(PACKAGE_NAME, CRASH_INFO_CLASS);
        intent.setComponent(comp);

        int launchFlags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED;

        intent.setFlags(launchFlags);

//        Writer writer = new StringWriter();
//        PrintWriter pw = new PrintWriter(writer);
//        throwable.printStackTrace(pw);
//        pw.append("\r\n");
//        Throwable cause = throwable.getCause();
//        // 循环着把所有的异常信息写入writer中
//        while (cause != null) {
//            cause.printStackTrace(pw);
//            pw.append("\r\n");
//            cause = cause.getCause();
//        }
//        pw.close();// 记得关闭
//        intent.putExtra("Exception", writer.toString().replaceAll("\\sat\\s", "\r\nat "));
        intent.putExtra("Exception", Log.getStackTraceString(throwable));
        try {

            String appName = applicationInfo.loadLabel(application.getPackageManager()).toString();
            StringBuilder stackTrace = new StringBuilder();
            Throwable p = throwable.getCause() != null ? throwable.getCause() : throwable;
            for (StackTraceElement stackTraceElement : p.getStackTrace()) {
                stackTrace.append("  at ")
                        .append(stackTraceElement)
                        .append("\r\n");
            }
            NotificationManager notificationManager = (NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE);
//            Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(applicationInfo.packageName, Context.CONTEXT_IGNORE_SECURITY);
            Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(pkgName, Context.CONTEXT_IGNORE_SECURITY);
            int flag = PendingIntent.FLAG_CANCEL_CURRENT;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                flag = flag | PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent contentIntent = PendingIntent.getActivity(c, 0, intent, flag);
            boolean targetO = applicationInfo.targetSdkVersion >= Build.VERSION_CODES.O;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && targetO) {
                NotificationChannel notificationChannel = new NotificationChannel("catch_exception", "catch exception[Xposed]", NotificationManager.IMPORTANCE_LOW);
                notificationManager.createNotificationChannel(notificationChannel);
                Notification notification = new Notification.Builder(application, "catch_exception")
                        .setContentTitle(moduleContext.getString(R.string.crash_tip) + " - " + appName)
                        .setContentText(throwable.getLocalizedMessage())
                        .setStyle(new Notification.BigTextStyle()
                                .bigText(throwable.getLocalizedMessage() + "\n-----\n" +
                                        stackTrace.toString()))
                        .setSmallIcon(applicationInfo.icon)
                        .setContentIntent(contentIntent)
                        .build();
                notificationManager.notify(new Random().nextInt(), notification);
            } else {
                Notification notification = new Notification.Builder(application)
                        .setContentTitle(moduleContext.getString(R.string.crash_tip) + " - " + appName)
                        .setContentText(throwable.getLocalizedMessage())
                        .setStyle(new Notification.BigTextStyle()
                                .bigText(throwable.getLocalizedMessage() + "\n-----\n" +
                                        stackTrace.toString()))
                        .setSmallIcon(applicationInfo.icon)
                        .setContentIntent(contentIntent)
                        .build();
                notificationManager.notify(new Random().nextInt(), notification);
            }
        } catch (Exception e) {
//            e.printStackTrace();
            XposedBridge.log(e);
        }
    }

    private static String getCrashTip(String packageName) {
        Context context;
        try {
            context = AndroidAppHelper.currentApplication().createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
            return context.getString(R.string.crash_tip) + ": ";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    private boolean selfCheck(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals(PACKAGE_NAME)) {
            Log.e("XposedEntry", "selfCheck:pkg: " + lpparam.packageName);
            try {
                //不能hook静态了
                XposedHelpers.findAndHookMethod(ActivityMain.class.getName(), lpparam.classLoader, "isModuleActived", XC_MethodReplacement.returnConstant(true));
            } catch (Exception e) {
                Log.e("XposedEntry", "selfCheckException: " + e.getMessage());
//                e.printStackTrace();
                XposedBridge.log(e);
            }
            return true;
        }
        return false;
    }
}
