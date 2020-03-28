package com.braincs.attrsc.opencamera.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import java.util.List;

/**
 * Creater: brain
 * Created Date: 2016/11/10 20:00
 * Description: 摄像头相关工具
 */
public class CameraUtils {
    private static final String TAG = CameraUtils.class.getSimpleName();
    public static final int FLASH_MODE_AUTO = 0;       // 闪光灯模式：自动
    public static final int FLASH_MODE_OPEN = 1;       // 闪光灯模式：打开
    public static final int FLASH_MODE_CLOSE = 2;      // 闪光灯模式：关闭
    private static int mFlashMode, mAEMode;
    private static String mFlashMode2;

    static {
        init();
    }

    private static void init() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            mAEMode = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
//            mFlashMode = CaptureRequest.FLASH_MODE_TORCH;
//        }
        mFlashMode2 = Camera.Parameters.FLASH_MODE_AUTO;
    }

    private static Camera mCamera;
    public static int mCameraRotation = 90;

    /**
     * 初始化Preview的Camera对象
     *
     * @param context
     */
    public static Camera initPreviewCamera(Context context, int cameraId) {
        Camera camera = Camera.open(cameraId);
        if (null == camera) {
            return null;
        }

        Camera.Parameters parameters = camera.getParameters();
//        int[] previewSizes = getSuitableSize(context, parameters.getSupportedPreviewSizes());
//        parameters.setPreviewSize(previewSizes[0], previewSizes[1]);                // 设置预览照片的大小
        parameters.setPreviewSize(Constants.FRAME_IMAGE_WIDTH, Constants.FRAME_IMAGE_HEIGHT);
        int[] pictureSizes = getSuitableSize(context, parameters.getSupportedPictureSizes());
        parameters.setPictureSize(pictureSizes[0], pictureSizes[1]);                // 设置照片的大小

        // 1, 设置旋转
        camera.setDisplayOrientation(mCameraRotation);

        // 2, 设置模式
        if (parameters.getSupportedFocusModes().contains("continuous-video")) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if (parameters.getSupportedFocusModes().contains("continuous-picture")) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        // 3, 设置preview数据类型
//        parameters.setPreviewFormat(ImageFormat.YV12);
        parameters.setPreviewFormat(ImageFormat.NV21);
        camera.setParameters(parameters);
        return camera;
    }

    /**
     * 初始化Preview的Camera对象
     *
     * @param context
     */
    public static Camera initPreviewCamera(Context context, int cameraId, int rotate) {
        Camera camera = null;
        try {
            camera = Camera.open(cameraId);
        }catch (Exception e) {
            e.printStackTrace();
        }
        if (null == camera) {
            return null;
        }

        Camera.Parameters parameters = null;
        try {
            parameters = camera.getParameters();
        }catch (RuntimeException e){
            Log.e(TAG, e.toString());
            return null;
        }
        if (parameters == null) return null;

//        int[] previewSizes = getSuitableSize(context, parameters.getSupportedPreviewSizes());
//        parameters.setPreviewSize(previewSizes[0], previewSizes[1]);
        parameters.setPreviewSize(Constants.FRAME_IMAGE_WIDTH, Constants.FRAME_IMAGE_HEIGHT);

        // 设置预览照片的大小
        int[] pictureSizes = getSuitableSize(context, parameters.getSupportedPictureSizes());
        parameters.setPictureSize(pictureSizes[0], pictureSizes[1]);                // 设置照片的大小

        // 1, 设置旋转
//        Camera.CameraInfo info = new Camera.CameraInfo();
//        Camera.getCameraInfo(cameraId,info);
//        rotate = (info.orientation + rotate)%360;
//        rotate = (360 - rotate) %360;
//        Log.d(TAG, "rotate : "+ rotate);
        camera.setDisplayOrientation(rotate);


        // 2, 设置模式 // not compatible with orbbec dabai
//        if (parameters.getSupportedFocusModes().contains("continuous-video")) {
//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//        } else if (parameters.getSupportedFocusModes().contains("continuous-picture")) {
//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//        }

        // 3, 设置preview数据类型
//        parameters.setPreviewFormat(ImageFormat.YV12);
        parameters.setPreviewFormat(ImageFormat.NV21);
        camera.setParameters(parameters);
        return camera;
    }
    /**
     * 初始化Carema的相关参数
     *
     * @param context
     * @param camera
     * @return
     */
    private static Camera.Parameters initDefaultCameraParameter(Context context, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        int[] previewSizes = getSuitableSize(context, parameters.getSupportedPreviewSizes());
        parameters.setPreviewSize(previewSizes[0], previewSizes[1]);                // 设置预览照片的大小
        int[] pictureSizes = getSuitableSize(context, parameters.getSupportedPictureSizes());
        parameters.setPictureSize(pictureSizes[0], pictureSizes[1]);                // 设置照片的大小
        return parameters;
    }

    /**
     * 根据手机屏幕，以及支持的预览分辨率，获取合适的高宽
     *
     * @param context
     * @param supportSizes
     * @return
     */
    public static int[] getSuitableSize(Context context, List<Camera.Size> supportSizes) {
        // 1, 获取手机屏幕高宽
        DisplayMetrics metrics = ScreenUtils.getScreenParams(context);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        // 2, 需要保证屏幕高宽比与图片、预览的高宽比一致，才不会变形
        int[] sizes = new int[2];
        int[] maxSizes = new int[2];
        double wishRate = (double) screenWidth / screenHeight;
        double maxRate = 0;
        for (int i = 0; i < supportSizes.size(); i++) {
            Camera.Size size = supportSizes.get(i);
            int supportWidth = size.width;
            int supportHeight = size.height;
            sizes[0] = supportWidth;
            sizes[1] = supportHeight;
            if (supportWidth == screenWidth && screenHeight == supportHeight) {
                return sizes;
            } else if (supportWidth == screenHeight && supportHeight == screenWidth) {
                return sizes;
            } else if (supportWidth * screenHeight == supportHeight * screenWidth && (double) screenHeight / supportHeight < 1.2) {
                maxSizes[0] = supportWidth;
                maxSizes[1] = supportHeight;
                maxRate = wishRate;
            } else if (supportWidth * screenWidth == supportHeight * screenHeight && (double) screenHeight / supportWidth < 1.2) {
                maxSizes[0] = supportWidth;
                maxSizes[1] = supportHeight;
                maxRate = wishRate;
            } else if (Math.abs(wishRate - (double) supportWidth / supportHeight) < Math.abs(wishRate - maxRate)) {
                maxSizes[0] = supportWidth;
                maxSizes[1] = supportHeight;
                maxRate = (double) supportWidth / supportHeight;
            } else if (Math.abs(wishRate - (double) supportHeight / supportWidth) < Math.abs(wishRate - maxRate)) {
                maxSizes[0] = supportWidth;
                maxSizes[1] = supportHeight;
                maxRate = (double) supportHeight / supportWidth;
            }
        }
        return maxSizes;
    }


    /* ** ** ** ** ** ————————— Camera2 API start ————————— ** ** ** ** ** */


    private static final String CAMERA_ID = "0";                                // 默认使用后置摄像头
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();    // 图片旋转角度对应值

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static CameraManager mCameraManager;
    private static CaptureRequest.Builder mPreviewBuilder;
    private static CameraDevice mCameraDevices;
    private static CameraCaptureSession mCameraCaptureSession;
    private static ImageReader mImageReader, mPreviewCallbackReader;
    private static boolean mIsPreviewing;

    /**
     * 释放所有的资源
     */
    public static void destroy(Camera camera) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (null != mCameraCaptureSession) {
//                try {
//                    mCameraCaptureSession.stopRepeating();
//                    mCameraCaptureSession.abortCaptures();
//                } catch (CameraAccessException e) {
//                    LogUtil.e(TAG, null == e.getMessage() ? e.toString() : e.getMessage());
//                }
//                mCameraCaptureSession.close();
//                mCameraCaptureSession = null;
//            }
//
//            if (null != mCameraDevices) {
//                mCameraDevices.close();
//                mCameraDevices = null;
//            }
//
//            if (null != mImageReader) {
//                mImageReader.close();
//                mImageReader = null;
//            }
//            if (null != mPreviewCallbackReader) {
//                mPreviewCallbackReader.close();
//                mPreviewCallbackReader = null;
//            }
//            if (null != mPreviewBuilder) {
//                mPreviewBuilder = null;
//            }
//            init();
//        } else {
            if (null != camera) {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            }
//        }
    }

