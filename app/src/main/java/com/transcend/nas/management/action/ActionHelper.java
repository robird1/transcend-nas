package com.transcend.nas.management.action;

import android.content.Loader;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;

import java.util.List;

/**
 * Created by ike_lee on 2016/12/21.
 */
public class ActionHelper extends AbstractActionManager {
    private static final String TAG = ActionHelper.class.getSimpleName();

    private int mCurrentLoaderID = -1;
    private Bundle mCurrentLoaderArgs = null;
    private Loader<Boolean> mCurrentLoader;
    private List<AbstractActionManager> mActionManagerList;

    public ActionHelper(List<AbstractActionManager> managers){
        mActionManagerList = managers;
    }

    public void setProgressLayout(RelativeLayout progressLayout) {
        if(mActionManagerList != null) {
            for(AbstractActionManager manager : mActionManagerList)
                manager.setProgressLayout(progressLayout);
        }
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        Loader<Boolean> loader = null;
        if(mActionManagerList != null) {
            for(AbstractActionManager manager : mActionManagerList) {
                Loader<Boolean> tmp = manager.onCreateLoader(id, args);
                if(tmp != null && loader != null)
                    Log.d(TAG, "some loader is duplicate : " + id);

                if(tmp != null) {
                    Log.w(TAG, "onCreateLoader: " + manager.getClass().getSimpleName() + " capture ");
                    loader = tmp;
                }
            }
        }

        mCurrentLoaderID = id;
        mCurrentLoaderArgs = args;
        mCurrentLoader = loader;
        return loader;
    }

    @Override
    public boolean onLoadFinished(Loader<Boolean> loader, Boolean success) {
        mCurrentLoaderID = -1;
        mCurrentLoaderArgs = null;
        mCurrentLoader = null;

        if(mActionManagerList != null) {
            for(AbstractActionManager manager : mActionManagerList) {
                if(manager.onLoadFinished(loader, success)) {
                    Log.w(TAG, "onLoaderFinished: " + manager.getClass().getSimpleName() + " capture ");
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {
        if(mActionManagerList != null) {
            for (AbstractActionManager manager : mActionManagerList) {
                manager.onLoaderReset(loader);
            }
        }
    }

    public int getCurrentLoaderID(){
        return mCurrentLoaderID;
    }

    public Bundle getCurrentLoaderArgs() {
        return mCurrentLoaderArgs;
    }

    public void destroyLoader(){
        if(mCurrentLoader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            mCurrentLoader.cancelLoad();
        mCurrentLoader = null;
    }

}
