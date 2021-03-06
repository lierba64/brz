package com.brz.mx5;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.brz.basic.Basic;
import com.brz.basic.IntentActions;
import com.brz.fragment.AuthDialogFragment;
import com.brz.fragment.SettingDialogFragment;
import com.brz.imageloader.ImageCache;
import com.brz.imageloader.ImageResizer2;

public class FullscreenActivity extends PermissionsActivity implements AuthDialogFragment.NoticeDialogListener {
    private static final String TAG = "FullscreenActivity";

    private final static int DEFAULT_IMAGE_WIDTH = 640;
    private final static int DEFAULT_IMAGE_HEIGHT = 480;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, IntentActions.ACTION_UPDATE_PROGRAMME)) {
                Log.d(TAG, "update programme");
                mDisplayManager.updateProgramme();
            }
        }
    };
    private static final int REQUEST_PERMISSION_RWS = 0x1001;
    private DisplayManager mDisplayManager;
    private ResourceManager mResourceManager;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private ImageResizer2 mImageResizer2;
    private static FullscreenActivity mInstance;
    private boolean mAllowQuit = false;
    private Rect mScreenLeft = new Rect(0, Basic.SCREEN_HEIGHT - 200, 200, Basic.SCREEN_HEIGHT);

    public static FullscreenActivity getIntance() {
        return mInstance;
    }

    public ImageResizer2 getImageLoader() {
        if (mImageResizer2 == null) {
            mImageResizer2 = new ImageResizer2(this, DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);
            mImageResizer2.addImageCache(getSupportFragmentManager(), new ImageCache.ImageCacheParams(this, "image_disk_cache"));
        }

        return mImageResizer2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        mInstance = this;

        // 获取屏幕大小
        Basic.SCREEN_WIDTH = getResources().getDisplayMetrics().widthPixels;
        Basic.SCREEN_HEIGHT = getResources().getDisplayMetrics().heightPixels;
        Log.d(TAG, "width: " + Basic.SCREEN_WIDTH);
        Log.d(TAG, "height: " + Basic.SCREEN_HEIGHT);

        mContentView = findViewById(R.id.fullscreen_content);
        hide();

        IntentFilter filter = new IntentFilter(IntentActions.ACTION_UPDATE_PROGRAMME);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter);

        if (Build.VERSION.SDK_INT < 23) {
            mResourceManager = ResourceManager.getInstance();
            if (mResourceManager.init()) mDisplayManager = new DisplayManager(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= 23) {
            requestPermission(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_PERMISSION_RWS);
        }
    }

    @Override
    public void permissionSuccess(int requestCode) {
        super.permissionSuccess(requestCode);
        switch (requestCode) {
            case REQUEST_PERMISSION_RWS:
                mResourceManager = ResourceManager.getInstance();
                if (mResourceManager.init()) mDisplayManager = new DisplayManager(this);
                break;
            default:
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // 获取屏幕大小
        Basic.SCREEN_WIDTH = getResources().getDisplayMetrics().widthPixels;
        Basic.SCREEN_HEIGHT = getResources().getDisplayMetrics().heightPixels;
        Log.d(TAG, "width: " + Basic.SCREEN_WIDTH);
        Log.d(TAG, "height: " + Basic.SCREEN_HEIGHT);


        mDisplayManager.onConfigurationChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mDisplayManager != null)
            mDisplayManager.display();
        else {
            mContentView.setBackgroundResource(R.drawable.default_bg);
        }
    }

    @Override
    public void onBackPressed() {
        if (!mAllowQuit) {
            showAuthDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void showSettingsDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SettingDialogFragment sf = SettingDialogFragment.newInstance();
        sf.show(ft, "SETTINGS");
    }

    private void showAuthDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        AuthDialogFragment fragment = AuthDialogFragment.newInstance();
        fragment.show(ft, "AUTH");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int x = (int) event.getRawX();
                int y = (int) event.getRawY();
                Log.d(TAG, "ACTION_DOWN: " + x + " " + y);
                if (mScreenLeft.contains(x, y)) {
                    showSettingsDialog();
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "ACTION_UP");
                break;
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mDisplayManager != null)
            mDisplayManager.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mImageResizer2 != null) {
            mImageResizer2.clearCache();
            mImageResizer2.closeCache();
            mImageResizer2 = null;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        mHideHandler.post(mHidePart2Runnable);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        mAllowQuit = true;
        onBackPressed();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

    }
}
