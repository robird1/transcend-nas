package com.transcend.nas.management;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;

import com.transcend.nas.R;

import java.util.List;

public class SectionDecoration extends RecyclerView.ItemDecoration {
    private static final String TAG = "SectionDecoration";

    private List<String> idList;

    private TextPaint textPaint;
    private Paint paint;
    private int topGap;


    public SectionDecoration(List<String> idList, Context context) {
        Resources res = context.getResources();
        this.idList = idList;
        //设置悬浮栏的画笔---paint  
        paint = new Paint();
        paint.setColor(ContextCompat.getColor(context, R.color.colorNavigationPrimaryBackground));

        //设置悬浮栏中文本的画笔  
        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(12 * context.getResources().getDisplayMetrics().density);
        textPaint.setColor(ContextCompat.getColor(context, R.color.textColorPrimary));
        textPaint.setTextAlign(Paint.Align.LEFT);

        topGap = res.getDimensionPixelSize(R.dimen.nav_header_vertical_spacing);
    }

    public void updateList(List<String> idList) {
        this.idList = idList;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int pos = parent.getChildAdapterPosition(view);
        if (isFirstInGroup(pos)) {
            outRect.top = topGap;
        } else {
            outRect.top = 0;
        }
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();
        int childCount = parent.getChildCount();
        int size = idList != null ? idList.size() : 0;
        for (int i = 0; i < childCount; i++) {
            View view = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(view);
            if (0 <= position && position < size) {
                String textLine = idList.get(position);
                if (isFirstInGroup(position)) {
                    float top = view.getTop() - topGap;
                    float bottom = view.getTop();
                    c.drawRect(left, top, right, bottom, paint);
                    c.drawText(textLine, left + 10, bottom - 10, textPaint);
                }
            }
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
        int itemCount = state.getItemCount() - 1;
        int childCount = parent.getChildCount();
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        String preGroupId = "";
        String groupId = "";
        int prePosition = -1;
        int position = -1;
        for (int i = 0; i < childCount; i++) {
            prePosition = position;
            View view = parent.getChildAt(i);
            position = parent.getChildAdapterPosition(view);
            if(prePosition > position)
                continue;

            if (0 <= position && position < itemCount) {
                preGroupId = groupId;
                groupId = idList.get(position);

                if (groupId != null && groupId.equals(preGroupId))
                    continue;

                int viewBottom = view.getBottom();
                float textY = Math.max(topGap, view.getTop());
                if (position + 1 < itemCount) {
                    String nextGroupId = idList.get(position + 1);
                    if (!nextGroupId.equals(groupId) && viewBottom < textY) {
                        textY = viewBottom;
                    }
                }

                //textY - topGap决定了悬浮栏绘制的高度和位置
                c.drawRect(left, textY - topGap, right, textY, paint);
                String textLine = idList.get(position);
                c.drawText(textLine, left + 10, textY - 10, textPaint);
            }
        }
    }


    /**
     * 判断是不是组中的第一个位置
     *
     * @param pos
     * @return
     */
    private boolean isFirstInGroup(int pos) {
        if (pos == 0) {
            return true;
        } else if (idList != null && 0 <= pos - 1 && pos < idList.size()) {
            String prevGroupId = idList.get(pos - 1);
            String groupId = idList.get(pos);
            if (prevGroupId.equals(groupId)) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }
}