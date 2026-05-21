package com.bluberry.common;

import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpTool {
    public final static String UA_IPAD = "Mozilla/5.0 (iPad; CPU OS 5_0 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/7534.48.3";
    public final static String UA_CHROME = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.62 Safari/537.36";
    public final static String REF_BAIDU = "http://www.baidu.com/";
    private static final String tag = "HttpTool";

    public static String getRealIP(String s) {
        String regexIP = "((?:(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d))";
        Pattern p = Pattern.compile(regexIP);
        Matcher m = p.matcher(s);
        if (m.find()) {
            return m.group();
        }
        return "N";
    }

    public static String getAddr(String s) {
        String regexAddr = "<span>(.*?)</span></p>";
        Pattern p = Pattern.compile(regexAddr);
        Matcher m = p.matcher(s);
        String[] ss = new String[20];
        if (m.find()) {
            ss = m.group().split(">|&");
            return ss[1];
        }
        return "N";
    }

    public static long getContentLength(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == 200) {
                return conn.getContentLengthLong();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }
        return 0L;
    }

    public static String doGET(String url) {
        return doGET(url, UA_CHROME, REF_BAIDU);
    }

    public static String doGET(String url, String userAgent, String refererURL) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Cache-Control", "max-age=0");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("Referer", refererURL);
            conn.setRequestProperty("User-Agent", userAgent);

            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                byte[] data = readFully(is);
                is.close();
                return new String(data, "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    public static String doPost(String url, String userAgent, String refererURL, String postString) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Cache-Control", "max-age=0");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("Referer", refererURL);
            conn.setRequestProperty("User-Agent", userAgent);

            OutputStream os = conn.getOutputStream();
            os.write(postString.getBytes("UTF-8"));
            os.flush();
            os.close();

            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                byte[] data = readFully(is);
                is.close();
                return new String(data, "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    public static String doPost(String url, String userAgent, String refererURL, List<Pair> nameValuePair) {
        HttpURLConnection conn = null;
        try {
            StringBuilder sb = new StringBuilder();
            if (nameValuePair != null) {
                for (int i = 0; i < nameValuePair.size(); i++) {
                    if (i > 0) sb.append("&");
                    Pair p = nameValuePair.get(i);
                    sb.append(p.first).append("=").append(p.second);
                }
            }

            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Cache-Control", "max-age=0");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("Referer", refererURL);
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            OutputStream os = conn.getOutputStream();
            os.write(sb.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                byte[] data = readFully(is);
                is.close();
                return new String(data, "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    public boolean netConn() {
        LoadHelper loadHelper = new LoadHelper();
        loadHelper.get("http://www.baidu.com/");
        try {
            while (!loadHelper.isDone())
                Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return loadHelper.isConnOK();
    }

    private static byte[] readFully(InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = is.read(buf)) != -1) {
            bos.write(buf, 0, len);
        }
        return bos.toByteArray();
    }

    public static class Pair {
        public String first;
        public String second;
        public Pair(String first, String second) {
            this.first = first;
            this.second = second;
        }
    }

    private class LoadHelper {
        public boolean connOK;
        private String link;
        private boolean done;
        private Runnable httpHelperRunnable = new Runnable() {
            @Override
            public void run() {
                String useragent = "Mozilla/5.0 (iPad; CPU OS 5_0 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/7534.48.3";
                String referer = "http://video.baidu.com/movie/?order=hot";
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(link);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                    conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
                    conn.setRequestProperty("Referer", referer);
                    conn.setRequestProperty("Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
                    conn.setRequestProperty("User-Agent", useragent);
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setUseCaches(false);
                    conn.connect();
                    Log.e("net conn ", " code  " + conn.getResponseCode());
                    Log.e("net conn ", " OK ");
                    connOK = true;
                } catch (Exception e) {
                    Log.e("net conn ", " ERROR ");
                } finally {
                    if (conn != null) conn.disconnect();
                }
                Log.e("net conn ", "done ");
                done = true;
            }
        };

        public void get(String url) {
            link = url;
            done = false;
            connOK = false;
            new Thread(httpHelperRunnable).start();
        }

        public boolean isDone() {
            return done;
        }

        public boolean isConnOK() {
            return connOK;
        }

        public Map<String, String> getHttpResponseHeader(HttpURLConnection http) {
            Map<String, String> header = new LinkedHashMap<>();
            for (int i = 0; ; i++) {
                String mine = http.getHeaderField(i);
                if (mine == null) break;
                header.put(http.getHeaderFieldKey(i), mine);
            }
            return header;
        }

        public void printResponseHeader(HttpURLConnection http) {
            Map<String, String> header = getHttpResponseHeader(http);
            for (Map.Entry<String, String> entry : header.entrySet()) {
                String key = entry.getKey() != null ? entry.getKey() + ":" : "";
                Log.e("hdr ", key + entry.getValue());
            }
        }
    }
}
