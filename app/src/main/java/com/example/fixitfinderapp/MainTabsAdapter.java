package com.example.fixitfinderapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainTabsAdapter extends FragmentStateAdapter {

    private final boolean isProvider;

    public MainTabsAdapter(@NonNull FragmentActivity activity, String role) {
        super(activity);
        isProvider = "provider".equalsIgnoreCase(role);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return isProvider ? new ProviderHomeFragment() : new UserHomeFragment();
            case 1:
                return isProvider ? new ProviderHistoryFragment() : new UserHistoryFragment();
            case 2:
                return new MessagesFragment();
            case 3:
                return isProvider ? new ProviderSettingsFragment() : new UserSettingsFragment();
            default:
                return new UserHomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
