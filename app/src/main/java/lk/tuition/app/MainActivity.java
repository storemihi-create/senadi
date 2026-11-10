package lk.tuition.app;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.print.PrintDocumentAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** වෙබ් පද්ධතිය (කාර්ය මණ්ඩල / සිසු ද්වාරය) පෙන්වන සරල WebView ඇප්. */
public class MainActivity extends Activity {
    private static final int REQ_CAMERA = 11;
    private static final int REQ_FILE = 12;

    private WebView web;
    private PermissionRequest pendingPerm;
    private ValueCallback<Uri[]> fileCb;
    private final List<WebView> printViews = new ArrayList<>();
    private String startUrl;
    private String host;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        startUrl = BuildConfig.BASE_URL + BuildConfig.START_PATH;
        host = Uri.parse(BuildConfig.BASE_URL).getHost();

        web = new WebView(this);
        web.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(s.getUserAgentString() + " TuitionApp/" + BuildConfig.VERSION_NAME);
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, false);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String sch = u.getScheme() == null ? "" : u.getScheme();
                boolean isWeb = sch.equals("http") || sch.equals("https");
                // CDN (QR library, fonts) සහ අපේ සේවාදායකය ඇතුළත ඇප් එකේම; WhatsApp/tel/mailto ආදිය පිටතින්
                if (isWeb && (host != null && host.equalsIgnoreCase(u.getHost()))) return false;
                if (isWeb && !r.isForMainFrame()) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, "Cannot open link", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) showOffline();
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest req) {
                runOnUiThread(() -> {
                    boolean wantsCam = false;
                    for (String r : req.getResources()) if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) wantsCam = true;
                    if (!wantsCam) { req.deny(); return; }
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        req.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    } else {
                        pendingPerm = req;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
                    }
                });
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try {
                    startActivityForResult(p.createIntent(), REQ_FILE);
                } catch (ActivityNotFoundException e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }
        });

        // CSV / backup ගොනු බාගත කිරීම
        web.setDownloadListener((url, ua, cd, mime, len) -> {
            try {
                DownloadManager.Request rq = new DownloadManager.Request(Uri.parse(url));
                rq.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                rq.addRequestHeader("User-Agent", ua);
                rq.setMimeType(mime);
                rq.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                rq.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, cd, mime));
                ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(rq);
                Toast.makeText(this, "Downloading…", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });

        if (b != null) web.restoreState(b); else load();
    }

    private void load() {
        if (online()) web.loadUrl(startUrl); else showOffline();
    }

    private boolean online() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo n = cm == null ? null : cm.getActiveNetworkInfo();
        return n != null && n.isConnected();
    }

    private void showOffline() {
        String html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>" +
                "<body style='font-family:sans-serif;text-align:center;padding:60px 24px;color:#14213D;background:#EAF1FB'>" +
                "<div style='font-size:56px'>📡</div><h2>No connection / සම්බන්ධතාවය නැත</h2>" +
                "<p style='color:#6A7690'>Check your internet and try again.<br>අන්තර්ජාලය පරීක්ෂා කර නැවත උත්සාහ කරන්න.</p>" +
                "<button onclick=\"AndroidBridge.retry()\" style='background:#1F4FD8;color:#fff;border:0;border-radius:12px;padding:14px 28px;font-size:16px;font-weight:700'>Retry / නැවත</button>" +
                "</body></html>";
        web.loadDataWithBaseURL(BuildConfig.BASE_URL, html, "text/html", "utf-8", null);
    }

    /** JS ↔ Android පාලම */
    private class Bridge {
        @JavascriptInterface
        public void retry() { runOnUiThread(MainActivity.this::load); }

        /** ප්‍රින්ට් (A4 වාර්තා, ID කාඩ්) – Android print සේවාව මගින් PDF/මුද්‍රණය */
        @JavascriptInterface
        public void printHtml(final String html, final String title) {
            runOnUiThread(() -> {
                final WebView pv = new WebView(MainActivity.this);
                pv.getSettings().setJavaScriptEnabled(false);
                printViews.add(pv);
                pv.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView v, String url) {
                        PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                        String name = (title == null || title.isEmpty()) ? "Report" : title;
                        PrintDocumentAdapter ad = v.createPrintDocumentAdapter(name);
                        PrintAttributes at = new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build();
                        pm.print(name, ad, at);
                        v.postDelayed(() -> printViews.remove(v), 120000);   // print අවසන් වන තුරු WebView එක රඳවා ගනී
                    }
                });
                pv.loadDataWithBaseURL(BuildConfig.BASE_URL, html, "text/html", "utf-8", null);
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == REQ_CAMERA && pendingPerm != null) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) pendingPerm.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
            else pendingPerm.deny();
            pendingPerm = null;
        }
    }

    @Override
    protected void onActivityResult(int code, int res, Intent data) {
        if (code == REQ_FILE && fileCb != null) {
            fileCb.onReceiveValue(res == RESULT_OK ? WebChromeClient.FileChooserParams.parseResult(res, data) : null);
            fileCb = null;
            return;
        }
        super.onActivityResult(code, res, data);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onPause() { super.onPause(); CookieManager.getInstance().flush(); }
}
