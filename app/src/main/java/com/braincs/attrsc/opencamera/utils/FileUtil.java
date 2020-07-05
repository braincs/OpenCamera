package com.braincs.attrsc.opencamera.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by Shuai
 * 2020-07-05.
 */
public class FileUtil {
    public static File getExternalFolder(Context context, String... names){
        if (null == names || names.length < 1) return null;
        if (null == context) return null;

        //sdcard
        File parent = Environment.getExternalStorageDirectory();
        if (parent == null) return null;
        String path = parent.getAbsolutePath();

        StringBuilder builder = new StringBuilder(path);
        for (String name : names) {
            builder.append(File.separator).append(name);
        }
        File file = new File(builder.toString());
        if (!file.exists()){
            file.mkdirs();
        }
        return file;
    }
}
