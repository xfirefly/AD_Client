package com.bluberry.setup;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import com.bluberry.adclient.R;
import com.bluberry.adclient.MainActivity;

/**
 * ��ʾ ѡԴ�� UI
 *
 * @author Administrator
 */
public class ShowOptionMenu {

    private PopupWindow popupWindow = null;
    private MainActivity myMainActivity;

    private int screenWidth = 0, screenHeight = 0;
    private OptionMenuLayout view;

    public ShowOptionMenu(MainActivity myMainActivity, int w, int h) {
        this.myMainActivity = myMainActivity;
        this.screenWidth = w;
        this.screenHeight = h;
    }

    public void showWindow(View parent) {
        int view_w = (int) (0.6 * screenWidth);
        if (screenHeight > 1000)
            view_w = (int) (0.42 * screenWidth);

        int view_h = (int) (0.6 * screenHeight);

        if (popupWindow == null) {

            LayoutInflater layoutInflater = (LayoutInflater) myMainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = (OptionMenuLayout) layoutInflater.inflate(R.layout.option_menu, null);

            view.initView();

            popupWindow = new PopupWindow(view, view_w, view_h);
            popupWindow.setAnimationStyle(R.style.setupAnim);

        } else {
            if (popupWindow.isShowing()) {
                popupWindow.dismiss();
                popupWindow = null;
                return;
            }
        }

        view.initView();

        popupWindow.setFocusable(true);

        popupWindow.setOutsideTouchable(true);

        popupWindow.setBackgroundDrawable(new BitmapDrawable());

        popupWindow.showAsDropDown(parent, (screenWidth - view_w) / 2, (screenHeight - view_h) / 2);

    }

    public void close() {
        if (null != popupWindow) {
            if (popupWindow.isShowing()) {
                popupWindow.dismiss();
                popupWindow = null;
                return;
            }
        }
    }

}
