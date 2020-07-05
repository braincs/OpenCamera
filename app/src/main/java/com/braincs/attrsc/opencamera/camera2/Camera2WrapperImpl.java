package com.braincs.attrsc.opencamera.camera2;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.braincs.attrsc.opencamera.utils.FrameUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@TargetApi(21)
public class Camera2WrapperImpl extends CameraWrapper {
    private static final String TAG = "Camera2WrapperImpl";

    // event for Camera
    private final static int EVENT_OPEN_CAMERA = 1;
    private final static int EVENT_START_PREVIEW = 2;

    // worker thread for camera
    private Context mContext;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    // UI handler for callback
    private Handler mUIHandler;

    // camera info
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private String mCameraId;
    private boolean mIsBackCamera;
    private CameraOpenCallback mCameraOpenCallback;
    private CameraCaptureSession mCameraCaptureSession;


    // preview info
    private ImageReader mImageReader;
    private IPreviewCallback mPreviewCallback;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceTexture mSurfaceTexture;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

//    long sum = 0L;
//    int count = 0;
    // Listener for frame data of preview
    private ImageReader.OnImageAvailableListener mImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            if (checkNotNull(mPreviewCallback)) {
                byte[] yuvData = new byte[image.getHeight() * image.getWidth() * 3 / 2];
//                long start = System.currentTimeMillis();
                //方法1：6ms
                FrameUtil.image2NV21(image, yuvData);

                //方法2：6ms
//                System.arraycopy(FrameUtil.getBytesFromImageAsType(image, 0), 0, yuvData, 0, yuvData.length);
//                sum += System.currentTimeMillis() - start;
//                count++;
//                Log.d(TAG, "420_nv21 average = " + (sum / count));
                mPreviewCallback.onPreviewFrame(yuvData, null);
            }

