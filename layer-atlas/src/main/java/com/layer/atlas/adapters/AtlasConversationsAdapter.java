package com.layer.atlas.adapters;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.layer.atlas.AtlasAvatar;
import com.layer.atlas.R;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.atlas.util.ConversationStyle;
import com.layer.atlas.util.Util;
import com.layer.atlas.util.picasso.transformations.CircleTransform;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.RecyclerViewController;
import com.layer.sdk.query.SortDescriptor;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AtlasConversationsAdapter extends RecyclerView.Adapter<AtlasConversationsAdapter.ViewHolder> implements AtlasBaseAdapter<Conversation>, RecyclerViewController.Callback {
    protected final LayerClient mLayerClient;
    protected final ParticipantProvider mParticipantProvider;
    protected final Picasso mPicasso;
    private final RecyclerViewController<Conversation> mQueryController;
    private final LayoutInflater mInflater;
    private long mInitialHistory = 0;

    private OnConversationClickListener mConversationClickListener;
    private ViewHolder.OnClickListener mViewHolderClickListener;

    private final DateFormat mDateFormat;
    private final DateFormat mTimeFormat;
    private ConversationStyle conversationStyle;

    private Options mOptions;

    public AtlasConversationsAdapter(Context context, LayerClient client, ParticipantProvider participantProvider, Picasso picasso, Options options) {
        this(context, client, participantProvider, picasso, null, options);
    }

    public AtlasConversationsAdapter(Context context, LayerClient client, ParticipantProvider participantProvider, Picasso picasso, Collection<String> updateAttributes, Options options) {
        Query.Builder<Conversation> queryBuilder = Query.builder(Conversation.class)
                /* Only show conversations we're still a member of */
                .predicate(new Predicate(Conversation.Property.PARTICIPANT_COUNT, Predicate.Operator.GREATER_THAN, 1))

                /* Sort by the last Message's receivedAt time */
                .sortDescriptor(new SortDescriptor(Conversation.Property.LAST_MESSAGE_RECEIVED_AT, SortDescriptor.Order.DESCENDING));

        if (options.getPredicates() != null) {
            for (Predicate predicate : options.getPredicates()) {
                queryBuilder.predicate(predicate);
            }
        }

        Query<Conversation> query = queryBuilder.build();
        mQueryController = client.newRecyclerViewController(query, updateAttributes, this);
        mLayerClient = client;
        mParticipantProvider = participantProvider;
        mPicasso = picasso;
        mInflater = LayoutInflater.from(context);
        mDateFormat = android.text.format.DateFormat.getDateFormat(context);
        mTimeFormat = android.text.format.DateFormat.getTimeFormat(context);
        mOptions = options;
        mViewHolderClickListener = new ViewHolder.OnClickListener() {
            @Override
            public void onClick(ViewHolder viewHolder) {
                if (mConversationClickListener == null) return;
                mConversationClickListener.onConversationClick(AtlasConversationsAdapter.this, viewHolder.getConversation());
            }

            @Override
            public boolean onLongClick(ViewHolder viewHolder) {
                if (mConversationClickListener == null) return false;
                return mConversationClickListener.onConversationLongClick(AtlasConversationsAdapter.this, viewHolder.getConversation());
            }
        };
        setHasStableIds(false);
    }

    /**
     * Refreshes this adapter by re-running the underlying Query.
     */
    public void refresh() {
        mQueryController.execute();
    }


    //==============================================================================================
    // Initial message history
    //==============================================================================================

    public AtlasConversationsAdapter setInitialHistoricMessagesToFetch(long initialHistory) {
        mInitialHistory = initialHistory;
        return this;
    }

    public void setStyle(ConversationStyle conversationStyle) {
        this.conversationStyle = conversationStyle;
    }

    private void syncInitialMessages(final int start, final int length) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long desiredHistory = mInitialHistory;
                if (desiredHistory <= 0) return;
                for (int i = start; i < start + length; i++) {
                    try {
                        final Conversation conversation = getItem(i);
                        if (conversation == null || conversation.getHistoricSyncStatus() != Conversation.HistoricSyncStatus.MORE_AVAILABLE) {
                            continue;
                        }
                        Query<Message> localCountQuery = Query.builder(Message.class)
                                .predicate(new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, conversation))
                                .build();
                        long delta = desiredHistory - mLayerClient.executeQueryForCount(localCountQuery);
                        if (delta > 0) conversation.syncMoreHistoricMessages((int) delta);
                    } catch (IndexOutOfBoundsException e) {
                        // Concurrent modification
                    }
                }
            }
        }).start();
    }


    //==============================================================================================
    // Listeners
    //==============================================================================================

    public AtlasConversationsAdapter setOnConversationClickListener(OnConversationClickListener conversationClickListener) {
        mConversationClickListener = conversationClickListener;
        return this;
    }


    //==============================================================================================
    // Adapter
    //==============================================================================================

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder viewHolder = new ViewHolder(mInflater.inflate(ViewHolder.RESOURCE_ID, parent, false), conversationStyle);
        viewHolder.setClickListener(mViewHolderClickListener);
        viewHolder.mAvatarCluster
                .init(mParticipantProvider, mPicasso)
                .setStyle(conversationStyle.getAvatarStyle());
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        mQueryController.updateBoundPosition(position);
        Conversation conversation = mQueryController.getItem(position);
        Message lastMessage = conversation.getLastMessage();
        Context context = viewHolder.itemView.getContext();

        viewHolder.setConversation(conversation);
        HashSet<String> participantIds = new HashSet<String>(conversation.getParticipants());
        participantIds.remove(mLayerClient.getAuthenticatedUserId());

        Uri conversationIcon = mOptions.getConversationIconUri(conversation);
        if (conversationIcon == null) {
            viewHolder.setParticipantAvatars(participantIds);
        } else {
            viewHolder.setConversationIcon(mPicasso, conversationIcon,
                                           mOptions.getConversationIconErrorResource());
        }

        viewHolder.mTitleView.setText(mOptions.getConversationTitle(mLayerClient, mParticipantProvider, conversation));
        viewHolder.applyStyle(conversation.getTotalUnreadMessageCount() > 0);

        if (lastMessage == null) {
            viewHolder.mMessageView.setText(null);
            viewHolder.mTimeView.setText(null);
        } else {
            viewHolder.mMessageView.setText(Util.getLastMessageString(context, lastMessage));
            if (lastMessage.getReceivedAt() == null) {
                viewHolder.mTimeView.setText(null);
            } else {
                viewHolder.mTimeView.setText(Util.formatTime(context, lastMessage.getReceivedAt(), mTimeFormat, mDateFormat));
            }
        }
    }

    @Override
    public int getItemCount() {
        return mQueryController.getItemCount();
    }

    @Override
    public Integer getPosition(Conversation conversation) {
        return mQueryController.getPosition(conversation);
    }

    @Override
    public Integer getPosition(Conversation conversation, int lastPosition) {
        return mQueryController.getPosition(conversation, lastPosition);
    }

    @Override
    public Conversation getItem(int position) {
        return mQueryController.getItem(position);
    }

    @Override
    public Conversation getItem(RecyclerView.ViewHolder viewHolder) {
        return ((ViewHolder) viewHolder).getConversation();
    }


    //==============================================================================================
    // UI update callbacks
    //==============================================================================================

    @Override
    public void onQueryDataSetChanged(RecyclerViewController controller) {
        syncInitialMessages(0, getItemCount());
        notifyDataSetChanged();
    }

    @Override
    public void onQueryItemChanged(RecyclerViewController controller, int position) {
        notifyItemChanged(position);
    }

    @Override
    public void onQueryItemRangeChanged(RecyclerViewController controller, int positionStart, int itemCount) {
        notifyItemRangeChanged(positionStart, itemCount);
    }

    @Override
    public void onQueryItemInserted(RecyclerViewController controller, int position) {
        syncInitialMessages(position, 1);
        notifyItemInserted(position);
    }

    @Override
    public void onQueryItemRangeInserted(RecyclerViewController controller, int positionStart, int itemCount) {
        syncInitialMessages(positionStart, itemCount);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    @Override
    public void onQueryItemRemoved(RecyclerViewController controller, int position) {
        notifyItemRemoved(position);
    }

    @Override
    public void onQueryItemRangeRemoved(RecyclerViewController controller, int positionStart, int itemCount) {
        notifyItemRangeRemoved(positionStart, itemCount);
    }

    @Override
    public void onQueryItemMoved(RecyclerViewController controller, int fromPosition, int toPosition) {
        notifyItemMoved(fromPosition, toPosition);
    }


    //==============================================================================================
    // Inner classes
    //==============================================================================================

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        // Layout to inflate
        public final static int RESOURCE_ID = R.layout.atlas_conversation_item;

        // View cache
        protected TextView mTitleView;
        protected AtlasAvatar mAvatarCluster;
        protected ImageView mAvatarCustom;
        protected TextView mMessageView;
        protected TextView mTimeView;

        protected ConversationStyle conversationStyle;
        protected Conversation mConversation;
        protected OnClickListener mClickListener;

        public ViewHolder(View itemView, ConversationStyle conversationStyle) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            this.conversationStyle = conversationStyle;

            mAvatarCluster = (AtlasAvatar) itemView.findViewById(R.id.avatar);
            mAvatarCustom = (ImageView) itemView.findViewById(R.id.avatar_custom);
            mTitleView = (TextView) itemView.findViewById(R.id.title);
            mMessageView = (TextView) itemView.findViewById(R.id.last_message);
            mTimeView = (TextView) itemView.findViewById(R.id.time);
            itemView.setBackgroundColor(conversationStyle.getCellBackgroundColor());
        }

        public void applyStyle(boolean unread) {
            mTitleView.setTextColor(unread ? conversationStyle.getTitleUnreadTextColor() : conversationStyle.getTitleTextColor());
            mTitleView.setTypeface(unread ? conversationStyle.getTitleUnreadTextTypeface() : conversationStyle.getTitleTextTypeface(), unread ? conversationStyle.getTitleUnreadTextStyle() : conversationStyle.getTitleTextStyle());
            mMessageView.setTextColor(unread ? conversationStyle.getSubtitleTextColor() : conversationStyle.getSubtitleTextColor());
            mMessageView.setTypeface(unread ? conversationStyle.getSubtitleUnreadTextTypeface() : conversationStyle.getSubtitleTextTypeface(), unread ? conversationStyle.getSubtitleUnreadTextStyle() : conversationStyle.getSubtitleTextStyle());
            mTimeView.setTextColor(conversationStyle.getDateTextColor());
            mTimeView.setTypeface(conversationStyle.getDateTextTypeface());
        }

        protected ViewHolder setClickListener(OnClickListener clickListener) {
            mClickListener = clickListener;
            return this;
        }

        public Conversation getConversation() {
            return mConversation;
        }

        public void setConversation(Conversation conversation) {
            mConversation = conversation;
        }

        public void setParticipantAvatars(Set<String> participantIds) {
            setAvatarView(mAvatarCluster);
            mAvatarCluster.setParticipants(participantIds);
        }

        public void setConversationIcon(Picasso picasso, Uri uri, @DrawableRes int errorResource) {
            setAvatarView(mAvatarCustom);
            picasso.load(uri)
                   .tag(AtlasAvatar.TAG)
                   .noPlaceholder()
                   .noFade()
                   .error(errorResource)
                   .transform(new CircleTransform(AtlasAvatar.TAG + ".single"))
                   .into(mAvatarCustom);
        }

        private void setAvatarView(View view) {
            boolean isCustom = view.equals(mAvatarCustom);
            mAvatarCluster.setVisibility(isCustom ? View.GONE : View.VISIBLE);
            mAvatarCustom.setVisibility(isCustom ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onClick(View v) {
            if (mClickListener == null) return;
            mClickListener.onClick(this);
        }

        @Override
        public boolean onLongClick(View v) {
            if (mClickListener == null) return false;
            return mClickListener.onLongClick(this);
        }

        interface OnClickListener {
            void onClick(ViewHolder viewHolder);

            boolean onLongClick(ViewHolder viewHolder);
        }
    }

    /**
     * Listens for item clicks on an IntegrationConversationsAdapter.
     */
    public interface OnConversationClickListener {
        /**
         * Alerts the listener to item clicks.
         *
         * @param adapter      The IntegrationConversationsAdapter which had an item clicked.
         * @param conversation The item clicked.
         */
        void onConversationClick(AtlasConversationsAdapter adapter, Conversation conversation);

        /**
         * Alerts the listener to long item clicks.
         *
         * @param adapter      The IntegrationConversationsAdapter which had an item long-clicked.
         * @param conversation The item long-clicked.
         * @return true if the long-click was handled, false otherwise.
         */
        boolean onConversationLongClick(AtlasConversationsAdapter adapter, Conversation conversation);
    }

    public static class Options {
        private Predicate[] predicates;

        public Options(Predicate... predicates) {
            this.predicates = predicates;
        }

        public Predicate[] getPredicates() {
            return predicates;
        }

        public String getConversationTitle(LayerClient layerClient,
                                           ParticipantProvider participantProvider,
                                           Conversation conversation) {
            return Util.getConversationTitle(layerClient, participantProvider, conversation);
        }

        /**
         * Provide an icon to be used in place of the default user avatars
         * @return an icon Uri to load instead of user avatars, null for the default behavior
         */
        public Uri getConversationIconUri(Conversation conversation) {
            return null;
        }

        /**
         * Provide a drawable resource to be shown when the custom conversation icon failed to load
         * @return
         */
        @DrawableRes
        public int getConversationIconErrorResource() {
            return R.drawable.atlas_avatar_error;
        }
    }
}