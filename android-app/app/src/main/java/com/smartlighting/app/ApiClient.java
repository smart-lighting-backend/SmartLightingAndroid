package com.smartlighting.app;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {
    public interface AuthFailureHandler {
        void onAuthFailure();
    }

    public interface Callback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String token;
    private AuthFailureHandler authFailureHandler;

    public ApiClient(String token) {
        this.token = token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setAuthFailureHandler(AuthFailureHandler authFailureHandler) {
        this.authFailureHandler = authFailureHandler;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public void login(String username, String password, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
        } catch (Exception e) {
            postError(callback, e.getMessage());
            return;
        }
        request("POST", "/api/auth/login", null, body, callback);
    }

    public void get(String path, JSONObject params, Callback callback) {
        request("GET", path, params, null, callback);
    }

    public void post(String path, JSONObject body, Callback callback) {
        request("POST", path, null, body == null ? new JSONObject() : body, callback);
    }

    public void put(String path, JSONObject body, Callback callback) {
        request("PUT", path, null, body == null ? new JSONObject() : body, callback);
    }

    public void delete(String path, Callback callback) {
        request("DELETE", path, null, null, callback);
    }

    private void request(String method, String path, JSONObject params, JSONObject body, Callback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String urlText = Constants.BASE_URL + path + buildQuery(params);
                conn = (HttpURLConnection) new URL(urlText).openConnection();
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(60000);
                conn.setRequestMethod(method);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                if (body != null && ("POST".equals(method) || "PUT".equals(method))) {
                    conn.setDoOutput(true);
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8));
                    writer.write(body.toString());
                    writer.flush();
                    writer.close();
                }

                int code = conn.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                String raw = readAll(stream);
                JSONObject response = raw.isEmpty() ? new JSONObject() : new JSONObject(raw);
                int bizCode = response.optInt("code", code);
                if (isAuthExpired(path, code, bizCode)) {
                    postAuthFailure(callback);
                } else if (code >= 200 && code < 300 && bizCode == 200) {
                    postSuccess(callback, response);
                } else {
                    postError(callback, response.optString("msg", "请求失败: HTTP " + code));
                }
            } catch (Exception e) {
                postError(callback, e.getMessage() == null ? "网络请求失败" : e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private boolean isAuthExpired(String path, int httpCode, int bizCode) {
        return !"/api/auth/login".equals(path) && (httpCode == 401 || bizCode == 401);
    }

    private String buildQuery(JSONObject params) throws Exception {
        if (params == null || params.length() == 0) return "";
        StringBuilder builder = new StringBuilder("?");
        Iterator<String> keys = params.keys();
        boolean first = true;
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = params.opt(key);
            if (value == null || JSONObject.NULL.equals(value)) continue;
            if (!first) builder.append('&');
            first = false;
            builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
            builder.append('=');
            builder.append(URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8.name()));
        }
        return first ? "" : builder.toString();
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) builder.append(line);
        reader.close();
        return builder.toString();
    }

    private void postSuccess(Callback callback, JSONObject response) {
        mainHandler.post(() -> callback.onSuccess(response));
    }

    private void postError(Callback callback, String message) {
        mainHandler.post(() -> callback.onError(message == null || message.isEmpty() ? "请求失败" : message));
    }

    private void postAuthFailure(Callback callback) {
        mainHandler.post(() -> {
            if (authFailureHandler != null) {
                authFailureHandler.onAuthFailure();
            } else {
                callback.onError("登录已过期，请重新登录");
            }
        });
    }
}
