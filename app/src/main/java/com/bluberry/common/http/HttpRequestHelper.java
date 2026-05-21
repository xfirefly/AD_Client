package com.bluberry.common.http;

import com.bluberry.common.IOUtil;
import com.bluberry.common.print;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpRequestHelper {
    private static final String TAG = "HttpRequestHelper";
    private static final String DEFAULT_UA = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36";

    private static HttpRequestHelper instance;
    private OkHttpClient httpClient;

    public HttpRequestHelper() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(9, TimeUnit.SECONDS)
                .readTimeout(9, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }

    public HttpRequestHelper(String userAgent) {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(9, TimeUnit.SECONDS)
                .readTimeout(9, TimeUnit.SECONDS)
                .followRedirects(true)
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .header("User-Agent", userAgent)
                                .build()))
                .build();
    }

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

    public void shutdown() {
        synchronized (HttpRequestHelper.class) {
            if (httpClient != null) {
                httpClient.dispatcher().executorService().shutdown();
                httpClient.connectionPool().evictAll();
            }
        }
    }

    public byte[] post(String url, Map<String, String> params) {
        return post(url, params, null);
    }

    public byte[] post(String url, Map<String, String> params, String contentType) {
        try {
            FormBody.Builder formBuilder = new FormBody.Builder();
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    formBuilder.add(entry.getKey(), entry.getValue());
                }
            }

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Cache-Control", "max-age=0")
                    .header("Connection", "keep-alive")
                    .post(formBuilder.build());

            if (contentType != null) {
                requestBuilder.header("Content-Type", contentType);
            }

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().bytes();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getUTF8String(String url) {
        return getString(url, "UTF-8");
    }

    public String getUTF8String(String url, String referer) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Cache-Control", "max-age=0")
                    .header("Connection", "keep-alive")
                    .header("Referer", referer)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getGBKString(String url) {
        return getString(url, "GBK");
    }

    public String getString(String url, String charset) {
        try {
            byte[] data = getBytes(url);
            if (data != null) {
                return new String(data, charset);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public InputStream getStream(String url) {
        byte[] data = getBytes(url);
        if (data != null) {
            return new java.io.ByteArrayInputStream(data);
        }
        return null;
    }

    public byte[] getBytes(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Cache-Control", "max-age=0")
                    .header("Connection", "keep-alive")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().bytes();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean download(String url, String filePath) {
        File file = new File(filePath);
        if (file.exists())
            file.delete();
        else
            file.getParentFile().mkdirs();

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Cache-Control", "max-age=0")
                    .header("Connection", "keep-alive")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    try (InputStream is = response.body().byteStream();
                         OutputStream os = new FileOutputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                        os.flush();
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getWithQuery(String url, String... query) {
        StringBuilder sb = new StringBuilder(url);
        sb.append("?");
        for (int i = 0; i < query.length - 1; i += 2) {
            if (i > 0) sb.append("&");
            sb.append(query[i]).append("=").append(query[i + 1]);
        }
        return getString(sb.toString(), "UTF-8");
    }
}
