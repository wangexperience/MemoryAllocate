package com.wxp.memoryallocate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class FloatingWindowService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_CHANNEL_ID = "com.wxp.memoryallocate";
    private static final String NOTIFICATION_CHANNEL_NAME = "Memory Floating Window";

    static View floatingWindow = null;
    static TextView textView = null;

    int displayWidth = 0;
    int displayHeight = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        addWindow();
        addNotification();
    }

    private void addNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);
            builder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        Notification notification = builder.setContentTitle(NOTIFICATION_CHANNEL_NAME)
                .setContentText("memory floating window is showing")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeWindow();
    }

    private void addWindow() {
        if (floatingWindow != null) {
            return;
        }
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.format = PixelFormat.TRANSPARENT;
        layoutParams.width = dp2pixel(200);
        layoutParams.height = dp2pixel(300);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            displayWidth = windowManager.getCurrentWindowMetrics().getBounds().width();
//            displayHeight = windowManager.getCurrentWindowMetrics().getBounds().height();
            displayWidth = getResources().getDisplayMetrics().widthPixels;
            displayHeight = getResources().getDisplayMetrics().heightPixels;
        } else {
            displayWidth = windowManager.getDefaultDisplay().getWidth();
            displayHeight = windowManager.getDefaultDisplay().getHeight();
        }
        Log.d("wxp_state", "displayWidth = " + displayWidth + ", displayHeight = " + displayHeight);
        layoutParams.x = 0;
        layoutParams.y = displayHeight / 2 - layoutParams.height / 2;
        floatingWindow = LayoutInflater.from(this).inflate(R.layout.floating_window, null);
        floatingWindow.setOnTouchListener(new View.OnTouchListener() {
            private float downX;
            private float downY;
            private float moveX;
            private float moveY;
            int startX = layoutParams.x;
            int startY = layoutParams.y;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        moveX = event.getRawX();
                        moveY = event.getRawY();
                        layoutParams.x = (int) (moveX - downX + startX);
                        layoutParams.y = (int) (moveY - downY + startY);
                        windowManager.updateViewLayout(v, layoutParams);
                        break;
                    case MotionEvent.ACTION_UP:
                        startX = layoutParams.x >= 0 ? (layoutParams.x <= (displayWidth - layoutParams.width) ? layoutParams.x : (displayWidth - layoutParams.width)) : 0;
                        startY = layoutParams.y >= 0 ? (layoutParams.y <= (displayHeight - layoutParams.height) ? layoutParams.y : (displayHeight - layoutParams.height)) : 0;
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        windowManager.addView(floatingWindow, layoutParams);
        textView = floatingWindow.findViewById(R.id.floating_window_text);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (floatingWindow != null) {
                    floatingWindow.post(new Runnable() {
                        @Override
                        public void run() {
                            if (textView != null) {
                                String systemMemory = MemoryUtils.statisticsSystemMemory();
                                textView.setText(systemMemory);
                            }
                        }
                    });
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    private void removeWindow() {
        if (floatingWindow != null) {
            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            windowManager.removeView(floatingWindow);
            floatingWindow = null;
            textView = null;
        }
    }

    private int dp2pixel(int dp) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        Log.d("wxp_state", "density = " + density);
        return Math.round(dp * density);
    }
}