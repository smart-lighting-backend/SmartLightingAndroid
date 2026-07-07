package com.smartlighting.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("SetTextI18n")
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(248, 250, 252);
    private static final int SURFACE = Color.rgb(255, 255, 255);
    private static final int FIELD_BG = Color.rgb(248, 250, 252);
    private static final int INK = Color.rgb(30, 41, 59);
    private static final int INK_DARK = Color.rgb(15, 23, 42);
    private static final int MUTED = Color.rgb(71, 85, 105);
    private static final int PRIMARY = Color.rgb(37, 99, 235);
    private static final int PRIMARY_DARK = Color.rgb(29, 78, 216);
    private static final int PRIMARY_SOFT = Color.rgb(219, 234, 254);
    private static final int BORDER = Color.rgb(226, 232, 240);
    private static final int CTA = Color.rgb(249, 115, 22);
    private static final int GREEN = Color.rgb(22, 160, 133);
    private static final int RED = Color.rgb(221, 72, 72);
    private static final int AMBER = Color.rgb(230, 147, 40);

    private final DecimalFormat numberFormat = new DecimalFormat("#,##0.##");
    private SharedPreferences prefs;
    private ApiClient api;
    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout tabBar;
    private String activeTab = "dashboard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        api = new ApiClient(prefs.getString(Constants.KEY_TOKEN, ""));
        api.setAuthFailureHandler(this::handleAuthExpired);
        if (prefs.getString(Constants.KEY_TOKEN, "").isEmpty()) {
            showLogin();
        } else {
            verifyToken();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (api != null) api.shutdown();
    }

    private void handleAuthExpired() {
        prefs.edit().clear().apply();
        api.setToken("");
        toast("登录已过期，请重新登录");
        showLogin();
    }

    @Override
    public void onBackPressed() {
        if (content != null && !"dashboard".equals(activeTab)) {
            showDashboard();
            return;
        }
        super.onBackPressed();
    }

    private void verifyToken() {
        api.get("/api/auth/me", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject data = response.optJSONObject("data");
                if (data != null) {
                    prefs.edit()
                        .putString(Constants.KEY_USERNAME, data.optString("username", "admin"))
                        .apply();
                }
                showMain();
            }

            @Override
            public void onError(String message) {
                prefs.edit().clear().apply();
                api.setToken("");
                showLogin();
            }
        });
    }

    private void showLogin() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(dp(24), dp(24), dp(24), dp(24));
        page.setBackground(verticalGradient(Color.rgb(8, 20, 44), Color.rgb(15, 35, 75)));

        TextView title = text("智慧路灯管理系统", 28, Color.WHITE, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        TextView subtitle = text("移动巡检与控制终端", 14, Color.rgb(191, 219, 254), Typeface.NORMAL);
        subtitle.setGravity(Gravity.CENTER);

        LinearLayout card = card();
        card.setPadding(dp(22), dp(22), dp(22), dp(22));
        EditText username = input("用户名", false);
        username.setText("admin");
        EditText password = input("密码", true);
        password.setText("admin123");
        Button login = primaryButton("登录");
        TextView hint = text("公网地址: " + Constants.BASE_URL, 12, MUTED, Typeface.NORMAL);

        card.addView(label("账号"));
        card.addView(username);
        card.addView(space(10));
        card.addView(label("密码"));
        card.addView(password);
        card.addView(space(16));
        card.addView(login, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        card.addView(space(12));
        card.addView(hint);

        page.addView(title);
        page.addView(space(8));
        page.addView(subtitle);
        page.addView(space(30));
        int loginWidth = Math.min(getResources().getDisplayMetrics().widthPixels - dp(48), dp(420));
        page.addView(card, new LinearLayout.LayoutParams(loginWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

        login.setOnClickListener(v -> {
            String user = username.getText().toString().trim();
            String pass = password.getText().toString();
            if (user.isEmpty() || pass.isEmpty()) {
                toast("请输入用户名和密码");
                return;
            }
            login.setEnabled(false);
            login.setText("登录中...");
            api.login(user, pass, new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject response) {
                    JSONObject data = response.optJSONObject("data");
                    String token = data == null ? "" : data.optString("token", "");
                    if (token.isEmpty()) {
                        onError("登录响应缺少 Token");
                        return;
                    }
                    prefs.edit()
                        .putString(Constants.KEY_TOKEN, token)
                        .putString(Constants.KEY_USERNAME, data.optString("username", user))
                        .apply();
                    api.setToken(token);
                    showMain();
                }

                @Override
                public void onError(String message) {
                    login.setEnabled(true);
                    login.setText("登录");
                    toast(message);
                }
            });
        });

        setContentView(page);
    }

    private void showMain() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        root.addView(buildHeader(), widthMatch());
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setGravity(Gravity.CENTER);
        tabBar.setPadding(dp(10), dp(8), dp(10), dp(10));
        tabBar.setBackground(rounded(SURFACE, dp(18), BORDER));
        tabBar.setElevation(dp(6));
        root.addView(tabBar, widthMatch());

        setContentView(root);
        showDashboard();
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(16), dp(14), dp(14));
        header.setBackground(verticalGradient(INK_DARK, Color.rgb(30, 64, 175)));
        header.setElevation(dp(4));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("智慧路灯运维", 20, Color.WHITE, Typeface.BOLD);
        TextView status = pill("在线", GREEN);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.setMargins(dp(8), 0, 0, 0);
        titleRow.addView(title);
        titleRow.addView(status, statusLp);
        TextView sub = text(prefs.getString(Constants.KEY_USERNAME, "admin") + " · 公网巡检终端", 12, Color.rgb(191, 219, 254), Typeface.NORMAL);
        titleBox.addView(titleRow);
        titleBox.addView(sub);
        header.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button logout = darkButton("退出");
        logout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            api.setToken("");
            showLogin();
        });
        header.addView(logout);
        return header;
    }

    private void renderTabs() {
        tabBar.removeAllViews();
        addTab("dashboard", "首页");
        addTab("devices", "设备");
        addTab("analytics", "报表");
        addTab("assistant", "助手");
        addTab("alarms", "告警");
    }

    private void addTab(String key, String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTextColor(key.equals(activeTab) ? Color.WHITE : MUTED);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setBackground(key.equals(activeTab)
            ? statefulRounded(PRIMARY, PRIMARY_DARK, dp(18), PRIMARY)
            : statefulRounded(Color.TRANSPARENT, PRIMARY_SOFT, dp(18), Color.TRANSPARENT));
        button.setOnClickListener(v -> {
            if ("dashboard".equals(key)) showDashboard();
            if ("devices".equals(key)) showDevices("");
            if ("analytics".equals(key)) showAnalytics();
            if ("assistant".equals(key)) showAssistant();
            if ("alarms".equals(key)) showAlarms();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1);
        lp.setMargins(dp(2), 0, dp(2), 0);
        tabBar.addView(button, lp);
    }

    private void startTab(String tab, String title) {
        activeTab = tab;
        content.removeAllViews();
        content.addView(sectionTitle(title));
        renderTabs();
    }

    private void showDashboard() {
        startTab("dashboard", "数字孪生概览");
        LinearLayout statsGrid = new LinearLayout(this);
        statsGrid.setOrientation(LinearLayout.VERTICAL);
        content.addView(statsGrid);
        content.addView(space(12));
        content.addView(cardText("边缘 AI 状态", "加载中..."));
        content.addView(edgeTriggerCard());
        content.addView(cardText("最近 AI 决策", "加载中..."));
        content.addView(space(12));
        content.addView(cardText("能耗趋势", "加载中..."));
        content.addView(space(12));
        content.addView(cardText("分区设备状态", "加载中..."));

        api.get("/api/dashboard/stats", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject d = response.optJSONObject("data");
                statsGrid.removeAllViews();
                if (d == null) return;
                addMetric(statsGrid, "设备总数", n(d, "totalDevices"), "在线 " + n(d, "onlineDevices"), PRIMARY);
                addMetric(statsGrid, "在线率", n(d, "onlineRate") + "%", "设备运行状态", GREEN);
                addMetric(statsGrid, "未处理告警", n(d, "alertCount"), "点击底部告警查看", RED);
                addMetric(statsGrid, "节能率", n(d, "energySavingRate") + "%", "今日能耗 " + n(d, "todayEnergy") + " kWh", AMBER);
            }

            @Override
            public void onError(String message) {
                addError(statsGrid, message);
            }
        });

        loadEdgeStatus();
        loadEdgeRecent();

        api.get("/api/dashboard/energy-trend", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject d = response.optJSONObject("data");
                replaceCard("能耗趋势", d == null ? null : lineChart(d.optJSONArray("current"), d.optJSONArray("lastWeek")));
            }

            @Override
            public void onError(String message) {
                replaceCard("能耗趋势", text(message, 13, RED, Typeface.NORMAL));
            }
        });

        api.get("/api/dashboard/districts", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray arr = response.optJSONArray("data");
                LinearLayout list = new LinearLayout(MainActivity.this);
                list.setOrientation(LinearLayout.VERTICAL);
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject d = arr.optJSONObject(i);
                        if (d == null) continue;
                        list.addView(text(d.optString("name") + "  在线 " + d.optInt("online") + " / 离线 " + d.optInt("offline") + " / 告警 " + d.optInt("warning"), 14, INK, Typeface.NORMAL));
                        list.addView(space(6));
                    }
                }
                replaceCard("分区设备状态", list);
            }

            @Override
            public void onError(String message) {
                replaceCard("分区设备状态", text(message, 13, RED, Typeface.NORMAL));
            }
        });
    }

    private LinearLayout edgeTriggerCard() {
        LinearLayout card = card();
        addCardHeader(card, "边缘 AI 联动", PRIMARY);
        card.addView(space(10));
        card.addView(text("手动触发一次边缘模拟，刷新策略命中和控制决策。", 13, MUTED, Typeface.NORMAL));
        card.addView(space(10));
        Button trigger = primaryButton("触发边缘模拟");
        trigger.setOnClickListener(v -> confirm("触发边缘模拟", "系统将立即执行一次边缘 AI 模拟并刷新首页数据。", () -> triggerEdgeSimulation(trigger)));
        card.addView(trigger, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        return card;
    }

    private void loadEdgeStatus() {
        api.get("/api/dashboard/edge-status", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject d = response.optJSONObject("data");
                if (d == null) {
                    replaceCard("边缘 AI 状态", text("暂无数据", 13, MUTED, Typeface.NORMAL));
                    return;
                }
                LinearLayout box = new LinearLayout(MainActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.addView(pill(d.optBoolean("enabled", false) ? "运行中" : "已停用", d.optBoolean("enabled", false) ? GREEN : MUTED));
                TextView total = text("  决策 " + n(d, "totalDecisions") + " · 命中 " + n(d, "hitCount"), 14, INK, Typeface.BOLD);
                row.addView(total);
                box.addView(row);
                box.addView(space(8));
                box.addView(text("最近模拟: " + valueOf(d, "lastSimulatedAt"), 13, MUTED, Typeface.NORMAL));
                replaceCard("边缘 AI 状态", box);
            }

            @Override
            public void onError(String message) {
                replaceCard("边缘 AI 状态", text(message, 13, RED, Typeface.NORMAL));
            }
        });
    }

    private void loadEdgeRecent() {
        api.get("/api/dashboard/edge/recent", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray arr = response.optJSONArray("data");
                if (arr == null || arr.length() == 0) {
                    replaceCard("最近 AI 决策", text("暂无记录", 13, MUTED, Typeface.NORMAL));
                    return;
                }
                LinearLayout box = new LinearLayout(MainActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                int count = Math.min(8, arr.length());
                for (int i = 0; i < count; i++) {
                    JSONObject item = arr.optJSONObject(i);
                    if (item == null) continue;
                    box.addView(text(valueOf(item, "createTime") + "  " + item.optString("deviceId", "-"), 13, MUTED, Typeface.NORMAL));
                    box.addView(text(item.optString("matchedPolicy", "-") + " · " + item.optString("actionTaken", "-"), 14, INK, Typeface.BOLD));
                    String result = item.optString("result", "-");
                    box.addView(pill(result, result.contains("EXECUTED") ? GREEN : AMBER));
                    if (i < count - 1) box.addView(space(8));
                }
                replaceCard("最近 AI 决策", box);
            }

            @Override
            public void onError(String message) {
                replaceCard("最近 AI 决策", text(message, 13, RED, Typeface.NORMAL));
            }
        });
    }

    private void triggerEdgeSimulation(Button trigger) {
        trigger.setEnabled(false);
        trigger.setText("触发中...");
        api.post("/api/dashboard/edge/trigger", new JSONObject(), new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                toast("边缘模拟已触发");
                showDashboard();
            }

            @Override
            public void onError(String message) {
                trigger.setEnabled(true);
                trigger.setText("触发边缘模拟");
                toast(message);
            }
        });
    }

    private void showDevices(String keyword) {
        startTab("devices", "设备管理");
        LinearLayout search = new LinearLayout(this);
        search.setOrientation(LinearLayout.HORIZONTAL);
        EditText keywordInput = input("设备名称/编号", false);
        keywordInput.setText(keyword);
        Button searchBtn = smallButton("搜索");
        search.addView(keywordInput, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams searchButtonLp = new LinearLayout.LayoutParams(dp(74), dp(48));
        searchButtonLp.setMargins(dp(8), 0, 0, 0);
        search.addView(searchBtn, searchButtonLp);
        content.addView(search);
        content.addView(space(10));

        Button add = primaryButton("新增");
        Button batchAdd = smallButton("批量新增");
        Button batchArea = smallButton("分配区域");
        add.setOnClickListener(v -> showDeviceForm(null, null));
        batchAdd.setOnClickListener(v -> showBatchAddDevices());
        batchArea.setOnClickListener(v -> showBatchAssignArea());
        addButtonRow(content, add, batchAdd, batchArea);
        content.addView(space(10));

        content.addView(cardText("健康概览", "加载中..."));
        loadDeviceHealthSummary();

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list);
        searchBtn.setOnClickListener(v -> showDevices(keywordInput.getText().toString().trim()));

        JSONObject params = new JSONObject();
        try {
            params.put("pageNum", 1);
            params.put("pageSize", 30);
            if (!keyword.isEmpty()) params.put("keyword", keyword);
        } catch (Exception ignored) {
        }
        list.addView(cardText("设备列表", "加载中..."));
        api.get("/api/devices/page", params, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                list.removeAllViews();
                JSONArray records = records(response);
                if (records.length() == 0) {
                    list.addView(cardText("设备列表", "暂无设备，可点击上方“新增”创建设备"));
                    return;
                }
                for (int i = 0; i < records.length(); i++) {
                    JSONObject device = records.optJSONObject(i);
                    if (device != null) addDeviceCard(list, device);
                }
            }

            @Override
            public void onError(String message) {
                list.removeAllViews();
                list.addView(cardText("加载失败", message));
            }
        });
    }

    private void addDeviceCard(LinearLayout list, JSONObject device) {
        LinearLayout card = card();
        String id = device.optString("deviceId", device.optString("id"));
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(text(device.optString("name", id), 17, INK_DARK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        titleRow.addView(pill(statusName(device.optInt("status", -1)), statusColor(device.optInt("status", -1))));
        card.addView(titleRow);
        card.addView(space(6));
        card.addView(text(id + " · " + device.optString("area", "未分区"), 13, MUTED, Typeface.NORMAL));
        LinearLayout stateRow = new LinearLayout(this);
        stateRow.setOrientation(LinearLayout.HORIZONTAL);
        stateRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView score = text("健康分 " + n(device, "healthScore"), 13, MUTED, Typeface.NORMAL);
        stateRow.addView(score);
        card.addView(space(8));
        card.addView(stateRow);
        Button detail = smallButton("详情");
        Button control = smallButton("控制");
        Button edit = smallButton("编辑");
        Button delete = dangerButton("删除");
        detail.setOnClickListener(v -> showDeviceDetail(id));
        control.setOnClickListener(v -> showDeviceControl(id, device.optString("name", id)));
        edit.setOnClickListener(v -> showDeviceForm(id, device));
        delete.setOnClickListener(v -> deleteDevice(id, device.optString("name", id)));
        card.addView(space(10));
        addButtonRow(card, detail, control);
        card.addView(space(8));
        addButtonRow(card, edit, delete);
        list.addView(card);
        list.addView(space(10));
    }

    private void loadDeviceHealthSummary() {
        api.get("/api/devices/health/summary", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject d = response.optJSONObject("data");
                if (d == null) {
                    replaceCard("健康概览", text("暂无数据", 13, MUTED, Typeface.NORMAL));
                    return;
                }
                LinearLayout box = new LinearLayout(MainActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.addView(text("总数 " + n(d, "totalDevices") + " · 平均分 " + n(d, "averageScore"), 15, INK, Typeface.BOLD));
                box.addView(text("健康 " + n(d, "healthyCount") + "  预警 " + n(d, "warningCount") + "  严重 " + n(d, "criticalCount"), 13, MUTED, Typeface.NORMAL));
                JSONArray list = d.optJSONArray("list");
                if (list != null && list.length() > 0) {
                    box.addView(space(8));
                    int count = Math.min(4, list.length());
                    for (int i = 0; i < count; i++) {
                        JSONObject item = list.optJSONObject(i);
                        if (item == null) continue;
                        box.addView(text(item.optString("deviceId", "-") + " · " + item.optString("name", "-") + " · " + n(item, "score") + "分 · " + item.optString("level", "-"), 13, MUTED, Typeface.NORMAL));
                    }
                }
                replaceCard("健康概览", box);
            }

            @Override
            public void onError(String message) {
                replaceCard("健康概览", text(message, 13, RED, Typeface.NORMAL));
            }
        });
    }

    private void showDeviceForm(String deviceId, JSONObject source) {
        boolean editing = deviceId != null && !deviceId.isEmpty();
        startTab("devices", editing ? "编辑设备" : "新增设备");
        Button back = smallButton("返回设备列表");
        back.setOnClickListener(v -> showDevices(""));
        content.addView(back, widthMatch());
        content.addView(space(10));

        LinearLayout form = card();
        EditText idInput = input("设备编号", false);
        idInput.setText(editing ? deviceId : "");
        idInput.setEnabled(!editing);
        EditText name = input("设备名称", false);
        EditText area = input("区域", false);
        EditText location = input("位置", false);
        EditText status = numberInput("状态：0停用 1在线 2离线 3异常", false);
        EditText health = numberInput("健康分：0-100", true);
        EditText topicPrefix = input("MQTT Topic 前缀", false);
        CheckBox enabled = new CheckBox(this);
        enabled.setText("启用设备");
        enabled.setTextColor(INK);
        enabled.setTextSize(14);

        if (source != null) {
            name.setText(source.optString("name", ""));
            area.setText(source.optString("area", ""));
            location.setText(source.optString("location", ""));
            if (!source.isNull("status")) status.setText(String.valueOf(source.optInt("status")));
            if (!source.isNull("healthScore")) health.setText(n(source, "healthScore"));
            topicPrefix.setText(source.optString("topicPrefix", "streetlight"));
            enabled.setChecked(source.optBoolean("enabled", true));
        } else {
            status.setText("1");
            health.setText("100");
            topicPrefix.setText("streetlight");
            enabled.setChecked(true);
        }

        form.addView(label("设备编号"));
        form.addView(idInput, fieldHeight());
        form.addView(space(10));
        form.addView(label("设备名称"));
        form.addView(name, fieldHeight());
        form.addView(space(10));
        form.addView(label("区域"));
        form.addView(area, fieldHeight());
        form.addView(space(10));
        form.addView(label("位置"));
        form.addView(location, fieldHeight());
        form.addView(space(10));
        form.addView(label("运行状态"));
        form.addView(status, fieldHeight());
        form.addView(space(10));
        form.addView(label("健康分"));
        form.addView(health, fieldHeight());
        form.addView(space(10));
        form.addView(label("Topic 前缀"));
        form.addView(topicPrefix, fieldHeight());
        form.addView(space(8));
        form.addView(enabled);
        form.addView(space(12));

        Button save = primaryButton(editing ? "保存修改" : "创建设备");
        save.setOnClickListener(v -> saveDevice(editing, deviceId, idInput, name, area, location, status, health, topicPrefix, enabled, save));
        form.addView(save, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        content.addView(form);
    }

    private void saveDevice(boolean editing, String deviceId, EditText idInput, EditText name, EditText area,
                            EditText location, EditText status, EditText health, EditText topicPrefix,
                            CheckBox enabled, Button save) {
        String id = idInput.getText().toString().trim();
        if (id.isEmpty()) {
            toast("请输入设备编号");
            return;
        }
        JSONObject body = new JSONObject();
        try {
            if (!editing) body.put("deviceId", id);
            body.put("name", name.getText().toString().trim());
            body.put("area", area.getText().toString().trim());
            body.put("location", location.getText().toString().trim());
            body.put("topicPrefix", topicPrefix.getText().toString().trim());
            body.put("enabled", enabled.isChecked());
            String statusText = status.getText().toString().trim();
            if (!statusText.isEmpty()) {
                int statusValue = Integer.parseInt(statusText);
                if (statusValue < 0 || statusValue > 3) {
                    toast("状态只能是 0-3");
                    return;
                }
                body.put("status", statusValue);
            }
            String healthText = health.getText().toString().trim();
            if (!healthText.isEmpty()) {
                double healthValue = Double.parseDouble(healthText);
                if (healthValue < 0 || healthValue > 100) {
                    toast("健康分必须在 0-100");
                    return;
                }
                body.put("healthScore", healthValue);
            }
        } catch (Exception e) {
            toast("请检查状态和健康分格式");
            return;
        }

        save.setEnabled(false);
        save.setText("保存中...");
        ApiClient.Callback callback = new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                toast(editing ? "设备已更新" : "设备已创建");
                showDevices("");
            }

            @Override
            public void onError(String message) {
                save.setEnabled(true);
                save.setText(editing ? "保存修改" : "创建设备");
                toast(message);
            }
        };
        if (editing) {
            api.put("/api/devices/" + deviceId, body, callback);
        } else {
            api.post("/api/devices", body, callback);
        }
    }

    private void deleteDevice(String deviceId, String name) {
        confirm("删除设备", "设备 " + deviceId + "（" + name + "）将被软删除并停用。", () ->
            api.delete("/api/devices/" + deviceId, toastAndRun("设备已删除", () -> showDevices("")))
        );
    }

    private void showBatchAddDevices() {
        startTab("devices", "批量新增设备");
        Button back = smallButton("返回设备列表");
        back.setOnClickListener(v -> showDevices(""));
        content.addView(back, widthMatch());
        content.addView(space(10));

        LinearLayout form = card();
        form.addView(text("每行格式：设备编号,设备名称,区域,位置", 14, INK, Typeface.BOLD));
        form.addView(space(8));
        EditText rows = multiInput("SL-101,南门-03,A区,南门东侧");
        form.addView(rows, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        form.addView(space(12));
        Button submit = primaryButton("提交批量新增");
        submit.setOnClickListener(v -> submitBatchAdd(rows.getText().toString(), submit));
        form.addView(submit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        content.addView(form);
    }

    private void submitBatchAdd(String raw, Button submit) {
        List<JSONObject> devices = new ArrayList<>();
        String[] lines = raw.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("[,，]", -1);
            String deviceId = parts.length > 0 ? parts[0].trim() : "";
            if (deviceId.isEmpty()) {
                toast("第 " + (i + 1) + " 行缺少设备编号");
                return;
            }
            JSONObject body = new JSONObject();
            try {
                body.put("deviceId", deviceId);
                body.put("name", parts.length > 1 ? parts[1].trim() : deviceId);
                body.put("area", parts.length > 2 ? parts[2].trim() : "");
                body.put("location", parts.length > 3 ? parts[3].trim() : "");
                body.put("status", 1);
                body.put("healthScore", 100);
                body.put("topicPrefix", "streetlight");
                body.put("enabled", true);
            } catch (Exception ignored) {
            }
            devices.add(body);
        }
        if (devices.isEmpty()) {
            toast("请输入设备数据");
            return;
        }
        submit.setEnabled(false);
        submit.setText("提交中...");
        int[] done = {0};
        int[] success = {0};
        StringBuilder errors = new StringBuilder();
        for (JSONObject body : devices) {
            String id = body.optString("deviceId");
            api.post("/api/devices", body, new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject response) {
                    finishBatch("批量新增", devices.size(), done, success, errors, submit, id, null);
                }

                @Override
                public void onError(String message) {
                    finishBatch("批量新增", devices.size(), done, success, errors, submit, id, message);
                }
            });
        }
    }

    private void showBatchAssignArea() {
        startTab("devices", "批量分配区域");
        Button back = smallButton("返回设备列表");
        back.setOnClickListener(v -> showDevices(""));
        content.addView(back, widthMatch());
        content.addView(space(10));

        LinearLayout form = card();
        form.addView(label("设备编号"));
        EditText ids = multiInput("SL-001, SL-002 或每行一个编号");
        form.addView(ids, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(140)));
        form.addView(space(10));
        form.addView(label("目标区域"));
        EditText area = input("如 A区 / 南门 / 图书馆", false);
        form.addView(area, fieldHeight());
        form.addView(space(12));
        Button submit = primaryButton("确认分配");
        submit.setOnClickListener(v -> submitBatchArea(ids.getText().toString(), area.getText().toString().trim(), submit));
        form.addView(submit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        content.addView(form);
    }

    private void submitBatchArea(String rawIds, String area, Button submit) {
        if (area.isEmpty()) {
            toast("请输入目标区域");
            return;
        }
        String[] parts = rawIds.split("[,，\\s]+");
        List<String> ids = new ArrayList<>();
        for (String part : parts) {
            String id = part.trim();
            if (!id.isEmpty()) ids.add(id);
        }
        if (ids.isEmpty()) {
            toast("请输入设备编号");
            return;
        }
        submit.setEnabled(false);
        submit.setText("分配中...");
        int[] done = {0};
        int[] success = {0};
        StringBuilder errors = new StringBuilder();
        for (String id : ids) {
            JSONObject body = new JSONObject();
            try { body.put("area", area); } catch (Exception ignored) {}
            api.put("/api/devices/" + id, body, new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject response) {
                    finishBatch("区域分配", ids.size(), done, success, errors, submit, id, null);
                }

                @Override
                public void onError(String message) {
                    finishBatch("区域分配", ids.size(), done, success, errors, submit, id, message);
                }
            });
        }
    }

    private void finishBatch(String title, int total, int[] done, int[] success, StringBuilder errors,
                             Button submit, String id, String error) {
        done[0]++;
        if (error == null) {
            success[0]++;
        } else {
            errors.append(id).append(": ").append(error).append('\n');
        }
        if (done[0] != total) return;
        submit.setEnabled(true);
        submit.setText(title.contains("新增") ? "提交批量新增" : "确认分配");
        if (errors.length() == 0) {
            toast(title + "完成，共 " + success[0] + " 条");
            showDevices("");
        } else {
            new AlertDialog.Builder(this)
                .setTitle(title + "完成")
                .setMessage("成功 " + success[0] + " / " + total + "\n\n失败：\n" + errors.toString().trim())
                .setPositiveButton("返回列表", (dialog, which) -> showDevices(""))
                .setNegativeButton("留在本页", null)
                .show();
        }
    }

    private void showDeviceDetail(String deviceId) {
        startTab("devices", "设备详情");
        Button back = smallButton("返回设备列表");
        back.setOnClickListener(v -> showDevices(""));
        content.addView(back, widthMatch());
        content.addView(space(10));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        content.addView(box);
        box.addView(cardText(deviceId, "加载中..."));

        api.get("/api/devices/" + deviceId, null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                box.removeAllViews();
                JSONObject d = response.optJSONObject("data");
                if (d != null) {
                    box.addView(cardText(d.optString("name", deviceId), "区域: " + d.optString("area", "-") + "\n位置: " + d.optString("location", "-") + "\n固件: " + d.optString("firmwareVersion", "-") + "\n状态: " + statusName(d.optInt("status", -1))));
                }
                loadHealth(box, deviceId);
                loadTelemetry(box, deviceId);
                loadTelemetryHistory(box, deviceId);
                addControlPanel(box, deviceId);
                loadControlHistory(box, deviceId);
            }

            @Override
            public void onError(String message) {
                box.removeAllViews();
                box.addView(cardText("加载失败", message));
            }
        });
    }

    private void loadHealth(LinearLayout box, String deviceId) {
        api.get("/api/devices/" + deviceId + "/health", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject d = response.optJSONObject("data");
                if (d == null) return;
                box.addView(cardText("健康评分", n(d, "overallScore") + " · " + d.optString("level", "-") + "\n建议: " + d.optString("suggestion", "-")));
            }

            @Override
            public void onError(String message) {
                box.addView(cardText("健康评分", message));
            }
        });
    }

    private void loadTelemetry(LinearLayout box, String deviceId) {
        api.get("/api/telemetry/latest/" + deviceId, null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject d = response.optJSONObject("data");
                if (d == null) return;
                JSONObject values = d.optJSONObject("data");
                if (values == null) values = d;
                String value = "光照 " + valueOf(values, "illuminance") + " lux"
                    + "\n温度 " + valueOf(values, "temperature") + " ℃  湿度 " + valueOf(values, "humidity") + "%"
                    + "\nPM2.5 " + valueOf(values, "pm25") + "  AQI " + valueOf(values, "aqi")
                    + "\n人体: " + (values.optInt("pir") == 1 ? "有人" : "无人") + "  车流: " + valueOf(values, "trafficFlow", "traffic")
                    + "\n采集: " + valueOf(values, "collectedAt", "lastHeartbeatAt");
                box.addView(cardText("最新遥测", value));
            }

            @Override
            public void onError(String message) {
                box.addView(cardText("最新遥测", message));
            }
        });
    }

    private void loadTelemetryHistory(LinearLayout box, String deviceId) {
        JSONObject body = new JSONObject();
        try {
            body.put("deviceId", deviceId);
            body.put("page", 1);
            body.put("size", 6);
        } catch (Exception ignored) {
        }
        api.post("/api/telemetry/history", body, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray records = records(response);
                if (records.length() == 0) {
                    box.addView(cardText("遥测历史", "暂无记录"));
                    return;
                }
                LinearLayout card = card();
                card.addView(text("遥测历史", 17, INK, Typeface.BOLD));
                card.addView(space(8));
                for (int i = 0; i < records.length(); i++) {
                    JSONObject r = records.optJSONObject(i);
                    if (r == null) continue;
                    String line = valueOf(r, "collectedAt") + "  光照 " + valueOf(r, "illuminance")
                        + "  温度 " + valueOf(r, "temperature") + "  AQI " + valueOf(r, "aqi");
                    card.addView(text(line, 13, MUTED, Typeface.NORMAL));
                    if (i < records.length() - 1) card.addView(space(5));
                }
                box.addView(card);
                box.addView(space(10));
            }

            @Override
            public void onError(String message) {
                box.addView(cardText("遥测历史", message));
            }
        });
    }

    private void addControlPanel(LinearLayout box, String deviceId) {
        LinearLayout card = card();
        card.addView(text("手动控制", 17, INK, Typeface.BOLD));
        TextView brightness = text("亮度: 60%", 14, MUTED, Typeface.NORMAL);
        SeekBar seek = new SeekBar(this);
        seek.setMax(100);
        seek.setProgress(60);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { brightness.setText("亮度: " + progress + "%"); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        Button on = smallButton("开灯");
        Button dim = smallButton("调光");
        Button off = smallButton("关灯");
        Button unlock = smallButton("解锁");
        on.setOnClickListener(v -> confirmControl(deviceId, "ON", -1));
        off.setOnClickListener(v -> confirmControl(deviceId, "OFF", -1));
        dim.setOnClickListener(v -> confirmControl(deviceId, "DIMMING", seek.getProgress()));
        unlock.setOnClickListener(v -> unlockDevice(deviceId));
        card.addView(space(8));
        card.addView(brightness);
        card.addView(seek);
        addButtonRow(card, on, dim);
        card.addView(space(8));
        addButtonRow(card, off, unlock);
        box.addView(card);
        box.addView(space(10));
    }

    private void showDeviceControl(String deviceId, String name) {
        startTab("devices", name);
        Button detail = smallButton("进入详情");
        detail.setOnClickListener(v -> showDeviceDetail(deviceId));
        content.addView(detail, widthMatch());
        content.addView(space(10));
        addControlPanel(content, deviceId);
    }

    private void confirmControl(String deviceId, String action, int brightness) {
        String label = "DIMMING".equals(action) ? "调光到 " + brightness + "%" : ("ON".equals(action) ? "开灯" : "关灯");
        confirm("确认下发控制", "设备 " + deviceId + "\n操作: " + label, () -> sendControl(deviceId, action, brightness));
    }

    private void sendControl(String deviceId, String action, int brightness) {
        JSONObject body = new JSONObject();
        try {
            body.put("action", action);
            if (brightness >= 0) body.put("brightness", brightness);
        } catch (Exception ignored) {
        }
        api.post("/api/devices/" + deviceId + "/control", body, simpleToast("控制指令已下发"));
    }

    private void unlockDevice(String deviceId) {
        confirm("解除手动锁定", "设备 " + deviceId + " 将恢复自动策略控制。", () ->
            api.delete("/api/devices/" + deviceId + "/manual-lock", simpleToast("已解除手动锁定"))
        );
    }

    private void loadControlHistory(LinearLayout box, String deviceId) {
        JSONObject params = new JSONObject();
        try {
            params.put("page", 1);
            params.put("size", 5);
        } catch (Exception ignored) {
        }
        api.get("/api/devices/" + deviceId + "/control-history", params, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray records = records(response);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < records.length(); i++) {
                    JSONObject r = records.optJSONObject(i);
                    if (r == null) continue;
                    sb.append(r.optString("issuedAt", "-")).append("  ")
                        .append(r.optString("action", "-")).append("  ")
                        .append(r.optString("status", "-")).append('\n');
                }
                box.addView(cardText("最近控制历史", sb.length() == 0 ? "暂无记录" : sb.toString().trim()));
            }

            @Override
            public void onError(String message) {
                box.addView(cardText("最近控制历史", message));
            }
        });
    }

    private void showAnalytics() {
        startTab("analytics", "数据报表");
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.VERTICAL);
        content.addView(stats);
        content.addView(cardText("月度能耗", "加载中..."));
        content.addView(cardText("分区能耗占比", "加载中..."));

        JSONObject yearParam = new JSONObject();
        try { yearParam.put("year", year); } catch (Exception ignored) {}
        api.get("/api/dashboard/energy/yearly-stats", yearParam, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject d = response.optJSONObject("data");
                stats.removeAllViews();
                if (d == null) return;
                addMetric(stats, "年度总能耗", n(d, "totalKwh") + " kWh", year + " 年累计", PRIMARY);
                addMetric(stats, "年度节省", n(d, "savedKwh") + " kWh", "节能收益", GREEN);
                addMetric(stats, "碳减排", n(d, "carbonReductionKg") + " kg", "CO2 等效", AMBER);
                addMetric(stats, "平均在线率", n(d, "avgOnlineRate") + "%", "设备可用性", PRIMARY);
            }

            @Override
            public void onError(String message) {
                addError(stats, message);
            }
        });

        api.get("/api/dashboard/energy/monthly", yearParam, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject d = response.optJSONObject("data");
                replaceCard("月度能耗", d == null ? null : barChart(d.optJSONArray("months"), d.optJSONArray("consumption"), d.optJSONArray("savings")));
            }

            @Override
            public void onError(String message) {
                replaceCard("月度能耗", text(message, 13, RED, Typeface.NORMAL));
            }
        });

        api.get("/api/dashboard/energy/district", yearParam, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray arr = response.optJSONArray("data");
                replaceCard("分区能耗占比", pieChart(arr));
            }

            @Override
            public void onError(String message) {
                replaceCard("分区能耗占比", text(message, 13, RED, Typeface.NORMAL));
            }
        });
    }

    private void showAssistant() {
        startTab("assistant", "智能助手");
        TextView conversation = text("可以问：灯不亮怎么办、通信中断如何排查、把阈值调到30", 14, INK, Typeface.NORMAL);
        LinearLayout chatCard = card();
        chatCard.addView(conversation);
        content.addView(chatCard);
        content.addView(space(10));

        EditText message = input("输入问题或控制意图", false);
        Button send = primaryButton("发送");
        content.addView(message, widthMatch());
        content.addView(space(8));
        content.addView(send, widthMatch());
        content.addView(space(14));

        LinearLayout diagnoseCard = card();
        diagnoseCard.addView(text("设备一键诊断", 17, INK, Typeface.BOLD));
        EditText device = input("设备编号，如 SL-001", false);
        EditText question = input("诊断问题", false);
        question.setText("最近频繁离线是什么原因");
        Button diagnose = smallButton("开始诊断");
        diagnoseCard.addView(space(8));
        diagnoseCard.addView(device);
        diagnoseCard.addView(space(8));
        diagnoseCard.addView(question);
        diagnoseCard.addView(space(8));
        diagnoseCard.addView(diagnose);
        content.addView(diagnoseCard);

        send.setOnClickListener(v -> {
            String text = message.getText().toString().trim();
            if (text.isEmpty()) return;
            appendConversation(conversation, "我", text);
            message.setText("");
            JSONObject body = new JSONObject();
            try { body.put("message", text); } catch (Exception ignored) {}
            api.post("/api/assistant/chat", body, assistantCallback(conversation));
        });

        diagnose.setOnClickListener(v -> {
            String id = device.getText().toString().trim();
            if (id.isEmpty()) {
                toast("请输入设备编号");
                return;
            }
            JSONObject body = new JSONObject();
            try {
                body.put("deviceId", id);
                body.put("question", question.getText().toString().trim());
            } catch (Exception ignored) {}
            appendConversation(conversation, "我", "诊断 " + id);
            api.post("/api/assistant/diagnose", body, assistantCallback(conversation));
        });
    }

    private ApiClient.Callback assistantCallback(TextView conversation) {
        return new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                Object data = response.opt("data");
                appendConversation(conversation, "助手", summarize(data));
            }

            @Override
            public void onError(String message) {
                appendConversation(conversation, "助手", message);
            }
        };
    }

    private void showAlarms() {
        startTab("alarms", "告警中心");
        content.addView(cardText("告警统计", "加载中..."));
        content.addView(cardText("告警趋势", "加载中..."));
        loadAlarmStats();
        loadAlarmTrend();

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list);
        list.addView(cardText("告警列表", "加载中..."));
        JSONObject params = new JSONObject();
        try {
            params.put("pageNum", 1);
            params.put("pageSize", 30);
        } catch (Exception ignored) {
        }
        api.get("/api/alarms/page", params, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                list.removeAllViews();
                JSONArray records = records(response);
                if (records.length() == 0) {
                    list.addView(cardText("告警列表", "暂无告警，当前运行状态正常"));
                    return;
                }
                for (int i = 0; i < records.length(); i++) {
                    JSONObject alarm = records.optJSONObject(i);
                    if (alarm != null) addAlarmCard(list, alarm);
                }
            }

            @Override
            public void onError(String message) {
                list.removeAllViews();
                list.addView(cardText("加载失败", message));
            }
        });
    }

    private void addAlarmCard(LinearLayout list, JSONObject alarm) {
        LinearLayout card = card();
        String id = String.valueOf(alarm.opt("id"));
        String status = alarm.optString("status", "-");
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(text(alarm.optString("type", "告警") + " · " + alarm.optString("level", "-"), 17, alarmStatusColor(status), Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        titleRow.addView(pill(status, alarmStatusColor(status)));
        card.addView(titleRow);
        String time = alarm.optString("createdAt", alarm.optString("alarmTime", alarm.optString("startAt", "-")));
        String desc = alarm.optString("description", alarm.optString("message", alarm.optString("reason", "-")));
        card.addView(space(8));
        card.addView(text("设备: " + alarm.optString("deviceId", "-") + "\n时间: " + time + "\n说明: " + desc, 13, INK, Typeface.NORMAL));
        Button detail = smallButton("详情");
        detail.setOnClickListener(v -> showAlarmDetail(id));
        if ("ACTIVE".equalsIgnoreCase(status)) {
            Button handle = warningButton("确认处理");
            handle.setOnClickListener(v -> handleAlarm(id));
            card.addView(space(10));
            addButtonRow(card, detail, handle);
        } else {
            card.addView(space(10));
            addButtonRow(card, detail);
        }
        list.addView(card);
        list.addView(space(10));
    }

    private void loadAlarmStats() {
        api.get("/api/alarms/stats", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject d = response.optJSONObject("data");
                if (d == null) {
                    replaceCard("告警统计", text("暂无数据", 13, MUTED, Typeface.NORMAL));
                    return;
                }
                LinearLayout box = new LinearLayout(MainActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.addView(text("ACTIVE " + n(d, "totalActive"), 24, RED, Typeface.BOLD));
                box.addView(text("按级别: " + countMap(d.optJSONObject("byLevel")), 13, MUTED, Typeface.NORMAL));
                box.addView(text("按类型: " + countMap(d.optJSONObject("byType")), 13, MUTED, Typeface.NORMAL));
                box.addView(text("按状态: " + countMap(d.optJSONObject("byStatus")), 13, MUTED, Typeface.NORMAL));
                replaceCard("告警统计", box);
            }

            @Override
            public void onError(String message) {
                replaceCard("告警统计", text(message, 13, RED, Typeface.NORMAL));
            }
        });
    }

    private void loadAlarmTrend() {
        JSONObject params = new JSONObject();
        try { params.put("days", 7); } catch (Exception ignored) {}
        api.get("/api/alarms/trend", params, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray arr = response.optJSONArray("data");
                if (arr == null || arr.length() == 0) {
                    replaceCard("告警趋势", text("暂无数据", 13, MUTED, Typeface.NORMAL));
                    return;
                }
                JSONArray counts = new JSONArray();
                int total = 0;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.optJSONObject(i);
                    if (item == null) continue;
                    int count = item.optInt("count", 0);
                    counts.put(count);
                    total += count;
                }
                LinearLayout box = new LinearLayout(MainActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.addView(text("近 7 天合计 " + total + " 条", 13, MUTED, Typeface.NORMAL));
                box.addView(lineChart(counts));
                replaceCard("告警趋势", box);
            }

            @Override
            public void onError(String message) {
                replaceCard("告警趋势", text(message, 13, RED, Typeface.NORMAL));
            }
        });
    }

    private void showAlarmDetail(String id) {
        startTab("alarms", "告警详情");
        Button back = smallButton("返回告警中心");
        back.setOnClickListener(v -> showAlarms());
        content.addView(back, widthMatch());
        content.addView(space(10));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        content.addView(box);
        box.addView(cardText("#" + id, "加载中..."));

        api.get("/api/alarms/" + id, null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                box.removeAllViews();
                JSONObject d = response.optJSONObject("data");
                if (d == null) {
                    box.addView(cardText("告警详情", "暂无数据"));
                    return;
                }
                LinearLayout card = card();
                String status = d.optString("status", "-");
                card.addView(text(d.optString("type", "告警") + " · " + d.optString("level", "-"), 18, alarmStatusColor(status), Typeface.BOLD));
                card.addView(space(8));
                card.addView(pill(status, alarmStatusColor(status)));
                card.addView(space(10));
                card.addView(text(
                    "编号: " + valueOf(d, "id")
                        + "\n设备: " + d.optString("deviceId", "-")
                        + "\n原因: " + d.optString("reason", d.optString("description", "-"))
                        + "\n开始: " + valueOf(d, "startAt", "createdAt", "alarmTime")
                        + "\n恢复: " + valueOf(d, "recoverAt")
                        + "\n处理人: " + valueOf(d, "handler"),
                    13, INK, Typeface.NORMAL));
                if ("ACTIVE".equalsIgnoreCase(status)) {
                    Button handle = warningButton("确认处理");
                    handle.setOnClickListener(v -> handleAlarm(id, () -> showAlarmDetail(id)));
                    card.addView(space(12));
                    card.addView(handle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
                }
                box.addView(card);
            }

            @Override
            public void onError(String message) {
                box.removeAllViews();
                box.addView(cardText("加载失败", message));
            }
        });
    }

    private void handleAlarm(String id) {
        handleAlarm(id, () -> showAlarms());
    }

    private void handleAlarm(String id, Runnable after) {
        if (id == null || id.isEmpty() || "null".equals(id)) {
            toast("告警 ID 无效");
            return;
        }
        JSONObject body = new JSONObject();
        try { body.put("remark", "移动端确认处理"); } catch (Exception ignored) {}
        confirm("确认处理告警", "告警 #" + id + " 将被标记为已处理。", () ->
            api.put("/api/alarms/" + id + "/handle", body, toastAndRun("告警已确认", after))
        );
    }

    private ApiClient.Callback simpleToast(String okMessage) {
        return new ApiClient.Callback() {
            @Override public void onSuccess(JSONObject response) { toast(okMessage); }
            @Override public void onError(String message) { toast(message); }
        };
    }

    private void replaceCard(String title, View body) {
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            Object tag = child.getTag();
            if (title.equals(tag)) {
                LinearLayout card = (LinearLayout) child;
                while (card.getChildCount() > 1) card.removeViewAt(1);
                card.addView(space(8));
                card.addView(body == null ? text("暂无数据", 13, MUTED, Typeface.NORMAL) : body);
                return;
            }
        }
    }

    private void addMetric(LinearLayout parent, String title, String value, String hint, int color) {
        LinearLayout card = card();
        card.setPadding(dp(0), 0, 0, 0);
        card.setBackground(rounded(tint(color, 11), dp(8), withAlpha(color, 70)));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        View stripe = new View(this);
        stripe.setBackgroundColor(color);
        row.addView(stripe, new LinearLayout.LayoutParams(dp(5), dp(92)));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(13), dp(14), dp(13));
        body.addView(text(title, 12, MUTED, Typeface.BOLD));
        body.addView(space(4));
        body.addView(text(value, 26, color, Typeface.BOLD));
        body.addView(space(4));
        body.addView(text(hint, 12, MUTED, Typeface.NORMAL));
        row.addView(body, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(row);
        parent.addView(card);
        parent.addView(space(10));
    }

    private void addError(LinearLayout parent, String message) {
        parent.removeAllViews();
        parent.addView(cardText("加载失败", message));
    }

    private LinearLayout cardText(String title, String body) {
        LinearLayout card = card();
        card.setTag(title);
        addCardHeader(card, title, PRIMARY);
        card.addView(space(10));
        card.addView(text(body, 13, stateColor(title, body), Typeface.NORMAL));
        return card;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(15), dp(15), dp(15));
        card.setBackground(rounded(SURFACE, dp(8), BORDER));
        card.setElevation(dp(2));
        LinearLayout.LayoutParams lp = widthMatch();
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);
        return card;
    }

    private void addCardHeader(LinearLayout card, String title, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        View accent = new View(this);
        accent.setBackground(rounded(color, dp(2), color));
        LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(dp(4), dp(18));
        row.addView(accent, accentLp);
        TextView titleView = text(title, 16, INK_DARK, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        titleLp.setMargins(dp(8), 0, 0, 0);
        row.addView(titleView, titleLp);
        card.addView(row);
    }

    private int stateColor(String title, String body) {
        String value = (title == null ? "" : title) + " " + (body == null ? "" : body);
        if (value.contains("失败") || value.contains("错误") || value.contains("异常")) return RED;
        if (value.contains("加载中")) return PRIMARY;
        return MUTED;
    }

    private TextView sectionTitle(String title) {
        TextView view = text(title, 23, INK_DARK, Typeface.BOLD);
        view.setPadding(0, dp(4), 0, dp(14));
        return view;
    }

    private TextView label(String text) {
        TextView label = text(text, 13, MUTED, Typeface.NORMAL);
        label.setPadding(0, 0, 0, dp(6));
        return label;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView t = new TextView(this);
        t.setText(value == null ? "" : value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, style);
        t.setLineSpacing(dp(2), 1f);
        return t;
    }

    private EditText input(String hint, boolean password) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextSize(15);
        input.setTextColor(INK_DARK);
        input.setHintTextColor(Color.rgb(100, 116, 139));
        input.setSingleLine(true);
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setBackground(rounded(FIELD_BG, dp(8), Color.rgb(203, 213, 225)));
        input.setInputType(password ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD) : InputType.TYPE_CLASS_TEXT);
        return input;
    }

    private EditText numberInput(String hint, boolean decimal) {
        EditText input = input(hint, false);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | (decimal ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0));
        return input;
    }

    private EditText multiInput(String hint) {
        EditText input = input(hint, false);
        input.setSingleLine(false);
        input.setGravity(Gravity.TOP);
        input.setMinLines(4);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        return input;
    }

    private LinearLayout.LayoutParams fieldHeight() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
    }

    private Button primaryButton(String label) {
        Button button = smallButton(label);
        button.setTextColor(Color.WHITE);
        button.setBackground(statefulRounded(PRIMARY, PRIMARY_DARK, dp(8), PRIMARY));
        return button;
    }

    private Button warningButton(String label) {
        Button button = smallButton(label);
        button.setTextColor(Color.WHITE);
        button.setBackground(statefulRounded(CTA, Color.rgb(234, 88, 12), dp(8), CTA));
        return button;
    }

    private Button dangerButton(String label) {
        Button button = smallButton(label);
        button.setTextColor(Color.WHITE);
        button.setBackground(statefulRounded(RED, Color.rgb(185, 28, 28), dp(8), RED));
        return button;
    }

    private Button darkButton(String label) {
        Button button = smallButton(label);
        button.setTextColor(Color.WHITE);
        button.setBackground(statefulRounded(Color.rgb(30, 41, 59), Color.rgb(51, 65, 85), dp(8), Color.rgb(51, 65, 85)));
        return button;
    }

    private Button smallButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(PRIMARY);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setGravity(Gravity.CENTER);
        button.setBackground(statefulRounded(PRIMARY_SOFT, Color.rgb(191, 219, 254), dp(8), Color.rgb(191, 219, 254)));
        button.setElevation(dp(1));
        return button;
    }

    private void addButtonRow(LinearLayout parent, Button... buttons) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < buttons.length; i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1);
            if (i > 0) lp.setMargins(dp(8), 0, 0, 0);
            row.addView(buttons[i], lp);
        }
        parent.addView(row);
    }

    private TextView pill(String value, int color) {
        TextView pill = text(value, 12, color, Typeface.BOLD);
        pill.setPadding(dp(10), dp(5), dp(10), dp(5));
        pill.setBackground(rounded(withAlpha(color, 28), dp(20), withAlpha(color, 90)));
        return pill;
    }

    private View space(int dp) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return view;
    }

    private LinearLayout.LayoutParams widthMatch() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private GradientDrawable rounded(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (stroke != Color.TRANSPARENT) drawable.setStroke(1, stroke);
        return drawable;
    }

    private StateListDrawable statefulRounded(int normal, int pressed, int radius, int stroke) {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, rounded(pressed, radius, stroke));
        drawable.addState(new int[]{android.R.attr.state_focused}, rounded(pressed, radius, stroke));
        drawable.addState(new int[]{}, rounded(normal, radius, stroke));
        return drawable;
    }

    private GradientDrawable verticalGradient(int top, int bottom) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
        drawable.setCornerRadius(0);
        return drawable;
    }

    private int tint(int color, int whiteWeight) {
        int divisor = Math.max(2, whiteWeight + 1);
        int red = (Color.red(color) + 255 * whiteWeight) / divisor;
        int green = (Color.green(color) + 255 * whiteWeight) / divisor;
        int blue = (Color.blue(color) + 255 * whiteWeight) / divisor;
        return Color.rgb(red, green, blue);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void confirm(String title, String message, Runnable action) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("取消", null)
            .setPositiveButton("确认", (dialog, which) -> action.run())
            .show();
    }

    private ApiClient.Callback toastAndRun(String okMessage, Runnable after) {
        return new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                toast(okMessage);
                if (after != null) after.run();
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        };
    }

    private LineChartView lineChart(JSONArray current, JSONArray lastWeek) {
        LineChartView chart = new LineChartView(this);
        chart.setData(toFloats(current), toFloats(lastWeek));
        chart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(210)));
        return chart;
    }

    private LineChartView lineChart(JSONArray current) {
        LineChartView chart = new LineChartView(this);
        chart.setData(toFloats(current), new ArrayList<>());
        chart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(210)));
        return chart;
    }

    private BarChartView barChart(JSONArray labels, JSONArray consumption, JSONArray savings) {
        BarChartView chart = new BarChartView(this);
        chart.setData(toStrings(labels), toFloats(consumption), toFloats(savings));
        chart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(240)));
        return chart;
    }

    private BarChartView barChart(JSONArray labels, JSONArray values) {
        BarChartView chart = new BarChartView(this);
        chart.setData(toStrings(labels), toFloats(values), new ArrayList<>());
        chart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
        return chart;
    }

    private PieChartView pieChart(JSONArray data) {
        PieChartView chart = new PieChartView(this);
        chart.setData(data);
        chart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(240)));
        return chart;
    }

    private List<Float> toFloats(JSONArray arr) {
        List<Float> values = new ArrayList<>();
        if (arr == null) return values;
        for (int i = 0; i < arr.length(); i++) values.add((float) arr.optDouble(i, 0));
        return values;
    }

    private List<String> toStrings(JSONArray arr) {
        List<String> values = new ArrayList<>();
        if (arr == null) return values;
        for (int i = 0; i < arr.length(); i++) values.add(arr.optString(i));
        return values;
    }

    private JSONArray records(JSONObject response) {
        Object data = response.opt("data");
        if (data instanceof JSONArray) return (JSONArray) data;
        if (data instanceof JSONObject) {
            JSONArray records = ((JSONObject) data).optJSONArray("records");
            return records == null ? new JSONArray() : records;
        }
        return new JSONArray();
    }

    private String n(JSONObject obj, String key) {
        Object value = obj.opt(key);
        if (value == null || JSONObject.NULL.equals(value)) return "--";
        if (value instanceof Number) return numberFormat.format(((Number) value).doubleValue());
        return String.valueOf(value);
    }

    private String valueOf(JSONObject obj, String... keys) {
        if (obj == null || keys == null) return "--";
        for (String key : keys) {
            if (key != null && obj.has(key) && !obj.isNull(key)) return n(obj, key);
        }
        return "--";
    }

    private String countMap(JSONObject obj) {
        if (obj == null || obj.length() == 0) return "--";
        StringBuilder builder = new StringBuilder();
        java.util.Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (builder.length() > 0) builder.append("  ");
            builder.append(key).append(' ').append(n(obj, key));
        }
        return builder.toString();
    }

    private String statusName(int status) {
        if (status == 0) return "停用";
        if (status == 1) return "在线";
        if (status == 2) return "离线";
        if (status == 3) return "异常";
        return "未知";
    }

    private int statusColor(int status) {
        if (status == 1) return GREEN;
        if (status == 2) return MUTED;
        if (status == 3) return RED;
        return AMBER;
    }

    private int alarmStatusColor(String status) {
        if ("ACTIVE".equalsIgnoreCase(status)) return RED;
        if ("ACKNOWLEDGED".equalsIgnoreCase(status)) return AMBER;
        if ("RECOVERED".equalsIgnoreCase(status)) return GREEN;
        return MUTED;
    }

    private String summarize(Object data) {
        if (data instanceof JSONObject) {
            JSONObject obj = (JSONObject) data;
            String answer = obj.optString("answer", obj.optString("message", obj.optString("content", "")));
            String type = obj.optString("type", "");
            if (!answer.isEmpty()) return (type.isEmpty() ? "" : "[" + type + "] ") + answer;
            return obj.toString();
        }
        return data == null || JSONObject.NULL.equals(data) ? "已完成" : String.valueOf(data);
    }

    private void appendConversation(TextView view, String who, String message) {
        String old = view.getText().toString();
        view.setText(old + "\n\n" + who + ": " + message);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    public static class LineChartView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF dot = new RectF();
        private List<Float> current = new ArrayList<>();
        private List<Float> compare = new ArrayList<>();

        public LineChartView(android.content.Context context) {
            super(context);
        }

        public void setData(List<Float> current, List<Float> compare) {
            this.current = current;
            this.compare = compare;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float max = maxOf(current, compare);
            drawGrid(canvas);
            drawSeries(canvas, compare, max, Color.rgb(148, 163, 184), 3f, false);
            drawSeries(canvas, current, max, Color.rgb(37, 99, 235), 5f, true);
        }

        private float maxOf(List<Float> first, List<Float> second) {
            float max = 1;
            if (first != null) {
                for (Float v : first) if (v != null && v > max) max = v;
            }
            if (second != null) {
                for (Float v : second) if (v != null && v > max) max = v;
            }
            return max;
        }

        private void drawGrid(Canvas canvas) {
            float left = 24;
            float right = getWidth() - 18;
            float top = 18;
            float bottom = getHeight() - 30;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            paint.setColor(Color.rgb(226, 232, 240));
            for (int i = 0; i < 4; i++) {
                float y = top + (bottom - top) * i / 3f;
                canvas.drawLine(left, y, right, y, paint);
            }
        }

        private void drawSeries(Canvas canvas, List<Float> values, float max, int color, float stroke, boolean drawDots) {
            if (values == null || values.isEmpty()) return;
            float left = 24;
            float top = 18;
            float w = getWidth() - 42;
            float h = getHeight() - 48;
            if (values.size() == 1) {
                float x = left + w / 2f;
                float y = top + h - h * values.get(0) / max;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color);
                dot.set(x - 5, y - 5, x + 5, y + 5);
                canvas.drawOval(dot, paint);
                return;
            }
            Path path = new Path();
            for (int i = 0; i < values.size(); i++) {
                float x = left + w * i / (float) (values.size() - 1);
                float y = top + h - h * values.get(i) / max;
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(color);
            canvas.drawPath(path, paint);
            if (!drawDots) return;
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < values.size(); i++) {
                float x = left + w * i / (float) (values.size() - 1);
                float y = top + h - h * values.get(i) / max;
                dot.set(x - 4, y - 4, x + 4, y + 4);
                canvas.drawOval(dot, paint);
            }
        }
    }

    public static class BarChartView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF barRect = new RectF();
        private List<String> labels = new ArrayList<>();
        private List<Float> consumption = new ArrayList<>();
        private List<Float> savings = new ArrayList<>();

        public BarChartView(android.content.Context context) {
            super(context);
        }

        public void setData(List<String> labels, List<Float> consumption, List<Float> savings) {
            this.labels = labels;
            this.consumption = consumption;
            this.savings = savings;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int count = Math.max(consumption.size(), savings.size());
            if (count == 0) return;
            boolean hasSavings = savings != null && !savings.isEmpty();
            float max = 1;
            for (Float v : consumption) if (v != null && v > max) max = v;
            for (Float v : savings) if (v != null && v > max) max = v;
            float left = 28;
            float bottom = getHeight() - 28;
            float chartH = getHeight() - 58;
            float groupW = (getWidth() - 56) / (float) count;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            paint.setColor(Color.rgb(226, 232, 240));
            for (int i = 0; i < 4; i++) {
                float y = 20 + chartH * i / 3f;
                canvas.drawLine(left, y, getWidth() - 18, y, paint);
            }
            for (int i = 0; i < count; i++) {
                float c = i < consumption.size() ? consumption.get(i) : 0;
                float s = i < savings.size() ? savings.get(i) : 0;
                float x = left + i * groupW;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(37, 99, 235));
                if (hasSavings) {
                    barRect.set(x + groupW * .18f, bottom - chartH * c / max, x + groupW * .42f, bottom);
                } else {
                    barRect.set(x + groupW * .28f, bottom - chartH * c / max, x + groupW * .72f, bottom);
                }
                canvas.drawRoundRect(barRect, 8, 8, paint);
                if (hasSavings) {
                    paint.setColor(Color.rgb(22, 160, 133));
                    barRect.set(x + groupW * .48f, bottom - chartH * s / max, x + groupW * .72f, bottom);
                    canvas.drawRoundRect(barRect, 8, 8, paint);
                }
                if (labels != null && i < labels.size() && i % 2 == 0) {
                    paint.setTextSize(22);
                    paint.setColor(Color.rgb(100, 116, 139));
                    canvas.drawText(labels.get(i), x + groupW * .1f, getHeight() - 5, paint);
                }
            }
        }
    }

    public static class PieChartView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();
        private JSONArray data;
        private final int[] colors = {
            Color.rgb(11, 92, 173),
            Color.rgb(22, 160, 133),
            Color.rgb(230, 147, 40),
            Color.rgb(122, 92, 190),
            Color.rgb(221, 72, 72)
        };

        public PieChartView(android.content.Context context) {
            super(context);
        }

        public void setData(JSONArray data) {
            this.data = data;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (data == null || data.length() == 0) return;
            float total = 0;
            for (int i = 0; i < data.length(); i++) {
                JSONObject obj = data.optJSONObject(i);
                if (obj != null) total += (float) obj.optDouble("value", 0);
            }
            if (total <= 0) return;
            float size = Math.min(getWidth(), getHeight()) * .58f;
            float stroke = Math.max(18, size * .18f);
            oval.set(20 + stroke / 2f, 20 + stroke / 2f, 20 + size - stroke / 2f, 20 + size - stroke / 2f);
            float start = -90;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setStrokeCap(Paint.Cap.BUTT);
            for (int i = 0; i < data.length(); i++) {
                JSONObject obj = data.optJSONObject(i);
                if (obj == null) continue;
                float sweep = (float) obj.optDouble("value", 0) / total * 360f;
                paint.setColor(colors[i % colors.length]);
                canvas.drawArc(oval, start, sweep, false, paint);
                start += sweep;
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(24);
            paint.setColor(Color.rgb(15, 23, 42));
            canvas.drawText("分区", 20 + size * .34f, 20 + size * .46f, paint);
            paint.setTextSize(24);
            float textX = 36 + size;
            for (int i = 0; i < data.length(); i++) {
                JSONObject obj = data.optJSONObject(i);
                if (obj == null) continue;
                paint.setColor(colors[i % colors.length]);
                canvas.drawCircle(textX, 32 + i * 34, 9, paint);
                paint.setColor(Color.rgb(30, 41, 59));
                float percent = total <= 0 ? 0 : (float) obj.optDouble("value", 0) * 100f / total;
                canvas.drawText(obj.optString("name") + " " + Math.round(percent) + "%", textX + 18, 40 + i * 34, paint);
            }
        }
    }
}
