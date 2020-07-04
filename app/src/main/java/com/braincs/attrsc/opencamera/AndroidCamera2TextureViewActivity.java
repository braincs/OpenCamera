package com.braincs.attrsc.opencamera;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.braincs.attrsc.opencamera.camera2.Camera2WrapperImpl;
import com.braincs.attrsc.opencamera.camera2.CameraWrapper;
import com.braincs.attrsc.opencamera.utils.Constants;
import com.braincs.attrsc.opencamera.utils.ScreenUtils;

/**
 * Created by Shuai
 * 27/03/2020.
 */
public class AndroidCamera2TextureViewActivity extends AppCompatActivity {
    private final static String TAG = AndroidCamera2TextureViewActivity.class.getSimpleName();

    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private Context mContext;
    private Intent mIntent;
    private boolean isFront;


    private Drawable mSuccDrawable, mFailDrawable;
    private TextView mTvCheckResult;
    private RelativeLayout rlCameraGroupView;
    private CameraWrapper mCameraWrapper;
    private boolean mIsCameraOpened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_android_camera);
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
        mTextureView = findViewById(R.id.texture_cam_preview);
        mTextureView.setSurfaceTextureListener(mCameraTextureListener);

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
            Log.d(TAG,"onOpenSuccess()..." );
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
                " mSurfaceTexture:" + mSurfaceTexture);
        if (mIsCameraOpened && null != mSurfaceTexture) {
            // set orientation
            mCameraWrapper.setDisplayOrientation(Constants.ORIENTATION_90);

            // start preview
            mCameraWrapper.startPreview(mSurfaceTexture);
            mCameraWrapper.setFrameCallback(rgbFrameCallback);
        }
    }

    TextureView.SurfaceTextureListener mCameraTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable()...rgb w = " + width + ", h = " + height);

//            rgbCameraWidth = width + Constants.CAMERA_OFFSET_WIDTH;
//            rgbCameraHeight = height + Constants.CAMERA_OFFSET_HEIGHT;
            mSurfaceTexture = surface;
            checkAndStartPreview();

//            openCamera(surface, Constants.CameraIDRGB, 0);
//            mCamera = openCamera(surface, Constants.CameraIDRGB, 0, rgbFrameCallback);

//            Log.i(TAG, "Camera-1 opened..");
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(TAG, "onSurfaceTextureDestroyed()...rgb");
//            destroyCamera();
//                mPresenter.onDestroy();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    //endregion


    @Override
    protected void onDestroy() {
        mCameraWrapper.closeCamera();
        super.onDestroy();
    }

    //region Camera callback
    private CameraWrapper.IPreviewCallback rgbFrameCallback = new CameraWrapper.IPreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            Log.d(TAG, "frame length = " + bytes.length);

        }
    };
    //endregion


    private void finishWithArgs(String msg, String path) {
        mIntent.putExtra(Constants.INTENT_KEY_RESULT, msg);
        setResult(RESULT_OK, mIntent);
        finish();
    }
}
