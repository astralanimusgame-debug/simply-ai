package com.simplyai.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import org.json.JSONObject;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private WebView web;
    private CredentialManager credentials;
    private final Executor background = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        credentials = CredentialManager.create(this);
        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportMultipleWindows(false);
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    openExternal(uri);
                    return true;
                }
                return false;
            }
        });
        setContentView(web);
        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl("file:///android_asset/index.html");
        }
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) {
        }
    }

    /* ---------- Sign in with Google (Credential Manager) ---------- */

    private boolean googleConfigured() {
        return BuildConfig.GOOGLE_WEB_CLIENT_ID != null && !BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty();
    }

    private void signIn() {
        if (!googleConfigured()) {
            sendError("not_configured");
            return;
        }
        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(option).build();
        credentials.getCredentialAsync(this, request, new CancellationSignal(), background,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        Credential credential = response.getCredential();
                        if (credential instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                            try {
                                GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(credential.getData());
                                JSONObject user = new JSONObject();
                                user.put("name", google.getDisplayName() != null ? google.getDisplayName() : "");
                                user.put("email", google.getId());
                                sendJs("window.onGoogleSignIn && window.onGoogleSignIn(" + user + ")");
                            } catch (Exception e) {
                                sendError("bad_credential");
                            }
                        } else {
                            sendError("unexpected_credential");
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        sendError(e instanceof GetCredentialCancellationException ? "cancelled" : "failed");
                    }
                });
    }

    private void signOut() {
        credentials.clearCredentialStateAsync(new ClearCredentialStateRequest(), new CancellationSignal(), background,
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                    }

                    @Override
                    public void onError(ClearCredentialException e) {
                    }
                });
    }

    private void sendError(String code) {
        sendJs("window.onGoogleSignInError && window.onGoogleSignInError(" + JSONObject.quote(code) + ")");
    }

    private void sendJs(String script) {
        runOnUiThread(() -> web.evaluateJavascript(script, null));
    }

    /* ---------- lifecycle ---------- */

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.appBack && window.appBack()) ? 'y' : 'n'", value -> {
            if (!"\"y\"".equals(value)) {
                finish();
            }
        });
    }

    /* ---------- bridge for the page ---------- */

    private class Bridge {
        @JavascriptInterface
        public void copy(String text) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("prompt", text));
            }
        }

        @JavascriptInterface
        public boolean hasGoogle() {
            return googleConfigured();
        }

        @JavascriptInterface
        public void signIn() {
            runOnUiThread(MainActivity.this::signIn);
        }

        @JavascriptInterface
        public void signOut() {
            runOnUiThread(MainActivity.this::signOut);
        }
    }
}
