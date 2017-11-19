package jp.kaoru.superweb;

/**
 * Created by kaoru on 17/11/19.
 */

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class DownloadReceiver extends BroadcastReceiver {

    /**
     * ダウンロード終了を検知
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            Toast.makeText(context, "ダウンロード完了しました", Toast.LENGTH_LONG).show();
        }
    }
}
