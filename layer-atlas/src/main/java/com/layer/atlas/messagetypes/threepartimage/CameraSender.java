package com.layer.atlas.messagetypes.threepartimage;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.layer.atlas.R;
import com.layer.atlas.messagetypes.AttachmentSender;
import com.layer.atlas.util.Log;
import com.layer.sdk.messaging.Message;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

public class CameraSender extends AttachmentSender {
    public static final int REQUEST_CODE = 47002;

    private final WeakReference<Activity> mActivity;
    private final AtomicReference<String> mPhotoFilePath = new AtomicReference<String>(null);

    public CameraSender(int titleResId, Integer iconResId, Activity activity) {
        this(activity.getString(titleResId), iconResId, activity);
    }

    public CameraSender(String title, Integer iconResId, Activity activity) {
        super(title, iconResId);
        mActivity = new WeakReference<Activity>(activity);
    }

    @Override
    public boolean requestSend() {
        Activity activity = mActivity.get();
        if (activity == null) {
            if (Log.isLoggable(Log.ERROR)) Log.e("Activity went out of scope.");
            return false;
        }
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Sending camera image");
        String fileName = "cameraOutput" + System.currentTimeMillis() + ".jpg";
        File file = new File(getContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), fileName);
        mPhotoFilePath.set(file.getAbsolutePath());
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final Uri outputUri = Uri.fromFile(file);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        activity.startActivityForResult(cameraIntent, REQUEST_CODE);
        return true;
    }

    @Override
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) return false;
        if (resultCode != Activity.RESULT_OK) {
            if (Log.isLoggable(Log.ERROR)) Log.e("Result: " + requestCode + ", data: " + data);
            return true;
        }
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Received camera response");
        try {
            String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
            Message message = ThreePartImageUtils.newThreePartImageMessage(activity, getLayerClient(), new File(mPhotoFilePath.get()));
            message.getOptions().pushNotificationMessage(getContext().getString(R.string.atlas_notification_image, myName));
            send(message);
        } catch (IOException e) {
            if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
        }
        return true;
    }

    /**
     * Saves photo file path during e.g. screen rotation
     */
    @Override
    public Parcelable onSaveInstanceState() {
        String path = mPhotoFilePath.get();
        if (path == null) return null;
        Bundle bundle = new Bundle();
        bundle.putString("photoFilePath", path);
        return bundle;
    }

    /**
     * Restores photo file path during e.g. screen rotation
     */
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state == null) return;
        String path = ((Bundle) state).getString("photoFilePath");
        mPhotoFilePath.set(path);
    }
}
