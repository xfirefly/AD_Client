package com.bluberry.setup;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bluberry.adclient.App;
import com.bluberry.adclient.Msg;
import com.bluberry.adclient.R;
import com.bluberry.adclient.MainActivity;
import com.bluberry.common.print;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class OptionMenuLayout extends LinearLayout {
    private static final String TAG = "OptionMenuLayout";
    TextView txt_curr_ip;
    TextView txt_curr_id;
    TextView txt_curr_time;
    private ArrowView mSubOnoffView;
    private Context mContext;
    private View.OnKeyListener mOnKeyListener;
    private ArrowView mResView;
    private MainActivity mMainActivity;
    private Button mPrevButton;
    private Button mNextButton;
    private Button mIpSetButton;
    private Button mIdSetButton;
    private Button mLangButton;
    private Button mDateButton;
    private int tvSys = 0;
    private Runnable switchTvSystem = new Runnable() {
        @Override
        public void run() {
            App.saveInt("tvsys", tvSys);
        }
    };

    public OptionMenuLayout(Context paramContext) {
        super(paramContext);
        mContext = paramContext;
        mMainActivity = (MainActivity) mContext;
    }

    public OptionMenuLayout(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        mContext = paramContext;

        if (isInEditMode()) {
            return;
        }
        mMainActivity = (MainActivity) mContext;
    }

    public OptionMenuLayout(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet);
        mContext = paramContext;
        mMainActivity = (MainActivity) mContext;
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%');
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        }
        return "";
    }

    private void initSubOnOffView() {
        mSubOnoffView.setData(Arrays.asList(mContext.getResources().getStringArray(R.array.autoboot)), App.getInt("sub_onoff"));
        mSubOnoffView.setOnItemSelectedLinstener(new onAutoBootView());
    }

    private void initLangView() {
    }

    private void initResView() {
        prepareAvailableItemsForTVSystem();
        mResView.setOnItemSelectedLinstener(new onResView());
    }

    public boolean dispatchKeyEvent(KeyEvent paramKeyEvent) {
        if ((paramKeyEvent.getAction() == 0) && (paramKeyEvent.getKeyCode() == KeyEvent.KEYCODE_MENU)) {
            mMainActivity.closeOptionDialog();
            return true;
        }
        return super.dispatchKeyEvent(paramKeyEvent);
    }

    public void initView() {
        initResView();
        initSubOnOffView();
        initLangView();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();

        if (isInEditMode())
            return;

        mResView = ((ArrowView) findViewById(R.id.video_res));

        txt_curr_ip = ((TextView) findViewById(R.id.txt_curr_ip));
        txt_curr_ip.setText(getIPAddress(true));

        txt_curr_time = ((TextView) findViewById(R.id.txt_time));
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        txt_curr_time.setText(sdf.format(new Date()));

        mSubOnoffView = ((ArrowView) findViewById(R.id.autoboot));

        txt_curr_id = ((TextView) findViewById(R.id.txt_curr_id));
        txt_curr_id.setText(App.getString("id"));

        mPrevButton = ((Button) findViewById(R.id.bt_prev));
        mNextButton = ((Button) findViewById(R.id.bt_next));

        mIpSetButton = ((Button) findViewById(R.id.bt_ipset));
        mIdSetButton = ((Button) findViewById(R.id.bt_idset));

        mLangButton = ((Button) findViewById(R.id.bt_lang));
        mLangButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMainActivity.startActivity(new Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS));
                mMainActivity.closeOptionDialog();
            }
        });

        mDateButton = ((Button) findViewById(R.id.bt_date));
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMainActivity.startActivity(new Intent(android.provider.Settings.ACTION_DATE_SETTINGS));
                mMainActivity.closeOptionDialog();
            }
        });

        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.sendMessage(Msg.MESSAGE_PREV_SCENE, this);
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.sendMessage(Msg.MESSAGE_NEXT_SCENE, this);
            }
        });

        mIpSetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$EthernetSettingsActivity"));
                try {
                    mMainActivity.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    mMainActivity.startActivity(new Intent("android.settings.ETHERNET_SETTINGS"));
                }
                mMainActivity.closeOptionDialog();
            }
        });

        mIdSetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMainActivity.closeOptionDialog();

                AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
                builder.setTitle("ID");

                final EditText input = new EditText(mMainActivity);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        App.saveString("id", input.getText().toString());
                        txt_curr_id.setText(App.getString("id"));
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        initView();
    }

    private boolean prepareAvailableItemsForTVSystem() {
        String[] myEntries = {"AUTO", "NTSC", "PAL", "480P", "576P", "720P @ 50Hz", "720P @ 60Hz", "1080I @ 50Hz", "1080I @ 60Hz", "1080P @ 50Hz", "1080P @ 60Hz",
                "3840x2160P @ 24Hz", "3840x2160P @ 25Hz", "3840x2160P @ 30Hz", "4096x2160P @ 24Hz", "1080P @ 24Hz"};

        String[] myEntryValues = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};

        String[] entries = new String[myEntryValues.length - 1];
        String[] entryValues = new String[myEntryValues.length - 1];
        int j = 0;
        for (int i = 1; i < myEntryValues.length; i++) {
            entries[j] = myEntries[i];
            entryValues[j] = myEntryValues[i];
            j++;
        }

        for (int i = 0; i < entries.length; i++) {
            Log.d(TAG, entries[i] + " : " + entryValues[i]);
        }

        mResView.setData(Arrays.asList(entries), App.getInt("tvsys"));

        return true;
    }

    public void setOnKeyListener(View.OnKeyListener paramOnKeyListener) {
        mOnKeyListener = paramOnKeyListener;
    }

    class onResView implements ArrowView.OnItemSelectedLinstener {
        @Override
        public void onItemSelected(ArrowView paramArrowView, int paramInt) {
            print.d(TAG, "onResView = " + paramInt);
        }
    }

    class onAutoBootView implements ArrowView.OnItemSelectedLinstener {
        @Override
        public void onItemSelected(ArrowView paramArrowView, int i) {
            print.d(TAG, "sub_onoff = " + i);
            switch (i) {
                case 0:
                    mMainActivity.sendMessage(Msg.MESSAGE_CHANGE_SUBTITLE_VISABLE, false);
                    break;
                case 1:
                    mMainActivity.sendMessage(Msg.MESSAGE_CHANGE_SUBTITLE_VISABLE, true);
                    break;
            }
        }
    }

    class onLangChgView implements ArrowView.OnItemSelectedLinstener {
        @Override
        public void onItemSelected(ArrowView paramArrowView, int i) {
            print.d(TAG, "onLangChgView = " + i);
            switch (i) {
                case 0:
                    mMainActivity.startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS), 0);
                    break;
                case 1:
                    break;
            }
        }
    }
}
