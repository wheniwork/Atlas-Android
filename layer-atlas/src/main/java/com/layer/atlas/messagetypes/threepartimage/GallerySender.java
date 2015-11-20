package com.layer.atlas.messagetypes.threepartimage;

import android.app.Activity;
import android.content.Intent;

import com.layer.atlas.R;
import com.layer.atlas.messagetypes.AttachmentSender;
import com.layer.atlas.util.Log;
import com.layer.sdk.messaging.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class GallerySender extends AttachmentSender {
    public static final int REQUEST_CODE = 47001;

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
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Sending gallery image");
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(Intent.createChooser(intent, getContext().getString(R.string.atlas_gallery_sender_chooser)), REQUEST_CODE);
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


}
