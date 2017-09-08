package com.transcend.nas.management;

import android.content.Context;
import android.text.TextUtils;

import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.management.firmware.ShareFolderManager;
import com.transcend.nas.service.FileRecentFactory;
import com.transcend.nas.service.FileRecentInfo;
import com.transcend.nas.service.FileRecentManager;

import java.net.MalformedURLException;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by silverhsu on 16/2/17.
 */
public class SmbFileMoveLoader extends SmbAbstractLoader {

    private List<String> mSrcs;
    private String mDest;

    public SmbFileMoveLoader(Context context, List<String> srcs, String dest) {
        super(context);
        mSrcs = srcs;
        mDest = dest;
        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID(this);
        mTotal = mSrcs.size();
        mCurrent = 0;
        setType(getContext().getString(R.string.move));
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return move();
        } catch (Exception e) {
            e.printStackTrace();
            setException(e);
            updateResult(getContext().getString(R.string.error), mDest);
        }
        return false;
    }

    @Override
    protected void updateResult(String result, String destination) {
        if(isLoadInBackgroundCanceled()) {
            return;
        }

        CustomNotificationManager.updateResult(getContext(), mNotificationID, getType(), result, destination, true);
    }

    private boolean move() throws MalformedURLException, SmbException {
        for (String path : mSrcs) {
            if(isLoadInBackgroundCanceled())
                return true;

            SmbFile source = new SmbFile(getSmbUrl(path));
            FileRecentInfo oldAction = FileRecentFactory.create(getContext(), source, null);

            SmbFile target;
            if (source.getParent().endsWith(mDest))
                continue;
            if (source.isDirectory())
                target = moveDirectory(source, getSmbUrl(mDest));
            else
                target = moveFile(source, getSmbUrl(mDest));

            if(oldAction != null && target != null) {
                if(oldAction.info != null) {
                    oldAction.info.path = path;
                    oldAction.realPath = ShareFolderManager.getInstance().getRealPath(path);
                }

                FileRecentInfo newAction = FileRecentFactory.create(getContext(), target, FileRecentInfo.ActionType.MOVE);
                if(newAction != null && newAction.info != null) {
                    String filePath = TextUtils.concat(mDest, target.getName().replace("/", "")).toString();
                    newAction.info.path = filePath;
                    newAction.realPath = ShareFolderManager.getInstance().getRealPath(filePath);
                }
                FileRecentManager.getInstance().setAction(oldAction, newAction);
            }
            mCurrent++;
        }
        updateResult(getContext().getString(R.string.done), mDest);
        return true;
    }

    private SmbFile moveDirectory(SmbFile source, String destination) throws MalformedURLException, SmbException {
        String name = createRemoteUniqueName(source, destination);
        SmbFile target = new SmbFile(destination, name);
        target.mkdirs();
        SmbFile[] files = source.listFiles();
        for (SmbFile file : files) {
            if (!file.isHidden())
                mTotal++;
        }
        String path = target.getPath();
        for (SmbFile file : files) {
            if(isLoadInBackgroundCanceled())
                return null;

            if(file.isHidden())
                continue;

            if (file.isDirectory())
                moveDirectory(file, path);
            else
                moveFile(file, path);
            mCurrent++;
        }
        source.delete();
        return target;
    }

    private SmbFile moveFile(SmbFile source, String destination) throws MalformedURLException, SmbException {
        String name = createRemoteUniqueName(source, destination);
        SmbFile target = new SmbFile(destination, name);
        source.renameTo(target);
        updateProgress(name, 0, 0, false);
        return target;
    }
}
