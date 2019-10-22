package com.bluberry.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.bluberry.adclient.App;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import cs.comm.Cmd;
import cs.comm.Command;

public class IpReceiver implements Runnable {
    private DatagramSocket socket;
    //private Thread commandThread;
    private CommandClient commandClient;

    public void Close() {
        System.out.println("IpReceiver Close  ");
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
                System.out.println("IpReceiver run ");
                while (!isOnline(App.getInstance().getApplicationContext())) {
                    System.out.println("network not conn");
                    Thread.sleep(3000);
                }
                // Keep a socket open to listen to all the UDP trafic that is
                // destined for this port
                socket = new DatagramSocket(Cmd.broadcast_port, InetAddress.getByName("0.0.0.0"));
                socket.setReuseAddress(true);
                socket.setBroadcast(true);

                // Receive a packet
                byte[] recvBuf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);

                // Packet received
                String ip = packet.getAddress().getHostAddress();
                System.out.println("Discovery packet received from: " + packet.getAddress().getHostAddress());
                System.out.println("Discovery packet received from port: " + packet.getPort());

                //byte[] slice = Arrays.copyOfRange(packet.getData(), 4, packet.getData().length);
                //System.out.println("Packet received; data: " + new String(packet.getData()));
                // See if the packet holds the right command (message)
                String message = new String(packet.getData()).trim();
                if (message.startsWith("/" + Command.server_ip)) {
                    socket.disconnect();
                    socket.close();

                    //					for (int i = 0; i < 100 ; i++) {
                    //						commandClient = new CommandClient(null, ip);
                    //						Thread commandThread = new Thread(commandClient);
                    //						commandThread.start();
                    //					}

                    commandClient = new CommandClient(null, ip);
                    Thread commandThread = new Thread(commandClient);
                    commandThread.start();

                    commandThread.join();

                    Thread.sleep(2000);

                } else if (message.equals("CELLLEAP_TEST_REQUEST")) {
                    // Send a response
                    //DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    //socket.send(sendPacket);
                }

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static IpReceiver getInstance() {
        return DiscoveryThreadHolder.INSTANCE;
    }

    private static class DiscoveryThreadHolder {

        private static final IpReceiver INSTANCE = new IpReceiver();
    }

    public boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}