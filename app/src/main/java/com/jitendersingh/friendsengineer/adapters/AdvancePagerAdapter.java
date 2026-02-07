package com.jitendersingh.friendsengineer.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.jitendersingh.friendsengineer.fragments.AcceptedRequestFragment;
import com.jitendersingh.friendsengineer.fragments.PendingRequestFragment;
import com.jitendersingh.friendsengineer.fragments.RejectedRequestFragment;

public class AdvancePagerAdapter extends FragmentStateAdapter {

    public PendingRequestFragment pendingFragment;
    public AcceptedRequestFragment acceptedFragment;
    public RejectedRequestFragment rejectedFragment;

    public AdvancePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);

        pendingFragment = new PendingRequestFragment();
        acceptedFragment = new AcceptedRequestFragment();
        rejectedFragment = new RejectedRequestFragment();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return acceptedFragment;
            case 2:
                return rejectedFragment;
            default:
                return pendingFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    // ✅ Refresh current fragment
    public void refreshFragment(int position) {
        switch (position) {
            case 1:
                if (acceptedFragment != null) acceptedFragment.applySearchAndFilter();
                break;
            case 2:
                if (rejectedFragment != null) rejectedFragment.applySearchAndFilter();
                break;
            default:
                if (pendingFragment != null) pendingFragment.applySearchAndFilter();
                break;
        }
    }
}
