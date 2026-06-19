package nota.android.crash;

import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.XposedHelpers;

public class CrashHandler {


    public static final String SOURCE_LOOPER = "looper";
    public static final String SOURCE_UNCAUGHT = "uncaught";

    public interface ExceptionHandler {

        void handlerException(Throwable throwable, String source);
    }

    private CrashHandler() {
    }

    private static ExceptionHandler sExceptionHandler;
    private static Thread.UncaughtExceptionHandler sUncaughtExceptionHandler;
    private static boolean sInstalled = false;


    public static synchronized void insert(ExceptionHandler exceptionHandler) {
        if (sInstalled) {
            return;
        }
        sInstalled = true;
        sExceptionHandler = exceptionHandler;

        final Looper targetLooper = (Looper) XposedHelpers.callStaticMethod(Looper.class, "getMainLooper");
        new Handler(targetLooper).post(() -> {
            //main 方法的退出就是Looper.loop执行完毕，所有事件都是Looper的监听，
            //当Crash的时候，说明App的线程已经无法loop了，这时候需要这段代码给APP续命
            while (true) {
                try {
                    XposedHelpers.callStaticMethod(Looper.class, "loop");
                } catch (Throwable e) {
                   /* if (e instanceof RuntimeException) {
                        return;
                    }*/
                    if (sExceptionHandler != null) {
                        sExceptionHandler.handlerException(e, SOURCE_LOOPER);
                    }
                }
            }
        });


        sUncaughtExceptionHandler = (Thread.UncaughtExceptionHandler) XposedHelpers.callStaticMethod(Thread.class, "getDefaultUncaughtExceptionHandler");
        //原来的不给他处理哦
        XposedHelpers.callStaticMethod(Thread.class, "setDefaultUncaughtExceptionHandler", (Thread.UncaughtExceptionHandler) (t, e) -> {
            if (sExceptionHandler != null) {
                sExceptionHandler.handlerException(e, SOURCE_UNCAUGHT);
            }
        });

    }


}
