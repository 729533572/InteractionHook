package com.rexy.example.extend;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.PermissionChecker;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Toast;

import com.rexy.example.widget.InteractionFloatViewHolder;
import com.rexy.hook.InteractionHook;
import com.rexy.hook.interfaces.IHandleListener;
import com.rexy.hook.interfaces.IHandleResult;
import com.rexy.hook.interfaces.IHookHandler;

/**
 * @author: rexy
 * @date: 2017-06-05 14:45
 */
public class BaseActivity extends FragmentActivity implements IHandleListener {
    private InteractionHook mInteractionHook;
    //真正使用不是让 BaseActivity 去监听IHandleListener，设置个全局的监听就可以。
    //本例是为了在弹出浮层上适时显示监听结果。
    protected InteractionFloatViewHolder mInteractionViewHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        mInteractionHook = new InteractionHook(this, true, "rexy_interaction");

        mInteractionViewHolder = InteractionFloatViewHolder.getInstance(this);

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        mInteractionViewHolder.updateViewWidth((int) (screenWidth * 0.9f), 0);
        ((MyApplication) getApplication()).registerHandleListener(this);

        if (PermissionChecker.PERMISSION_GRANTED == PermissionChecker.checkSelfPermission(this, Manifest.permission.SYSTEM_ALERT_WINDOW)) {
            mInteractionViewHolder.show();
        } else {
            if (Build.VERSION.SDK_INT > 23 && !Settings.canDrawOverlays(BaseActivity.this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1000);
            } else {
                try {
                    mInteractionViewHolder.show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "require SYSTEM_ALERT_WINDOW permission", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean handled = mInteractionHook == null ? false : mInteractionHook.onTouch(ev);
        if (handled) {
            final long now = SystemClock.uptimeMillis();
            MotionEvent cancelEvent = MotionEvent.obtain(now, now,
                    MotionEvent.ACTION_CANCEL, ev.getX(), ev.getY(), 0);
            cancelEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            if (mInteractionHook != null) {
                mInteractionHook.onTouch(cancelEvent);
            }
            super.dispatchTouchEvent(cancelEvent);
        }
        return handled || super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onReceiveHandleError(IHookHandler handler, Throwable error, String category) {
        mInteractionViewHolder.recordResult(new ErrorResult(null, "error", error));
        return false;
    }

    @Override
    public boolean onReceiveHandleResult(IHookHandler handler, IHandleResult result) {
        mInteractionViewHolder.recordResult(result);
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        InteractionHook.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        InteractionHook.onPause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInteractionHook != null) {
            mInteractionHook.destroy();
        }
        ((MyApplication) getApplication()).unregisterHandleListener(this);
        InteractionHook.onDestroy(this);
    }
}
