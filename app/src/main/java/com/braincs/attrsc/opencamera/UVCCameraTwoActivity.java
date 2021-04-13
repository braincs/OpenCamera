package com.braincs.attrsc.opencamera;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.braincs.attrsc.opencamera.utils.Constants;
import com.braincs.attrsc.opencamera.utils.ScreenUtils;
import com.braincs.attrsc.opencamera.widget.SimpleUVCCameraTextureView;
import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Shuai
 * 27/03/2020.
 */
public class UVCCameraTwoActivity extends BaseActivity implements CameraDialog.CameraDialogParent {
    private final static String TAG = UVCCameraTwoActivity.class.getSimpleName();
    private final Object mSync = new Object();

    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCameraL;
    private UVCCamera mUVCCameraR;
    private SimpleUVCCameraTextureView mUVCCameraViewL;
    private SimpleUVCCameraTextureView mUVCCameraViewR;
    private Surface mPreviewSurfaceL;
    private Surface mPreviewSurfaceR;
    private List<UsbDevice> UVCDevices = new ArrayList<>(3);
    private ByteBuffer uvcBuffer;

    private Context mContext;
    private Intent mIntent;
    private boolean isFront;
    private boolean isOpened;


    private Drawable mSuccDrawable, mFailDrawable;
    private TextView mTvCheckResult;
    private RelativeLayout rlCameraGroupView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_two_view_uvc_camera2);
        mContext = this;

        Log.d(TAG, "--onCreate--");
        initIntent();
        initView();
    }
//    @Override
//    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
//        super.onCreate(savedInstanceState, persistentState);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        setContentView(R.layout.activity_android_camera);
//        mContext = this;
//
//        Log.d(TAG, "--onCreate--");
//        initIntent();
//        initView();
//    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart:");
        mUSBMonitor.register();
        List<UsbDevice> deviceList = mUSBMonitor.getDeviceList();
        for (UsbDevice device : deviceList) {
            String msg = "Device -- " +
                    "getDeviceName = " + device.getProductName() + ", " +
                    "getDeviceId = " + device.getDeviceId() + ", " +
                    "getProductId = " + device.getProductId();
            Log.d("debug-devices", msg);
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();

        }
