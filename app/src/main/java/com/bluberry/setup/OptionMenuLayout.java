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
import com.bluberry.adclient.RTKSourceInActivity;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

//import com.realtek.hardware.RtkHDMIManager;

public class OptionMenuLayout extends LinearLayout {
    private String TAG = "OptionMenuLayout";

    private ArrowView mSubOnoffView;
    private Context mContext;
    private String[] mCurrentChannel;
    private View.OnKeyListener mOnKeyListener;
    //private ArrowView mLangView;
    private ArrowView mResView;
    //private ArrowView mVideoSourceView;
    private RTKSourceInActivity mMainActivity;
    //private ImageView mFavView;
    private Button mPrevButton;
    private Button mNextButton;
    private Button mIpSetButton;
    private Button mIdSetButton;

    private Button mLangButton;
    private Button mDateButton;

    //private RtkHDMIManager mRtkHDMIManager;

    public OptionMenuLayout(Context paramContext) {
        super(paramContext);
        mContext = paramContext;
        mMainActivity = (RTKSourceInActivity) mContext;
    }

    public OptionMenuLayout(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        mContext = paramContext;

        if (isInEditMode()) {
            return;
        }
        mMainActivity = (RTKSourceInActivity) mContext;
    }

    public OptionMenuLayout(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet);
        mContext = paramContext;
        mMainActivity = (RTKSourceInActivity) mContext;
    }

    private void initSubOnOffView() {

        mSubOnoffView.setData(Arrays.asList(mContext.getResources().getStringArray(R.array.autoboot)), App.getInt("sub_onoff"));
        mSubOnoffView.setOnItemSelectedLinstener(new onAutoBootView());
    }

    private void initLangView() {

        //mLangView.setData(Arrays.asList(mContext.getResources().getStringArray(R.array.lang)), App.getInt("lang"));
        //mLangView.setOnItemSelectedLinstener(new onLangChgView());
    }


    private void initResView() {
        //mResView.setData(Arrays.asList(mContext.getResources().getStringArray(R.array.screen_proportion)), 1);

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
        //   this.mCurrentChannel = this.activity.a();
        initResView();

        initSubOnOffView();
        initLangView();
    }

    TextView txt_curr_ip;
    TextView txt_curr_id;

    TextView txt_curr_time;

    protected void onFinishInflate() {
        super.onFinishInflate();

        if (isInEditMode())
            return;
        //mLangView = ((ArrowView) findViewById(R.id.lang));
        //	activity = ((TvUiActivity)this.mContext);
        //mRatioView = ((ArrowView) findViewById(R.id.ratio));
        mResView = ((ArrowView) findViewById(R.id.video_res));

        txt_curr_ip = ((TextView) findViewById(R.id.txt_curr_ip));
        txt_curr_ip.setText(getIPAddress(true));

        txt_curr_time = ((TextView) findViewById(R.id.txt_time));
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        txt_curr_time.setText(sdf.format(new Date()));

        mSubOnoffView = ((ArrowView) findViewById(R.id.autoboot));

        txt_curr_id = ((TextView) findViewById(R.id.txt_curr_id));
        txt_curr_id.setText(App.getString("id"));
        //mFavView = ((ImageView) findViewById(R.id.fav));

        mPrevButton = ((Button) findViewById(R.id.bt_prev));
        mNextButton = ((Button) findViewById(R.id.bt_next));

        mIpSetButton = ((Button) findViewById(R.id.bt_ipset));
        mIdSetButton = ((Button) findViewById(R.id.bt_idset));

        mLangButton = ((Button) findViewById(R.id.bt_lang));
        mLangButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mMainActivity.startActivity(new Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS));
                mMainActivity.closeOptionDialog();
            }
        });

        mDateButton = ((Button) findViewById(R.id.bt_date));
        mDateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mMainActivity.startActivity(new Intent(android.provider.Settings.ACTION_DATE_SETTINGS));
                mMainActivity.closeOptionDialog();
            }
        });

        mPrevButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //mMainActivity.closeOptionDialog();

                RTKSourceInActivity.thiz.sendMessage(Msg.MESSAGE_PREV_SCENE, this);
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //mMainActivity.closeOptionDialog();

                RTKSourceInActivity.thiz.sendMessage(Msg.MESSAGE_NEXT_SCENE, this);
            }
        });

        mIpSetButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                //Intent i = mMainActivity.getPackageManager().getLaunchIntentForPackage("com.keily.ethernetconfig");
                //如果该程序不可启动（像系统自带的包，有很多是没有入口的）会返回NULL
                //if (i != null)
                //	App.getInstance().startActivity(i);
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
                // TODO Auto-generated method stub
                mMainActivity.closeOptionDialog();

                //Within the OnClickListener of your button (or in a function called from there):

                AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
                builder.setTitle("ID");

                // Set up the input
                final EditText input = new EditText(mMainActivity);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //m_Text = input.getText().toString();
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

