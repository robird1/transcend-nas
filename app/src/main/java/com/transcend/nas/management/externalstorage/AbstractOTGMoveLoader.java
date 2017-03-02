package com.transcend.nas.management.externalstorage;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.provider.DocumentFile;

import com.transcend.nas.management.LocalAbstractLoader;

import org.apache.commons.io.FilenameUtils;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;

/**
 * Created by steve_su on 2017/1/9.
 */

public class AbstractOTGMoveLoader extends LocalAbstractLoader {
    private static final String TAG = AbstractOTGMoveLoader.class.getSimpleName();
    private Context mContext;

    public AbstractOTGMoveLoader(Context context) {
        super(context);
        mContext = context;
    }

    protected String createUniqueName(DocumentFile source, DocumentFile dest) throws MalformedURLException, SmbException {
        List<String> names = new ArrayList<String>();
        for (DocumentFile file : dest.listFiles()) names.add(file.getName());
        String origin = source.getName();
        String unique = origin;
        String ext = FilenameUtils.getExtension(origin);
        String prefix = FilenameUtils.getBaseName(origin);
        String suffix = ext.isEmpty() ? "" : String.format(".%s", ext);
        int index = 2;
        while (names.contains(unique)) {
            unique = String.format(prefix + "_%d" + suffix, index++);
        }
        return unique;
    }

    protected void startProgressWatcher(final DocumentFile target, final int total) {
        try {
            mThread = new HandlerThread(TAG);
            mThread.start();
            mHandler = new Handler(mThread.getLooper());
            mHandler.post(mWatcher = new Runnable() {
                @Override
                public void run() {
                    if(isLoadInBackgroundCanceled())
                        return;

                    if (target != null) {
                        int count = (int) target.length();
                        updateProgress(target.getName(), count, total);
                    }

                    if (mHandler != null) {
                        mHandler.postDelayed(mWatcher, 1000);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
