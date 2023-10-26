package com.wxp.memoryallocate;

import android.util.Log;

public class Test {

    // These are the various interesting memory levels that we will give to
    // the OOM killer.  Note that the OOM killer only supports 6 slots, so we
    // can't give it a different value for every possible kind of process.
    private static final int[] mOomAdj = new int[]{
            0, 100, 200,
            250, 900, 950
    };

    // These are the low-end OOM level limits.  This is appropriate for an
    // HVGA or smaller phone with less than 512MB.  Values are in KB.
    private final int[] mOomMinFreeLow = new int[]{
            12288, 18432, 24576,
            36864, 43008, 49152
    };

    // These are the high-end OOM level limits.  This is appropriate for a
    // 1280x800 or larger screen with around 1GB RAM.  Values are in KB.
    private final int[] mOomMinFreeHigh = new int[]{
            73728, 92160, 110592,
            129024, 147456, 184320
    };

    // The actual OOM killer memory levels we are using.
    private static final int[] mOomMinFree = new int[]{18432, 23040, 27648, 32256, 55296, 80640};

    public static void adjustMinFreeTest() {

        // -1
        int minfree_abs = 100000;
        // 0
        int minfree_adj = 1000000;

        if (minfree_abs >= 0) {
            for (int i = 0; i < mOomAdj.length; i++) {
                mOomMinFree[i] = (int) ((float) minfree_abs * mOomMinFree[i]
                        / mOomMinFree[mOomAdj.length - 1]);
                Log.d("wxp_state", mOomMinFree[i] + " : " + mOomAdj[i]);
            }
        }

        if (minfree_adj != 0) {
            for (int i = 0; i < mOomAdj.length; i++) {
                mOomMinFree[i] += (int) ((float) minfree_adj * mOomMinFree[i]
                        / mOomMinFree[mOomAdj.length - 1]);
                if (mOomMinFree[i] < 0) {
                    mOomMinFree[i] = 0;
                }
                Log.d("wxp_state", mOomMinFree[i] + " : " + mOomAdj[i]);
            }
        }
    }
}
