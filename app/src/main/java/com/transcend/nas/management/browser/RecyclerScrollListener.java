package com.transcend.nas.management.browser;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Created by steve_su on 2017/6/13.
 */

public abstract class RecyclerScrollListener extends RecyclerView.OnScrollListener {
    private int mPreviousTotal = 0; // The total number of items in the dataset after the last load
    private boolean mLoading = true; // True if we are still waiting for the last set of data to load.
    private int mVisibleThreshold = 5; // The minimum amount of items to have below your current scroll position before loading more.
    private int mFirstVisibleItem, mVisibleItemCount, mTotalItemCount;
    private int mCurrentPage = 1;
    private boolean mIsFirstWaiting;

    public RecyclerScrollListener() {

    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            mVisibleItemCount = recyclerView.getChildCount();
            mTotalItemCount = layoutManager.getItemCount();
            mFirstVisibleItem = layoutManager.findFirstVisibleItemPosition();

            if (isSwipeRefresh()) {
                return;
            }

            if (mTotalItemCount < mPreviousTotal) {
                reset();
                return;
            }

            if (mLoading) {
                if (mTotalItemCount > mPreviousTotal) {
                    mLoading = false;
                    mPreviousTotal = mTotalItemCount;

                } else {
                    doWaitingProcess();
                }

            } else {
                if ((mTotalItemCount - mVisibleItemCount) <= (mFirstVisibleItem + mVisibleThreshold)) {
                    // End has been reached
                    mCurrentPage++;
                    mLoading = true;
                    mIsFirstWaiting = true;
                    onLoadMore(mCurrentPage);
//                    mLoading = true;
                }
            }
        }

    }

    private void doWaitingProcess() {
        if (mIsFirstWaiting) {
            mIsFirstWaiting = false;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(20000);

                        if (mLoading == true) {
                            mLoading = false;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private boolean isSwipeRefresh() {
        if (mFirstVisibleItem == -1 && mTotalItemCount < mPreviousTotal) {
//            mLoading = true;
            mPreviousTotal = 0;
            return true;
        }
        return false;
    }

    public abstract void onLoadMore(int current_page);

    public void cancelLoadMore() {
        mCurrentPage--;
        mLoading = false;
    }

    private void reset() {
        mPreviousTotal = 0;
        mLoading = true;
        mFirstVisibleItem = 0;
        mVisibleItemCount= 0;
        mTotalItemCount = 0;
        mCurrentPage = 1;
    }

}