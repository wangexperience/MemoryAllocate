package com.wxp.memoryallocate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public class FloatingWindowManager {

    private static volatile FloatingWindowManager INSTANCE = null;

    private FloatingWindowManager() {}

    public static FloatingWindowManager getInstance() {
        if (INSTANCE == null) {
            synchronized (FloatingWindowManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FloatingWindowManager();
                }
            }
        }
        return INSTANCE;
    }

    public void addFloatingWindow(Context context) {
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            context.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName())));
        } else {
            Intent intent = new Intent();
            intent.setClass(context, FloatingWindowService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        }
    }

    public void removeFloatingWindow(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, FloatingWindowService.class);
        context.stopService(intent);
    }
}