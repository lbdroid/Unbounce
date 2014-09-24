package com.ryansteckler.nlpunbounce;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ryansteckler.nlpunbounce.models.BaseStats;
import com.ryansteckler.nlpunbounce.models.InterimWakelock;
import com.ryansteckler.nlpunbounce.models.UnbounceStatsCollection;
import com.ryansteckler.nlpunbounce.models.WakelockStats;

import java.util.HashMap;

public class XposedReceiver extends BroadcastReceiver {

    public XposedReceiver() {
    }

    public final static String RESET_ACTION = "com.ryansteckler.nlpunbounce.RESET_STATS";
    public final static String REFRESH_ACTION = "com.ryansteckler.nlpunbounce.REFRESH_STATS";
    public final static String STAT_NAME = "stat_name";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("com.ryansteckler.nlpunbounce.RESET_STATS")) {
            String statName = intent.getStringExtra(STAT_NAME);
            UnbounceStatsCollection collection = UnbounceStatsCollection.getInstance();
            if (statName == null) {
                collection.resetStats(context);
            } else {
                collection.resetStats(context, statName);
            }
        } else if (action.equals("com.ryansteckler.nlpunbounce.REFRESH_STATS")) {
//            UnbounceStatsCollection collection = UnbounceStatsCollection.getInstance();
//            collection.resetStats(context);
        }
    }
}
