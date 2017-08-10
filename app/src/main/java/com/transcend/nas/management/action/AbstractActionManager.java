package com.transcend.nas.management.action;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import com.transcend.nas.NASApp;

/**
 * Created by ike_lee on 2016/12/21.
 */
public abstract class AbstractActionManager {

    public Context mContext;
    public LoaderManager.LoaderCallbacks mCallbacks;
    public RelativeLayout mProgressLayout;

    public AbstractActionManager(){

    }

    public AbstractActionManager(Context context, LoaderManager.LoaderCallbacks callbacks, RelativeLayout progressLayout) {
        mContext = context;
        mCallbacks = callbacks;
        mProgressLayout = progressLayout;
    }

    protected Context getContext(){
        if (mContext != null)
            return mContext;
        else
            return NASApp.getContext();
    }

    public void setProgressLayout(RelativeLayout progressLayout){
        mProgressLayout = progressLayout;
    }

    public RelativeLayout getProgressLayout(){
        return mProgressLayout;
    }

    protected boolean createLoader(int id, Bundle args) {
        if (args == null)
            args = new Bundle();

        if (id > 0 && mContext != null && mCallbacks != null) {
            ((Activity) mContext).getLoaderManager().restartLoader(id, args, mCallbacks).forceLoad();
            return true;
        }
        return false;
    }

    protected void showProgress(){
        if (mProgressLayout != null)
            mProgressLayout.setVisibility(View.VISIBLE);
    }

    protected void hideProgress(){
        if (mProgressLayout != null)
            mProgressLayout.setVisibility(View.INVISIBLE);
    }

    abstract Loader<Boolean> onCreateLoader(int id, Bundle args);
    abstract boolean onLoadFinished(Loader<Boolean> loader, Boolean success);
    abstract void onLoaderReset(Loader<Boolean> loader);
}
