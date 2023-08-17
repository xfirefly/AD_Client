package com.bluberry.adclient;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;
import android.widget.TextView;

import cs.ipc.Direction;

// http://stackoverflow.com/questions/8970927/marquee-set-speed
public class ScrollTextView extends TextView {

    /*
     Great answer! Two things I changed though: |1. Currently speed depends on text's length. To fix it,
     make int duration = (int) (1000f * distance / mScrollSpeed);, where mScrollSpeed is around 100f.
     |2. To fix problems with speed changing after first run, start scroll after layout is calculated -
     use OnGlobalLayoutListener for that:
     */
    public int mScrollSpeed = 100;
    public Direction mDir;
    // scrolling feature
    private Scroller mSlr = null;
    // milliseconds for a round of scrolling
    private int mRndDuration = 10000;
    // the X offset when paused
    private int mXPaused = 0;

    // whether it's being paused
    private boolean mPaused = true;
    private Rect rect;
    private boolean textShort;

    /*
     * constructor
     */
    public ScrollTextView(Context context) {
        this(context, null);
        // customize the TextView
        setSingleLine();
        setEllipsize(null);
        setVisibility(INVISIBLE);
    }

    /*
     * constructor
     */
    public ScrollTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
        // customize the TextView
        setSingleLine();
        setEllipsize(null);
        setVisibility(INVISIBLE);
    }

    /*
     * constructor
     */
    public ScrollTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // customize the TextView
        setSingleLine();
        setEllipsize(null);
        setVisibility(INVISIBLE);
    }

    //	07-08 19:30:16.010: W/startScroll(8256): mXPaused 0
    //	07-08 19:30:16.010: W/startScroll(8256): distance 14683
    //	07-08 19:30:16.010: W/startScroll(8256): duration 139838
    //	07-08 19:30:16.030: I/computeScroll(8256): getCurrX 2
    //	07-08 19:30:16.100: I/computeScroll(8256): getCurrX 10
    //	07-08 19:30:16.130: I/computeScroll(8256): getCurrX 13

    /**
     * begin to scroll the text from the original position
     */
    public void startScroll() {
        if (mDir == Direction.Static) {
            setVisibility(VISIBLE);
            return;
        } else if (mDir == Direction.ToRight) {
            mXPaused = 0;
        } else {
            // begin from the very right side
            mXPaused = -1 * getWidth();
        }

        // assume it's paused
        mPaused = true;
        resumeScroll();
    }

    /**
     * resume the scroll from the pausing point
     */
    private void resumeScroll() {

        if (!mPaused)
            return;

        // Do not know why it would not scroll sometimes
        // if setHorizontallyScrolling is called in constructor.
        setHorizontallyScrolling(true);

        // use LinearInterpolator for steady scrolling
        mSlr = new Scroller(this.getContext(), new LinearInterpolator());
        setScroller(mSlr);

        int scrollingLen = calculateScrollingLen();
        if (mDir == Direction.ToRight) {
            int distance;
            int duration;

            if (textShort) {
                distance = getWidth(); // scrollingLen - (getWidth() + mXPaused);
                //int duration = (new Double(mRndDuration * distance * 1.00000 / scrollingLen)).intValue();
                duration = (int) (1000f * distance / mScrollSpeed);

                setVisibility(VISIBLE);
                //mSlr.startScroll(mXPaused, 0,  distance, 0, duration);

                mSlr.startScroll(rect.width(), 0, -(getWidth() + rect.width()), 0, duration);
            } else {
                distance = scrollingLen - (getWidth() + mXPaused);
                //int duration = (new Double(mRndDuration * distance * 1.00000 / scrollingLen)).intValue();
                duration = (int) (1000f * distance / mScrollSpeed);

                setVisibility(VISIBLE);
                mSlr.startScroll(rect.width(), 0, -(getWidth() + rect.width()), 0, duration);
            }
        } else {

            int distance = scrollingLen - (getWidth() + mXPaused);
            //int duration = (new Double(mRndDuration * distance * 1.00000 / scrollingLen)).intValue();
            int duration = (int) (1000f * distance / mScrollSpeed);

            setVisibility(VISIBLE);
            mSlr.startScroll(mXPaused, 0, distance, 0, duration);
        }
        //print.w("startScroll", "mXPaused " + mXPaused);

        invalidate();
        mPaused = false;
    }

    /**
     * calculate the scrolling length of the text in pixel
     *
     * @return the scrolling length in pixels
     */
    private int calculateScrollingLen() {
        TextPaint tp = getPaint();
        rect = new Rect();
        String strTxt = getText().toString();
        tp.getTextBounds(strTxt, 0, strTxt.length(), rect);
        int scrollingLen = rect.width() + getWidth();

        //print.w("resumeScroll", "scrollingLen " + scrollingLen);

        if (rect.width() < getWidth()) { //文字宽度 小于 控件宽度
            textShort = true;

        }

        return scrollingLen;
    }

    /**
     * pause scrolling the text
     */
    public void pauseScroll() {
        if (null == mSlr)
            return;

        if (mPaused)
            return;

        mPaused = true;

        // abortAnimation sets the current X to be the final X,
        // and sets isFinished to be true
        // so current position shall be saved
        mXPaused = mSlr.getCurrX();

        mSlr.abortAnimation();
    }

    @Override
    /*
     * override the computeScroll to restart scrolling when finished so as that
     * the text is scrolled forever
     */
    public void computeScroll() {
        super.computeScroll();

        if (null == mSlr)
            return;

        //print.i("computeScroll", "getCurrX " + mSlr.getCurrX() );

        if (mSlr.isFinished() && (!mPaused)) {

            //scrollTo(0, 0);//scroll to initial position or whatever position you want it to scroll
            this.startScroll();
        }
    }

    public int getRndDuration() {
        return mRndDuration;
    }

    public void setRndDuration(int duration) {
        this.mRndDuration = duration;
    }

    public boolean isPaused() {
        return mPaused;
    }

    @Override
    public boolean isFocused() {
        return true;
    }

    public void setDirection(Direction d) {
        mDir = d;
    }

    public void setSpeed(int speed) {
        mScrollSpeed = speed * 15;

    }

    public void distory() {
        pauseScroll();
    }
}