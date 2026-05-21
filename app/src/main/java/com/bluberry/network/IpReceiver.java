package com.bluberry.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.bluberry.adclient.App;
import com.bluberry.common.print;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import cs.comm.Cmd;
import cs.comm.Command;

public class IpReceiver implements Runnable {
    private static final String TAG = "IpReceiver";
    private DatagramSocket socket;
    private CommandClient commandClient;

    public static IpReceiver getInstance() {
        return DiscoveryThreadHolder.INSTANCE;
    }

    public void Close() {
        print.d(TAG, "IpReceiver Close");
        if (commandClient != null) {
            commandClient.stopClient();
        }
        if (socket != null) {
            socket.close();
        }
    }

    @Override
    public void run() {
        while (App.Alive) {
            try {
                print.d(TAG, "IpReceiver run");
                while (!isOnline(App.getInstance().getApplicationContext())) {
                    print.d(TAG, "network not connected");
                    Thread.sleep(3000);
                }

                socket = new DatagramSocket(Cmd.broadcast_port, InetAddress.getByName("0.0.0.0"));
                socket.setReuseAddress(true);
                socket.setBroadcast(true);

                byte[] recvBuf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);

                String ip = packet.getAddress().getHostAddress();
                print.d(TAG, "Discovery packet received from: " + ip + ":" + packet.getPort());

                String message = new String(packet.getData()).trim();
                if (message.startsWith("/" + Command.server_ip)) {
                    socket.disconnect();
                    socket.close();

                    commandClient = new CommandClient(null, ip);
                    Thread commandThread = new Thread(commandClient);
                    commandThread.start();
                    commandThread.join();

                    Thread.sleep(2000);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null;
    }

    private static class DiscoveryThreadHolder {
        private static final IpReceiver INSTANCE = new IpReceiver();
    }
}
