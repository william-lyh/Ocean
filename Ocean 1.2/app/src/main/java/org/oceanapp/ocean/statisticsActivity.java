package org.oceanapp.ocean;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

public class statisticsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    ProgressBar loadingPB;
    final String db_name = "agendaDB";
    final String worktb_name = "workTB";
    final String runtb_name = "runTB";
    final String[] FROM = new String[] {"_id", "agenda",  "year",  "month",  "day", "status"};
    SQLiteDatabase db;
    Cursor workCur;
    Cursor runCur;
    String[] allArgs;
    String[] finishedArgs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        loadingPB = (ProgressBar) findViewById(R.id.indeterminateBar);
        loadingPB.setVisibility(View.GONE);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        db = openOrCreateDatabase(db_name, Context.MODE_PRIVATE, null);
        String createWorkTable = "CREATE TABLE IF NOT EXISTS " + worktb_name +
                "(_id VARCHAR(60), " +
                "agenda VARCHAR(50), " +
                "year VARCHAR(4), " +
                "month VARCHAR(2), " +
                "day VARCHAR(2), " +
                "status VARCHAR(1))";
        String createRunTable = "CREATE TABLE IF NOT EXISTS " + runtb_name +
                "(_id VARCHAR(60), " +
                "agenda VARCHAR(50), " +
                "year VARCHAR(4), " +
                "month VARCHAR(2), " +
                "day VARCHAR(2), " +
                "status VARCHAR(1))";
        db.execSQL(createWorkTable);
        db.execSQL(createRunTable);
        workCur = db.rawQuery("SELECT * FROM " + worktb_name, null);
        runCur = db.rawQuery("SELECT * FROM " + runtb_name, null);
        int totalAgendaNum = workCur.getCount() + runCur.getCount();
        finishedArgs = new String[]{"Done"};
        workCur = db.rawQuery("SELECT * FROM " + worktb_name + " WHERE status = ?", finishedArgs);
        runCur = db.rawQuery("SELECT * FROM " + runtb_name + " WHERE status = ?", finishedArgs);
        int totalFinishedNum = workCur.getCount() + runCur.getCount();
        String statisticsDisplay = "Total Agenda Added: " + String.valueOf(totalAgendaNum) +
                                   "\nTotal Agenda Finished: "+ String.valueOf(totalFinishedNum);
        TextView statisticsViewO = (TextView)findViewById(R.id.statisticsView);
        statisticsViewO.setText(statisticsDisplay);
        db.close();
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
//        getMenuInflater().inflate(R.menu.statistics, menu);
//        return true;
//    }

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
    protected void onResume() {
        super.onResume();
        loadingPB.setVisibility(View.GONE);
    }
}
