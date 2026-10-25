
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
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private float touchStartY;
    private boolean firstResume = true;

    private static final String WEBSITE =
        "https://geeras866-lab.github.io/JJ-Movies/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.rgb(9, 13, 24));

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportMultipleWindows(false);

        // Always check the website for updated content.
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, String url) {
                return handleUrl(url);
            }
        });

        // Pull down at the top of the page to refresh.
        webView.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartY = event.getY();
                    break;

                case MotionEvent.ACTION_UP:
                    float distance = event.getY() - touchStartY;

                    if (distance > 150 && webView.getScrollY() == 0) {
                        refreshWebsite();
                    }
                    break;
            }

            return false;
        });

        // Handle direct file downloads.
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

        setContentView(rootLayout);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(WEBSITE);
        }
    }

    private void refreshWebsite() {
        if (webView != null) {
            webView.clearCache(false);
            webView.loadUrl(WEBSITE);

            Toast.makeText(
                this,
                "Refreshing JJ Movies...",
                Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (firstResume) {
            firstResume = false;
        } else if (webView != null) {
            refreshWebsite();
        }
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
                (host.equalsIgnoreCase("geeras866-lab.github.io")
                || host.equalsIgnoreCase(
                    "tswyabgdldbsaopipvug.supabase.co"))) {
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
    public void onBackPressed() {
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
