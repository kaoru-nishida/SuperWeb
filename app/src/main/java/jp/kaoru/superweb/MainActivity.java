package jp.kaoru.superweb;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements DownloadListener, WebView.FindListener, SwipeRefreshLayout.OnRefreshListener {

    private WebView webview;

    private SwipeRefreshLayout swipeLayout;

    private ArrayList<String> loadResources = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_main);
        this.setProgressBarIndeterminate(true);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (this.getIntent().hasExtra("backbutton")) {
            getSupportActionBar().setDisplayShowHomeEnabled(this.getIntent().getBooleanExtra("backbutton", true));
            getSupportActionBar().setDisplayHomeAsUpEnabled(this.getIntent().getBooleanExtra("backbutton", true));
        }

        swipeLayout = findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        webview = findViewById(R.id.webView);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setAllowContentAccess(true);
        webview.getSettings().setAllowFileAccess(true);
        webview.getSettings().setAllowFileAccessFromFileURLs(true);
        webview.getSettings().setAppCachePath(getExternalCacheDir().getPath());
        webview.getSettings().setDisplayZoomControls(true);
        webview.getSettings().setSupportZoom(true);
        webview.setDownloadListener(this);
        webview.setFindListener(this);
        webview.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 8.0.0; Pixel Build/OPR3.170623.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.98 Mobile Safari/537.36");
        webview.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                loadResources.clear();
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                Log.i("onLoadResource", url);
                loadResources.add(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (swipeLayout.isRefreshing()) {
                    swipeLayout.setRefreshing(false);
                }
                new Handler().postDelayed(() -> {
                    setTitle(webview.getTitle());
                    getSupportActionBar().setSubtitle(url);
                    getSupportActionBar().setLogo(new BitmapDrawable(getResources(), webview.getFavicon()));
                }, 1000);
            }

            @Override
            public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
                Log.i("shouldOverrideKeyEvent", "" + event.getKeyCode());
                return super.shouldOverrideKeyEvent(view, event);
            }
        });
        webview.addJavascriptInterface(new WebAppInterface(this), "Android");
        webview.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webview.getSettings().setAppCacheEnabled(true);
        webview.getSettings().setAppCachePath(this.getExternalCacheDir().getPath());
        CookieManager.getInstance().setAcceptCookie(true);

        if (this.getIntent().hasExtra("url")) {
            webview.loadUrl(this.getIntent().getStringExtra("url"));
        } else {
            webview.loadUrl("https://www.google.com");
        }

    }

    /**
     * キャプチャを撮る
     *
     * @param view 撮りたいview
     * @return 撮ったキャプチャ(Bitmap)
     */
    public Bitmap getViewCapture(View view) {
        view.setDrawingCacheEnabled(true);

        // Viewのキャッシュを取得
        Bitmap cache = view.getDrawingCache();
        Bitmap screenShot = Bitmap.createBitmap(cache);
        view.setDrawingCacheEnabled(false);
        return screenShot;
    }

    /**
     * 撮ったキャプチャを保存
     *
     * @param view
     * @param file 書き込み先ファイルfile
     */
    public void saveCapture(View view, File file) {
        // キャプチャを撮る
        Bitmap capture = getViewCapture(view);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, false);
            // 画像のフォーマットと画質と出力先を指定して保存
            capture.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ie) {
                    fos = null;
                }
            }
        }
    }

    /**
     * ダウンロードタスクをダウンロードマネージャに追加
     *
     * @param url         URL
     * @param title       タイトル
     * @param description 詳細記述
     * @param wifiOnly    WiFi回線のみを使用するか
     * @return ダウンロードアイテムID
     */
    private long pushDownloadTask(String url, String title, String description, boolean wifiOnly) {
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);

        DownloadManager.Request request = new DownloadManager.Request(uri);
        String[] s = {};
        try {
            s = new URL(url).getPath().split("/");
            Log.i("pushDownloadTask", s[s.length - 1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("Download", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + s[s.length - 1]);
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + s[s.length - 1]);
        request.setDestinationUri(Uri.fromFile(file));
        // ダウンロード時のタイトルを設定
        if (title != null && title.length() > 0) {
            request.setTitle(title);
        } else {
            request.setTitle("Download");
        }

        // ダウンロードタスクの詳細記述を設定
        if (description != null && description.length() > 0) {
            request.setDescription(description);
        }

        if (wifiOnly) {
            // ダウンロード時、WiFi回線のみを使用
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        } else {
            // 指定がない場合3G/WiFiを両方使用
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE
                    | DownloadManager.Request.NETWORK_WIFI);
        }

        // キューにダウンロードタスクを追加、タスクIDを同時に取得
        long id = manager.enqueue(request);

        return id;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == 4 && webview.canGoBack()) {
            webview.goBack();
        } else {
            webview.goBack();
        }

        return false;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onRefresh() {
        webview.reload();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_info:
                MaterialDialog.Builder builder = new MaterialDialog.Builder(this)
                        .icon(new BitmapDrawable(getResources(), webview.getFavicon()))
                        .title("このページの詳細")
                        .content(
                                "・タイトル\n" + webview.getTitle() +
                                        "\n\n・URL\n" + webview.getUrl() +
                                        "\n\n・オリジナルURL\n" + webview.getOriginalUrl());

                MaterialDialog dialog = builder.build();
                dialog.show();
                break;
            case R.id.action_screenshot:
                String filename = new Date().getMonth() + "_" + new Date().getDate() + "_" + new Date().getHours() + "_" + new Date().getMinutes() + "_" + new Date().getSeconds() + ".png";
                if (!new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + File.separator + "superweb").exists()) {
                    new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + File.separator + "superweb").mkdir();
                }
                saveCapture(findViewById(R.id.webView), new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + File.separator + "superweb" + File.separator + filename));
                break;
            case R.id.action_resources:
                Intent intent = new Intent();
                intent.setClass(this, LoadResourcesActivity.class);
                intent.putExtra("urls", loadResources);
                startActivity(intent);
                break;
            case R.id.action_settings:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * ダウンロードの要求された時
     *
     * @param s  url
     * @param s1 User-Agent
     * @param s2 不明
     * @param s3 mine-type
     */
    @Override
    public void onDownloadStart(String s, String s1, String s2, String s3, long l) {
        MaterialDialog md = new MaterialDialog.Builder(this)
                .title(R.string.download_ask)
                .positiveText(getString(R.string.onPositiveText))
                .negativeText(getString(R.string.onNegativeText))
                .content(s)
                .onPositive((dialog, which) -> {
                    try {
                        pushDownloadTask(s, new URL(s).getHost(), s + "からダウンロード", true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .onNegative((dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onFindResultReceived(int i, int i1, boolean b) {
        Log.i("onFindResultReceived", "i:" + i + " i1:" + i1 + " b:" + b);
    }
}