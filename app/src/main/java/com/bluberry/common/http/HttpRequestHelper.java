/*
 * Copyright (C) 2012 ENTERTAILION LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluberry.common.http;

import com.bluberry.common.IOUtil;
import com.bluberry.common.print;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * see:
 * http://hc.apache.org/httpcomponents-client-dev/tutorial/html/connmgmt.html
 * <p>
 * <p>
 * Thread safe , Utility class to handle HTTP requests.
 * <p>
 * 如果同时需要下载多个文件, 比如 vod 的海报列表页 需要 new HttpRequestHelper() 来处理, 其他情况可使用
 * getInstance() 返回的对象
 * <p>
 * 在做耗时较长的http请求交互的时候，重新new一个httpClient对象，而不是用一个单例的httpclient对象进行管理所有的http请求。
 */
public class HttpRequestHelper {
    private static final String TAG = "HttpRequestHelper";

    public final static String UA_IPAD = "Mozilla/5.0 (iPad; CPU OS 5_0 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/7534.48.3";
    public final static String UA_CHROME = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.62 Safari/537.36";

    public final static String REF_BAIDU = "http://www.baidu.com/";

    // private IdleConnectionMonitorThread monitorThread;

    private DefaultHttpClient httpClient;
    private String refererURL = REF_BAIDU;

    private static HttpRequestHelper instance;


    /**
     * Returns singleton class instance
     */
    public static HttpRequestHelper getInstance() {
        if (instance == null) {
            synchronized (HttpRequestHelper.class) {
                if (instance == null) {
                    instance = new HttpRequestHelper();
                }
            }
        }
        return instance;
    }

    public HttpRequestHelper() {
        httpClient = createHttpClient(UA_CHROME);

        // monitorThread = new IdleConnectionMonitorThread();
        // monitorThread.start();
    }

    public HttpRequestHelper(String userAgent) {
        httpClient = createHttpClient(userAgent);
    }

