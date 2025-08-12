package com.jitendersingh.friendsengineer.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.jitendersingh.friendsengineer.fragments.PendingRequestFragment;
import com.jitendersingh.friendsengineer.fragments.AcceptedRequestFragment;
import com.jitendersingh.friendsengineer.fragments.RejectedRequestFragment;

public class AdvancePagerAdapter extends FragmentStateAdapter {
    public AdvancePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return new AcceptedRequestFragment();
            case 2:
                return new RejectedRequestFragment();
            default:
                return new PendingRequestFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
