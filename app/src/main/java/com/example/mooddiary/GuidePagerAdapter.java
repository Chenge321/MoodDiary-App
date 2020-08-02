package com.example.mooddiary;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;



public class GuidePagerAdapter extends FragmentPagerAdapter {

    public GuidePagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    /**
     * This is to getItem according to position
     * return the item
     */
    public Fragment getItem(int position) {
        return GuideFragment.newInstance(position + 1);

    }

    /**
     * This is to get the count
     * @return 3
     */
    @Override
    public int getCount() {
        return 3;
    }

    /**
     * This is to get pageTitle
     * @param position This is the postion
     * @return return the number of page
     */
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "PAGE ONE";
            case 1:
                return "PAGE TWO";
            case 2:
                return "PAGE THREE";
        }
        return null;
    }

}