//    /**
//     * 展示摄像头捕获的内容
//     *
//     * @param context
//     * @param textureView
//     */
//    @RequiresApi(api = Build.VERSION_CODES.M)
//    public static void startPreview(final Context context, final TextureView textureView) {
//        startPreview(context, textureView, null);
//    }

//    /**
//     * 展示摄像头捕获的内容
//     *
//     * @param context
//     * @param textureView
//     */
//    @RequiresApi(api = Build.VERSION_CODES.M)
//    public static void startPreview(final Context context, final TextureView textureView, final ImageReader.OnImageAvailableListener listener) {
//        if (null != mCameraCaptureSession && null != mCameraDevices) {
//            try {
//                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, mAEMode);       // 曝光模式
//                mPreviewBuilder.set(CaptureRequest.FLASH_MODE, mFlashMode);         // 闪光灯模式
//                mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, MyApplication.getCameraHandler());
//                mIsPreviewing = true;
//            } catch (CameraAccessException e) {
//                LogUtil.e(TAG, null == e.getMessage() ? e.toString() : e.getMessage());
//            }
//        } else if (null != textureView) {
//            openCamera(context, new CameraDevice.StateCallback() {
//                @Override
//                public void onOpened(@NonNull CameraDevice camera) {
//                    LogUtil.d("Kael", "open");
//                    mIsPreviewing = true;
//                    mCameraDevices = camera;
//                    createCaptureSession(camera, textureView, context, listener);
//                }
//
//                @Override
//                public void onDisconnected(@NonNull CameraDevice camera) {
//                    LogUtil.d("Kael", "disconnect");
//                    mIsPreviewing = false;
//                }
//
//                @Override
//                public void onError(@NonNull CameraDevice camera, int error) {
//                    LogUtil.d("Kael", "error");
//                    mIsPreviewing = false;
//                    LogUtil.e(TAG, "Camera Open Failed.");
//                }
//            });
//        }
//    }
//
//    /**
//     * 拍照（拍照之前需要打开摄像头）
//     *
//     * @param activity
//     * @param imageListener
//     */
//    @RequiresApi(api = Build.VERSION_CODES.M)
//    public static void takePicture(final Activity activity, final ImageReader.OnImageAvailableListener imageListener) {
//        if (null != mCameraCaptureSession && null != mImageReader && null != mCameraDevices) {
//            try {
//                mImageReader.setOnImageAvailableListener(imageListener, MyApplication.getCameraHandler());
//                mCameraCaptureSession.stopRepeating();
//                mCameraCaptureSession.capture(generateCaptureRequest(activity, mImageReader), null, MyApplication.getCameraHandler());
//            } catch (CameraAccessException e) {
//                LogUtil.e(e);
//            }
//        }
//    }
//
//    /**
//     * 打开摄像头
//     *
//     * @param context       上下文
//     * @param stateCallback 状态监听
//     */
//    @RequiresApi(api = Build.VERSION_CODES.M)
//    public static void openCamera(Context context, CameraDevice.StateCallback stateCallback) {
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            ((Activity) context).requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
//            return;
//        }
//        try {
//            if (null == mCameraManager)
//                mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
//            mCameraManager.openCamera(CAMERA_ID, stateCallback, MyApplication.getCameraHandler());
//        } catch (CameraAccessException e) {
//            LogUtil.e(e);
//        }
//    }

    /**
     * 停止预览
     */
    public static void stopPreview() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            LogUtil.d("Kael", "mIsPreviewing = " + mIsPreviewing);
