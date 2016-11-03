package org.omnirom.omniswitch.launcher;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.android.internal.app.AssistUtils;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;

/**
 */
public class AssistManager {

    private static final String TAG = "AssistManager";

    private static final long TIMEOUT_SERVICE = 2500;
    private static final long TIMEOUT_ACTIVITY = 1000;

    private final Context mContext;
    private final AssistUtils mAssistUtils;

    private IVoiceInteractionSessionShowCallback mShowCallback =
            new IVoiceInteractionSessionShowCallback.Stub() {

        @Override
        public void onFailed() throws RemoteException {
        }

        @Override
        public void onShown() throws RemoteException {
        }
    };

    public AssistManager(Context context) {
        mContext = context;
        mAssistUtils = new AssistUtils(context);
    }

    public void startAssist(Bundle args) {
        final ComponentName assistComponent = getAssistInfo();
        if (assistComponent == null) {
            return;
        }

        final boolean isService = assistComponent.equals(getVoiceInteractorComponentName());
        startAssistInternal(args, assistComponent, isService);
    }

    public void hideAssist() {
        mAssistUtils.hideCurrentSession();
    }

    private void startAssistInternal(Bundle args, @NonNull ComponentName assistComponent,
            boolean isService) {
        if (isService) {
            startVoiceInteractor(args);
        } else {
            startAssistActivity(args, assistComponent);
        }
    }

    private void startAssistActivity(Bundle args, @NonNull ComponentName assistComponent) {
        boolean structureEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_STRUCTURE_ENABLED, 1) != 0;

        final Intent intent = ((SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE))
                .getAssistIntent(structureEnabled);
        if (intent == null) {
            return;
        }
        intent.setComponent(assistComponent);
        intent.putExtras(args);

        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    mContext.startActivity(intent);
                }
            });
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Activity not found for " + intent.getAction());
        }
    }

    private void startVoiceInteractor(Bundle args) {
        mAssistUtils.showSessionForActiveService(args,
                VoiceInteractionSession.SHOW_SOURCE_ASSIST_GESTURE, mShowCallback, null);
    }

    public ComponentName getVoiceInteractorComponentName() {
        return mAssistUtils.getActiveServiceComponentName();
    }

    private boolean isVoiceSessionRunning() {
        return mAssistUtils.isSessionRunning();
    }

    @Nullable
    private ComponentName getAssistInfo() {
        return getAssistComponent();
    }

    private ComponentName getAssistComponent() {
        final String setting = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT);
        if (setting != null) {
            return ComponentName.unflattenFromString(setting);
        }

        // Fallback to keep backward compatible behavior when there is no user setting.
        if (mAssistUtils.activeServiceSupportsAssistGesture()) {
            return mAssistUtils.getActiveServiceComponentName();
        }

        Intent intent = ((SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE))
                .getAssistIntent(false);
        android.content.pm.PackageManager pm = mContext.getPackageManager();
        ResolveInfo info = pm.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
        if (info != null) {
            return new ComponentName(info.activityInfo.applicationInfo.packageName,
                    info.activityInfo.name);
        }
        return null;
    }
}
