
package com.jjmovies.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private FrameLayout rootLayout;
    private LinearLayout offlinePanel;

    private float touchStartY;
    private String currentPageUrl =
        "https://geeras866-lab.github.io/JJ-Movies/";

    private static final String WEBSITE =
        "https://geeras866-lab.github.io/JJ-Movies/";

    private static final String SITE_HOST =
        "geeras866-lab.github.io";

    private static final String SUPABASE_HOST =
        "tswyabgdldbsaopipvug.supabase.co";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.rgb(9, 13, 24));

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportMultipleWindows(false);

        // Allow normal WebView caching.
        // Do not disable cache or clear it on every refresh.
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, String url) {
                return handleUrl(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public void onPageStarted(
                    WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

                if (isInternalUrl(url)) {
                    currentPageUrl = url;
                }
            }

            @Override
            public void onPageFinished(
                    WebView view, String url) {
                super.onPageFinished(view, url);

                if (isInternalUrl(url)) {
                    currentPageUrl = url;
                    hideOfflineMessage();
                }
            }

            // Android 6 and newer: handle main-page errors only.
            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {

                super.onReceivedError(view, request, error);

                if (request.isForMainFrame()) {
                    showOfflineMessage();
                }
            }

            // Compatibility with older Android versions.
            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(
                    WebView view,
                    int errorCode,
                    String description,
                    String failingUrl) {

                super.onReceivedError(
                    view, errorCode, description, failingUrl);

                if (failingUrl != null
                        && failingUrl.equals(view.getUrl())) {
                    showOfflineMessage();
                }
            }
        });

        // Pull down at the top of the current page to refresh.
        webView.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartY = event.getY();
                    break;

                case MotionEvent.ACTION_UP:
                    float distance = event.getY() - touchStartY;

                    if (distance > 150 && webView.getScrollY() == 0) {
                        refreshCurrentPage();
                    }
                    break;
            }

            return false;
        });

        // Keep the existing file-download functionality.
        webView.setDownloadListener(
            new DownloadListener() {
                @Override
                public void onDownloadStart(
                        String url,
                        String userAgent,
                        String contentDisposition,
                        String mimeType,
                        long contentLength) {

                    try {
                        Uri uri = Uri.parse(url);
                        DownloadManager.Request request =
                            new DownloadManager.Request(uri);

                        String fileName = URLUtil.guessFileName(
                            url, contentDisposition, mimeType);

                        request.setTitle(fileName);
                        request.setDescription(
                            "Downloading from JJ Movies");

                        request.setNotificationVisibility(
                            DownloadManager.Request
                                .VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                        String cookies =
                            CookieManager.getInstance().getCookie(url);

                        if (cookies != null) {
                            request.addRequestHeader("Cookie", cookies);
                        }

                        if (userAgent != null) {
                            request.addRequestHeader(
                                "User-Agent", userAgent);
                        }

                        request.setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            fileName);

                        DownloadManager manager =
                            (DownloadManager) getSystemService(
                                DOWNLOAD_SERVICE);

                        if (manager != null) {
                            manager.enqueue(request);

                            Toast.makeText(
                                MainActivity.this,
                                "Download started. Check notifications.",
                                Toast.LENGTH_LONG
                            ).show();
                        } else {
                            openExternal(url);
                        }

                    } catch (Exception e) {
                        Toast.makeText(
                            MainActivity.this,
                            "Download failed. Please try again.",
                            Toast.LENGTH_LONG
                        ).show();
                    }
                }
            }
        );

        rootLayout.addView(webView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        createOfflinePanel();
        setContentView(rootLayout);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(WEBSITE);
        }
    }

    private void createOfflinePanel() {
        offlinePanel = new LinearLayout(this);
        offlinePanel.setOrientation(LinearLayout.VERTICAL);
        offlinePanel.setGravity(Gravity.CENTER);
        offlinePanel.setPadding(32, 32, 32, 32);
        offlinePanel.setBackgroundColor(Color.rgb(9, 13, 24));
        offlinePanel.setVisibility(View.GONE);

        TextView logo = new TextView(this);
        logo.setText("JJ Movies");
        logo.setTextColor(Color.rgb(255, 56, 103));
        logo.setTextSize(30);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView icon = new TextView(this);
        icon.setText("\uD83D\uDCF6");
        icon.setTextSize(42);
        icon.setGravity(Gravity.CENTER);
        icon.setPadding(0, 24, 0, 16);

        TextView title = new TextView(this);
        title.setText("Check your internet connection");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView message = new TextView(this);
        message.setText(
            "Please connect to the internet and try again.");
        message.setTextColor(Color.rgb(190, 198, 214));
        message.setTextSize(15);
        message.setGravity(Gravity.CENTER);
        message.setPadding(8, 12, 8, 24);

        Button retryButton = new Button(this);
        retryButton.setText("Try Again");
        retryButton.setTextColor(Color.WHITE);
        retryButton.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(255, 56, 103)));

        retryButton.setOnClickListener(v -> retryCurrentPage());

        offlinePanel.addView(logo);
        offlinePanel.addView(icon);
        offlinePanel.addView(title);
        offlinePanel.addView(message);
        offlinePanel.addView(retryButton);

        FrameLayout.LayoutParams panelParams =
            new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        rootLayout.addView(offlinePanel, panelParams);
    }

    private void showOfflineMessage() {
        if (offlinePanel != null) {
            offlinePanel.setVisibility(View.VISIBLE);
            offlinePanel.bringToFront();
        }
    }

    private void hideOfflineMessage() {
        if (offlinePanel != null) {
            offlinePanel.setVisibility(View.GONE);
        }
    }

    private void retryCurrentPage() {
        if (webView == null) {
            return;
        }

        String target = currentPageUrl;

        if (target == null || target.trim().isEmpty()) {
            target = WEBSITE;
        }

        if (!isInternalUrl(target)) {
            target = WEBSITE;
        }

        webView.loadUrl(target);
    }

    private void refreshCurrentPage() {
        if (webView == null) {
            return;
        }

        // Refresh the current page, not always the home page.
        String target = webView.getUrl();

        if (target == null || !isInternalUrl(target)) {
            target = currentPageUrl;
        }

        if (target == null || !isInternalUrl(target)) {
            target = WEBSITE;
        }

        webView.loadUrl(target);

        Toast.makeText(
            this,
            "Refreshing JJ Movies...",
            Toast.LENGTH_SHORT
        ).show();
    }

    private boolean isInternalUrl(String url) {
        if (url == null) {
            return false;
        }

        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme == null || host == null) {
            return false;
        }

        if (!"http".equalsIgnoreCase(scheme)
                && !"https".equalsIgnoreCase(scheme)) {
            return false;
        }

        return host.equalsIgnoreCase(SITE_HOST)
            || host.equalsIgnoreCase(SUPABASE_HOST);
    }

    private boolean handleUrl(String url) {
        if (url == null) {
            return true;
        }

        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme == null) {
            return true;
        }

        if ("http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme)) {

            String path = uri.getPath() == null
                ? "" : uri.getPath().toLowerCase();

            if (host != null &&
                (host.equalsIgnoreCase(SITE_HOST)
                || host.equalsIgnoreCase(SUPABASE_HOST))) {
                return false;
            }

            if (path.matches(
                ".*\\.(mp4|mkv|avi|mov|webm|zip|rar|pdf|apk)$")) {
                return false;
            }
        }

        openExternal(url);
        return true;
    }

    private void openExternal(String url) {
        try {
            Intent intent =
                new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(
                this,
                "No compatible app found for this link.",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Intentionally do not reload here.
        // Returning from another app should preserve the current page.
    }

    @Override
    public void onBackPressed() {
        if (offlinePanel != null
                && offlinePanel.getVisibility() == View.VISIBLE) {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
            } else {
                super.onBackPressed();
            }
            return;
        }

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) {
            webView.saveState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
