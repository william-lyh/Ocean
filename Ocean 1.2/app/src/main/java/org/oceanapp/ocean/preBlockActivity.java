package org.oceanapp.ocean;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.system.Os;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class preBlockActivity extends AppCompatActivity {

    private static final int UsagestatsCode = 1101;
    final String blockdb_name = "blockDB";
    final String appListTB_name = "appListTB";
    final String idtb_name = "idTB";
    final String scheduleTB_name = "scheduleTB";
    final String[] FROM = new String[] {"_id", "name", "packname", "lock", "restlock"};
    final String[] FROMDisplay = new String[] {"name", "lock", "restlock"};
    final String[] FROMID = new String[] {"_id", "id"};
    final String[] FROMSCHEDULE = new String[]{"_id", "minute", "type"};
    final String[] FROMSCHEDULEDP = new String[]{"minute", "type"};
    String scheduleType;
    String[] searchArgs;
    public static SQLiteDatabase blockdb;
    SimpleCursorAdapter adapter;
    SimpleCursorAdapter scheduleAdapter;
    Cursor cur;
    Cursor idCur;
    Cursor searchCur;
    Cursor scheduleCur;
    ListView lv;
    ListView scheduleLv;
    int id;
    int scheduleId;
    int thisBlockId;
    ArrayList<String> packnamelist = new ArrayList<String>();
    Context preBlockContext;
    AlertDialog scheduleDialog;
    TextView blockStatusTV;
    Button preStartBtn;
    public static CountDownTimer serviceTimer;
    AdapterView.OnItemClickListener thislistener;
    public static preBlockActivity instance = null;
    PowerManager powermanager;
    PowerManager.WakeLock wakelock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_block);
        preBlockContext = this;
        instance = this;
        blockdb = openOrCreateDatabase(blockdb_name, Context.MODE_PRIVATE, null);
        String createTable = "CREATE TABLE IF NOT EXISTS " + appListTB_name +
                "(_id VARCHAR(5), " +
                "name VARCHAR(32), " +
                "packname VARCHAR(64), " +
                "lock VARCHAR(3), " +
                "restlock VARCHAR(3))";
        String createIDTable = "CREATE TABLE IF NOT EXISTS " + idtb_name +
                "(_id VARCHAR(1), " +
                "id VARCHAR(10))";
        blockdb.execSQL(createTable);
        blockdb.execSQL(createIDTable);
        cur = blockdb.rawQuery("SELECT * FROM " + appListTB_name, null);
        List<PackageInfo> allPacks =getPackageManager().getInstalledPackages(0);
        for(int i=0;i<allPacks.size();i++)
        {
            PackageInfo thisPackage;
            thisPackage = allPacks.get(i);
            if(!((thisPackage.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0)){
                if (!(thisPackage.packageName.equals("org.oceanapp.ocean"))) {
                    if (cur.getCount() == 0) {
                        addData(thisPackage.applicationInfo.loadLabel(getPackageManager()).toString(),
                                thisPackage.packageName, "yes", "yes");
                        packnamelist.add(thisPackage.packageName);
                    }
                    else {
                        searchArgs = new String[]{thisPackage.packageName};
                        searchCur = blockdb.rawQuery("SELECT * FROM " + appListTB_name +
                                " WHERE packname = ?", searchArgs);
                        if (searchCur.getCount() == 0) {
                            addData(thisPackage.applicationInfo.loadLabel(getPackageManager()).toString(),
                                    thisPackage.packageName, "yes", "yes");
                            packnamelist.add(thisPackage.packageName);
                        }
                    }
                }
            }
        }
        Context context = this;
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://www.oceanapp.org"));
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : list) {
            String packname = info.activityInfo.packageName;
            if(!(packnamelist.contains(packname))){
                try {
                    String appname = (String)packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(packname,PackageManager.GET_META_DATA));
                    if (cur.getCount() == 0) {
                        addData(appname,packname,"yes", "yes");
                        packnamelist.add(packname);
                    }
                    else {
                        searchArgs = new String[]{packname};
                        searchCur = blockdb.rawQuery("SELECT * FROM " + appListTB_name +
                                " WHERE packname = ?", searchArgs);
                        if (searchCur.getCount() == 0) {
                            addData(appname,packname,"yes", "yes");
                            packnamelist.add(packname);
                        }
                    }

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        cur = blockdb.rawQuery("SELECT * FROM " + appListTB_name, null);

        adapter = new SimpleCursorAdapter(this, R.layout.applistitem, cur,
                FROMDisplay,
                new int[]{R.id.nameView, R.id.statusView, R.id.restStatusView}, 0);
        lv = (ListView)findViewById(R.id.appListView);
        lv.setAdapter(adapter);
        thislistener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                cur.moveToPosition(position);
                AlertDialog.Builder maniListDialogB = new AlertDialog.Builder(preBlockContext);
                String appName = cur.getString(cur.getColumnIndex(FROM[1]));
                String appWorkLockDisp = "NA";
                String appRestLockDisp = "NA";
                String appWorkLock = cur.getString(cur.getColumnIndex(FROM[3]));
                if(appWorkLock.equals("yes")){
                    appWorkLockDisp = "Blocked";
                }
                else{
                    appWorkLockDisp = "Allowed";
                }
                String appRestLock = cur.getString(cur.getColumnIndex(FROM[4]));
                if(appRestLock.equals("yes")){
                    appRestLockDisp = "Blocked";
                }
                else{
                    appRestLockDisp = "Allowed";
                }
                maniListDialogB.setTitle("Change Block Setting");
                maniListDialogB.setMessage(appName + "\n\n" + "Working: " + appWorkLockDisp +
                                          "\n\nRelaxing: " + appRestLockDisp);
                if(appWorkLock.equals("yes")){
                    maniListDialogB.setPositiveButton("Allow while WORKING", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String appId = cur.getString(cur.getColumnIndex(FROM[0]));
                            ContentValues cv = new ContentValues();
                            cv.put("lock", "no");
                            blockdb.update(appListTB_name, cv, "_id=?", new String[]{appId});
                            requery();
                            Toast.makeText(preBlockContext, "Allowed while working!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else
                {
                    maniListDialogB.setPositiveButton("Block while WORKING", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String appId = cur.getString(cur.getColumnIndex(FROM[0]));
                            ContentValues cv = new ContentValues();
                            cv.put("lock", "yes");
                            blockdb.update(appListTB_name, cv, "_id=?", new String[]{appId});
                            requery();
                            Toast.makeText(preBlockContext, "Blocked while working!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                if(appRestLock.equals("yes")){
                    maniListDialogB.setNegativeButton("Allow while RELAXING", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String appId = cur.getString(cur.getColumnIndex(FROM[0]));
                            ContentValues cv = new ContentValues();
                            cv.put("restlock", "no");
                            blockdb.update(appListTB_name, cv, "_id=?", new String[]{appId});
                            requery();
                            Toast.makeText(preBlockContext, "Allowed while relaxing!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else{
                    maniListDialogB.setNegativeButton("Block while RELAXING", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String appId = cur.getString(cur.getColumnIndex(FROM[0]));
                            ContentValues cv = new ContentValues();
                            cv.put("restlock", "yes");
                            blockdb.update(appListTB_name, cv, "_id=?", new String[]{appId});
                            requery();
                            Toast.makeText(preBlockContext, "Blocked while relaxing!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                maniListDialogB.setNeutralButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(preBlockContext, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                });
                AlertDialog maniListDialog = maniListDialogB.create();
                maniListDialog.show();
            }
        };
        lv.setOnItemClickListener(thislistener);
        boolean serStatus = isServiceWorking();
        if(serStatus){
            preStartBtn = (Button) findViewById(R.id.startBtn);
            blockStatusTV = (TextView) findViewById(R.id.blockStatusView);
            preStartBtn.setVisibility(View.GONE);
            blockStatusTV.setVisibility(View.VISIBLE);
            lv.setOnItemClickListener(null);
        }
        powermanager = (PowerManager) getSystemService(POWER_SERVICE);
        wakelock = powermanager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag:");
    }

    private void addData(String name, String pname, String lock, String restLock) {
        String[] idArgs = new String[]{String.valueOf(0)};
        idCur = blockdb.rawQuery("SELECT * FROM " + idtb_name +" WHERE _id = ?", idArgs);
        if(idCur.getCount() == 0){
            ContentValues cv = new ContentValues(2);
            cv.put("_id", 0);
            cv.put("id", 0);
            blockdb.insert(idtb_name, null, cv);
            id = 0;
        }
        else
        {
            idCur.moveToFirst();
            id = idCur.getInt(idCur.getColumnIndex(FROMID[1]));
        }
        id += 1;
        ContentValues cv = new ContentValues(5);
        cv.put("_id", id);
        cv.put("name", name);
        cv.put("packname", pname);
        cv.put("lock", lock);
        cv.put("restlock", restLock);
        blockdb.insert(appListTB_name, null, cv);
        String[] idargs = new String[]{String.valueOf(0)};
        ContentValues idcv = new ContentValues();
        idcv.put("id", id);
        blockdb.update(idtb_name, idcv, "_id = ?", idargs);
    }

    private void requery(){
        cur=blockdb.rawQuery("SELECT * FROM " + appListTB_name, null);
        adapter.changeCursor(cur);
    }

    public void prestart(View v){
        if(perGranted()) {
            AlertDialog.Builder scheduleDialogB = new AlertDialog.Builder(preBlockContext);
            LayoutInflater inflator = LayoutInflater.from(preBlockContext);
            final View scheduleview = inflator.inflate(R.layout.scheduledialog, null);
            final EditText edminute;
            edminute = (EditText)scheduleview.findViewById(R.id.editMinute);
            scheduleDialogB.setView(scheduleview);
            blockdb.execSQL("DROP TABLE IF EXISTS " + scheduleTB_name);
            String createScheduleTable = "CREATE TABLE IF NOT EXISTS " + scheduleTB_name +
                    "(_id VARCHAR(5), " +
                    "minute VARCHAR(3), " +
                    "type VARCHAR(5))";
            blockdb.execSQL(createScheduleTable);
            scheduleId = 0;
            scheduleCur = blockdb.rawQuery("SELECT * FROM " + scheduleTB_name , null);
            scheduleAdapter = new SimpleCursorAdapter(this, R.layout.scheduleitem, scheduleCur,
                    FROMSCHEDULEDP,
                    new int[]{R.id.minuteView, R.id.typeView}, 0);
            scheduleLv = (ListView) scheduleview.findViewById(R.id.ScheduleLV);
            scheduleLv.setAdapter(scheduleAdapter);
            final TextView noItemTVO = (TextView) scheduleview.findViewById(R.id.noItemTV);
            noItemTVO.setVisibility(View.VISIBLE);
            Button addScheduleBtnO = (Button) scheduleview.findViewById(R.id.addScheduleBtn);
            addScheduleBtnO.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isInteger(edminute.getText().toString())
                            && Integer.valueOf(edminute.getText().toString()) <= 100){
                        String minuteAddedStr = edminute.getText().toString();
                        int minuteadded = Integer.valueOf(minuteAddedStr);
                        RadioGroup typeRGroup = (RadioGroup) scheduleview.findViewById(R.id.typeRadioGroup);
                        if(typeRGroup.getCheckedRadioButtonId() == R.id.relaxRBtn){
                            scheduleType = "relax";
                        }
                        else
                        {
                            scheduleType = "work";
                        }
                        scheduleId += 1;
                        ContentValues scheduleCV = new ContentValues(2);
                        scheduleCV.put("_id", scheduleId);
                        scheduleCV.put("minute", minuteadded);
                        scheduleCV.put("type", scheduleType);
                        blockdb.insert(scheduleTB_name, null, scheduleCV);
                        scheduleCur=blockdb.rawQuery("SELECT * FROM " + scheduleTB_name, null);
                        scheduleAdapter.changeCursor(scheduleCur);
                        noItemTVO.setVisibility(View.GONE);
                    }
                }
            });
            Button startBlockBtnO = (Button) scheduleview.findViewById(R.id.startBtn);
            startBlockBtnO.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startService();
                    scheduleDialog.dismiss();
                }
            });
            scheduleDialog = scheduleDialogB.create();
            scheduleDialog.show();
        }
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !perGranted()){
            startActivityForResult(
                    new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                    UsagestatsCode);
        }

    }

    public boolean isInteger(String s){
        try {
            Integer.parseInt(s);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    public void startService(){
        scheduleCur = blockdb.rawQuery("SELECT * FROM " + scheduleTB_name , null);
        if(scheduleCur.moveToFirst()){
            if(!wakelock.isHeld()) {
                wakelock.acquire();
            }
            preStartBtn = (Button) findViewById(R.id.startBtn);
            blockStatusTV = (TextView) findViewById(R.id.blockStatusView);
            preStartBtn.setVisibility(View.GONE);
            blockStatusTV.setVisibility(View.VISIBLE);
            lv.setOnItemClickListener(null);
            thisBlockId = scheduleCur.getInt(scheduleCur.getColumnIndex(FROMSCHEDULE[0]));
            int thisBlockTime = scheduleCur.getInt(scheduleCur.getColumnIndex("minute"))*60*1000;
            final String thisblocktype = scheduleCur.getString(scheduleCur.getColumnIndex(FROMSCHEDULEDP[1]));
            final Intent blockServiceInt = new Intent(preBlockContext, blockService.class);
            blockServiceInt.putExtra("type", thisblocktype);
            blockServiceInt.putExtra("id", String.valueOf(thisBlockId));
            startService(blockServiceInt);
            serviceTimer = new CountDownTimer(thisBlockTime, 60000){
                public void onTick(long miliisUntilFinished){
                    long minUntilFinish = miliisUntilFinished/60000 + 1;
                    String outputText = "Task " + thisblocktype + " " + String.valueOf(thisBlockId)
                            + ": " + minUntilFinish + " min left";
                    blockStatusTV.setText(outputText);
                }
                public void onFinish() {
                    stopService(blockServiceInt);
                    String[] deleteArgs = new String[]{String.valueOf(thisBlockId)};
                    blockdb.delete(scheduleTB_name, "_id = ?", deleteArgs);
                    preStartBtn.setVisibility(View.VISIBLE);
                    blockStatusTV.setVisibility(View.GONE);
                    lv.setOnItemClickListener(thislistener);
                    startService();
                }
            }.start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UsagestatsCode) {
            if (!perGranted()) {
                startActivityForResult(
                        new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                        UsagestatsCode);
            }
        }
    }
    private boolean perGranted() {
        AppOpsManager appOps = (AppOpsManager)
                getSystemService(Context.APP_OPS_SERVICE);
        int mode = 0;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName());
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public boolean isServiceWorking(){
        return blockService.isSerRunning();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK){
            moveTaskToBack(true);
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDestroy()
    {
        instance = null;
        blockdb.close();
        super.onDestroy();
    }
}