//            if (null != mCameraCaptureSession && mIsPreviewing) {
//                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);   // 曝光模式
//                mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);              // 闪光灯模式
//                try {
//                    mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), new CameraCaptureSession.CaptureCallback() {
//                        @Override
//                        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                                super.onCaptureCompleted(session, request, result);
//                                try {
//                                    if (null != mCameraCaptureSession)
//                                        mCameraCaptureSession.stopRepeating();
//                                } catch (CameraAccessException e) {
//                                    LogUtil.e(e);
//                                }
//                                mIsPreviewing = false;
//                            }
//                        }
//                    }, MyApplication.getCameraHandler());
//                } catch (CameraAccessException e) {
//                    LogUtil.e(e);
//                }
//            }
//        } else {
        if (null != mCamera) mCamera.stopPreview();
//        }
    }


    /**
     * 根据摄像头旋转角度，获取图片的旋转角度
     *
     * @param activity
     * @return
     */
    public static int getImageRotation(Activity activity) {
        // 1, 获取手机方向
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        // 2, 返回手机方向对应的方向值
        return ORIENTATIONS.get(rotation);
    }

    /**
     * 根据手机屏幕，以及支持的预览分辨率，获取合适的高宽
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static int[] getSuitableSize(Context context, CameraManager manager) {
        int[] size = new int[2];
        try {
            CameraCharacteristics character = manager.getCameraCharacteristics("0");
            StreamConfigurationMap map = character.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            DisplayMetrics screenParams = ScreenUtils.getScreenParams(context);
            Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);
            for (Size s : outputSizes) {
                size[0] = s.getWidth();
                size[1] = s.getHeight();
                if (screenParams.widthPixels * size[0] == screenParams.heightPixels * size[1])
                    return size;
            }
        } catch (CameraAccessException e) {
            Log.e(TAG,e.getMessage());
        }
        size[0] = 1920;
        size[1] = 1080;
        return size;
    }

//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    private static void createCaptureSession(final CameraDevice device, final TextureView textureView, Context context, final ImageReader.OnImageAvailableListener listener) {
//        try {
//            // 1, 计算Preview显式区域大小
//            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
//            if (null == surfaceTexture) return;
//            int[] suitableSize = getSuitableSize(context, mCameraManager);
//            surfaceTexture.setDefaultBufferSize(suitableSize[0], suitableSize[1]);
//            final Surface surface = new Surface(surfaceTexture);
//            mImageReader = ImageReader.newInstance(suitableSize[0], suitableSize[1], ImageFormat.JPEG, 15);
//            mPreviewCallbackReader = ImageReader.newInstance(suitableSize[0], suitableSize[1], ImageFormat.YUV_420_888, 1);
//            if (null != listener)
//                mPreviewCallbackReader.setOnImageAvailableListener(listener, MyApplication.getCameraHandler());
//
//            // 2, 连接CameraDevice，创建Session
//            List<Surface> outputs = Arrays.asList(surface, mImageReader.getSurface(), mPreviewCallbackReader.getSurface());
//            device.createCaptureSession(outputs, new CameraCaptureSession.StateCallback() {
//                @Override
//                public void onConfigured(@NonNull CameraCaptureSession session) {
//                    mCameraCaptureSession = session;
//                    try {
//                        // 3, 开始请求Preview
//                        if (null == listener)
//                            mPreviewBuilder = generateCallbackBuilder(device, Collections.singletonList(surface));
//                        else
//                            mPreviewBuilder = generateCallbackBuilder(device, Arrays.asList(surface, mPreviewCallbackReader.getSurface()));
//                        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, mAEMode);   // 曝光模式
//                        mPreviewBuilder.set(CaptureRequest.FLASH_MODE, mFlashMode);     // 闪光灯模式
//                        mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, MyApplication.getCameraHandler());
////                        List<CaptureRequest> requests = new ArrayList<>(2);
////                        requests.add(generatePreviewBuilder(device, Collections.singletonList(surface)).build());
////                        requests.add(generateCallbackBuilder(device, Collections.singletonList(mPreviewCallbackReader.getSurface())).build());
////                        mCameraCaptureSession.setRepeatingBurst(requests, null, MyApplication.getCameraHandler());
//                    } catch (CameraAccessException e) {
//                        LogUtil.e(e);
//                    }
//                }
//
//                @Override
//                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
////                    ToastUtil.show(context, "相机预览请求失败");
//                    if (null != mCameraDevices) mCameraDevices.close();
//                    mCameraDevices = null;
//                    if (null != mCameraCaptureSession) mCameraCaptureSession.close();
//                    mCameraCaptureSession = null;
//                    mIsPreviewing = false;
//                }
//            }, MyApplication.getCameraHandler());
//
//            // 4, 重新设置TextureView的大小（根据宽度，设置高度）
//            int width = textureView.getWidth();
//            int height = (int) ((double) width / suitableSize[1] * suitableSize[0]);
//            final ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
//            layoutParams.height = height;
//            MyApplication.getHandler().post(new Runnable() {
//                @Override
//                public void run() {
//                    textureView.setLayoutParams(layoutParams);
//                }
//            });
//        } catch (CameraAccessException e) {
//            LogUtil.e(e);
//        }
//    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static CaptureRequest.Builder generatePreviewBuilder(CameraDevice device, List<Surface> surfaces) throws CameraAccessException {
        CaptureRequest.Builder builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        builder.set(CaptureRequest.JPEG_QUALITY, (byte) 85);
        builder.set(CaptureRequest.JPEG_THUMBNAIL_QUALITY, (byte) 85);

//        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
//        builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_FAST);
//        builder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_FAST);
//        builder.set(CaptureRequest.CONTROL_AE_MODE, mAEMode);                                           // 曝光模式
//        if (-1 != mFlashMode) builder.set(CaptureRequest.FLASH_MODE, mFlashMode);                       // 闪光灯模式
//        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO); // 闪光灯模式
//        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);  // 自动白平衡
//        builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
////        builder.set(CaptureRequest.SCALER_CROP_REGION, new Rect(100, 100, 300, 300));         // 缩放区域
//        builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
////        builder.set(CaptureRequest.JPEG_GPS_LOCATION, new Location(Location.convert(33.10232, Location.FORMAT_DEGREES)));
//        builder.set(CaptureRequest.SENSOR_FRAME_DURATION, 0L);


//        DisplayMetrics params = ScreenUtils.getScreenParams(MyApplication.getContext());
//        double offsetX = params.widthPixels * 0.1;
//        double offsetY = params.heightPixels * 0.1;
//        builder.set(CaptureRequest.SCALER_CROP_REGION, new Rect((int) offsetX, (int) offsetY, (int) (params.widthPixels - offsetX + .5f), (int) (params.heightPixels - offsetY + .5f)));
        for (Surface surface : surfaces) {
            builder.addTarget(surface);
        }
        return builder;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static CaptureRequest.Builder generateCallbackBuilder(CameraDevice device, List<Surface> surfaces) throws CameraAccessException {
        CaptureRequest.Builder builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        builder.set(CaptureRequest.JPEG_QUALITY, (byte) 85);
        builder.set(CaptureRequest.JPEG_THUMBNAIL_QUALITY, (byte) 85);
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        builder.set(CaptureRequest.SENSOR_FRAME_DURATION, 500L);

//        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
//        builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_FAST);
//        builder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_FAST);
//        builder.set(CaptureRequest.CONTROL_AE_MODE, mAEMode);                                           // 曝光模式
//        if (-1 != mFlashMode) builder.set(CaptureRequest.FLASH_MODE, mFlashMode);                       // 闪光灯模式
//        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO); // 闪光灯模式
//        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);  // 自动白平衡
//        builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
////        builder.set(CaptureRequest.SCALER_CROP_REGION, new Rect(100, 100, 300, 300));         // 缩放区域
//        builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
////        builder.set(CaptureRequest.JPEG_GPS_LOCATION, new Location(Location.convert(33.10232, Location.FORMAT_DEGREES)));
//        builder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
//        builder.set(CaptureRequest.SENSOR_FRAME_DURATION, 0L);

//        DisplayMetrics params = ScreenUtils.getScreenParams(MyApplication.getContext());
//        double offsetX = params.widthPixels * 0.1;
//        double offsetY = params.heightPixels * 0.1;
//        builder.set(CaptureRequest.SCALER_CROP_REGION, new Rect((int) offsetX, (int) offsetY, (int) (params.widthPixels - offsetX + .5f), (int) (params.heightPixels - offsetY + .5f)));
        for (Surface surface : surfaces) {
            builder.addTarget(surface);
        }
        return builder;
    }

    /**
     * 生成Capture的CaptureRequest请求对象，用于拍照请求
     *
     * @param activity
     * @param imageReader
     * @return
     * @throws CameraAccessException
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static CaptureRequest generateCaptureRequest(Activity activity, ImageReader imageReader) throws CameraAccessException {
        CaptureRequest.Builder builder = mCameraDevices.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        builder.addTarget(imageReader.getSurface());
        // 自动对焦
//        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//        // 自动曝光
//        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
//        builder.set(CaptureRequest.CONTROL_AE_MODE, mAEMode);
//        if (-1 != mFlashMode) builder.set(CaptureRequest.FLASH_MODE, mFlashMode);
//        // 根据设备方向计算设置照片的方向
        builder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(getImageRotation(activity)));
//        builder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);

//        DisplayMetrics params = ScreenUtils.getScreenParams(MyApplication.getContext());
//        double offsetX = params.widthPixels * 0.1;
//        double offsetY = params.heightPixels * 0.1;
//        builder.set(CaptureRequest.SCALER_CROP_REGION, new Rect((int) offsetX, (int) offsetY, (int) (params.widthPixels - offsetX + .5f), (int) (params.heightPixels - offsetY + .5f)));
        return builder.build();
    }

    /**
     * 设置闪光灯
     *
     * @param isTorchMode
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void setTorchMode(Context context, boolean isTorchMode) {
        try {
            if (null == mCameraManager)
                mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            mCameraManager.setTorchMode("0", isTorchMode);
        } catch (CameraAccessException e) {
            Log.e(TAG,e.getMessage());
        }
    }

    /**
     * 获取摄像头相关信息
     *
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static String getCameraInfoAndCharacter(Context context) {
        StringBuilder msg = new StringBuilder();
        if (null == mCameraManager)
            mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = mCameraManager.getCameraIdList();
            msg.append("一共有").append(cameraIdList.length).append("个相机：");
            for (int i = 0; i < cameraIdList.length; i++) {
                msg.append("\n\ncamera-").append(i).append("\n");

                CameraCharacteristics character = mCameraManager.getCameraCharacteristics(i + "");
                List<CaptureRequest.Key<?>> keys = character.getAvailableCaptureRequestKeys();
                for (CaptureRequest.Key<?> key : keys) {
                    msg.append("\t").append(key.getName()).append("\n");
                }

                List<CaptureResult.Key<?>> results = character.getAvailableCaptureResultKeys();
                for (CaptureResult.Key<?> reslut : results) {
                    msg.append("\t").append(reslut.getName()).append("\n");
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG,e.getMessage());
        }
        return msg.toString();
    }

    /**
     * 切换闪光灯模式
     *
     * @param mode
     */
    public static void switchFlashMode(int mode) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            switch (mode) {
//                case FLASH_MODE_AUTO:
//                    mAEMode = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
//                    mFlashMode = CaptureRequest.FLASH_MODE_TORCH;
//                    break;
//                case FLASH_MODE_OPEN:
//                    mAEMode = CaptureRequest.CONTROL_AE_MODE_ON;
//                    mFlashMode = CaptureRequest.FLASH_MODE_TORCH;
//                    break;
//                case FLASH_MODE_CLOSE:
//                    mAEMode = CaptureRequest.CONTROL_AE_MODE_OFF;
//                    mFlashMode = CaptureRequest.FLASH_MODE_OFF;
//                    break;
//            }
//        } else {
        switch (mode) {
            case FLASH_MODE_AUTO:
                mFlashMode2 = Camera.Parameters.FLASH_MODE_AUTO;
                break;
            case FLASH_MODE_OPEN:
                mFlashMode2 = Camera.Parameters.FLASH_MODE_TORCH;
                break;
            case FLASH_MODE_CLOSE:
                mFlashMode2 = Camera.Parameters.FLASH_MODE_OFF;
                break;
        }
        if (null == mCamera) return;
        Camera.Parameters params = mCamera.getParameters();
        if (null == params) return;
        params.setFlashMode(mFlashMode2);
        mCamera.setParameters(params);
//        }
    }

    /**
     * 获取当前闪光灯模式
     *
     * @return
     */
    public static int getFlashMode() {
        return mFlashMode;
    }
}
