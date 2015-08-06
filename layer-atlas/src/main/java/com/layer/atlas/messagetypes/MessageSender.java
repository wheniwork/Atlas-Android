package com.layer.atlas.messagetypes;

import android.content.Context;

import com.layer.atlas.provider.ParticipantProvider;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;

/**
 * MessageSenders handle the construction- and sending- of Messages.
 */
public abstract class MessageSender {
    private Conversation mConversation;

    private Context mContext;
    private LayerClient mLayerClient;
    private ParticipantProvider mParticipantProvider;

    public void init(Context context, LayerClient layerClient, ParticipantProvider participantProvider) {
        mContext = context;
        mLayerClient = layerClient;
        mParticipantProvider = participantProvider;
    }

    /**
     * Sets the Conversation used for sending generated Messages.
     *
     * @param conversation The Conversation to send generated Messages.
     */
    public void setConversation(Conversation conversation) {
        mConversation = conversation;
    }

    protected Context getContext() {
        return mContext;
    }

    protected LayerClient getLayerClient() {
        return mLayerClient;
    }

    protected ParticipantProvider getParticipantProvider() {
        return mParticipantProvider;
    }

    protected Conversation getConversation() {
        return mConversation;
    }
}
