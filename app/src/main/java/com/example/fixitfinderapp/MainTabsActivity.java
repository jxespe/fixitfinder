package com.example.fixitfinderapp;

import android.os.Bundle;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.example.fixitfinderapp.notifications.BookingUpdateListener;
import com.example.fixitfinderapp.notifications.PendingFcmNotificationQueue;

public class MainTabsActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_TAB_ID = "initial_tab_id";

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private MainTabsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tabs);

        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        String role = SessionManager.getRole(this);
        adapter = new MainTabsAdapter(this, role);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);

        if (bottomNavigation != null) {
            int initialTabId = getIntent().getIntExtra(EXTRA_INITIAL_TAB_ID, R.id.nav_home);
            int initialIndex = NavigationHelper.tabIndexForMenu(initialTabId);
            if (initialIndex < 0) {
                initialIndex = 0;
                initialTabId = R.id.nav_home;
            }
            bottomNavigation.setSelectedItemId(initialTabId);
            viewPager.setCurrentItem(initialIndex, false);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int index = NavigationHelper.tabIndexForMenu(item.getItemId());
                if (index >= 0) {
                    viewPager.setCurrentItem(index, true);
                    return true;
                }
                return false;
            });
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int itemId = NavigationHelper.menuForTabIndex(position);
                if (bottomNavigation != null && itemId != 0) {
                    bottomNavigation.setSelectedItemId(itemId);
                }
            }
        });

        viewPager.post(this::configureViewPagerRecyclerClip);
    }

    private void configureViewPagerRecyclerClip() {
        View child = viewPager.getChildAt(0);
        if (child instanceof RecyclerView) {
            RecyclerView rv = (RecyclerView) child;
            rv.setClipToPadding(false);
            rv.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        NavigationHelper.ensureLoggedIn(this);
        NavigationHelper.updateMessageBadge(this, bottomNavigation);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            PendingFcmNotificationQueue.drainForUser(this, user.getUid());
            if (!"provider".equalsIgnoreCase(SessionManager.getRole(this))) {
                BookingUpdateListener.getInstance().start(this);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationHelper.updateMessageBadge(this, bottomNavigation);
    }
}
