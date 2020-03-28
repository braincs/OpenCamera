package com.braincs.attrsc.opencamera.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * Creater: brain
 * Created Date: 2016/11/10 20:21
 * Description: 获取手机屏幕相关参数
 */
public class ScreenUtils {

    /**
     * 获取当前手机屏幕的相关信息
     *
     * @return
     */
    public static DisplayMetrics getScreenParams(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics;
    }

}
