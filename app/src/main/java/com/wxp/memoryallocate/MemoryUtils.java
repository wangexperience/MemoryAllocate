package com.wxp.memoryallocate;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Build;
import android.os.Debug;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MemoryUtils {

    private static String TAG = "wxp_memory";

    public static long unit_MB = 1 * 1024 * 1024;

    public static String printAppMemoryInfo() {
        // 单位是字节 byte
        long totalMemory = Runtime.getRuntime().totalMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();

        long totalMemoryMB = totalMemory / unit_MB;
        long maxMemoryMB = maxMemory / unit_MB;
        long freeMemoryMB = freeMemory / unit_MB;

        StringBuilder stringBuilder = new StringBuilder("当前进程的内存信息:\n");
        stringBuilder.append("totalMemory : ").append(totalMemoryMB).append("MB\n")
                .append("maxMemory : ").append(maxMemoryMB).append("MB\n")
                .append("freeMemory : ").append(freeMemoryMB).append("MB\n");
        return stringBuilder.toString();
    }

    public static String printDeviceMemoryInfo(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo memoryInfo = new MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        long totalMem = memoryInfo.totalMem / unit_MB;
        long availMem = memoryInfo.availMem / unit_MB;
        long threshold = memoryInfo.threshold / unit_MB;
        boolean lowMemory = memoryInfo.lowMemory;

        StringBuilder stringBuilder = new StringBuilder("设备内存信息:\n");
        stringBuilder.append("availMem : ").append(availMem).append("MB\n")
                .append("totalMem : ").append(totalMem).append("MB\n")
                .append("threshold : ").append(threshold).append("MB\n")
                .append("lowMemory : ").append(lowMemory).append("\n");
        return stringBuilder.toString();
    }

    public static String printAppInfo(Context context) {
        int pid = -1;
        int uid = -1;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        for (RunningAppProcessInfo processInfo : runningAppProcesses) {
            if (processInfo.processName.equals(context.getPackageName())) {
                pid = processInfo.pid;
                uid = processInfo.uid;
            }
        }
        Debug.MemoryInfo[] processMemoryInfo = activityManager.getProcessMemoryInfo(new int[]{pid});
        for (Debug.MemoryInfo memoryInfo : processMemoryInfo) {
            Map<String, String> memoryStats = memoryInfo.getMemoryStats();
            for (String key : memoryStats.keySet()) {
//                Log.d(TAG, "当前进程内存信息：" + key + ", value:"+ memoryStats.get(key));
            }
        }

        StringBuilder stringBuilder = new StringBuilder("应用信息:\n");
        stringBuilder.append("packageName : ").append(context.getPackageName()).append("\n")
                .append("pid : ").append(pid).append("\n")
                .append("uid : ").append(uid).append("\n\n");
        return stringBuilder.toString();
    }

    /**
     * 获取系统属性
     * 注意：sys.lmk.minfree_levels 无法通过反射获取
     * // TODO 尝试通过代码执行shell命令获取
     * @param prop
     * @return
     */
    public static String getSystemProperties(String prop) {
        try {
            Class<?> aClass = Class.forName("android.os.SystemProperties");
            Method get = aClass.getMethod("get", String.class, String.class);
            get.setAccessible(true);
            String result = (String) get.invoke(null, prop, "null");
            StringBuilder stringBuilder = new StringBuilder(prop);
            stringBuilder.append(" : ").append(result).append("\n");
            return stringBuilder.toString();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static String execShell(String command) {
        OutputStream outputStream = null;
        DataOutputStream dataOutputStream = null;
        InputStream inputStream = null;
        DataInputStream dataInputStream = null;
        try {
//            Process process = new ProcessBuilder()
//                    .command("/system/xbin/su")
//                    .redirectErrorStream(true).start();

            Process process = Runtime.getRuntime().exec("su");

            outputStream = process.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.writeBytes(command + "\n");
            dataOutputStream.flush();

            inputStream = process.getInputStream();
            dataInputStream = new DataInputStream(inputStream);
            String result = "";
            while ((result = dataInputStream.readLine()) != null) {
                Log.e("wxp_state", "read info : " + result);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(5, TimeUnit.SECONDS);
            }
            int i = process.exitValue();
        } catch (IOException e) {
            System.out.println(e);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static void getThreadSmaps(Context context) {
        int pid = -1;

        String path = context.getFilesDir().getPath() + "/smaps_all.txt";
        Log.d("wxp_state", "文件输出至：" + path);
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        boolean newFile = false;
        try {
            newFile = file.createNewFile();
        } catch (IOException e) {
            System.out.println(e);
        }
        Log.d("wxp_state", "文件创建文件结果：" + newFile);

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        for (RunningAppProcessInfo processInfo : runningAppProcesses) {
            if (!TextUtils.isEmpty(processInfo.processName)) {
                pid = processInfo.pid;

                InputStream inputStream = null;
                BufferedReader bufferedReader = null;
                Process process = null;
                FileWriter fileWriter = null;
                BufferedWriter bufferedWriter = null;

//                String command = "cat /proc/" + pid + "/smaps_rollup | grep -E 'Rss|Pss|Clean|Dirty' ";
                String command = "cat /proc/" + pid + "/smaps_rollup | grep 'Rss'";
                String[] commands = {"/bin/sh", "-c", command};
                try {
                    process = Runtime.getRuntime().exec(commands);
                } catch (IOException e) {
                    System.out.println(e);
                }

                inputStream = process.getInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                final String newline = System.lineSeparator();
                try {

                    fileWriter = new FileWriter(file,true);
                    bufferedWriter = new BufferedWriter(fileWriter);
                    fileWriter.write(processInfo.processName + "  ");
                    String line = bufferedReader.readLine();
                    if (TextUtils.isEmpty(line)) {
                        bufferedWriter.append(newline);
                    } else {
                        while (line != null) {
                            try {
                                bufferedWriter.append(line + newline);
                                Log.e("wxp_state", "read info : " + processInfo.processName + ":" + line);
                                line = bufferedReader.readLine();
                            } catch (IOException e) {
                                System.out.println(e);
                            }
                        }
                    }
                    bufferedWriter.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (bufferedReader != null) {
                            bufferedReader.close();
                        }
                        if (fileWriter != null) {
                            fileWriter.close();
                        }
                        if (bufferedWriter == null) {
                            bufferedWriter.close();
                        }
                    } catch (IOException e) {
                        System.out.println(e);
                    }
                }
            }
        }
    }

    /**
     * 统计内存使用情况
     */
    public static String statisticsMemory() {
        String systemMemory = statisticsSystemMemory();
        String jvmMemory = statisticsJVMMemory();
        String nativeMemory = statisticsNativeMemory();
        String processMemory = statisticsProcessMemory();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(systemMemory).append(jvmMemory)
                .append(nativeMemory).append(processMemory);

        return stringBuilder.toString();
    }

    public static String statisticsJVMMemory() {
        Runtime runtime = Runtime.getRuntime();
        // 虚拟机将尝试使用的最大内存量
        long maxMemory = runtime.maxMemory();
        // Java 虚拟机中的内存总量
        long totalMemory = runtime.totalMemory();
        // 当前可用于将来分配的对象的内存总量的近似值
        long freeMemory = runtime.freeMemory();

        long maxMemoryMB = maxMemory / unit_MB;
        long totalMemoryMB = totalMemory / unit_MB;
        long freeMemoryMB = freeMemory / unit_MB;
        long usedMemoryMB = totalMemoryMB - freeMemoryMB;
        float usedMemoryRate = ((float)usedMemoryMB / maxMemoryMB);

        StringBuilder stringBuilder = new StringBuilder("当前JVM的内存信息：\n");
        stringBuilder.append("maxMemory : ").append(maxMemoryMB).append(" MB\n")
                .append("totalMemory : ").append(totalMemoryMB).append(" MB\n")
                .append("freeMemory : ").append(freeMemoryMB).append(" MB\n")
                .append("usedMemory : ").append(usedMemoryMB).append(" MB\n")
                .append("usedMemoryRate : ").append(getTwoDecimalPercentage(usedMemoryRate)).append("\n\n");

        return stringBuilder.toString();
    }

    public static String statisticsNativeMemory() {
        // native堆内存大小
        long nativeHeapSize = Debug.getNativeHeapSize();
        // native已分配的堆内存大小
        long nativeHeapAllocatedSize = Debug.getNativeHeapAllocatedSize();
        // native空闲的堆内存大小
        long nativeHeapFreeSize = Debug.getNativeHeapFreeSize();

        long totalNativeMB = nativeHeapSize / unit_MB;
        long freeNativeMB = nativeHeapFreeSize / unit_MB;
        long allocatedNativeMB = nativeHeapAllocatedSize / unit_MB;
        float allocatedNativeRate = ((float)allocatedNativeMB / totalNativeMB);

        StringBuilder stringBuilder = new StringBuilder("当前Native的内存信息：\n");
        stringBuilder.append("totalNative : ").append(totalNativeMB).append(" MB\n")
                .append("freeNative : ").append(freeNativeMB).append(" MB\n")
                .append("usedNative : ").append(allocatedNativeMB).append(" MB\n")
                .append("usedNativeRate : ").append(getTwoDecimalPercentage(allocatedNativeRate)).append("\n\n");

        return stringBuilder.toString();
    }

    public static String statisticsProcessMemory() {
        String CMD_APP_STATUS = "/proc/self/status";

        StringBuilder stringBuilder = new StringBuilder("当前进程的内存信息：\n");

        File file = new File(CMD_APP_STATUS);
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("VmSize") || line.startsWith("VmRSS")) {
                    String[] array = line.split("\\t");
                    String name = array[0].split(":")[0];
                    String value = array[1].replace("kB","").trim();
                    stringBuilder.append(name).append(" : ").append(Integer.valueOf(value) / 1024).append(" MB\n");
                }
            }
            stringBuilder.append("\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            close(fileReader);
            close(bufferedReader);
        }
        return stringBuilder.toString();
    }

    public static String statisticsSystemMemory() {
        String CMD_SYSTEM_MEMINFO = "/proc/meminfo";

        StringBuilder titleString = new StringBuilder("当前系统的内存信息：\n");
        StringBuilder availableMemoryRateString = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();

        File file = new File(CMD_SYSTEM_MEMINFO);
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            String total = null;
            String available = null;
            String free = null;
            String buffers = null;
            String cached = null;
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("MemTotal") || line.startsWith("MemFree")
                    || line.startsWith("MemAvailable") || line.startsWith("Buffers")
                    || line.startsWith("Cached")|| line.startsWith("SwapCached")
                    || line.startsWith("Active:")|| line.startsWith("Inactive:")
                    || line.startsWith("Active(anon)")|| line.startsWith("Inactive(anon)")
                    || line.startsWith("Active(file)")|| line.startsWith("Inactive(file)")) {
                    String[] array = line.split(":");
                    String name = array[0];
                    String value = array[1].replace("kB","").trim();
                    stringBuilder.append(name).append(" : ").append(Integer.valueOf(value) / 1024).append(" MB\n");

                    if (name.startsWith("MemTotal")) {
                        total = value;
                    } else if (name.startsWith("MemAvailable")) {
                        available = value;
                    } else if (total != null && available == null) { // 低版本手机可能没有MemAvailable，需单独处理
                        if (name.startsWith("MemFree")) {
                            free = value;
                        } else if (name.startsWith("Buffers")) {
                            buffers = value;
                        } else if (name.startsWith("Cached")) {
                            cached = value;
                        }
                        if (!TextUtils.isEmpty(free) && !TextUtils.isEmpty(buffers) && !TextUtils.isEmpty(cached)) {
                            available = String.valueOf(Integer.valueOf(free) + Integer.valueOf(buffers) + Integer.valueOf(cached));
                        }
                    }

                    if (total != null && available != null) {
                        float systemFreeRate = Float.valueOf(available) / Float.valueOf(total);
                        availableMemoryRateString.append("systemAvailableMemoryRate").append(" : ").append(getTwoDecimalPercentage(systemFreeRate)).append("\n");
                        total = null;
                        available = null;
                    }

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            close(fileReader);
            close(bufferedReader);
        }

        titleString.append(availableMemoryRateString).append(stringBuilder).append("\n");

        return titleString.toString();
    }

    private static String getTwoDecimal(float f) {
        String format = String.format("%.2f", f);
        return format;
    }

    private static String getTwoDecimalPercentage(float f) {
        f = f * 100;
        String format = getTwoDecimal(f) + "%";
        return format;
    }

    private static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}