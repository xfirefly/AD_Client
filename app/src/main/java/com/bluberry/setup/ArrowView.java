package com.bluberry.setup;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bluberry.adclient.R;

import java.util.List;

public class ArrowView extends LinearLayout implements View.OnClickListener {

    private int currentIndex;
    private ImageView mArrowLeft;
    private ImageView mArrowRight;
    private TextView mContentView;
    private List mList;

    private OnItemSelectedLinstener mOnItemSelectedLinstener;

    public ArrowView(Context paramContext) {
        super(paramContext);
    }

    public ArrowView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
    }

    public ArrowView(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext);
    }

    @Override
    public void onClick(View paramView) {
        // TODO Auto-generated method stub
        switch (paramView.getId()) {
            case R.id.context_view:
                break;
            case R.id.arrow_left:
                doSwitch(-1);
                break;
            case R.id.arrow_right:
                doSwitch(1);
                break;
        }

    }

    private void doSwitch(int paramInt) {

        if (getCount() > 1) {
            currentIndex = (paramInt + currentIndex);

            if (currentIndex >= mList.size()) {
                currentIndex = 0;
            } else if (currentIndex < 0) {
                currentIndex = -1 + mList.size();
            }

            mContentView.setText((CharSequence) mList.get(currentIndex));
            onItemSelected(currentIndex);
        }

    }

    private void onItemSelected(int paramInt) {
        if (mOnItemSelectedLinstener != null) {
            mOnItemSelectedLinstener.onItemSelected(ArrowView.this, paramInt);

        }

    }

    public boolean dispatchKeyEvent(KeyEvent paramKeyEvent) {
        if (paramKeyEvent.getAction() == 0) {
            switch (paramKeyEvent.getKeyCode()) {

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mArrowLeft.setPressed(true);
                    doSwitch(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mArrowRight.setPressed(true);
                    doSwitch(1);
                    return true;
            }
        } else if (paramKeyEvent.getAction() != 1) {
            switch (paramKeyEvent.getKeyCode()) {

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mArrowLeft.setPressed(false);
                    doSwitch(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mArrowRight.setPressed(false);
                    doSwitch(1);
                    return true;
            }

        }
        return super.dispatchKeyEvent(paramKeyEvent);
    }

    public int getCount() {
        if (mList != null) {
            return mList.size();
        }
        return 0;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();

        mArrowLeft = ((ImageView) findViewById(R.id.arrow_left));
        mArrowRight = ((ImageView) findViewById(R.id.arrow_right));

        if (isInEditMode()) {
            return;
        }
        mArrowLeft.setOnClickListener(this);
        mArrowRight.setOnClickListener(this);
        mContentView = ((TextView) findViewById(R.id.context_view));
    }

    protected void onFocusChanged(boolean paramBoolean, int paramInt, Rect paramRect) {
        super.onFocusChanged(paramBoolean, paramInt, paramRect);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setSelected(paramBoolean);
        }
    }

    public void setData(List paramList) {
        setData(paramList, 0);
    }

    public void setData(List paramList, int paramInt) {
        if ((paramList != null) && (paramList.size() != 0)) {
            currentIndex = paramInt;
            mContentView.setText((CharSequence) paramList.get(paramInt));
            mList = paramList;

            if (paramList.size() == 1) {
                mArrowLeft.setVisibility(View.INVISIBLE);
                mArrowRight.setVisibility(View.INVISIBLE);
            } else {
                mArrowLeft.setVisibility(View.VISIBLE);
                mArrowRight.setVisibility(View.VISIBLE);
            }
            postInvalidate();
        }

    }

    public void setLeftArrowIcon(Drawable paramDrawable) {
        if (mArrowLeft != null) {
            mArrowLeft.setImageDrawable(paramDrawable);
        }

    }

    public void setOnItemSelectedLinstener(ArrowView.OnItemSelectedLinstener paramOnItemSelectedLinstener) {
        mOnItemSelectedLinstener = paramOnItemSelectedLinstener;
    }

    public void setRightArrowIcon(Drawable paramDrawable) {
        if (mArrowRight != null) {
            mArrowRight.setImageDrawable(paramDrawable);
        }

    }

    public abstract interface OnItemSelectedLinstener {
        public abstract void onItemSelected(ArrowView paramArrowView, int paramInt);
    }
}
