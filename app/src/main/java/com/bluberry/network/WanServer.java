package com.bluberry.network;

import com.bluberry.adclient.App;
import com.bluberry.common.http.HttpRequestHelper;
import com.bluberry.common.print;
import com.bluberry.config.PkgConfig;
import com.google.gson.Gson;

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

public class WanServer {
    private DatagramSocket socket;
    //private Thread commandThread;
    private CommandClient commandClient;

    public void Close() {
        System.out.println("WanServer Close  ");
        if (commandClient != null) {
            commandClient.stopClient();
        }
        if (socket != null) {
            socket.close();
        }
    }

    public class Cust2IP {
        public String cust;
        public String ip;
    }

    public class Cust2IPList {
        public List<Cust2IP> list = new ArrayList<Cust2IP>();
    }

    public class WanConnectRun implements Runnable {
        private String getServer() {
            String url = "http://file.fooyou.org/apk/ad/custom2ip.json?cust=" + PkgConfig.customer;
            Gson gson = new Gson();

            Cust2IPList lst = gson.fromJson(HttpRequestHelper.getInstance().getUTF8String(url), Cust2IPList.class);
            for (Cust2IP c : lst.list) {
                if (c.cust.equals(PkgConfig.customer)) {
                    return c.ip;
                }
            }
            return null;
        }

        public void run() {
//			String servip = getServer();
//			if (servip == null) {
//				return;
//			}

            String servip = App.getString("wan_ip");
            if (!validIP(servip))
                return;

            print.w("ip", "wan ip is: " + servip);
            while (App.Alive) {
                commandClient = new CommandClient(null, servip);
                Thread th = new Thread(commandClient);
                th.start();

                try {
                    th.join();
                    commandClient.stopClient();
                    commandClient = null;
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public boolean validIP(String ip) {
            try {
                if (ip == null || ip.isEmpty()) {
                    return false;
                }

                String[] parts = ip.split("\\.");
                if (parts.length != 4) {
                    return false;
                }

                for (String s : parts) {
                    int i = Integer.parseInt(s);
                    if ((i < 0) || (i > 255)) {
                        return false;
                    }
                }
                if (ip.endsWith(".")) {
                    return false;
                }

                return true;
            } catch (NumberFormatException nfe) {
                return false;
            }
        }
    }

    public void connectWanServerCmd() {
        new Thread(new WanConnectRun()).start();
    }

    public static WanServer getInstance() {
        return DiscoveryThreadHolder.INSTANCE;
    }

    private static class DiscoveryThreadHolder {

        private static final WanServer INSTANCE = new WanServer();
    }

}