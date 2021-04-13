package com.braincs.attrsc.opencamera.utils;

/**
 * Created by Shuai
 * 27/03/2020.
 */
public class Constants {

    public static final String INTENT_KEY_CAMERA_FRONT = "INTENT_KEY_CAMERA_FRONT";
    public static final String INTENT_KEY_RESULT = "INTENT_KEY_RESULT";

    public static final int REQUEST_CODE_AUTH = 9898;
    public static final int REQUEST_CODE = 9876;

    public static int CameraIDBack = 0;
    public static int CameraIDFront = 1;
    public final static int FRAME_IMAGE_WIDTH = 640;
    public final static int FRAME_IMAGE_HEIGHT = 480;
    public final static float FRAME_HEIGHT_WIDTH_RATIO = 1.0f * FRAME_IMAGE_WIDTH / FRAME_IMAGE_HEIGHT;


    public final static int ORIENTATION_0 = 0;
    public final static int ORIENTATION_90 = 90;
    public final static int ORIENTATION_180 = 180;
    public final static int ORIENTATION_270 = 270;


    public static int CAMERA_OFFSET_WIDTH = 50;
    public static int CAMERA_OFFSET_HEIGHT = 0;



    public static void swapCamera(){
        int tmp = CameraIDFront;
        CameraIDFront = CameraIDBack;
        CameraIDBack = tmp;
    }
}
