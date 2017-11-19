package jp.kaoru.superweb;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import static android.content.Context.CLIPBOARD_SERVICE;

public class WebAppInterface {
    Context mContext;

    /**
     * Instantiate the interface and set the context
     */
    WebAppInterface(Context c) {
        mContext = c;
    }

    /**
     * Show a toast from the web page
     */
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void showDialog(final String title, final String content) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext)
                .title(title)
                .content(content);
        MaterialDialog dialog = builder.build();
        dialog.show();
    }

    @JavascriptInterface
    public void copy(String st) {
        //クリップボードに格納するItemを作成
        ClipData.Item item = new ClipData.Item(st);
        //MIMETYPEの作成
        String[] mimeType = new String[1];
        mimeType[0] = ClipDescription.MIMETYPE_TEXT_URILIST;
        //クリップボードに格納するClipDataオブジェクトの作成
        ClipData cd = new ClipData(new ClipDescription("text_data", mimeType), item);
        //クリップボードにデータを格納
        ClipboardManager cm = (ClipboardManager) mContext.getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(cd);
        Toast.makeText(mContext, st + "をコピーしました", Toast.LENGTH_SHORT).show();
    }
}