            // close
            image.close();
        }
    };

    public Camera2WrapperImpl() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw new RuntimeException("can't use this class");
        }

        // Create ImageReader
        initImageReader();
    }

    /**
     * state of opening Camera
     */
    private final CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "onOpened()...");
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            callbackToCaller(Result.RESULT_SUCCESS);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "onDisconnected()...");
            mCameraOpenCloseLock.release();
            mCameraDevice.close();
            mCameraDevice = null;
            callbackToCaller(Result.RESULT_DISCONNECTED);
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int errorCode) {
            Log.d(TAG, "onError()...code:" + errorCode);
            mCameraOpenCloseLock.release();
            mCameraDevice.close();
            mCameraDevice = null;
            callbackToCaller(Result.RESULT_FAILED);
        }
    };

    /**
     * Open Camera asynchronously
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    @Override
    public void openCamera(boolean isBackCamera, Context context, CameraOpenCallback callback, int width, int height) {
        Log.d(TAG, "openCamera()...back camera:" + isBackCamera);
        mWidth = width;
        mHeight = height;
        // init worker thread
        initEventLooper(context);

        // save params
        mContext = context;
        mIsBackCamera = isBackCamera;
        mCameraOpenCallback = callback;

        // obtain message
        Message msg = Message.obtain(mHandler, EVENT_OPEN_CAMERA);
        msg.sendToTarget();
    }

    /**
     * Start Preview
     * @param surfaceHolder surface holder
     */
    @Override
    public void startPreview(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "startPreview()...");

        // save holder
        mSurfaceHolder = surfaceHolder;

        // obtain message
        Message msg = Message.obtain(mHandler, EVENT_START_PREVIEW);
        msg.sendToTarget();
    }

    /**
     * Start Preview
     * @param surfaceTexture surface texture
     */
    @Override
    public void startPreview(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "startPreview()...");

        // save holder
        mSurfaceTexture = surfaceTexture;

        // obtain message
        Message msg = Message.obtain(mHandler, EVENT_START_PREVIEW);
        msg.sendToTarget();
    }

    /**
     * Begin to detect and receive frame data of preview
     * @param callback receive frame data of preview
     */
    @Override
    public void setFrameCallback(IPreviewCallback callback) {
        setPreviewCallback(callback);
    }

    /**
     * Stop detecting，
     */
    @Override
    public void removeFrameCallback() {
        setPreviewCallback(null);
    }

    /**
     * Stop preview
     */
    @Override
    public void stopPreview() {
        Log.d(TAG, "stopPreview()...");
        if (checkNotNull(mCameraCaptureSession)) {
            try {
                mCameraCaptureSession.stopRepeating();
                mCameraCaptureSession.close();
            } catch (Exception e ) {
                e.printStackTrace();
                Log.e(TAG, "exception when stop repeating");
            }
            mCameraCaptureSession = null;
        }

        if (checkNotNull(mCameraDevice)) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        // set Image Reader
        if (checkNotNull(mImageReader)) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    /**
     * Close camera
     */
    @Override
    public void closeCamera() {
        Log.d(TAG, "closeCamera()...");
        try {
            mCameraOpenCloseLock.acquire();
            stopPreview();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }

        // reset camera info
        resetPreviewInfo();

        // exit event looper
        exitEventLooper();
    }

    /**
     * Set Orientation of Display
     * @param angle 0~360
     */
    @Override
    public void setDisplayOrientation(int angle) {
    }

    // init Image Reader
    private void initImageReader() {
        mImageReader = ImageReader.newInstance(mWidth, mHeight, ImageFormat.YUV_420_888, 3);
        mImageReader.setOnImageAvailableListener(mImageAvailableListener, mHandler);
    }
    // init event thread
    private void initEventLooper(Context context) {
        Log.d(TAG, "initEventLooper()...");
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Log.d(TAG, "handleMessage()..." + msg);
                switch (msg.what) {
                    case EVENT_OPEN_CAMERA:
                        handleOpenCamera();
                        break;
                    case EVENT_START_PREVIEW:
                        handleStartPreview();
                        break;
                    default:
                        Log.d(TAG, "Unsupport event type");
                        break;
                }
                return false;
            }
        });

        mUIHandler = new Handler(context.getMainLooper());
    }
    private void exitEventLooper() {
        Log.d(TAG, "exitEventLooper()...");
        if (checkNotNull(mUIHandler)) {
            mUIHandler.removeCallbacksAndMessages(null);
        }
        if (checkNotNull(mHandler)) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (checkNotNull(mHandlerThread)) {
            mHandlerThread.quit();
        }
        mHandlerThread = null;
        mHandler = null;
        mUIHandler = null;
    }
    /**
     * Open Camera
     */
    // handle for opening Camera
    private void handleOpenCamera() {
        Log.d(TAG, "handleOpenCamera()...");

        mCameraManager = (CameraManager)mContext.getSystemService(Activity.CAMERA_SERVICE);
        if (!checkNotNull(mCameraManager)) {
            callbackToCaller(Result.RESULT_FAILED);
            return;
        }

        // select back or front camera
        mCameraId = selectBackOrFrontCamera();
        if (!checkNotNull(mCameraId)) {
            Log.e(TAG, "Camera id is invalid, return!");
            callbackToCaller(Result.RESULT_FAILED);
            return;
        }

        // Open Camera
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            mCameraManager.openCamera(mCameraId, mCameraStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to open Camera");
            e.printStackTrace();
            callbackToCaller(Result.RESULT_FAILED);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to open Camera");
            e.printStackTrace();
            callbackToCaller(Result.RESULT_FAILED);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start Preview
     */
    private void handleStartPreview() {
        Log.d(TAG, "handleStartPreview()...");

        // check if Camera already opened
        if (!checkNotNull(mCameraDevice)) {
            Log.e(TAG, "Invalid Camera device");
            return;
        }

        // Camera already opened and go to preview
        List<Surface> surfaces = new ArrayList<>();
        surfaces.add(mImageReader.getSurface());
        if (checkNotNull(mSurfaceHolder)) {
            surfaces.add(mSurfaceHolder.getSurface());
        }
        if (checkNotNull(mSurfaceTexture)){
            mSurfaceTexture.setDefaultBufferSize(mWidth, mHeight);
            surfaces.add(new Surface(mSurfaceTexture));
        }
        try {
            // create Capture request
            final CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mImageReader.getSurface());
            if (checkNotNull(mSurfaceHolder)) {
                builder.addTarget(mSurfaceHolder.getSurface());
            }
            if (checkNotNull(mSurfaceTexture)){
                mSurfaceTexture.setDefaultBufferSize(mWidth, mHeight);
                builder.addTarget(new Surface(mSurfaceTexture));
            }
            // create Capture Session
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (!checkNotNull(mCameraDevice)) {
                        return;
                    }
                    try {
                        // auto focus
                        builder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                        mCameraCaptureSession = session;

                        // send request
                        session.setRepeatingRequest(builder.build(),null, mHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "on configure failed");
                }
            }, mHandler);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to start preview!");
        }
    }

    private void setPreviewCallback(final IPreviewCallback previewCallback) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mPreviewCallback = previewCallback;
            }
        });
    }

    private void callbackToCaller(final int result) {
        Log.d(TAG, "callbackToCaller...");
        if (checkNotNull(mUIHandler)) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (checkNotNull(mCameraOpenCallback)) {
                        switch (result) {
                            case Result.RESULT_SUCCESS:
                                mCameraOpenCallback.onOpenSuccess();
                                break;
                            case Result.RESULT_DISCONNECTED:
                                mCameraOpenCallback.onDisconnected();
                                break;
                            case Result.RESULT_FAILED:
                                mCameraOpenCallback.onOpenFailed();
                                break;
                            default:
                                break;
                        }
                    }
                }
            });
        }
    }
    // check if Camera is null
    private boolean checkNotNull(Object object) {
        return (null != object);
    }
    // select back or front Camera
    private String selectBackOrFrontCamera() {
        Log.d(TAG, "selectBackOrFrontCamera()...");
        String cameraId = null;

        try {
            // get id list
            String[] idList = mCameraManager.getCameraIdList();
            for (String id : idList) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null) {
                    if (mIsBackCamera && // back camera
                            CameraCharacteristics.LENS_FACING_BACK == facing) {
                        cameraId = id;
//                        Log.d(TAG, "isBack : " + mIsBackCamera +", cameraid = " + cameraId);
                        break;
                    } else if (!mIsBackCamera && CameraCharacteristics.LENS_FACING_FRONT == facing) {
                        cameraId = id;
//                        Log.d(TAG, "isBack : " + mIsBackCamera +", cameraid = " + cameraId);
                        break;
                    }
                }

                Log.d(TAG, "Camera Id:" + cameraId);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Invalid Camera Id");
        }

        return cameraId;
    }
    // reset preview info
    private void resetPreviewInfo() {
        mCameraId = null;
        mSurfaceHolder = null;
        mCameraOpenCallback = null;

        mPreviewCallback = null;
        mIsBackCamera = false;
    }
}
