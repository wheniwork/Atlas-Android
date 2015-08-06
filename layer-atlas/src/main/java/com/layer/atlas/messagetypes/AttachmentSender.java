package com.layer.atlas.messagetypes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.layer.atlas.provider.ParticipantProvider;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;

/**
 * AttachmentSenders populate the AtlasMessageComposer attachment menu and handle message sending
 * requests.  AttachmentSenders can interact with the Activity lifecycle to preserve instance state
 * and receive activity results when needed.
 */
public abstract class AttachmentSender extends MessageSender {
    private final String mTitle;
    private final Integer mIcon;

    private Conversation mConversation;

    private Context mContext;
    private LayerClient mLayerClient;
    private ParticipantProvider mParticipantProvider;

    public AttachmentSender(String title, Integer icon) {
        mTitle = title;
        mIcon = icon;
    }

    /**
     * Begins an attachment sending operation.  This might launch an Intent for selecting from a
     * gallery, taking a camera photo, or simply sending a message of the given type.  If an Intent
     * is generated for a result, consider overriding onActivityResult().
     *
     * @return `true` if a send operation is started, or `false` otherwise.
     * @see #onActivityResult(Activity, int, int, Intent)
     */
    public abstract boolean requestSend();

    public void init(Context context, LayerClient layerClient, ParticipantProvider participantProvider) {
        mContext = context;
        mLayerClient = layerClient;
        mParticipantProvider = participantProvider;
    }

    /**
     * Override to save instance state.
     *
     * @return new saved instance state.
     * @see #onRestoreInstanceState(Parcelable)
     */
    public Parcelable onSaveInstanceState() {
        // Optional override
        return null;
    }

    /**
     * Override if saved instance state is required.
     *
     * @param state State previously created with onSaveInstanceState().
     * @see #onSaveInstanceState()
     */
    public void onRestoreInstanceState(Parcelable state) {
        // Optional override
    }

    /**
     * Override to handle results from onActivityResult.
     *
     * @return true if the result was handled, or false otherwise.
     */
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        // Optional override
        return false;
    }

    /**
     * Returns the title for this AttachmentSender, typically for use in the AtlasMessageComposer
     * attachment menu.
     *
     * @return The title for this AttachmentSender.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the icon resource ID for this AttachmentSender, typically for use in the
     * AtlasMessageComposer attachment menu, or `null` for none.
     *
     * @return The icon resource ID for this AttachmentSender.
     */
    public Integer getIcon() {
        return mIcon;
    }

    @Override
    public void setConversation(Conversation conversation) {
        mConversation = conversation;
    }

    @Override
    protected Context getContext() {
        return mContext;
    }

    @Override
    protected LayerClient getLayerClient() {
        return mLayerClient;
    }

    @Override
    protected ParticipantProvider getParticipantProvider() {
        return mParticipantProvider;
    }

    @Override
    protected Conversation getConversation() {
        return mConversation;
    }
}
