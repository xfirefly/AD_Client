package com.bluberry.network;

import android.app.AlarmManager;
import android.content.Context;
import android.media.AudioManager;

import com.bluberry.adclient.App;
import com.bluberry.adclient.Msg;
import com.bluberry.adclient.RTKSourceInActivity;
import com.bluberry.common.IOUtil;
import com.bluberry.common.print;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import cs.comm.Cmd;
import cs.comm.Command;

public class CommandClient implements Runnable {
    private String tag = "CommandClient";

    private String serverMessage;
    public String serverIP = " "; //your computer IP address

    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;

    private PrintWriter out;

    private Socket socket;

    private Thread heartbeatThread;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages
     * received from server
     */
    public CommandClient(OnMessageReceived listener, String ip) {
        mMessageListener = listener;
        serverIP = ip;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendCommand(String message) {
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
        }
    }

    public void stopClient() {
        mRun = false;
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        mRun = true;
        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(serverIP);
            System.out.println("CC: Connecting...");

            //create a socket to make the connection with the server
            socket = new Socket(serverAddr, Cmd.cmd_port);

            try {
                //send the message to the server
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                heartbeatThread = new Thread(new Heartbeat(out));
                heartbeatThread.start();
                //out.println("haqo");
                String idName = App.getString("name") + "/:" + App.getString("id");
                sendCommand(Cmd.create(Command.connect, idName));

                //receive the message which the server sends back
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //in this while the client listens for the messages sent by the server
                while (mRun) {
                    serverMessage = in.readLine();
                    print.i("CC", "CC: Received Message: '" + serverMessage + "'");

                    if (serverMessage != null) {
                        if (!process(serverMessage))
                            break;

                        if (mMessageListener != null) {
                            //call the method messageReceived from MyActivity class
                            mMessageListener.messageReceived(serverMessage);
                        }
                        serverMessage = null;
                    } else {
                        System.out.println("CC: null Message, ERROR ");
                        break;
                    }

                }

            } catch (Exception e) {
                System.out.println("CC: Error " + e);

            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                stopClient();
                System.out.println("CC: Done.");

                //App.IpRecv.execute(IpReceiver.getInstance());
            }
        } catch (Exception e) {
            System.out.println("CC: Error2 " + e);
            //e.printStackTrace();
        }
    }

    private String nowTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); // HH:mm:ss
        String date = df.format(Calendar.getInstance().getTime());
        return date;
    }

    private boolean process(String message) {
        try {
            String[] strArr = message.split("/:");
            String cmd = strArr[0].substring(1).trim();
            String param = strArr[1].trim();

            System.out.println("cmd:" + cmd);
            System.out.println("param:" + param);

            switch (Command.valueOf(cmd)) {
                case send_ad:
                    // create client to server , get ad zip file
                    FileClient fileClient = new FileClient(serverIP, FileClient.FileType.FILE_AD_ZIP, Long.parseLong(param));
                    new Thread(fileClient).start();
                    break;
                case set_time:  // '/set_time/:2016/9/26 15:29:52'
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Calendar c = Calendar.getInstance();
                    c.setTime(sdf.parse(param));
                    System.out.println("cccc:" + c);
                    AlarmManager am1 = (AlarmManager) RTKSourceInActivity.thiz.getSystemService(Context.ALARM_SERVICE);
                    am1.setTime(c.getTimeInMillis());
                    break;
                case get_time:
                    System.out.println("nowTime: " + nowTime());
                    sendCommand(Cmd.create(Command.curr_time, nowTime()));
                    break;

                case set_name:
                    App.saveString("name", param);
                    break;
                case set_id:
                    App.saveString("id", param);
                    break;
                case get_screen:
                    RTKSourceInActivity.thiz.sendMessage(Msg.MESSAGE_TakeScreenshot, this);
                    break;

                case prev_scene:
                    RTKSourceInActivity.thiz.sendMessage(Msg.MESSAGE_PREV_SCENE, this);
                    break;

                case next_scene:
                    RTKSourceInActivity.thiz.sendMessage(Msg.MESSAGE_NEXT_SCENE, this);
                    break;

                case set_volume:
                    AudioManager audm = (AudioManager) App.getContext().getSystemService(App.getContext().AUDIO_SERVICE);
                    int max1 = audm.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int vol = (max1 * Integer.parseInt(param)) / 100;
                    print.w(tag, "set_volume " + vol);
                    audm.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
                    break;
                case get_volume:
                    AudioManager am = (AudioManager) App.getContext().getSystemService(App.getContext().AUDIO_SERVICE);
                    int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int volume_level = am.getStreamVolume(AudioManager.STREAM_MUSIC);

                    print.w(tag, "max " + am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                    print.w(tag, "volume_level " + volume_level);
                    volume_level = (100 * volume_level) / max;
                    print.w(tag, "volume_level 100 :" + volume_level);

                    sendCommand(Cmd.create(Command.curr_volume, "" + volume_level));
                    break;

                case reboot:
                    App.RunAsApp("reboot");
                    break;

                case clear:
                    RTKSourceInActivity.thiz.sendMessage(Msg.MESSAGE_NO_DATA, this);

                    try {
                        Process su;
                        su = Runtime.getRuntime().exec("sh");
                        String cmdrm = "rm -fr " + App.AdDir + "* " + "\n" + "exit\n";
                        print.w(tag, cmdrm);

                        su.getOutputStream().write(cmdrm.getBytes());
                        if ((su.waitFor() != 0)) {
                            throw new SecurityException();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        //throw new SecurityException();
                    }

                    Thread.sleep(2000);
                    for (String str : App.getDirs(App.AdDir)) {
                        String tmp = App.AdDir + str + "/";
                        IOUtil.delete(new File(tmp));
                    }
                    break;

                case install_apk:
                    FileClient apkClient = new FileClient(serverIP, FileClient.FileType.FILE_APK, -1);
                    new Thread(apkClient).start();
                    break;

                case server_close:
                    return false;

                case set_wan_ip:
                    App.saveString("wan_ip", param);

                    WanServer.getInstance().connectWanServerCmd();
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return true;
    }

    class Heartbeat implements Runnable {
        private PrintWriter out;

        public Heartbeat(PrintWriter out) {
            this.out = out;
        }

        public void run() {
            try {
                while (mRun) {
                    Thread.sleep(11600);
                    out.println(Cmd.create(Command.heartbeat, ""));
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}

/*
import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer extends Thread
{
    public static final int SERVER_PORT = 4444;
    private boolean running = false;
    private PrintWriter mOut;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;
    PrintWriter out;

    public TCPServer(OnMessageReceived listener){
        this.mMessageListener = listener;
    }


    public static void main(String[] args) 
    {
        ServerBoard frame = new ServerBoard();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void sendMessage(String message){
        if(mOut != null && !mOut.checkError()){
            mOut.print(message);
            mOut.flush();
        }
    }

    @Override
    public void run(){
        super.run();
        running = true;

        try{
            System.out.println("S: Connecting...");
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Socket client = serverSocket.accept();
            System.out.println("S: Receiving...");

            try{
                mOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())),true);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                while(running){
                    String msg = in.readLine();
                    if(msg != null && mMessageListener != null){
                        mMessageListener.messageReceived(msg);
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public interface OnMessageReceived
    {
        public void messageReceived(String message);
    }
}
*/