//        mUSBMonitor.requestPermission(deviceList.get(0));
        synchronized (mSync) {
            if (mUVCCameraL != null) {
                mUVCCameraL.startPreview();
            }
        }
        synchronized (mSync) {
            if (mUVCCameraR != null) {
                mUVCCameraR.startPreview();
            }
        }
    }

    @Override
    protected void onStop() {
        synchronized (mSync) {
            if (mUVCCameraL != null) {
                mUVCCameraL.stopPreview();
            }
            if (mUVCCameraR != null) {
                mUVCCameraR.stopPreview();
            }
            if (mUSBMonitor != null) {
                mUSBMonitor.unregister();
//                mUSBMonitor = null;
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy:");
        synchronized (mSync) {

            releaseCamera();
            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
            if (mPreviewSurfaceL != null) {
                mPreviewSurfaceL.release();
                mPreviewSurfaceL = null;
            }
            mUVCCameraViewL = null;

            if (mPreviewSurfaceR != null) {
                mPreviewSurfaceR.release();
                mPreviewSurfaceR = null;
            }
            mUVCCameraViewR = null;

        }
        super.onDestroy();
    }

    //region init: UI
    private void initIntent() {
        Log.d(TAG, "--initIntent--");
        mIntent = getIntent();
        isFront = mIntent.getBooleanExtra(Constants.INTENT_KEY_CAMERA_FRONT, false);
        Log.d(TAG, "is front camera: "+ isFront);
    }

    private void initView() {
        int rotate = 90;

        initUVCCamera();

        rlCameraGroupView = findViewById(R.id.rlCameraGroupView);
//        mTvPreview2 = findViewById(R.id.cam_preview_ir);
//        mTvPreview2.setSurfaceTextureListener(mCameraIRTextureListener);

        autoConfigCameraView();


        mSuccDrawable = new ColorDrawable(Color.GREEN);
        mFailDrawable = new ColorDrawable(Color.RED);


    }

    private void autoConfigCameraView() {
        //auto config (fit screen size)
        Log.d(TAG, "autoConfigCameraView()...");

        boolean isLandscape;
        Configuration configuration = getResources().getConfiguration();
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;
        } else {
            isLandscape = false;
        }
        DisplayMetrics screenParams = ScreenUtils.getScreenParams(this);
        ViewGroup.LayoutParams params = rlCameraGroupView.getLayoutParams();
//        ViewGroup.LayoutParams params2 = mTvPreview2.getLayoutParams();
        if (isLandscape) {
            Log.d(TAG, "landscape");
            // height < width
            params.height = Math.min(screenParams.heightPixels, screenParams.widthPixels);
            params.width = (int) (params.height / Constants.FRAME_HEIGHT_WIDTH_RATIO);
//            params2.height = params.height / Constants.VIEW_SCALE_FACTOR;
//            params2.width = params.width / Constants.VIEW_SCALE_FACTOR;

        } else {
            Log.d(TAG, "portrait");
            // height > width
            if (screenParams.heightPixels / screenParams.widthPixels >= Constants.FRAME_HEIGHT_WIDTH_RATIO) {
                params.width = screenParams.widthPixels;
                params.height = (int) (screenParams.widthPixels * Constants.FRAME_HEIGHT_WIDTH_RATIO);
            } else {
                params.height = screenParams.heightPixels;
                params.width = (int) (screenParams.heightPixels / Constants.FRAME_HEIGHT_WIDTH_RATIO);
            }
//            params2.height = params.height / Constants.VIEW_SCALE_FACTOR;
//            params2.width = params.width / Constants.VIEW_SCALE_FACTOR;
        }
        rlCameraGroupView.setLayoutParams(params);
//        mTvPreview2.setLayoutParams(params2);
    }
    //endregion



    //region Camera
    private void initUVCCamera() {
        isOpened = false;
        mUVCCameraViewL = (SimpleUVCCameraTextureView) findViewById(R.id.camera_view_L);
        mUVCCameraViewL.setAspectRatio((float) UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mUVCCameraViewL.setRotation(Constants.ORIENTATION_90);
        mUVCCameraViewL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, UVCDevices.get(0).toString());
                mUSBMonitor.requestPermission(UVCDevices.get(0));
            }
        });

        mUVCCameraViewR = (SimpleUVCCameraTextureView) findViewById(R.id.camera_view_R);
        mUVCCameraViewR.setAspectRatio((float) UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mUVCCameraViewR.setRotation(Constants.ORIENTATION_90);
        mUVCCameraViewR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, UVCDevices.get(1).toString());
                mUSBMonitor.requestPermission(UVCDevices.get(1));
            }
        });

        mUSBMonitor = new USBMonitor(mContext, mOnDeviceConnectListener);
            final List<DeviceFilter> filters = DeviceFilter.getDeviceFilters(this, R.xml.device_filter);
            mUSBMonitor.setDeviceFilter(filters);

    }

    private synchronized void releaseCamera() {
        synchronized (mSync) {
            if (mUVCCameraL != null) {
                try {
                    mUVCCameraL.setStatusCallback(null);
                    mUVCCameraL.setButtonCallback(null);
                    mUVCCameraL.close();
                    mUVCCameraL.destroy();
                } catch (final Exception e) {
                    //
                }
                mUVCCameraL = null;
            }
            if (mPreviewSurfaceL != null) {
                mPreviewSurfaceL.release();
                mPreviewSurfaceL = null;
            }
            if (mUVCCameraR != null) {
                try {
                    mUVCCameraR.setStatusCallback(null);
                    mUVCCameraR.setButtonCallback(null);
                    mUVCCameraR.close();
                    mUVCCameraR.destroy();
                } catch (final Exception e) {
                    //
                }
                mUVCCameraR = null;
            }
            if (mPreviewSurfaceR != null) {
                mPreviewSurfaceR.release();
                mPreviewSurfaceR = null;
            }
        }
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
//            Toast.makeText(LivenessActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "--USB_DEVICE_ATTACHED-- " + device.getProductId());

            if (!UVCDevices.contains(device)) UVCDevices.add(device);
//            mUSBMonitor.requestPermission(device);
//            final List<DeviceFilter> filters = DeviceFilter.getDeviceFilters(Liveness3DActivity.this, com.serenegiant.uvccamera.R.xml.device_filter);
//            for (final DeviceFilter filter : filters) {
//                if ((filter != null) && filter.matches(device)) {
//                    // when filter matches
//                    if (mUSBMonitor != null && !filter.isExclude) {
////                        result.add(device);
//                        mUSBMonitor.requestPermission(device);
//
//                    }
//                }
//            }
            synchronized (mSync) {
                if (mUSBMonitor != null && !isOpened && UVCDevices.size() > 1){
                    isOpened = true;
                    mUSBMonitor.requestPermission(UVCDevices.get(0));
                }

            }

        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            Log.d(TAG, "--USB_DEVICE_CONNECTED-- " + device.getProductId());
            if (UVCDevices.indexOf(device) == 0){
                // 第一个设备
                Log.d(TAG, "--USB_DEVICE_CONNECTED_0-- " + device.getProductId());

                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        final UVCCamera camera = new UVCCamera();
                        camera.open(ctrlBlock);
                        if (mPreviewSurfaceL != null) {
                            mPreviewSurfaceL.release();
                            mPreviewSurfaceL = null;
                        }
                        try {
                            camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, 1, 0.5f);
                        } catch (final IllegalArgumentException e1) {
                            camera.destroy();
                            return;
                        }
                        final SurfaceTexture st = mUVCCameraViewL.getSurfaceTexture();
                        if (st != null) {
                            mPreviewSurfaceL = new Surface(st);
                            camera.setPreviewDisplay(mPreviewSurfaceL);
//                        camera.setFrameCallback(uvcFrameCallBackL, UVCCamera.PIXEL_FORMAT_NV21/*UVCCamera.PIXEL_FORMAT_RGB565*/);
                            camera.setFrameCallback(uvcFrameCallBackL, UVCCamera.PIXEL_FORMAT_YUV420SP/*UVCCamera.PIXEL_FORMAT_RGB565*/);
                            camera.startPreview();
                        }
                        synchronized (mSync) {
                            mUVCCameraL = camera;
                        }
                        mUSBMonitor.requestPermission(UVCDevices.get(1));
                    }
                }, 0);

            }else{
                // 第二个设备
                Log.d(TAG, "--USB_DEVICE_CONNECTED_1-- " + device.getProductId());

                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mUVCCameraR = new UVCCamera();
                        mUVCCameraR.open(ctrlBlock);
                        if (mPreviewSurfaceR != null) {
                            mPreviewSurfaceR.release();
                            mPreviewSurfaceR = null;
                        }
                        try {
                            mUVCCameraR.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, 1 , 0.5f);
                        } catch (final IllegalArgumentException e1) {
                            mUVCCameraR.destroy();
                            return;
                        }
                        final SurfaceTexture st = mUVCCameraViewR.getSurfaceTexture();
                        if (st != null) {
                            mPreviewSurfaceR = new Surface(st);
                            mUVCCameraR.setPreviewDisplay(mPreviewSurfaceR);
//                        camera.setFrameCallback(uvcFrameCallBackL, UVCCamera.PIXEL_FORMAT_NV21/*UVCCamera.PIXEL_FORMAT_RGB565*/);
                            mUVCCameraR.setFrameCallback(uvcFrameCallBackR, UVCCamera.PIXEL_FORMAT_YUV420SP/*UVCCamera.PIXEL_FORMAT_RGB565*/);
                            mUVCCameraR.startPreview();
                        }
//                        synchronized (mSync) {
//                            mUVCCameraR = camera;
//                        }
                    }
                }, 0);
            }


        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            // XXX you should check whether the coming device equal to camera device that currently using
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    releaseCamera();
                }
            },0);
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(mContext, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };
    //endregion


    //region Camera callback
    // UVC RGBFrameCallBack
    private IFrameCallback uvcFrameCallBackL = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer frame) {
            uvcBuffer = frame;
//            Log.d(TAG, "rgb frame remain = " + frame.remaining());
        }
    };

    private IFrameCallback uvcFrameCallBackR = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer frame) {
            uvcBuffer = frame;
//            Log.d(TAG, "ir frame remain = " + frame.remaining());
        }
    };

    //endregion


    private void finishWithArgs(String msg, String path) {
        mIntent.putExtra(Constants.INTENT_KEY_RESULT, msg);
        setResult(RESULT_OK, mIntent);
        finish();
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean b) {

    }
}
