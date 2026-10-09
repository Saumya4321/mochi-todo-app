package app.mochitodo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

/**
 * Mochi Todo: shows the synced Mochi Todo page full-screen in an in-app web view.
 * No browser bar, no hand-off to Chrome. Sign-in to Claude happens inside the app
 * and the login cookie is kept, so you only sign in once.
 */
public class MainActivity extends Activity {

    static final String HOME = "https://claude.ai/artifact/1BKvSkHxw9yNx6iYJPbvkq";
    static final int MATCH = FrameLayout.LayoutParams.MATCH_PARENT;

    private FrameLayout root;
    private WebView web;
    private WebView popup; // sign-in popups (e.g. "Continue with Google") open here

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        CookieManager.getInstance().setAcceptCookie(true);

        root = new FrameLayout(this);
        web = makeWebView();
        root.addView(web, new FrameLayout.LayoutParams(MATCH, MATCH));
        setContentView(root);

        String target = urlFromIntent(getIntent());
        boolean restored = saved != null && web.restoreState(saved) != null;
        if (target != null) web.loadUrl(target);
        else if (!restored) web.loadUrl(HOME);
    }

    private WebView makeWebView() {
        WebView w = new WebView(this);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        // Present as regular mobile Chrome so Google sign-in is offered inside the app.
        s.setUserAgentString(cleanUserAgent(s.getUserAgentString()));
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
        w.setBackgroundColor(getColor(R.color.bg));
        w.setWebViewClient(new Client());
        w.setWebChromeClient(new Chrome());
        return w;
    }

    static String cleanUserAgent(String ua) {
        return ua.replace("; wv)", ")").replaceAll(" Version/[0-9.]+", "");
    }

    private class Client extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri u = request.getUrl();
            String scheme = u.getScheme() == null ? "" : u.getScheme();
            switch (scheme) {
                case "http": case "https": case "about": case "data": case "blob": case "javascript":
                    return false; // stay inside the app
            }
            // mailto:, tel:, intent: etc. go to the matching app on the phone
            try {
                Intent i = "intent".equals(scheme)
                        ? Intent.parseUri(u.toString(), Intent.URI_INTENT_SCHEME)
                        : new Intent(Intent.ACTION_VIEW, u);
                i.addCategory(Intent.CATEGORY_BROWSABLE);
                i.setComponent(null);
                i.setSelector(null);
                startActivity(i);
            } catch (Exception ignored) { }
            return true;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (view == web && request.isForMainFrame()) {
                view.loadDataWithBaseURL(null, OFFLINE_PAGE, "text/html", "utf-8", null);
            }
        }
    }

    private class Chrome extends WebChromeClient {
        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean userGesture, Message resultMsg) {
            closePopup();
            popup = makeWebView();
            root.addView(popup, new FrameLayout.LayoutParams(MATCH, MATCH));
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(popup);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            if (window == popup) closePopup();
        }
    }

    private void closePopup() {
        if (popup != null) {
            root.removeView(popup);
            popup.destroy();
            popup = null;
        }
    }

    private static String urlFromIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return null;
        Uri data = intent.getData();
        return data != null && "https".equals(data.getScheme()) ? data.toString() : null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String target = urlFromIntent(intent);
        if (target != null) {
            closePopup();
            web.loadUrl(target);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (popup != null) {
            if (popup.canGoBack()) popup.goBack(); else closePopup();
        } else if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush(); // keep you signed in
    }

    @Override
    protected void onDestroy() {
        closePopup();
        if (web != null) web.destroy();
        super.onDestroy();
    }

    static final String OFFLINE_PAGE =
        "<!doctype html><meta name=viewport content='width=device-width,initial-scale=1'>"
      + "<style>body{margin:0;height:100vh;display:grid;place-items:center;font-family:sans-serif;"
      + "background:#fff7f9;color:#3f2a35;text-align:center}"
      + "@media(prefers-color-scheme:dark){body{background:#1f1720;color:#f7e8ee}}"
      + "button{margin-top:16px;font:700 16px sans-serif;border:0;border-radius:14px;padding:12px 22px;"
      + "background:#ff7fa3;color:#fff}</style>"
      + "<div><div style='font-size:20px;font-weight:800'>Mochi can't reach the internet</div>"
      + "<div style='opacity:.7;margin-top:6px'>Check your connection and try again.</div>"
      + "<button onclick=\"location.href='" + HOME + "'\">Try again</button></div>";
}
