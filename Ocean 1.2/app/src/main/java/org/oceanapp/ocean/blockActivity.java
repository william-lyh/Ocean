package org.oceanapp.ocean;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class blockActivity extends AppCompatActivity {
    Intent serviceint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block);
        Bundle bundle = getIntent().getExtras();
        serviceint = bundle.getParcelable("serviceint");
    }

    public void stopService(View v){
        AlertDialog.Builder confirmStopBuilder = new AlertDialog.Builder(this);
        confirmStopBuilder.setTitle("Confirm Stopping");
        confirmStopBuilder.setMessage("Do you really want to stop working right now?");
        confirmStopBuilder.setPositiveButton("No", null);
        confirmStopBuilder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                blockService.mHandlerThread.getLooper().quit();
                stopService(serviceint);
                preBlockActivity.serviceTimer.cancel();
                preBlockActivity.instance.finish();
                finish();
            }
        });
        AlertDialog confirmStopDialog = confirmStopBuilder.create();
        confirmStopDialog.show();
    }
}