    /**
     * it is important to shut down its connection manager to ensure that all
     * connections kept alive by the manager get closed and system resources
     * allocated by those connections are released.
     */
    public void shutdown() {
        // monitorThread.shutdown();
        synchronized (HttpRequestHelper.class) {
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
                print.e(TAG, "shutdown");
            }
        }
    }

    private DefaultHttpClient createHttpClient(String userAgent) {
        HttpParams params = new BasicHttpParams();

        // FIX: Invalid cookie header
        HttpClientParams.setCookiePolicy(params, CookiePolicy.BROWSER_COMPATIBILITY);

        // 设置一些基本参数
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET); // HTTP.UTF_8
        HttpProtocolParams.setUserAgent(params, userAgent);


        // 设置最大连接数 , 默认 20
        ConnManagerParams.setMaxTotalConnections(params, 200);
        // 设置每个路由最大连接数 , 默认 2
        ConnPerRouteBean connPerRoute = new ConnPerRouteBean(20);
        ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);

        /* 从连接池中取连接的超时时间 */
        ConnManagerParams.setTimeout(params, 5000);
        /* 连接超时 */
        HttpConnectionParams.setConnectionTimeout(params, 9000);
        /* 请求超时 */
        HttpConnectionParams.setSoTimeout(params, 9000);

        // 设置我们的HttpClient支持HTTP和HTTPS两种模式
        SchemeRegistry schReg = new SchemeRegistry();
        schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

        // 使用线程安全的连接管理来创建HttpClient
        ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);

        return new DefaultHttpClient(conMgr, params);
    }

    public void clearCookies() {
        httpClient.getCookieStore().clear();
    }

    /*
     * public void abort() { try { if (httpClient != null) { print.i(TAG,
     * "Abort."); httpPost.abort(); } } catch (Throwable e) {
     * e.printStackTrace(); print.e(TAG, "Failed to abort", e); } }
     */

    /**
     * post string
     *
     * @param url
     * @param params key-value pair
     * @return
     */
    public byte[] post(String url, Map<String, String> params) {
        return post(url, params, null);
    }

    public byte[] post(String url, Map<String, String> params, String contentType) {
        HttpContext localContext = new BasicHttpContext();
        HttpPost httpPost = null;

        try {
            // httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY,
            // CookiePolicy.RFC_2109);

            httpPost = new HttpPost(url);
            HttpResponse response = null;

            // set http Header
            httpPost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            httpPost.setHeader("Accept-Encoding", "gzip"); // enable gzip
            httpPost.setHeader("Cache-Control", "max-age=0");
            httpPost.setHeader("Connection", "keep-alive");
            httpPost.setHeader("Referer", refererURL);

            if (contentType != null) {
                httpPost.setHeader("Content-Type", contentType);
            } else {
                httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            }

            httpPost.setEntity(buildData(params));
            response = httpClient.execute(httpPost, localContext);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                httpPost.abort();
            } else {
                // ret = EntityUtils.toString(response.getEntity());

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                getResponseContent(response, bos);
                return bos.toByteArray();
            }
        } catch (Throwable e) {
            httpPost.abort();
            e.printStackTrace();
        }

        return null;
    }

    /**
     * get utf-8 encode string
     *
     * @param url
     * @return 失败返回 null
     */
    public String getUTF8String(String url) {
        return getString(url, "UTF-8");
    }


    public String getUTF8String(String url, String referer) {
        refererURL = referer;

        return getString(url, "UTF-8");
    }


    /**
     * get gbk encode string
     *
     * @param url
     * @return 失败返回 null
     */
    public String getGBKString(String url) {
        return getString(url, "GBK");
    }

    /**
     * get string
     *
     * @param url
     * @param charset "ISO-8859-1", "UTF-8", "GBK"
     * @return 失败返回 null
     */
    public String getString(String url, String charset) {
        try {
            byte[] data = getBytes(url);
            if (data != null) {
                return new String(data, charset);
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * get stream
     *
     * @param url
     * @param charset GBK , UTF-8
     * @return 失败返回 null
     */
    public InputStream getStream(String url) {
        byte[] data = getBytes(url);
        if (data != null) {
            return new ByteArrayInputStream(data);
        }

        return null;
    }

    /**
     * get byte array
     *
     * @param url
     * @param charset GBK , UTF-8
     * @return 失败返回 null
     */
    public byte[] getBytes(String url) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        doGet(url, bos);
        if (bos.size() > 0)
            return bos.toByteArray();
        else
            return null;
    }

    /**
     * 下载文件, 大一点的文件使用这个函数下载
     *
     * @param url
     * @param filePath
     * @return 失败返回 false
     */
    public boolean download(String url, String filePath) {
        File file = new File(filePath);
        if (file.exists())
            file.delete();
        else
            file.getParentFile().mkdirs(); // 建立上级目录

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return doGet(url, fos);
    }

    private boolean doGet(String url, OutputStream os) {
        HttpResponse response = null;
        HttpGet httpGet = new HttpGet(url);

        // print.e(TAG, url);

        // set http Header
        httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        httpGet.setHeader("Accept-Encoding", "gzip"); // enable gzip
        httpGet.setHeader("Cache-Control", "max-age=0");
        httpGet.setHeader("Connection", "keep-alive");
        httpGet.setHeader("Referer", refererURL);

        boolean result = false;
        try {
            response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                httpGet.abort();
            } else {
                result = getResponseContent(response, os);
            }
        } catch (Exception e) {
            httpGet.abort();
            e.printStackTrace();
        }

        return result;
    }


    /**
     * return UTF-8 encode string
     *
     * @param url
     * @param query
     * @return
     */
    public String getWithQuery(String url, String... query) {
        String queryParams = buildgetData(query);
        return getString(url + "?" + queryParams, "UTF-8");
    }

    /**
     * 返回解压过的 byte array
     *
     * @param response
     * @return
     * @throws IOException
     */
    private boolean getResponseContent(HttpResponse response, OutputStream os) throws Exception {
        HttpEntity entity = response.getEntity();
        InputStream is = entity.getContent();
        Header header = entity.getContentEncoding();
        long len = entity.getContentLength();
        boolean result = false;

        try {
            if (header != null && header.getValue().equalsIgnoreCase("gzip")) {
                print.i(TAG, "gzip len:" + len);

                IOUtil.decompressGZIP(is, os);
            } else {
                // print.i(TAG, "byte:" + len);

                IOUtil.writeStream(is, os, len);
            }

            result = true;
        } catch (IOException e) {
            throw e;
        } finally {
            is.close(); // 下载完成, 释放资源

            entity.consumeContent(); // 保证连接能释放回管理器
        }

        return result;
    }

    private HttpEntity buildData(Map<String, String> map) throws UnsupportedEncodingException {
        if (null != map && !map.isEmpty()) {
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            for (String name : map.keySet()) {
                parameters.add(new BasicNameValuePair(name, map.get(name)));
            }
            return new UrlEncodedFormEntity(parameters, "UTF-8");
        }
        return null;
    }

    private String buildgetData(String... list) {
        if (null == list || list.length == 0)
            return null;
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        for (int i = 0; i < list.length - 1; i = i + 2) {
            qparams.add(new BasicNameValuePair(list[i], list[i + 1]));
        }
        return URLEncodedUtils.format(qparams, "UTF-8");
    }

    /**
     * see
     * http://hc.apache.org/httpcomponents-client-dev/tutorial/html/connmgmt.
     * html
     * <p>
     * <p>
     * 2.5. Connection eviction policy
     */
    private class IdleConnectionMonitorThread extends Thread {
        private boolean isShutdown = false;

        @Override
        public void run() {
            try {
                while (!isShutdown) {
                    synchronized (this) {
                        wait(10000);
                        if (isShutdown)
                            break;
                        print.e(TAG, "closeExpiredConnections & closeIdleConnections");
                        // Close expired connections
                        httpClient.getConnectionManager().closeExpiredConnections();
                        // Optionally, close connections
                        // that have been idle longer than 30 sec
                        httpClient.getConnectionManager().closeIdleConnections(30, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                // terminate
            }
        }

        public void shutdown() {
            isShutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    // unit test
    public static void test() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final HttpRequestHelper httpReq = new HttpRequestHelper();
                class GetThread extends Thread {
                    private String url;

                    public GetThread(String url) {
                        this.url = url;
                    }

                    @Override
                    public void run() {
                        // String file =
                        // Environment.getExternalStorageDirectory().getAbsolutePath()
                        // + "/xzq";
                        String str = httpReq.getUTF8String(url);
                        if (str != null)
                            print.e(TAG, "GOT : " + str.substring(0, 30));
                    }
                }

                print.e(TAG, "test begin");

                final int LEN = 200;

                // create a thread for each URI
                Thread[] threads = new Thread[LEN];
                for (int i = 0; i < threads.length; i++) {
                    threads[i] = new GetThread("http://www.baidu.com/");
                }

                // start the threads
                for (int j = 0; j < threads.length; j++) {
                    threads[j].start();
                }

                // join the threads
                for (int j = 0; j < threads.length; j++) {
                    try {
                        threads[j].join();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                httpReq.shutdown();
                print.e(TAG, "test end ");

            }
        }).start();
    }
}
