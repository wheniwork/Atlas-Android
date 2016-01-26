package com.layer.atlas.messagetypes.threepartimage;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.layer.atlas.R;
import com.layer.atlas.messagetypes.AttachmentSender;
import com.layer.atlas.util.Log;
import com.layer.sdk.messaging.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class GallerySender extends AttachmentSender {
    public static final int REQUEST_CODE = 47001;
    public static final int GALLERY_REQUEST_CODE = 48;

    private final WeakReference<Activity> mActivity;

    public GallerySender(int titleResId, Integer iconResId, Activity activity) {
        this(activity.getString(titleResId), iconResId, activity);
    }

    public GallerySender(String title, Integer iconResId, Activity activity) {
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
        if (hasGalleryPermission()) {
            startGalleryIntent();
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    GALLERY_REQUEST_CODE);
        }
        return true;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == GALLERY_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGalleryIntent();
            } else {
                if (Log.isLoggable(Log.VERBOSE)) Log.v("Gallery permission denied");
            }
        }
        return true;
    }

    @Override
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) return false;
        if (resultCode != Activity.RESULT_OK) {
            if (Log.isLoggable(Log.ERROR)) Log.e("Result: " + requestCode + ", data: " + data);
            return true;
        }
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Received gallery response");
        try {
            String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
            Message message = ThreePartImageUtils.newThreePartImageMessage(activity, getLayerClient(), data.getData());
            message.getOptions().pushNotificationMessage(getContext().getString(R.string.atlas_notification_image, myName));
            send(message);
        } catch (IOException e) {
            if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
        }
        return true;
    }

    private void startGalleryIntent() {
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Sending gallery image");
        Activity activity = mActivity.get();
        if (activity == null) {
            if (Log.isLoggable(Log.ERROR)) Log.e("Activity went out of scope.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(Intent.createChooser(intent, getContext().getString(R.string.atlas_gallery_sender_chooser)), REQUEST_CODE);
    }

    private boolean hasGalleryPermission() {
        Activity activity = mActivity.get();
        if (activity == null) {
            if (Log.isLoggable(Log.ERROR)) Log.e("Activity went out of scope.");
            return false;
        } else {
            int hasGalleryPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
            return hasGalleryPermission == PackageManager.PERMISSION_GRANTED;
        }
    }

}
