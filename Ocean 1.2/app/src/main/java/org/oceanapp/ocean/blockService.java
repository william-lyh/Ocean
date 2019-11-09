package org.oceanapp.ocean;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.VersionedPackage;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.widget.SimpleCursorAdapter;

import java.util.List;

public class blockService extends Service {
    private static blockService serInstance = null;
    private final int monitorInterval = 1000;
    private final int MSG_CHECK_FG_APP = 1;
    private int lockCIndex = 3;
    public static HandlerThread mHandlerThread;
    private Handler mHandler;
    static final String db_name = "blockDB";
    static final String tb_name = "appListTB";
    static final String[] FROM = new String[] {"_id", "name", "packname", "lock", "restlock"};
    SQLiteDatabase db;
    SimpleCursorAdapter adapter;
    Cursor cur;
    PowerManager powermanager;
    PowerManager.WakeLock wakelock;
    int sysVersion;
    lockScreenListener lsListener;
    Context serviceContext;
    public blockService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private String getTopApp(){
        UsageStatsManager topAppManager = (UsageStatsManager)getApplicationContext().getSystemService(Context.USAGE_STATS_SERVICE);
        long presentTime = System.currentTimeMillis();
        List<UsageStats> queryResult = topAppManager.queryUsageStats(topAppManager.INTERVAL_BEST,0,presentTime);
        if(queryResult == null || queryResult.isEmpty()){
            return null;
        }
        UsageStats topAppStat = null;
        for(UsageStats recentStat : queryResult)
        {
            if(topAppStat == null) {
                topAppStat = recentStat;
            }
            else if(topAppStat.getLastTimeUsed() < recentStat.getLastTimeUsed()){
                topAppStat = recentStat;
            }
        }
        return topAppStat.getPackageName();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        serInstance = this;
        serviceContext = this;
        powermanager = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        wakelock = powermanager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OceanTaskService:");
        wakelock.acquire();
        sysVersion = Build.VERSION.SDK_INT;
        String tasktype = intent.getExtras().getString("type");
        String taskid = intent.getExtras().getString("id");
        if(tasktype != null && tasktype.equals("relax")){
            lockCIndex = 4;
        }
        String CHANNEL_ID = "org.oceanapp.ocean";
        String CHANNEL_NAME = "BlockNoti_Channel";
        NotificationChannel notificationChannel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder notibuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, preBlockActivity.class), 0);
        notibuilder.setContentIntent(contentIntent);
        notibuilder.setSmallIcon(android.R.drawable.ic_lock_lock);
        notibuilder.setTicker("Start");
        notibuilder.setContentTitle("Ocean App");
        notibuilder.setContentText(tasktype + " task " + taskid);
        Notification notification = notibuilder.build();
        startForeground(3, notification);
        lsListener = new lockScreenListener();
        startReceiver();
        mHandlerThread = new HandlerThread("OceanTaskThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_CHECK_FG_APP:
                        String topApp = getTopApp();
                        if(shouldBeBlocked(topApp)){
                            Intent lockInt = new Intent(blockService.this, blockActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putParcelable("serviceint", intent);
                            lockInt.putExtras(bundle);
//                            lockInt.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                            lockInt.putExtra("packname", topApp);
                            lockInt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(lockInt);
                        }
                        break;
                }
                mHandler.sendEmptyMessageDelayed(MSG_CHECK_FG_APP, monitorInterval);
            }
        };
        mHandler.sendEmptyMessage(MSG_CHECK_FG_APP);
        return START_REDELIVER_INTENT;
    }

    private boolean shouldBeBlocked(String pName){
        if (pName == null || pName.equals("org.oceanapp.ocean")){
            return false;
        }
        db = openOrCreateDatabase(db_name, Context.MODE_PRIVATE, null);
        String createTable = "CREATE TABLE IF NOT EXISTS " + tb_name +
                "(_id VARCHAR(5), " +
                "name VARCHAR(32), " +
                "packname VARCHAR(64), " +
                "lock VARCHAR(3), " +
                "restlock VARCHAR(3))";
        db.execSQL(createTable);
        cur = db.rawQuery("SELECT * FROM " + tb_name, null);
        if(cur.getCount() != 0) {
            cur.moveToFirst();
            String firstPackName = cur.getString(cur.getColumnIndex(FROM[2]));
            if(firstPackName.equals(pName)){
                String firstLock = cur.getString(cur.getColumnIndex(FROM[lockCIndex]));
                if(firstLock.equals("yes")){
                    return true;
                }
            }
            while(cur.moveToNext()){
                String appPackName = cur.getString(cur.getColumnIndex(FROM[2]));
                if(appPackName.equals(pName)){
                    String appLock = cur.getString(cur.getColumnIndex(FROM[lockCIndex]));
                    if(appLock.equals("yes")){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onDestroy()
    {
        serInstance = null;
        super.onDestroy();
    }

    public static boolean isSerRunning(){
        if(serInstance == null){
            return false;
        }
        else
        {
            return true;
        }
    }
    private void startReceiver(){
        IntentFilter intFilter = new IntentFilter();
        intFilter.addAction(Intent.ACTION_SCREEN_OFF);
        serviceContext.registerReceiver(lsListener, intFilter);
    }
}
