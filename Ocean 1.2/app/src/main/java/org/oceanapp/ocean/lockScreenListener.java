package org.oceanapp.ocean;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class lockScreenListener extends BroadcastReceiver {
    private String action = null;

    @Override
    public void onReceive(Context context, Intent intent){
        action = intent.getAction();
        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            Intent preBlockInt = new Intent(context, preBlockActivity.class);
            preBlockInt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(preBlockInt);
        }
    }
}
