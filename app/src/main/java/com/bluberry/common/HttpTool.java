package com.bluberry.common;


import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HttpTool {
    private static final String tag = "HttpTool";

    /**
     * @param s http://ip.qq.com/ 网页
     * @return
     */
    public static String getRealIP(String s) {
        String regexIP = "((?:(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d))";

        Pattern p = Pattern.compile(regexIP);
        Matcher m = p.matcher(s);

        if (m.find()) {
            return m.group();
        }

        return "N";
    }

    /**
     * @param s http://ip.qq.com/ 网页
     * @return
     */
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

    private class LoadHelper {
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
            Map<String, String> header = new LinkedHashMap<String, String>();

            for (int i = 0; ; i++) {
                String mine = http.getHeaderField(i);
                if (mine == null)
                    break;
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

        private Runnable httpHelperRunnable = new Runnable() {
            @Override
            public void run() {
                String useragent = "Mozilla/5.0 (iPad; CPU OS 5_0 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/7534.48.3";
                String referer = "http://video.baidu.com/movie/?order=hot";

                HttpURLConnection conn = null;
                try {
                    URL url = new URL(link);
                    conn = (HttpURLConnection) url.openConnection();

                    conn.setConnectTimeout(5 * 1000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                    conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
                    conn.setRequestProperty("Referer", referer);
                    conn.setRequestProperty("Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
                    conn.setRequestProperty("User-Agent", useragent);
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setUseCaches(false);
                    conn.connect();
                    //printResponseHeader(conn);
                    Log.e("net conn ", " code  " + conn.getResponseCode());
                    Log.e("net conn ", " OK ");
                    connOK = true;
					
					/*
					if (conn.getResponseCode() == 200) {	不能用 == 300 的方式判断是否联通, 302 也是成功
						Log.e("net conn ", " OK ");
						connOK = true;
					} else {
						Log.e("net conn ", "server no response ");
					}	*/
                } catch (Exception e) {
                    Log.e("net conn ", " ERROR ");
                    //e.printStackTrace();
                } finally {
                    conn.disconnect();

                }

                Log.e("net conn ", "done ");
                done = true;
            }
        };

        private String link;
        public boolean connOK;
        private boolean done;
    }


    public boolean netConn() {
        LoadHelper loadHelper = new LoadHelper();
        loadHelper.get("http://www.baidu.com/");

        try {
            while (loadHelper.isDone() == false)
                Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return loadHelper.isConnOK();
    }

    public static long getContentLength(String url) {
        try {
            HttpGet hg = new HttpGet(url);
            BasicHttpParams param = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(param, 8000);
            HttpResponse resp = new DefaultHttpClient(param).execute(hg);

            if ((resp != null) && (200 == resp.getStatusLine().getStatusCode())) {
                return resp.getEntity().getContentLength();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0L;
    }


    public final static String UA_IPAD = "Mozilla/5.0 (iPad; CPU OS 5_0 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/7534.48.3";
    public final static String UA_CHROME = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.62 Safari/537.36";

    public final static String REF_BAIDU = "http://www.baidu.com/";

    public static String doGET(String url) {
        return doGET(url, UA_CHROME, REF_BAIDU);
    }

    public static String doGET(String url, String userAgent, String refererURL) {
        try {
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            //httpGet.setHeader("Accept-Encoding", "gzip"); // enable gzip
            httpGet.setHeader("Cache-Control", "max-age=0");
            httpGet.setHeader("Connection", "close");
            httpGet.setHeader("Referer", refererURL);
            httpGet.setHeader("User-Agent", userAgent);

            BasicHttpParams param = new BasicHttpParams();

            /* 连接超时 */
            HttpConnectionParams.setConnectionTimeout(param, 8000);
            /* 请求超时 */
            HttpConnectionParams.setSoTimeout(param, 8000);

            HttpResponse resp = new DefaultHttpClient(param).execute(httpGet);

            if ((resp != null) && (HttpStatus.SC_OK == resp.getStatusLine().getStatusCode())) {
                return EntityUtils.toString(resp.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static String doPost(String url, String userAgent, String refererURL, String postString) {
        try {
            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            // httpGet.setHeader("Accept-Encoding", "gzip"); // enable gzip
            httpPost.setHeader("Cache-Control", "max-age=0");
            httpPost.setHeader("Connection", "close");
            httpPost.setHeader("Referer", refererURL);
            httpPost.setHeader("User-Agent", userAgent);

            StringEntity reqEntity;
            try {
                reqEntity = new StringEntity(postString);    //format:  "firstname=abcde&lastname=fghi"
                //reqEntity.setContentType("application/x-www-form-urlencoded");
                httpPost.setEntity(reqEntity);
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            BasicHttpParams param = new BasicHttpParams();
            /* 连接超时 */
            HttpConnectionParams.setConnectionTimeout(param, 8000);
            /* 请求超时 */
            HttpConnectionParams.setSoTimeout(param, 8000);
            HttpResponse resp = new DefaultHttpClient(param).execute(httpPost);

            if ((resp != null) && (HttpStatus.SC_OK == resp.getStatusLine().getStatusCode())) {
                return EntityUtils.toString(resp.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String doPost(String url, String userAgent, String refererURL, List<BasicNameValuePair> nameValuePair) {
        try {
            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            // httpGet.setHeader("Accept-Encoding", "gzip"); // enable gzip
            httpPost.setHeader("Cache-Control", "max-age=0");
            httpPost.setHeader("Connection", "close");
            httpPost.setHeader("Referer", refererURL);
            httpPost.setHeader("User-Agent", userAgent);

			/*	Post Data
	        List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(2);
	        nameValuePair.add(new BasicNameValuePair("username", "test_user"));
	        nameValuePair.add(new BasicNameValuePair("password", "123456789"));
	 		*/

            //Encoding POST data
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            BasicHttpParams param = new BasicHttpParams();
            /* 连接超时 */
            HttpConnectionParams.setConnectionTimeout(param, 8000);
            /* 请求超时 */
            HttpConnectionParams.setSoTimeout(param, 8000);
            HttpResponse resp = new DefaultHttpClient(param).execute(httpPost);

            if ((resp != null) && (HttpStatus.SC_OK == resp.getStatusLine().getStatusCode())) {
                return EntityUtils.toString(resp.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
