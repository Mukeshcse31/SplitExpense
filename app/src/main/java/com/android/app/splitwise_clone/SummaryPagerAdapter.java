package com.android.app.splitwise_clone;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.android.app.splitwise_clone.friends.FriendsFragment;
import com.android.app.splitwise_clone.groups.GroupsFragment;

public class SummaryPagerAdapter extends FragmentPagerAdapter {


    private Context mContext;

    public SummaryPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    // This determines the fragment for each tab
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return new FriendsFragment();
        } else {
            return new GroupsFragment();
        }
    }

    // This determines the number of tabs
    @Override
    public int getCount() {
        return 2;
    }

    // This determines the title for each tab
    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position) {
            case 0:
                return mContext.getString(R.string.friends);
            case 1:
                return mContext.getString(R.string.groups);
            default:
                return null;
        }
    }
}
