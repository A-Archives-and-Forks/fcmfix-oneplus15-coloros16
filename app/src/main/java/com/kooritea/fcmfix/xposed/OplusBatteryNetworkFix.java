package com.kooritea.fcmfix.xposed;

import android.content.Intent;
import android.content.pm.PackageManager;

import com.kooritea.fcmfix.libxposed.XC_MethodHook;
import com.kooritea.fcmfix.libxposed.XposedBridge;
import com.kooritea.fcmfix.libxposed.XposedHelpers;

import java.lang.reflect.Method;

/**
 * ColorOS Battery's GoogleRestrictionController applies POLICY_REJECT_ALL when its
 * Google connectivity probe fails. This intercepts every matching Google UID reject-all
 * write inside com.oplus.battery, not exclusively that controller's call site.
 * Calls made directly by Settings/TrafficMonitor are outside this hook's process.
 */
public class OplusBatteryNetworkFix extends XposedModule {

    private static final String NETWORK_CONTROL_MANAGER =
            "android.net.OplusNetworkingControlManager";
    private static final String GOOGLE_RESTRICT_CHANGE = "oplus.intent.action.google_restrict_change";
    private static final String EXTRA_RESTRICT_ENABLE = "restrict_enable";
    private static final int POLICY_REJECT_ALL = 4;
    private static final int POLICY_NONE = 0;
    private static final String[] GOOGLE_NETWORK_PACKAGES = new String[]{
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending",
            "com.google.android.configupdater"
    };

    public OplusBatteryNetworkFix(ClassLoader classLoader) {
        super(classLoader);
        try {
            startHookGoogleNetworkPolicy();
        } catch (Throwable e) {
            printLog("hook error Oplus Battery GMS network policy: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        try {
            startHookGoogleRestrictBroadcast();
        } catch (Throwable e) {
            printLog("hook error Oplus Battery Google restrict broadcast: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * The same failed Google probe also broadcasts google_restrict_change. On ColorOS 17
     * system_server then downgrades GMS wakeup alarms (OplusGoogleAlarmRestrict) and puts
     * Google packages in the RARE standby bucket, so the FCM heartbeat/reconnect stops in
     * Doze. Clear only the restrict_enable=true flag; list updates pass through unchanged.
     */
    private void startHookGoogleRestrictBroadcast() {
        Class<?> contextImpl = XposedHelpers.findClass("android.app.ContextImpl", classLoader);
        int hooks = 0;
        for (Method method : contextImpl.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (!method.getName().startsWith("sendBroadcast")
                    || parameters.length == 0 || parameters[0] != Intent.class) {
                continue;
            }
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Intent intent = (Intent) param.args[0];
                    if (intent == null || !GOOGLE_RESTRICT_CHANGE.equals(intent.getAction())
                            || !intent.getBooleanExtra(EXTRA_RESTRICT_ENABLE, false)) {
                        return;
                    }
                    intent.putExtra(EXTRA_RESTRICT_ENABLE, false);
                    printLog("Oplus Battery Google restrict broadcast cleared", true);
                }
            });
            hooks++;
        }
        if (hooks == 0) throw new NoSuchMethodError("ContextImpl#sendBroadcast(Intent...)");
        printLog("Oplus Battery Google restrict broadcast hooks active: " + hooks);
    }

    private void startHookGoogleNetworkPolicy() {
        Class<?> managerClass = XposedHelpers.findClassIfExists(
                NETWORK_CONTROL_MANAGER, classLoader);
        if (managerClass == null) {
            throw new NoClassDefFoundError(NETWORK_CONTROL_MANAGER);
        }

        int hooks = 0;
        for (Method method : managerClass.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (!"setUidPolicy".equals(method.getName())
                    || parameters.length != 2
                    || parameters[0] != int.class
                    || parameters[1] != int.class) {
                continue;
            }

            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int uid = (Integer) param.args[0];
                    int policy = (Integer) param.args[1];
                    if (policy != POLICY_REJECT_ALL || !isGoogleNetworkUid(uid)) {
                        return;
                    }

                    // Let the original method clear any stale reject-all state in netd.
                    param.args[1] = POLICY_NONE;
                    printLog("Oplus Battery GMS network reject bypass: uid=" + uid, true);
                }
            });
            hooks++;
            printLog("Oplus Battery network hook active: " + method);
        }

        if (hooks == 0) {
            throw new NoSuchMethodError(NETWORK_CONTROL_MANAGER + "#setUidPolicy(int,int)");
        }
    }

    private boolean isGoogleNetworkUid(int uid) {
        if (context == null) {
            printLog("Oplus Battery network hook skipped before context initialization");
            return false;
        }

        PackageManager packageManager = context.getPackageManager();
        for (String packageName : GOOGLE_NETWORK_PACKAGES) {
            try {
                if (packageManager.getPackageUid(packageName, 0) == uid) {
                    return true;
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return false;
    }
}