/*		mRtkHDMIManager = new RtkHDMIManager();
		if (mRtkHDMIManager == null)
			Log.d(TAG, "mRtkHDMIManager is null!");*/
    }

    /* Utility function for TV System List Preference.
     * 1. Have EDID info: List EDID resolution & Auto.
     * 2. No EDID: List NTSC (or PAL) 480P, 576P, 720P/I@50Hz, 720P/I@60Hz, 1080P/I@50Hz, 1080P/I@60Hz.
     */
    private boolean prepareAvailableItemsForTVSystem() {
        CharSequence[] myEntries = {"AUTO", "NTSC", "PAL", "480P", "576P", "720P @ 50Hz", "720P @ 60Hz", "1080I @ 50Hz", "1080I @ 60Hz", "1080P @ 50Hz", "1080P @ 60Hz",
                "3840x2160P @ 24Hz", "3840x2160P @ 25Hz", "3840x2160P @ 30Hz", "4096x2160P @ 24Hz", "1080P @ 24Hz"};

        CharSequence[] myEntryValues = {"0", //TV_SYS_HDMI_AUTO_DETECT,
                "1", //TV_SYS_NTSC,
                "2", //TV_SYS_PAL,
                "3", //TV_SYS_480P,
                "4", //TV_SYS_576P,
                "5", //TV_SYS_720P_50HZ,
                "6", //TV_SYS_720P_60HZ,
                "7", //TV_SYS_1080I_50HZ,
                "8", //TV_SYS_1080I_60HZ,
                "9", //TV_SYS_1080P_50HZ,
                "10", //TV_SYS_1080P_60HZ,
                "11", //TV_SYS_2160P_24HZ,
                "12", //TV_SYS_2160P_25HZ,
                "13", //TV_SYS_2160P_30HZ,
                "14", //TV_SYS_4096_2160P_24HZ
                "15", //TV_SYS_1080P_24HZ
        };

        //if (mRtkHDMIManager != null && mRtkHDMIManager.checkIfHDMIPlugged() && mRtkHDMIManager.getHDMIEDIDReady()) {
        if (false) {

            int[] supportVideoFormat = new int[0]; // mRtkHDMIManager.getVideoFormat();
            int numSupportVideoFormat = 0;

            for (int i = 0; i < myEntryValues.length; i++) {
                Log.d(TAG, "VideoFormat[" + i + "] : " + supportVideoFormat[i]);
                if (supportVideoFormat[i] == 1)
                    numSupportVideoFormat++;
            }
            Log.d(TAG, "Num of SupoortVideoFormat:" + numSupportVideoFormat);

            CharSequence[] entries = new CharSequence[numSupportVideoFormat + 1];
            CharSequence[] entryValues = new CharSequence[numSupportVideoFormat + 1];
            int j = 0;
            for (int i = 0; i < myEntryValues.length; i++) {
                if (i == 0 || supportVideoFormat[i] == 1) {
                    entries[j] = myEntries[i];
                    entryValues[j] = myEntryValues[i];
                    j++;
                }
            }

            for (int i = 0; i < entries.length; i++) {
                Log.d(TAG, entries[i] + " : " + entryValues[i]);
            }

            //mTVSystemPreference.setEntries(entries);
            //mTVSystemPreference.setEntryValues(entryValues);

            mResView.setData(Arrays.asList(entries), App.getInt("tvsys"));

        } else {
            CharSequence[] entries = new CharSequence[myEntryValues.length - 1];
            CharSequence[] entryValues = new CharSequence[myEntryValues.length - 1];
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

            //mTVSystemPreference.setEntries(entries);
            //mTVSystemPreference.setEntryValues(entryValues);
        }

        return true;
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @param ipv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }

    public void setOnKeyListener(View.OnKeyListener paramOnKeyListener) {
        mOnKeyListener = paramOnKeyListener;
    }

    class onResView implements ArrowView.OnItemSelectedLinstener {
        @Override
        public void onItemSelected(ArrowView paramArrowView, int paramInt) {
            // TODO Auto-generated method stub
            System.out.println("onResView = " + paramInt);
            //mMainActivity.setscale(paramInt);
            //mMainActivity.ScreenScale();

/*			if (mRtkHDMIManager != null) {
				Log.d(TAG, "Call RtkHDMIManager to set HDMI TV System:" + paramInt);

				tvSys = paramInt;

				mMainActivity.handimpl.removeCallbacks(switchTvSystem);
				mMainActivity.handimpl.postDelayed(switchTvSystem, 3000L);
			}*/
        }
    }

    private int tvSys = 0;
    private Runnable switchTvSystem = new Runnable() {
        @Override
        public void run() {
            //mRtkHDMIManager.setTVSystem(tvSys);
            App.saveInt("tvsys", tvSys);
        }
    };

    class onAutoBootView implements ArrowView.OnItemSelectedLinstener {

        @Override
        public void onItemSelected(ArrowView paramArrowView, int i) {
            // TODO Auto-generated method stub
            System.out.println("sub_onoff = " + i);

            switch (i) {
                case 0:
                    //mMainActivity.enableSubtitle(true);
                    mMainActivity.sendMessage(Msg.MESSAGE_CHANGE_SUBTITLE_VISABLE, false);
                    break;
                case 1:
                    //mMainActivity.enableSubtitle(false);
                    mMainActivity.sendMessage(Msg.MESSAGE_CHANGE_SUBTITLE_VISABLE, true);
                    break;
            }
        }
    }


    class onLangChgView implements ArrowView.OnItemSelectedLinstener {

        @Override
        public void onItemSelected(ArrowView paramArrowView, int i) {
            // TODO Auto-generated method stub
            System.out.println("onLangChgView = " + i);

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
