package com.example.fixitfinderapp;

import android.os.Bundle;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

/**
 * Applies swipe-right (fling) to go back, including over scrollable content, by observing
 * touches in {@link #dispatchTouchEvent} before they are dispatched to child views.
 */
public abstract class BaseSwipeActivity extends AppCompatActivity {

    private GestureDetectorCompat swipeBackDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        swipeBackDetector = NavigationHelper.newSwipeBackGestureDetector(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (swipeBackDetector != null) {
            swipeBackDetector.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }
}
