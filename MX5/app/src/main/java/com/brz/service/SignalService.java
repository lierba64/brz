package com.brz.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;

import com.brz.http.service.TerminalService;
import com.brz.mx5.R;

/**
 * Created by macro on 16/7/18.
 */
public class SignalService extends Service {

    private static final int NOTIFICATION_ID = 1000;
    private TerminalService mService;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private static final String TAG = "SignalService";
    private static final int MSG_HEARTBEAT = 0x1000;
    private static final String EXTRA_PARAM_TERM_ID = "extra_param_term_id";
    private String mTermId;

    @Override
    public void onCreate() {
        super.onCreate();

        mHandlerThread = new HandlerThread("signal_thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_HEARTBEAT:
                        break;
                    default:break;
                }
            }
        };

        mService = TerminalService.getInstance();

        // 设置成前台服务
        Notification.Builder builder = new Notification.Builder(this);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                SignalService.class), 0);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setTicker("Foreground SignalService Start");
        builder.setContentTitle("Foreground SignalService");
        builder.setContentText("Make this service run in the foreground.");
        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHandlerThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mHandlerThread.quitSafely();
            } else {
                mHandlerThread.quit();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mTermId = intent.getStringExtra(EXTRA_PARAM_TERM_ID);
            mHandler.sendEmptyMessage(MSG_HEARTBEAT);
        }

        return START_STICKY;
    }

    private void sendMsg(int what, Object o) {
        Message message = mHandler.obtainMessage();
        message.what = what;
        message.obj = o;
        message.sendToTarget();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void doHeartBeat() {
//        mService.heartBeat();
    }
}