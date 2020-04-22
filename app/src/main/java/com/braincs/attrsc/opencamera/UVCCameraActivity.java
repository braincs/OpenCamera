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
public class UVCCameraActivity extends BaseActivity implements CameraDialog.CameraDialogParent {
    private final static String TAG = UVCCameraActivity.class.getSimpleName();
    private final Object mSync = new Object();

    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private SimpleUVCCameraTextureView mUVCCameraView;
    private Surface mPreviewSurface;
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
        setContentView(R.layout.activity_uvc_camera);
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
            if (mUVCCamera != null) {
                mUVCCamera.startPreview();
            }
        }
    }

    @Override
    protected void onStop() {
        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.stopPreview();
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
            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }
            mUVCCameraView = null;

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
        mUVCCameraView = (SimpleUVCCameraTextureView) findViewById(R.id.simpleUVCCameraTextureView);
        mUVCCameraView.setAspectRatio((float) UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mUVCCameraView.setRotation(Constants.ORIENTATION_90);

        mUSBMonitor = new USBMonitor(mContext, mOnDeviceConnectListener);
            final List<DeviceFilter> filters = DeviceFilter.getDeviceFilters(this, R.xml.device_filter);
            mUSBMonitor.setDeviceFilter(filters);

    }

    private synchronized void releaseCamera() {
        synchronized (mSync) {
            if (mUVCCamera != null) {
                try {
                    mUVCCamera.setStatusCallback(null);
                    mUVCCamera.setButtonCallback(null);
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                } catch (final Exception e) {
                    //
                }
                mUVCCamera = null;
            }
            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
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
            int index = isFront? 0:1;
            synchronized (mSync) {
                if (mUSBMonitor != null && !isOpened && UVCDevices.size() > index){
                    isOpened = true;
                    mUSBMonitor.requestPermission(UVCDevices.get(index));
                }

            }

        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            Log.d(TAG, "--USB_DEVICE_CONNECTED-- " + device.getProductId());
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    final UVCCamera camera = new UVCCamera();
                    camera.open(ctrlBlock);
                    if (mPreviewSurface != null) {
                        mPreviewSurface.release();
                        mPreviewSurface = null;
                    }
                    try {
                        camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
                    } catch (final IllegalArgumentException e1) {
                        camera.destroy();
                        return;
                    }
                    final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
                    if (st != null) {
                        mPreviewSurface = new Surface(st);
                        camera.setPreviewDisplay(mPreviewSurface);
//                        camera.setFrameCallback(uvcFrameCallBack, UVCCamera.PIXEL_FORMAT_NV21/*UVCCamera.PIXEL_FORMAT_RGB565*/);
                        camera.setFrameCallback(uvcFrameCallBack, UVCCamera.PIXEL_FORMAT_YUV420SP/*UVCCamera.PIXEL_FORMAT_RGB565*/);
                        camera.startPreview();
                    }
                    synchronized (mSync) {
                        mUVCCamera = camera;
                    }
                }
            }, 0);

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
    private IFrameCallback uvcFrameCallBack = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer frame) {
            uvcBuffer = frame;
            Log.d(TAG, "rgb frame remain = " + frame.remaining());
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
