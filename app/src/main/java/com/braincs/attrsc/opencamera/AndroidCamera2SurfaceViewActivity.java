package com.braincs.attrsc.opencamera;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.braincs.attrsc.opencamera.camera2.Camera2WrapperImpl;
import com.braincs.attrsc.opencamera.camera2.CameraWrapper;
import com.braincs.attrsc.opencamera.utils.Constants;
import com.braincs.attrsc.opencamera.utils.DateUtil;
import com.braincs.attrsc.opencamera.utils.FileUtil;
import com.braincs.attrsc.opencamera.utils.SaveFrameTask;
import com.braincs.attrsc.opencamera.utils.ScreenUtils;

import java.io.File;

/**
 * Created by Shuai
 * 27/03/2020.
 */
public class AndroidCamera2SurfaceViewActivity extends AppCompatActivity {
    private final static String TAG = AndroidCamera2SurfaceViewActivity.class.getSimpleName();
    private static final String APP_FOLDER = "OpenCamera";

    private SurfaceView mSurfaceView;
    private Context mContext;
    private Intent mIntent;
    private boolean isFront;


    private Drawable mSuccDrawable, mFailDrawable;
    private TextView mTvCheckResult;
    private RelativeLayout rlCameraGroupView;
    private CameraWrapper mCameraWrapper;
    private SurfaceHolder mSurfaceHolder;
    private boolean mIsCameraOpened;
    private volatile boolean isSavePreview = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_android_camera2);
        mContext = this;

        Log.d(TAG, "--onCreate--");
        initIntent();
        initView();
        createCamera();
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

    //region init: UI
    private void initIntent() {
        Log.d(TAG, "--initIntent--");
        mIntent = getIntent();
        isFront = mIntent.getBooleanExtra(Constants.INTENT_KEY_CAMERA_FRONT, false);
        Log.d(TAG, "is front camera: " + isFront);
    }

    private void initView() {

        rlCameraGroupView = findViewById(R.id.rlCameraGroupView);
        mSurfaceView = findViewById(R.id.surface_cam_preview);
        mSurfaceHolder = mSurfaceView.getHolder();

//        mTvPreview2 = findViewById(R.id.cam_preview_ir);
//        mTvPreview2.setSurfaceTextureListener(mCameraIRTextureListener);


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

    private void createCamera() {
        mCameraWrapper = new Camera2WrapperImpl();
        mCameraWrapper.openCamera(!isFront, mContext, mCameraOpenListener, 640,480);
    }

    private CameraWrapper.CameraOpenCallback mCameraOpenListener = new CameraWrapper.CameraOpenCallback() {
        @Override
        public void onOpenSuccess() {
            mIsCameraOpened = true;
            checkAndStartPreview();
        }

        @Override
        public void onDisconnected() {
            Toast.makeText(mContext, "camera disconnected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onOpenFailed() {
            Toast.makeText(mContext, "open camera failed", Toast.LENGTH_SHORT).show();

        }
    };

    private void checkAndStartPreview() {
        Log.d(TAG, "checkAndStartPreview()...Camera Opened:" + mIsCameraOpened +
                " Holder:" + mSurfaceHolder);
        if (mIsCameraOpened && null != mSurfaceHolder) {
            // set orientation
            mCameraWrapper.setDisplayOrientation(Constants.ORIENTATION_90);

            // start preview
            mCameraWrapper.startPreview(mSurfaceHolder);
            mCameraWrapper.setFrameCallback(rgbFrameCallback);
        }
    }

//    TextureView.SurfaceTextureListener mCameraRGBTextureListener = new TextureView.SurfaceTextureListener() {
//        @Override
//        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//            Log.d(TAG, "onSurfaceTextureAvailable()...rgb w = " + width + ", h = " + height);
//
////            rgbCameraWidth = width + Constants.CAMERA_OFFSET_WIDTH;
////            rgbCameraHeight = height + Constants.CAMERA_OFFSET_HEIGHT;
//            Surface surf = new Surface(surface);
//            surf.getHolder();
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 0);
//                    return;
//                }
//            }
////            openCamera(surface, Constants.CameraIDRGB, 0);
////            mCamera = openCamera(surface, Constants.CameraIDRGB, 0, rgbFrameCallback);
//            startCameraPreview(surface, mCamera, rgbFrameCallback);
//            Log.i(TAG, "Camera-1 opened..");
//        }
//
//        @Override
//        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//
//        }
//
//        @Override
//        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//            Log.d(TAG, "onSurfaceTextureDestroyed()...rgb");
////            destroyCamera();
//            destroyCamera(mCamera);
////                mPresenter.onDestroy();
//            return false;
//        }
//
//        @Override
//        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//
//        }
//    };

//    public Camera startCameraPreview(SurfaceTexture holder, Camera mCamera, Camera.PreviewCallback callback) {
//        if (null == mCamera) {
//            finishWithArgs("无法打开摄像头，请重新打开app", "");
//            return null;
//        }
//
//        // 2, 开始预览
//        try {
//            mCamera.setPreviewCallback(callback);
//            mCamera.setPreviewTexture(holder);
//            mCamera.startPreview();
//        } catch (IOException e) {
//            Log.e(TAG, null == e.getMessage() ? e.toString() : e.getMessage());
//            finishWithArgs("开始摄像头预览错误，请重新打开app", "");
//            return mCamera;
//        }
//        return mCamera;
//    }

    public void destroyCamera(Camera camera) {
        if (null != camera) {
            Log.i(TAG, "close camera");
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
    //endregion




    //region Camera callback
    private CameraWrapper.IPreviewCallback rgbFrameCallback = new CameraWrapper.IPreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            Log.d(TAG, "frame length = " + bytes.length);
            if (isSavePreview){
                isSavePreview = false;
                String dateTimeMs = DateUtil.getDateTimeMs();
                SaveFrameTask saveFrameTask = new SaveFrameTask(bytes,
                        new File(FileUtil.getExternalFolder(mContext, APP_FOLDER),  dateTimeMs + ".yuv"),
                        new File(FileUtil.getExternalFolder(mContext, APP_FOLDER), dateTimeMs + ".jpg"));
                new Thread(saveFrameTask).start();
            }

        }
    };
    //endregion


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraWrapper.closeCamera();
    }

    private void finishWithArgs(String msg, String path) {
        mIntent.putExtra(Constants.INTENT_KEY_RESULT, msg);
        setResult(RESULT_OK, mIntent);
        finish();
    }

    public void onClickCapture(View view) {
        if (!isSavePreview){
            isSavePreview = true;
        }
    }
}
