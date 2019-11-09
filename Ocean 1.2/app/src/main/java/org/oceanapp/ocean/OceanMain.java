package org.oceanapp.ocean;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;


public class OceanMain extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        DatePickerDialog.OnDateSetListener, AdapterView.OnItemClickListener{

    Button pickDateBtnO;
    ProgressBar loadingPB;
    int year;
    int month;
    int day;
    int id;
    final String db_name = "agendaDB";
    final String worktb_name = "workTB";
    final String runtb_name = "runTB";
    final String idtb_name = "idTB";
    final String[] FROMDisplay = new String[] {"agenda", "status"};
    final String[] FROM = new String[] {"_id", "agenda",  "year",  "month",  "day", "status"};
    final String[] FROMID = new String[] {"_id", "id"};
    SQLiteDatabase db;
    SimpleCursorAdapter workAdapter;
    Cursor workCur;
    Cursor workDPCur;
    SimpleCursorAdapter runAdapter;
    Cursor runCur;
    Cursor runDPCur;
    Cursor idCur;
    ListView workLV;
    ListView runLV;
    TextView noWorkAgendaO;
    TextView noRunAgendaO;
    Context mainContext;
    String[] dateArgs;
    String[] onclickArgs;
    String tableName;
    String agendaAdded;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocean_main);
        loadingPB = (ProgressBar) findViewById(R.id.indeterminateBar);
        loadingPB.setVisibility(View.GONE);
        noWorkAgendaO = (TextView)findViewById(R.id.noWorkAgenda);
        noRunAgendaO = (TextView)findViewById(R.id.noRunAgenda);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.addAgendaBtn);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addAgenda();
            }
        });

        mainContext = this;
        pickDateBtnO = (Button)findViewById(R.id.pickDateBtn);
        String date;
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        date = String.valueOf(month+1) + "/" +
                String.valueOf(day) + "/" +
                String.valueOf(year);
        pickDateBtnO.setText(date);
        db = openOrCreateDatabase(db_name, Context.MODE_PRIVATE, null);
        String createWorkTable = "CREATE TABLE IF NOT EXISTS " + worktb_name +
                "(_id VARCHAR(60), " +
                "agenda VARCHAR(50), " +
                "year VARCHAR(4), " +
                "month VARCHAR(2), " +
                "day VARCHAR(2), " +
                "status VARCHAR(4))";
        String createRunTable = "CREATE TABLE IF NOT EXISTS " + runtb_name +
                "(_id VARCHAR(60), " +
                "agenda VARCHAR(50), " +
                "year VARCHAR(4), " +
                "month VARCHAR(2), " +
                "day VARCHAR(2), " +
                "status VARCHAR(4))";
        String createIDTable = "CREATE TABLE IF NOT EXISTS " + idtb_name +
                "(_id VARCHAR(1), " +
                "id VARCHAR(60))";
        db.execSQL(createWorkTable);
        db.execSQL(createRunTable);
        db.execSQL(createIDTable);
        workCur = db.rawQuery("SELECT * FROM " + worktb_name, null);
        runCur = db.rawQuery("SELECT * FROM " + runtb_name, null);
        dateArgs = new String[]{String.valueOf(year), String.valueOf(month), String.valueOf(day)};
        workDPCur = db.rawQuery("SELECT * FROM " + worktb_name +
                " WHERE year = ? AND month = ? AND day = ?", dateArgs);
        runDPCur = db.rawQuery("SELECT * FROM " + runtb_name +
                " WHERE year = ? AND month = ? AND day = ?", dateArgs);
        if(workDPCur.getCount() == 0){
            noWorkAgendaO.setVisibility(View.VISIBLE);
        }
        else{
            noWorkAgendaO.setVisibility(View.GONE);
        }
        if(runDPCur.getCount() == 0){
            noRunAgendaO.setVisibility(View.VISIBLE);
        }
        else{
            noRunAgendaO.setVisibility(View.GONE);
        }
        workAdapter = new SimpleCursorAdapter(this, R.layout.agendaitem, workDPCur, FROMDisplay,
                new int[]{R.id.agenda, R.id.status}
                , 0);
        runAdapter = new SimpleCursorAdapter(this, R.layout.agendaitem, runDPCur, FROMDisplay,
                new int[]{R.id.agenda, R.id.status}
                , 0);
        workLV = (ListView)findViewById(R.id.workAgendaLv);
        runLV = (ListView)findViewById(R.id.exerciseAgendaLv);
        workLV.setAdapter(workAdapter);
        runLV.setAdapter(runAdapter);
        workLV.setOnItemClickListener(this);
        runLV.setOnItemClickListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.ocean_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_agenda) {
            // Handle the camera action
            Intent newInt = new Intent(this, OceanMain.class);
            startActivity(newInt);
        } else if (id == R.id.nav_startwork) {
            loadingPB.setVisibility(View.VISIBLE);
            Intent newInt = new Intent(this, preBlockActivity.class);
            startActivity(newInt);
        } else if (id == R.id.nav_statistics) {
            Intent newInt = new Intent(this, statisticsActivity.class);
            startActivity(newInt);
        }
        else if (id == R.id.nav_about) {
            Intent newInt = new Intent(this, aboutActivity.class);
            startActivity(newInt);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        requery();
        if(parent==workLV){
            workCur.moveToPosition(position);
            AlertDialog.Builder clickdialog = new AlertDialog.Builder(this);
            int posid = workCur.getInt(workCur.getColumnIndex(FROM[0]));
            onclickArgs = new String[]{String.valueOf(posid)};
            String agendaclicked = workCur.getString(workCur.getColumnIndex(FROM[1]));
            String statusclicked = workCur.getString(workCur.getColumnIndex(FROM[5]));
            clickdialog.setTitle("Action");
            clickdialog.setMessage(agendaclicked);
            if(statusclicked.equals("ToDo")){
                clickdialog.setPositiveButton("MARK AS FINISHED", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ContentValues cv = new ContentValues();
                        cv.put("status", "Done");
                        db.update(worktb_name, cv, "_id = ?", onclickArgs);
                        queryanddisplay();
                        Toast.makeText(mainContext, "Finished!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else{
                clickdialog.setPositiveButton("MARK AS UNFINISHED", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ContentValues cv = new ContentValues();
                        cv.put("status", "ToDo");
                        db.update(worktb_name, cv, "_id = ?", onclickArgs);
                        queryanddisplay();
                        Toast.makeText(mainContext, "Unfinished!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            clickdialog.setNegativeButton("DELETE ANGENDA", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    db.delete(worktb_name, "_id = ?", onclickArgs);
                    queryanddisplay();
                    Toast.makeText(mainContext, "Deleted!", Toast.LENGTH_SHORT).show();
                }
            });
            clickdialog.setNeutralButton("RETURN", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            AlertDialog cdialog = clickdialog.create();
            cdialog.show();
        }
        else if(parent==runLV){
            AlertDialog.Builder clickdialog = new AlertDialog.Builder(this);
            runCur.moveToPosition(position);
            int posid = runCur.getInt(runCur.getColumnIndex(FROM[0]));
            onclickArgs = new String[]{String.valueOf(posid)};
            String agendaclicked = runCur.getString(runCur.getColumnIndex(FROM[1]));
            String statusclicked = runCur.getString(workCur.getColumnIndex(FROM[5]));
            clickdialog.setTitle("Action");
            clickdialog.setMessage(agendaclicked);
            if(statusclicked.equals("ToDo")){
                clickdialog.setPositiveButton("MARK AS FINISHED", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ContentValues cv = new ContentValues();
                        cv.put("status", "Done");
                        db.update(runtb_name, cv, "_id = ?", onclickArgs);
                        queryanddisplay();
                        Toast.makeText(mainContext, "Finished!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else{
                clickdialog.setPositiveButton("MARK AS UNFINISHED", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ContentValues cv = new ContentValues();
                        cv.put("status", "ToDo");
                        db.update(runtb_name, cv, "_id = ?", onclickArgs);
                        queryanddisplay();
                        Toast.makeText(mainContext, "Unfinished!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            clickdialog.setNegativeButton("DELETE ANGENDA", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    db.delete(runtb_name, "_id = ?", onclickArgs);
                    queryanddisplay();
                    Toast.makeText(mainContext, "Deleted!", Toast.LENGTH_SHORT).show();
                }
            });
            clickdialog.setNeutralButton("RETURN", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            AlertDialog cdialog = clickdialog.create();
            cdialog.show();
        }
    }

    public void pickDay(View v){
        DatePickerDialog pickdatedialog = new DatePickerDialog(this, OceanMain.this, year, month, day);
        pickdatedialog.show();
    }

    @Override
    public void onDateSet(DatePicker v, int y, int m, int d){
        year = y;
        month = m;
        day = d;
        String newdate = String.valueOf(month+1) + "/" +
                String.valueOf(day) + "/" +
                String.valueOf(year);
        pickDateBtnO.setText(newdate);
        queryanddisplay();
    }

    private void addData(String agenda, int year, int month, int day, String status, String tb_name) {
        String[] idArgs = new String[]{String.valueOf(0)};
        idCur = db.rawQuery("SELECT * FROM " + idtb_name +" WHERE _id = ?", idArgs);
        if(idCur.getCount() == 0){
            ContentValues cv = new ContentValues(2);
            cv.put("_id", 0);
            cv.put("id", 0);
            db.insert(idtb_name, null, cv);
            id = 0;
        }
        else
        {
            idCur.moveToFirst();
            id = idCur.getInt(idCur.getColumnIndex(FROMID[1]));
        }
        id += 1;
        ContentValues cv = new ContentValues(6);
        cv.put("_id", id);
        cv.put("agenda", agenda);
        cv.put("year", year);
        cv.put("month", month);
        cv.put("day", day);
        cv.put("status", status);
        db.insert(tb_name, null, cv);
        String[] idargs = new String[]{String.valueOf(0)};
        ContentValues idcv = new ContentValues();
        idcv.put("id", id);
        db.update(idtb_name, idcv, "_id = ?", idargs);
    }

    private void requery(){
        workCur = db.rawQuery("SELECT * FROM " + worktb_name +" WHERE year = ? AND month = ? AND day = ?", dateArgs);
        runCur = db.rawQuery("SELECT * FROM " + runtb_name +" WHERE year = ? AND month = ? AND day = ?", dateArgs);
    }

    public void cleardisplay(){
        workLV.setAdapter(null);
        runLV.setAdapter(null);
    }

    private void queryanddisplay(){
        cleardisplay();
        dateArgs = new String[]{String.valueOf(year), String.valueOf(month), String.valueOf(day)};
        workDPCur = db.rawQuery("SELECT * FROM " + worktb_name +" WHERE year = ? AND month = ? AND day = ?", dateArgs);
        runDPCur = db.rawQuery("SELECT * FROM " + runtb_name +" WHERE year = ? AND month = ? AND day = ?", dateArgs);
        if(workDPCur.getCount() == 0){
            noWorkAgendaO.setVisibility(View.VISIBLE);
        }
        else{
            noWorkAgendaO.setVisibility(View.GONE);
        }
        if(runDPCur.getCount() == 0){
            noRunAgendaO.setVisibility(View.VISIBLE);
        }
        else{
            noRunAgendaO.setVisibility(View.GONE);
        }
        workAdapter.changeCursor(workDPCur);
        runAdapter.changeCursor(runDPCur);
        workLV.setAdapter(workAdapter);
        runLV.setAdapter(runAdapter);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        String[] idargs = new String[]{String.valueOf(0)};
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        db.update(idtb_name, cv, "_id = ?", idargs);
        cleardisplay();
        db.close();
    }

    public void addAgenda(){
        AlertDialog.Builder tbbuilder = new AlertDialog.Builder(this);
        tbbuilder.setTitle("Choose Type");
        final String[] typelist = new String[]{"work", "exercise"};
        tableName = worktb_name;
        tbbuilder.setSingleChoiceItems(typelist, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String choice = typelist[which];
                if(choice.equals("work")){
                    tableName = worktb_name;
                }
                else if(choice.equals("exercise")){
                    tableName = runtb_name;
                }
            }
        });
        tbbuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(mainContext, "CANCELLED", Toast.LENGTH_SHORT).show();
            }
        });
        tbbuilder.setPositiveButton("CONTINUE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AlertDialog.Builder addbuilder = new AlertDialog.Builder(mainContext);
                LayoutInflater inflator = LayoutInflater.from(mainContext);
                View addview = inflator.inflate(R.layout.adddialog, null);
                final EditText edagenda;
                edagenda = (EditText)addview.findViewById(R.id.editAgenda);
                addbuilder.setView(addview);
                addbuilder.setTitle("ADD AGENDA");
                addbuilder.setPositiveButton("ADD", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        agendaAdded = edagenda.getText().toString().trim();
                        DatePickerDialog pickdatedialog = new DatePickerDialog(mainContext, new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                addData(agendaAdded, year, month, dayOfMonth, "ToDo", tableName);
                                setDate(year, month, dayOfMonth);
                                String date = String.valueOf(month+1) + "/" +
                                        String.valueOf(day) + "/" +
                                        String.valueOf(year);
                                pickDateBtnO.setText(date);
                                queryanddisplay();
                            }
                        }, year, month, day);
                        pickdatedialog.show();
                    }
                });
                addbuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(mainContext, "CANCELLED", Toast.LENGTH_SHORT).show();
                    }
                });
                final Dialog adddialog = addbuilder.create();
                dialog.dismiss();
                adddialog.show();
            }
        });
        AlertDialog tbdialog = tbbuilder.create();
        tbdialog.show();
    }

    public void setDate(int y, int m, int d){
        year = y;
        month = m;
        day = d;
    }

    public void beginpreBlock(View v)
    {
        loadingPB.setVisibility(View.VISIBLE);
        Intent preBlockInt = new Intent(mainContext, preBlockActivity.class);
        startActivity(preBlockInt);
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadingPB.setVisibility(View.GONE);
    }
}
