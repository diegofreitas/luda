package io.luda;



import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import io.luda.model.Content;


public class ViewPagerAdapter extends FragmentStatePagerAdapter {



    // Sparse array to keep track of registered fragments in memory

    private List<Content> contents = Arrays.asList(new Content());
    private FragmentManager fragmentManager;

    public ViewPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
        this.fragmentManager = fragmentManager;
    }

    // Returns total number of pages
    @Override
    public int getCount() {
        return contents.size();
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        Content dummyItem = (Content) ((MediaFragment) object).getContent();
        int position = contents.indexOf(dummyItem);
        if (position >= 0) {
            // The current data matches the data in this active fragment, so let it be as it is.
            return position;
        } else {
            // Returning POSITION_NONE means the current data does not matches the data this fragment is showing right now.  Returning POSITION_NONE constant will force the fragment to redraw its view layout all over again and show new data.
            return POSITION_NONE;
        }
    }

    // Returns the fragment to display for that page
    @Override
    public Fragment getItem(int position) {


        Content dummyItem = contents.get(position);
        //Create a new instance of the fragment and return it.
        MediaFragment fragment = MediaFragment.newInstance();
        Log.i("VIEWPAGER", "********instantiateItem position:" + position + " NewFragmentCreated");
        //We will not pass the data through bundle because it will not gets updated by calling notifyDataSetChanged()  method. We will do it through getter and setter.
        fragment.setContent(dummyItem);

        Bundle args = new Bundle();
        args.putInt("INDEX", position);
        fragment.setArguments(args);

        return fragment;
    }



    public void updateFeedContent(List<Content> contents) {
        this.contents = contents;
    }


    public List<Content> getContents() {
        return this.contents;
    }
}