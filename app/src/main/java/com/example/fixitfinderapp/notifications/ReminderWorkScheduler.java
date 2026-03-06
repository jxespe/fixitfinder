package com.example.fixitfinderapp.notifications;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class ReminderWorkScheduler {

    private static final String UNIQUE_WORK_NAME = "booking_reminder_worker";

    private ReminderWorkScheduler() {
    }

    public static void schedule(Context context) {
        if (context == null) {
            return;
        }
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ReminderWorker.class,
                15, TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        UNIQUE_WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        request
                );
    }
}
