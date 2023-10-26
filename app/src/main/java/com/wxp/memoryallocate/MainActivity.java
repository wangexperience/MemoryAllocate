package com.wxp.memoryallocate;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.util.SparseLongArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.wxp.memoryallocate.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Used to load the 'memoryallocate' library on application startup.
    static {
        System.loadLibrary("memoryallocate");
    }

    private ActivityMainBinding binding;
    private TextView memory_info;

    Button showMemoryInfo = null;
    Button btnStartAllocMemory = null;
    Button btnStopAllocMemory = null;
    Button btnFreeAllocMemory = null;
    Button finishApp = null;
    Button addFloatingWindow = null;
    Button removeFloatingWindow = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        memory_info = binding.memoryInfo;

        showMemoryInfo = binding.showMemoryInfo;
        btnStartAllocMemory = binding.btnStartAllocMemory;
        btnStopAllocMemory = binding.btnStopAllocMemory;
        btnFreeAllocMemory = binding.btnFreeAllocMemory;
        finishApp = binding.finishApp;
        addFloatingWindow = binding.addFloatingWindow;
        removeFloatingWindow = binding.removeFloatingWindow;

        showMemoryInfo.setOnClickListener(this);
        btnStartAllocMemory.setOnClickListener(this);
        btnStopAllocMemory.setOnClickListener(this);
        btnFreeAllocMemory.setOnClickListener(this);
        finishApp.setOnClickListener(this);
        addFloatingWindow.setOnClickListener(this);
        removeFloatingWindow.setOnClickListener(this);

        if (mAllocateState) {
            btnStartAllocMemory.setEnabled(false);
        }
    }

    /**
     * A native method that is implemented by the 'memoryallocate' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    private boolean isFinished = false;

    public native long allocMemoryFromJNI();

    public native void freeMemoryFromJNI(long ptr);

    private long ptr = 0;

    private static int i = 0;

    private static boolean mAllocateState = false;

    private static final SparseLongArray POINTER_ARRAY = new SparseLongArray();

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.show_memory_info) {
            showMemoryInfo();
        } else if (id == R.id.btn_start_alloc_memory) {
            startAllocMemory();
        } else if (id == R.id.btn_stop_alloc_memory) {
            stopAllocMemory();
        }  else if (id == R.id.btn_free_alloc_memory) {
            freeAllocMemory();
        } else if (id == R.id.finish_app) {
            finishApp();
        } else if (id == R.id.add_floating_window) {
            FloatingWindowManager.getInstance().addFloatingWindow(MainActivity.this);
        } else if (id == R.id.remove_floating_window) {
            FloatingWindowManager.getInstance().removeFloatingWindow(MainActivity.this);
        }
    }

    private void finishApp() {
        isFinished = true;
        stopAllocMemory();
        finish();
    }

    private void showMemoryInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isFinished) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    String appInfo = MemoryUtils.printAppInfo(MainActivity.this);
                    String statisticsMemory = MemoryUtils.statisticsMemory();

                    StringBuffer stringBuffer = new StringBuffer();
                    stringBuffer.append(appInfo).append(statisticsMemory);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            memory_info.setText(stringBuffer.toString());
                        }
                    });
                }
            }
        }).start();
    }

    private void startAllocMemory() {
        mAllocateState = true;
        btnStartAllocMemory.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    ptr = allocMemoryFromJNI();
                    POINTER_ARRAY.put(i, ptr);
                    Log.e("wxp_state", "i = " + i + ", ptr = " + ptr);
                    i++;
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (!mAllocateState) {
                        break;
                    }
                }
            }
        }).start();
    }

    private void stopAllocMemory() {
        mAllocateState = false;
        btnStartAllocMemory.setEnabled(true);
    }

    private void freeAllocMemory() {
        Log.d("wxp_state", "size = " + POINTER_ARRAY.size());
        for (int i = 0; i < POINTER_ARRAY.size(); i++) {
            long ptr = POINTER_ARRAY.get(i);
            Log.d("wxp_state", "i = " + i + " , value = " + ptr);
            freeMemoryFromJNI(ptr);
        }
        i = 0;
        POINTER_ARRAY.clear();
    }
}