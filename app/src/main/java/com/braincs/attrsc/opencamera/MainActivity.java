package com.braincs.attrsc.opencamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.braincs.attrsc.opencamera.utils.Constants;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private final String[] permissionList = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};//, Manifest.permission.CAMERA};
    private final int REQUEST_PERMISSION = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermissions();
    }

    //region permissions
    private void getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissionList, REQUEST_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_PERMISSION:
                if (grantResults.length > 0 ) {
                    //Permission was granted. Now you can call your method to open camera, fetch contact or whatever
                    int gsize = 0;
                    for (int g : grantResults) {
                        if (g == PackageManager.PERMISSION_GRANTED){
                            gsize++;
                        }
                    }
                    if (gsize == permissionList.length) {
                        // all permission granted
                        Log.d(TAG, "all permission granted");
//                        initAuthorizeSDK();
//                        startAuthorizationActivity();
                    }else{
                        Toast.makeText(this, "未获取所需权限", Toast.LENGTH_SHORT).show();
//                        tvResultMain.setText("未获取所需权限，请到手机配置中给与权限后再次尝试");
                    }

                } else {
                    // Permission was denied.......
                    // You can again ask for permission from here
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
//                    tvResultMain.setText("未获取所需权限，请到手机配置中给与权限后再次尝试");
                }
                break;
        }
    }
    public void startCamera0(View view) {
        Intent intent = new Intent(MainActivity.this, AndroidCameraActivity.class);
        intent.putExtra(Constants.INTENT_KEY_CAMERA_FRONT, false);
        startActivityForResult(intent, Constants.REQUEST_CODE);
    }

    public void startCamera1(View view) {
        Intent intent = new Intent(MainActivity.this, AndroidCameraActivity.class);
        intent.putExtra(Constants.INTENT_KEY_CAMERA_FRONT, true);
        startActivityForResult(intent, Constants.REQUEST_CODE);
    }


    public void startUvcCamera1(View view) {
        Intent intent = new Intent(MainActivity.this, UVCCameraActivity.class);
        intent.putExtra(Constants.INTENT_KEY_CAMERA_FRONT, false);
        startActivityForResult(intent, Constants.REQUEST_CODE);
    }


    public void startUvcCamera2(View view) {
        Intent intent = new Intent(MainActivity.this, UVCCameraActivity.class);
        intent.putExtra(Constants.INTENT_KEY_CAMERA_FRONT, true);
        startActivityForResult(intent, Constants.REQUEST_CODE);
    }

    public void startUVCTwoViewCamera(View view) {
        Intent intent = new Intent(MainActivity.this, UVCTwoViewActivity.class);
        intent.putExtra(Constants.INTENT_KEY_CAMERA_FRONT, false);
        startActivityForResult(intent, Constants.REQUEST_CODE);
    }

    public void startUVCAutoTwoViewCamera(View view) {
        Intent intent = new Intent(MainActivity.this, UVCAutoTwoViewActivity.class);
        intent.putExtra(Constants.INTENT_KEY_CAMERA_FRONT, false);
        startActivityForResult(intent, Constants.REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    String msg = data.getStringExtra(Constants.INTENT_KEY_RESULT);
//                    tvResultMain.setText(msg);
//                    String path = data.getStringExtra(Constants.INTENT_KEY_RESULT_PATH);
//                    LogUtils.d(TAG, msg + "\n" + path);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                } else {
//                    Toast.makeText(GuideActivity.this, "path: null", Toast.LENGTH_SHORT).show();
                }
        }
    }
}
