package com.braincs.attrsc.opencamera.utils;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Shuai
 * 10/07/2019.
 */
public class SaveFrameTask implements Runnable {
    private static final String TAG = SaveFrameTask.class.getSimpleName();
    private byte[] bytes;
    private int width = 640;
    private int height = 480;
    private File file;
    private File jpgfile;
    private File facefile;
    private final Rect faceRect;

    public SaveFrameTask(byte[] bytes, File file) {
        this.bytes = bytes;
        this.file = file;
        this.jpgfile = null;
        this.facefile = null;
        this.faceRect = null;
    }

    public SaveFrameTask(byte[] bytes, File file, File jpgfile) {
        this.file = file;
        this.jpgfile = jpgfile;
        this.bytes = bytes;
        this.facefile = null;
        this.faceRect = null;
    }
    public SaveFrameTask(byte[] bytes, File file, File jpgfile, File facefile, Rect faceRect) {
        this.file = file;
        this.jpgfile = jpgfile;
        this.bytes = bytes;
        this.facefile = facefile;
        this.faceRect = faceRect;
    }

    public SaveFrameTask(byte[] bytes, int width, int height, File file) {
        this.bytes = bytes;
        this.width = width;
        this.height = height;
        this.file = file;
        this.jpgfile = null;
        this.facefile = null;
        this.faceRect = null;
    }

    public SaveFrameTask(byte[] bytes, int width, int height, File file, File jpgfile) {
        this.bytes = bytes;
        this.width = width;
        this.height = height;
        this.file = file;
        this.jpgfile = jpgfile;
        this.facefile = null;
        this.faceRect = null;
    }


    @Override
    public void run() {
        FileOutputStream fos = null, bitFos = null, ffos = null;

        try {
            if (file != null) {
                bitFos = new FileOutputStream(file);
                bitFos.write(bytes);
                bitFos.flush();
//            LogUtils.d(TAG, "save raw file success " + file.getAbsolutePath());
            }

            if (jpgfile != null) {
                fos = new FileOutputStream(jpgfile);
                YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, width, height, null);
                yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, fos);
                fos.flush();
//                LogUtils.d(TAG, "save jpg file success " + jpgfile.getAbsolutePath());
            }
            if (facefile != null && faceRect != null) {
                ffos = new FileOutputStream(facefile);
                YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, width, height, null);
                yuvImage.compressToJpeg(filter(faceRect), 100, ffos);
                ffos.flush();
//                LogUtils.d(TAG, "save face file success " + facefile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (ffos != null ){
                try {
                    ffos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (fos != null ){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (bitFos != null){
                try {
                    bitFos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Rect filter(Rect rect){
        if (rect != null){
            rect.left = Math.min(Math.max(rect.left, 0), width);
            rect.right = Math.min(Math.max(rect.right, 0), width);
            rect.top = Math.min(Math.max(rect.top, 0), height);
            rect.bottom = Math.min(Math.max(rect.bottom, 0), height);
        }
        return rect;
    }
}
