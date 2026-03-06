package com.example.fixitfinderapp.notifications;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fixitfinderapp.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return Result.success();
        }
        String role = SessionManager.getRole(getApplicationContext());
        if ("provider".equalsIgnoreCase(role)) {
            ReminderScheduler.scheduleProviderAcceptedReminders(getApplicationContext(), user.getUid());
        } else {
            ReminderScheduler.scheduleAcceptedReminders(getApplicationContext(), user.getUid());
        }
        return Result.success();
    }
}
