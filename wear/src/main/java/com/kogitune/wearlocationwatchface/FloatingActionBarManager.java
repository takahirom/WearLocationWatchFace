package com.kogitune.wearlocationwatchface;

import android.content.Context;
import android.graphics.PixelFormat;
import android.support.wearable.view.CircledImageView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by takam on 2015/01/18.
 */
public class FloatingActionBarManager {

    private static final String TAG = "FloatingActionBarManager";
    private final CircledImageView circledImageView;
    private final WindowManager windowManager;
    private final Context context;
    private int lastX;
    private int lastY;

    public FloatingActionBarManager(Context context) {
        this.context = context;
        WindowManager.LayoutParams params = createLayoutParams(0, 0);  // viewを透明にする
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        circledImageView = new CircledImageView(context);
        circledImageView.setElevation(10f);
        windowManager.addView(circledImageView, params);
    }

    public void update(int color, int x, int y) {
        lastX = x;
        lastY = y;
        circledImageView.setImageResource(R.drawable.ic_sync_white_18dp);
        circledImageView.setCircleColor(color);

        circledImageView.setCircleRadius(context.getResources().getDimensionPixelSize(R.dimen.action_button_radius));
        windowManager.updateViewLayout(circledImageView, createLayoutParams(x, y));
    }

    public void setVisible(boolean visible){
        if (visible) {
            if (circledImageView.getParent() == null) {
                windowManager.addView(circledImageView, createLayoutParams(lastX, lastY));
            }
        }else{
            if (circledImageView.getParent() != null) {
                windowManager.removeView(circledImageView);
            }
        }
    }


    private WindowManager.LayoutParams createLayoutParams(int x, int y) {
        Log.d(TAG, "x:" + x + " y:" + y);
        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                context.getResources().getDimensionPixelSize(R.dimen.action_button_diameter),
                context.getResources().getDimensionPixelSize(R.dimen.action_button_diameter),
                x,
                y,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,       // アプリケーションのTOPに配置
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |  // フォーカスを当てない(下の画面の操作がd系なくなるため)
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |        // OverlapするViewを全画面表示
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,  // モーダル以外のタッチを背後のウィンドウへ送信
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        return layoutParams;
    }

    public void setOnClickListener(View.OnClickListener listener) {
        circledImageView.setOnClickListener(listener);
    }

}
