package com.transcend.nas.management.browser;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

/**
 * Created by steve_su on 2017/6/13.
 */

public abstract class FileManageScrollListener extends RecyclerView.OnScrollListener {
    private static String TAG = FileManageScrollListener.class.getSimpleName();

    private int previousTotal = 0; // The total number of items in the dataset after the last load
    private boolean loading = true; // True if we are still waiting for the last set of data to load.
    private int visibleThreshold = 5; // The minimum amount of items to have below your current scroll position before loading more.
    int firstVisibleItem, visibleItemCount, totalItemCount;
    private int current_page = 1;

    public FileManageScrollListener() {

    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            visibleItemCount = recyclerView.getChildCount();
            totalItemCount = layoutManager.getItemCount();
            firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
            Log.d(TAG, "loading: "+ loading+ " totalItemCount: " + totalItemCount+ " previousTotal: "+ previousTotal+ " firstVisibleItem: "+ firstVisibleItem);

            if (isSwipeRefresh()) {
                return;
            }

            if (loading) {
                if (totalItemCount > previousTotal) {
                    Log.d(TAG, "[Enter] totalItemCount > previousTotal loading = false");

                    loading = false;
                    previousTotal = totalItemCount;
                }
            } else {
//                Log.d(TAG, "totalItemCount - visibleItemCount "+ (totalItemCount - visibleItemCount));
//                Log.d(TAG, "firstVisibleItem + visibleThreshold "+ (firstVisibleItem + visibleThreshold));
                if ((totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                    // End has been reached
                    Log.d(TAG, "[Enter] onLoadMore loading = true");

                    current_page++;

                    onLoadMore(current_page);

                    loading = true;
                }
            }
        }

    }

    private boolean isSwipeRefresh() {
        if (firstVisibleItem == -1 && totalItemCount < previousTotal) {
//            loading = true;
            previousTotal = 0;
            return true;
        }
        return false;
    }

    public abstract void onLoadMore(int current_page);
}