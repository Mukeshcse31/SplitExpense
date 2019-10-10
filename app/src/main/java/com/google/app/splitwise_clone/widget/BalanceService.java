package com.google.app.splitwise_clone.widget;

/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.google.app.splitwise_clone.R;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class BalanceService extends IntentService {

    public static final String UPDATE_WIDGETS = "com.google.app.splitwise_clone.action.update_widgets";

    public BalanceService() {
        super("BalanceService");
    }

    /**
     * Starts this service to perform UpdatePlantWidgets action with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUpdateWidgets(Context context) {
        Intent intent = new Intent(context, BalanceService.class);
        intent.setAction(UPDATE_WIDGETS);
        context.startService(intent);
    }

    /**
     * @param intent
     */

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (UPDATE_WIDGETS.equals(action)) {
                handleActionUpdatePlantWidgets();
            }
        }
    }

    // Handle action UpdatePlantWidgets in the provided background thread
    private void handleActionUpdatePlantWidgets() {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, BalanceWidgetProvider.class));
        //Trigger data update to handle the GridView widgets and force a data refresh
//        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_grid_view);
        //Now update all widgets
        BalanceWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetIds);
    }

}
