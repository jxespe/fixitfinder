package com.example.fixitfinderapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class FixItFinderApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (activity == null) {
                    return;
                }
                WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
                View root = activity.findViewById(android.R.id.content);
                if (root == null) {
                    return;
                }
                ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
                    Object tag = view.getTag(R.id.insets_initial_padding);
                    int[] base;
                    if (tag instanceof int[]) {
                        base = (int[]) tag;
                    } else {
                        base = new int[]{
                                view.getPaddingLeft(),
                                view.getPaddingTop(),
                                view.getPaddingRight(),
                                view.getPaddingBottom()
                        };
                        view.setTag(R.id.insets_initial_padding, base);
                    }
                    Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    Insets cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
                    view.setPadding(
                            base[0] + cutout.left,
                            base[1] + sysBars.top,
                            base[2] + cutout.right,
                            base[3] + sysBars.bottom
                    );
                    return insets;
                });
                ViewCompat.requestApplyInsets(root);
            }

            @Override public void onActivityStarted(Activity activity) { }
            @Override public void onActivityResumed(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
    }
}
