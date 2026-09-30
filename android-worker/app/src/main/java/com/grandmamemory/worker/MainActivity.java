package com.grandmamemory.worker;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "grandma_worker";
    private static final int DEEP = 0xFF243447;
    private static final int GOLD = 0xFFD89B2B;
    private static final int PAPER = 0xFFFFFAF0;
    private static final int LINE = 0xFFE4DED2;
    private static final int TEXT = 0xFF17202D;
    private static final int MUTED = 0xFF667085;

    private EditText serverUrlInput;
    private EditText pairCodeInput;
    private TextView statusView;
    private TextView titleView;
    private TextView phoneView;
    private TextView messageView;
    private ImageView grandmaPhoto;
    private LinearLayout jobPanel;
    private Handler handler;
    private JSONObject currentJob;
    private boolean polling;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadLatestJob(false);
            if (polling) {
                handler.postDelayed(this, 5000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
        buildUi();
        restoreSettings();
        startPolling();
    }

    @Override
    protected void onDestroy() {
        polling = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(PAPER);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(24));
        scroll.addView(root);

        TextView brand = text("Grandma Memory Worker", 26, TEXT, true);
        root.addView(brand);
        TextView intro = text("This APK connects your Android phone to the Render control panel, receives the birthday job, then opens WhatsApp for the message and call.", 15, MUTED, false);
        intro.setPadding(0, dp(8), 0, dp(18));
        root.addView(intro);

        serverUrlInput = input("https://your-render-service.onrender.com");
        pairCodeInput = input("PAIR CODE");
        root.addView(label("Render website URL"));
        root.addView(serverUrlInput);
        root.addView(label("Pair code from control panel"));
        root.addView(pairCodeInput);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(12), 0, dp(10));
        root.addView(row);

        Button saveButton = button("Save + Load", DEEP, 0xFFFFFFFF);
        Button refreshButton = button("Refresh", GOLD, 0xFF1F1608);
        row.addView(saveButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        refreshParams.setMargins(dp(8), 0, 0, 0);
        row.addView(refreshButton, refreshParams);

        statusView = text("Waiting for job.", 15, MUTED, false);
        statusView.setPadding(0, dp(6), 0, dp(14));
        root.addView(statusView);

        jobPanel = new LinearLayout(this);
        jobPanel.setOrientation(LinearLayout.VERTICAL);
        jobPanel.setPadding(dp(14), dp(14), dp(14), dp(14));
        jobPanel.setBackgroundColor(0xFFFFFFFF);
        jobPanel.setVisibility(View.GONE);
        root.addView(jobPanel);

        titleView = text("Birthday call", 22, TEXT, true);
        phoneView = text("", 16, MUTED, false);
        grandmaPhoto = new ImageView(this);
        grandmaPhoto.setAdjustViewBounds(true);
        grandmaPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        messageView = text("", 16, TEXT, false);
        messageView.setPadding(0, dp(12), 0, dp(12));

        jobPanel.addView(titleView);
        jobPanel.addView(phoneView);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(260));
        imageParams.setMargins(0, dp(12), 0, dp(12));
        jobPanel.addView(grandmaPhoto, imageParams);
        jobPanel.addView(text("Message to send first", 14, MUTED, true));
        jobPanel.addView(messageView);

        Button openWhatsApp = button("Open WhatsApp with Message", DEEP, 0xFFFFFFFF);
        Button openBusiness = button("Open WhatsApp Business with Message", GOLD, 0xFF1F1608);
        Button openChat = button("Open Chat for Video Call", 0xFFFFFFFF, TEXT);
        Button markStarted = button("I Started the Video Call", DEEP, 0xFFFFFFFF);
        addButton(jobPanel, openWhatsApp);
        addButton(jobPanel, openBusiness);
        addButton(jobPanel, openChat);
        addButton(jobPanel, markStarted);

        TextView note = text("After WhatsApp opens, send the message first. Then tap the video-call button inside WhatsApp. Your sister does not install anything.", 14, MUTED, false);
        note.setPadding(0, dp(8), 0, 0);
        jobPanel.addView(note);

        saveButton.setOnClickListener(v -> {
            saveSettings();
            loadLatestJob(true);
        });
        refreshButton.setOnClickListener(v -> loadLatestJob(true));
        openWhatsApp.setOnClickListener(v -> openWhatsApp(true, "com.whatsapp"));
        openBusiness.setOnClickListener(v -> openWhatsApp(true, "com.whatsapp.w4b"));
        openChat.setOnClickListener(v -> openWhatsApp(false, null));
        markStarted.setOnClickListener(v -> updateJobStatus("call-started-on-android"));

        setContentView(scroll);
    }

    private TextView label(String text) {
        TextView view = text(text, 14, DEEP, true);
        view.setPadding(0, dp(10), 0, dp(6));
        return view;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.2f);
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return view;
    }

    private EditText input(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setTextColor(TEXT);
        field.setHintTextColor(MUTED);
        field.setSingleLine(true);
        field.setTextSize(16);
        field.setPadding(dp(12), 0, dp(12), 0);
        return field;
    }

    private Button button(String text, int background, int foreground) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(foreground);
        btn.setTextSize(14);
        btn.setAllCaps(false);
        btn.setBackgroundColor(background);
        return btn;
    }

    private void addButton(LinearLayout parent, Button button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        params.setMargins(0, dp(8), 0, 0);
        parent.addView(button, params);
    }

    private void restoreSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        serverUrlInput.setText(prefs.getString("serverUrl", ""));
        pairCodeInput.setText(prefs.getString("pairCode", ""));
    }

    private void saveSettings() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putString("serverUrl", serverUrlInput.getText().toString().trim())
            .putString("pairCode", pairCodeInput.getText().toString().trim().toUpperCase(Locale.ROOT))
            .apply();
    }

    private void startPolling() {
        polling = true;
        handler.post(pollRunnable);
    }

    private String baseUrl() {
        String url = serverUrlInput.getText().toString().trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }

    private String pairCode() {
        return pairCodeInput.getText().toString().trim().toUpperCase(Locale.ROOT);
    }

    private void loadLatestJob(boolean showToast) {
        final String server = baseUrl();
        final String pair = pairCode();
        if (server.isEmpty() || pair.isEmpty()) {
            setStatus("Enter Render URL and pair code.");
            return;
        }
        saveSettings();
        setStatus("Checking Render for birthday job...");

        new Thread(() -> {
            try {
                String endpoint = server + "/api/jobs/latest?pairCode=" + encode(pair);
                JSONObject response = new JSONObject(httpGet(endpoint));
                JSONObject job = response.optJSONObject("job");
                runOnUiThread(() -> {
                    if (job == null) {
                        currentJob = null;
                        jobPanel.setVisibility(View.GONE);
                        setStatus("No job yet. Create it on the Render control panel.");
                    } else {
                        renderJob(job);
                        if (showToast) toast("Job loaded.");
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> setStatus("Could not reach Render. Check the URL and internet."));
            }
        }).start();
    }

    private void renderJob(JSONObject job) {
        currentJob = job;
        jobPanel.setVisibility(View.VISIBLE);
        String grandma = job.optString("grandmaName", "Grandma");
        String sister = job.optString("sisterName", "Sister");
        titleView.setText(grandma + " birthday call for " + sister);
        phoneView.setText("+" + job.optString("sisterPhone", ""));
        messageView.setText(job.optString("birthdayMessage", ""));
        setStatus("Job received. Open WhatsApp from this phone.");
        renderPhoto(job.optString("photoDataUrl", ""));
    }

    private void renderPhoto(String dataUrl) {
        try {
            if (dataUrl == null || dataUrl.isEmpty()) {
                grandmaPhoto.setImageBitmap(null);
                grandmaPhoto.setBackgroundColor(LINE);
                return;
            }
            int comma = dataUrl.indexOf(',');
            String base64 = comma >= 0 ? dataUrl.substring(comma + 1) : dataUrl;
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            grandmaPhoto.setImageBitmap(bitmap);
        } catch (Exception ignored) {
            grandmaPhoto.setImageBitmap(null);
            grandmaPhoto.setBackgroundColor(LINE);
        }
    }

    private void openWhatsApp(boolean includeMessage, String packageName) {
        if (currentJob == null) {
            toast("Load a job first.");
            return;
        }
        try {
            String phone = cleanPhone(currentJob.optString("sisterPhone", ""));
            String message = currentJob.optString("birthdayMessage", "");
            String url = "https://wa.me/" + phone;
            if (includeMessage && !message.isEmpty()) {
                url += "?text=" + encode(message);
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (packageName != null) intent.setPackage(packageName);
            startActivity(intent);
        } catch (ActivityNotFoundException missing) {
            toast("WhatsApp is not installed on this phone.");
        } catch (Exception error) {
            toast("Could not open WhatsApp.");
        }
    }

    private void updateJobStatus(String status) {
        if (currentJob == null) {
            toast("Load a job first.");
            return;
        }
        final String server = baseUrl();
        final String id = currentJob.optString("id", "");
        new Thread(() -> {
            try {
                String payload = "{\"status\":\"" + status + "\"}";
                httpPatch(server + "/api/jobs/" + encode(id), payload);
                runOnUiThread(() -> {
                    setStatus("Status saved: call started.");
                    toast("Call status saved.");
                });
            } catch (Exception error) {
                runOnUiThread(() -> toast("Could not update status."));
            }
        }).start();
    }

    private String httpGet(String endpoint) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        connection.setRequestMethod("GET");
        return readResponse(connection);
    }

    private String httpPatch(String endpoint, String payload) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        connection.setRequestMethod("PATCH");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setDoOutput(true);
        try (OutputStream os = connection.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            writer.write(payload);
        }
        return readResponse(connection);
    }

    private String readResponse(HttpURLConnection connection) throws Exception {
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
        }
        if (code < 200 || code >= 300) {
            throw new IllegalStateException(builder.toString());
        }
        return builder.toString();
    }

    private String encode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }

    private String cleanPhone(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private void setStatus(String value) {
        statusView.setText(value);
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
