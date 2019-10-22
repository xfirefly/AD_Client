package com.bluberry.adclient.sp;

import android.serialport.SerialPort;
import android.util.Log;
import android.view.View;

import com.bluberry.adclient.App;
import com.bluberry.adclient.Msg;
import com.bluberry.adclient.RTKSourceInActivity;
import com.bluberry.common.FileUtil;
import com.bluberry.common.IOUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class SerialPortBiz {

    FileOutputStream mOutputStream;
    FileInputStream mInputStream;
    SerialPort sp;
    ReadThread mReadThread;

    public void init() {

        Log.e("D", "gpio  gpio  gpio  gpio  gpio  ");

        try {
            sp = new SerialPort(new File("/dev/ttyS1"), 115200);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

		/*
		Log.e("D", "gpio   " + sp.readGPIO(32) );
		Log.e("D", "gpio   " + sp.readGPIO(33) );
		Log.e("D", "gpio   " + sp.readGPIO(34) );
		Log.e("D", "gpio   " + sp.readGPIO(35) );
		Log.e("D", "gpio   " + sp.readGPIO(4) );
		
		App.RunAsApp("chmod 777 /sys/class/gpio/export");
		*/

        mInputStream = (FileInputStream) sp.getInputStream();
        mReadThread = new ReadThread();
        mReadThread.start();

    }

    public void uninit() {
        if (sp != null)
            sp.close();

        mReadThread.stop();
    }

    public void onClick(View v) {

        try {
            mOutputStream = (FileOutputStream) sp.getOutputStream();

            mOutputStream.write(new String("send").getBytes());
            mOutputStream.write('\n');
            mOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    byte[] prefix = {(byte) 0xca, (byte) 0xfe, (byte) 0xca, (byte) 0xfe};

    void process(final byte[] src) {
        byte[] dest = new byte[src.length];
        System.arraycopy(src, 0, dest, 0, src.length);

        //mReception.append(new String(buffer, 0, size));
        Log.d("D", "total " + dest.length);
        Log.d("D", bytesToHex(dest, dest.length));

        byte[] slice = Arrays.copyOfRange(dest, 0, 4);
        if (!Arrays.equals(slice, prefix)) {
            Log.d("D", "prefix not equal ");
            return;
        }

        int id = Integer.valueOf(App.getString("id"));

        int id_count = byteArrayToLeInt(Arrays.copyOfRange(dest, 4, 8));
        Log.d("D", "id_count " + id_count);
        Log.d("D", "local id  " + id);

        boolean found = false;
        byte[] ids = Arrays.copyOfRange(dest, 8, 8 + id_count);
        for (int i = 0; i < ids.length; i++) {
            Log.d("D", "for id  " + (ids[i] & 0xff));
            if ((ids[i] & 0xff) == id) {
                found = true;
                break;
            }
        }

        if (!found) {
            return;
        }

        int zip_len_pos = 4 + 4 + id_count + 4;
        int zip_len = byteArrayToLeInt(Arrays.copyOfRange(dest, zip_len_pos, zip_len_pos + 4));
        Log.d("D", "zip_len " + zip_len);

        File f = new File(App.zipFile);
        IOUtil.delete(f);

        try {
            FileOutputStream fos = new FileOutputStream(App.zipFile);
            fos.write(dest, zip_len_pos + 4, zip_len);
            fos.flush();
            fos.close();

            RTKSourceInActivity.thiz.sendMessage(Msg.MESSAGE_RECV_JSON_BEGIN, "");

            String dir = App.getNextDir();
            FileUtil.Unzip(App.zipFile, dir);

            RTKSourceInActivity.thiz.parseJson(dir, true);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static int byteArrayToLeInt(byte[] b) {
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes, int len) {
        char[] hexChars = new char[len * 2];
        for (int j = 0; j < len; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private class ReadThread extends Thread {

		/*
		public void run() {
			super.run();
			while (!isInterrupted()) {
				int size;
				byte[] buffer = sp.readArray(0xff);
				
				if (buffer != null) {
					Log.d("D", "buffer len  " + buffer.length );
					Log.d("D", i + ": " + bytesToHex(buffer));
					i++;  
				}	 
			}
		}
		*/

        @Override
        public void run() {
            byte[] buffer = new byte[512];

            ByteArrayOutputStream bObj = new ByteArrayOutputStream();
            bObj.reset();

            while (App.Alive) {
                int size;
                try {
                    if (mInputStream == null)
                        return;

                    size = mInputStream.read(buffer, 0, buffer.length);

                    if (size > 0) {
                        bObj.write(buffer, 0, size);

                        if (isEnd(buffer, size)) {
                            //Log.d("www", bytesToHex(buffer, size));
                            process(bObj.toByteArray());
                            bObj.reset();
                        }
						/*
						if (size >= 4) {
							Log.d("www", bytesToHex(buffer));
							byte[] slice = Arrays.copyOfRange(buffer, size - 4, size - 1);
							if (Arrays.equals(slice, end)) {
								process(bObj.toByteArray());
							}
						}*/
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

            }
        }

        int endState = 0; //比较到第几个

        //byte[] end = { 0xc1, 0x31, 0x78, 0xfe };

        boolean isEnd(final byte[] buffer, int size) {
            for (int i = 0; i < size; i++) {
                switch (endState) {
                    case 0: //init state
                        if (buffer[i] == (byte) 0xc1) {
                            endState = 1;
                        }
                        break;
                    case 1:
                        if (buffer[i] == (byte) 0x31) {
                            endState = 2;
                        } else {
                            endState = 0;
                        }
                        break;
                    case 2:
                        if (buffer[i] == (byte) 0x78) {
                            endState = 3;
                        } else {
                            endState = 0;
                        }
                        break;
                    case 3:
                        if (buffer[i] == (byte) 0xfe) {
                            endState = 0;
                            return true;
                        }

                        break;
                    default:
                        break;
                }
            }

            return false;
        }

    }

}