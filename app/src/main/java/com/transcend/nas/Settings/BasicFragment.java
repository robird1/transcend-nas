package com.transcend.nas.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.transcend.nas.R;

/**
 * Created by ikelee on 16/11/23.
 */

public class BasicFragment extends PreferenceFragment implements SettingsActivity.onFragmentChanged {
    public static final String TAG = BasicFragment.class.getSimpleName();
    protected BasicFragment prevFragment;
    protected SettingsActivity.ChangeFragmentListener mListener;

    public BasicFragment() {

    }

    public void setListener(BasicFragment fragment, SettingsActivity.ChangeFragmentListener listener){
        mListener = listener;
        if(listener != null) {
            listener.onChangeFragment(fragment);
        }
    }

    public SettingsActivity.ChangeFragmentListener getFragmentListener() {
        return mListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void setPreviousFragment(BasicFragment f) {
        prevFragment = f;
    }

    @Override
    public void showPreviousFragment() {
        if(prevFragment != null) {
            getActivity().getFragmentManager().beginTransaction().replace(R.id.settings_frame, prevFragment).commit();
            prevFragment.setListener(prevFragment, mListener);
        } else
            getActivity().finish();
    }

    public void showNextFragement(BasicFragment next){
        getActivity().getFragmentManager().beginTransaction().replace(R.id.settings_frame, next).commit();
    }
}
