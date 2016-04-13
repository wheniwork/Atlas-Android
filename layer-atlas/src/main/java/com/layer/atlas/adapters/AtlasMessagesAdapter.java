package com.layer.atlas.adapters;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.layer.atlas.R;
import com.layer.atlas.adapters.viewholders.CellViewHolder;
import com.layer.atlas.adapters.viewholders.ViewHolder;
import com.layer.atlas.messagetypes.AtlasCellFactory;
import com.layer.atlas.messagetypes.MessageStyle;
import com.layer.atlas.provider.Participant;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.atlas.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Actor;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.ListViewController;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.RecyclerViewController;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * AtlasMessagesAdapter drives an AtlasMessagesList.  The AtlasMessagesAdapter itself handles
 * rendering sender names, avatars, dates, left/right alignment, and message clustering, and leaves
 * rendering message content up to registered CellFactories.  Each CellFactory knows which Messages
 * it can render, can create new View hierarchies for its Message types, and can render (bind)
 * Message data with its created View hierarchies.  Typically, CellFactories are segregated by
 * MessagePart MIME types (e.g. "text/plain", "image/jpeg", and "application/vnd.geo+json").
 *
 * Under the hood, the AtlasMessagesAdapter is a RecyclerView.Adapter, which automatically recycles
 * its list items within view-type "buckets".  Each registered CellFactory actually creates two such
 * view-types: one for cells sent by the authenticated user, and another for cells sent by remote
 * actors.  This allows the AtlasMessagesAdapter to efficiently render images sent by the current
 * user aligned on the left, and images sent by others aligned on the right, for example.  In case
 * this sent-by distinction is of value when rendering cells, it provided as the `isMe` argument.
 *
 * When rendering Messages, the AtlasMessagesAdapter first determines which CellFactory to handle
 * the Message with calling CellFactory.isBindable() on each of its registered CellFactories. The
 * first CellFactory to return `true` is used for that Message.  Then, the adapter checks for
 * available CellHolders of that type.  If none are found, a new one is created with a call to
 * CellFactory.createCellHolder().  After creating a new CellHolder (or reusing an available one),
 * the CellHolder is rendered in the UI with Message data via CellFactory.bindCellHolder().
 *
 * @see AtlasCellFactory
 */
public class AtlasMessagesAdapter extends RecyclerView.Adapter<ViewHolder> implements AtlasBaseAdapter<Message>, RecyclerViewController.Callback {
    private final static int VIEW_TYPE_FOOTER = 0;

    protected final LayerClient mLayerClient;
    protected final ParticipantProvider mParticipantProvider;
    protected final Picasso mPicasso;
    private final RecyclerViewController<Message> mQueryController;
    protected final LayoutInflater mLayoutInflater;
    protected final Handler mUiThreadHandler;
    protected OnMessageAppendListener mAppendListener;
    protected final DisplayMetrics mDisplayMetrics;

    // Cells
    protected int mViewTypeCount = VIEW_TYPE_FOOTER;
    protected final Set<AtlasCellFactory> mCellFactories = new LinkedHashSet<AtlasCellFactory>();
    protected final Map<Integer, CellType> mCellTypesByViewType = new HashMap<Integer, CellType>();
    protected final Map<AtlasCellFactory, Integer> mMyViewTypesByCell = new HashMap<AtlasCellFactory, Integer>();
    protected final Map<AtlasCellFactory, Integer> mTheirViewTypesByCell = new HashMap<AtlasCellFactory, Integer>();

    // Dates and Clustering
    private final Map<Uri, Cluster> mClusterCache = new HashMap<Uri, Cluster>();

    // Read and delivery receipts
    private Map<Message.RecipientStatus, MessagePosition> mReceiptMap = new HashMap<Message.RecipientStatus, MessagePosition>();

    private View mFooterView;
    private int mFooterPosition = 0;

    //Stye
    private MessageStyle mMessageStyle;

    private RecyclerView mRecyclerView;

    private Options mOptions;

    public AtlasMessagesAdapter(Context context, LayerClient layerClient, ParticipantProvider participantProvider, Picasso picasso) {
        this(context, layerClient, participantProvider, picasso, new Options(context));
    }

    public AtlasMessagesAdapter(Context context, LayerClient layerClient, ParticipantProvider participantProvider, Picasso picasso,
                                Options options) {
        mLayerClient = layerClient;
        mParticipantProvider = participantProvider;
        mPicasso = picasso;
        mLayoutInflater = LayoutInflater.from(context);
        mUiThreadHandler = new Handler(Looper.getMainLooper());
        mOptions = options;
        mDisplayMetrics = context.getResources().getDisplayMetrics();

        mQueryController = layerClient.newRecyclerViewController(null, null, this);
        mQueryController.setPreProcessCallback(new ListViewController.PreProcessCallback<Message>() {
            @Override
            public void onCache(ListViewController listViewController, Message message) {
                for (AtlasCellFactory factory : mCellFactories) {
                    if (factory.isBindable(message)) {
                        factory.getParsedContent(mLayerClient, mParticipantProvider, message);
                        break;
                    }
                }
            }
        });

        setHasStableIds(false);
    }

    /**
     * Sets this AtlasMessagesAdapter's Message Query.
     *
     * @param query Query drive this AtlasMessagesAdapter.
     * @return This AtlasMessagesAdapter.
     */
    public AtlasMessagesAdapter setQuery(Query<Message> query) {
        mQueryController.setQuery(query);
        return this;
    }

    /**
     * Refreshes this adapter by re-running the underlying Query.
     */
    public void refresh() {
        mQueryController.execute();
    }

    public AtlasMessagesAdapter setRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        return this;
    }

    public void setStyle(MessageStyle messageStyle) {
        this.mMessageStyle = messageStyle;
    }

    public void setFooterView(View footerView) {
        boolean isNull = footerView == null;
        boolean wasNull = mFooterView == null;
        mFooterView = footerView;

        if (wasNull && !isNull) {
            // Insert
            notifyItemInserted(mFooterPosition);
        } else if (!wasNull && isNull) {
            // Delete
            notifyItemRemoved(mFooterPosition);
        } else if (!wasNull && !isNull) {
            // Change
            notifyItemChanged(mFooterPosition);
        }
    }

    public View getFooterView() {
        return mFooterView;
    }


    //==============================================================================================
    // Listeners
    //==============================================================================================

    /**
     * Sets the OnAppendListener for this AtlasQueryAdapter.  The listener will be called when items
     * are appended to the end of this adapter.  This is useful for implementing a scroll-to-bottom
     * feature.
     *
     * @param listener The OnAppendListener to notify about appended items.
     * @return This AtlasQueryAdapter.
     */
    public AtlasMessagesAdapter setOnMessageAppendListener(OnMessageAppendListener listener) {
        mAppendListener = listener;
        return this;
    }


    //==============================================================================================
    // Adapter and Cells
    //==============================================================================================

    /**
     * Registers one or more CellFactories for the AtlasMessagesAdapter to manage.  CellFactories
     * know which Messages they can render, and handle View caching, creation, and binding.
     *
     * @param cellFactories Cells to register.
     * @return This AtlasMessagesAdapter.
     */
    public AtlasMessagesAdapter addCellFactories(AtlasCellFactory... cellFactories) {
        for (AtlasCellFactory CellFactory : cellFactories) {
            CellFactory.setStyle(mMessageStyle);
            mCellFactories.add(CellFactory);

            mViewTypeCount++;
            CellType me = new CellType(true, CellFactory);
            mCellTypesByViewType.put(mViewTypeCount, me);
            mMyViewTypesByCell.put(CellFactory, mViewTypeCount);

            mViewTypeCount++;
            CellType notMe = new CellType(false, CellFactory);
            mCellTypesByViewType.put(mViewTypeCount, notMe);
            mTheirViewTypesByCell.put(CellFactory, mViewTypeCount);
        }
        return this;
    }

    public Set<AtlasCellFactory> getCellFactories() {
        return mCellFactories;
    }

    @Override
    public int getItemViewType(int position) {
        if (mFooterView != null && position == mFooterPosition) return VIEW_TYPE_FOOTER;
        Message message = getItem(position);
        boolean isMe = mLayerClient.getAuthenticatedUserId().equals(message.getSender().getUserId());
        for (AtlasCellFactory factory : mCellFactories) {
            if (!factory.isBindable(message)) continue;
            return isMe ? mMyViewTypesByCell.get(factory) : mTheirViewTypesByCell.get(factory);
        }
        return -1;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FOOTER) {
            return new ViewHolder(mLayoutInflater.inflate(ViewHolder.RESOURCE_ID_FOOTER, parent, false));
        }

        CellType cellType = mCellTypesByViewType.get(viewType);
        int rootResId = cellType.mMe ? mOptions.getLayoutResourceMe() : mOptions.getLayoutResourceThem();
        CellViewHolder rootViewHolder = mOptions.getViewHolder(mLayoutInflater.inflate(rootResId, parent, false), mParticipantProvider, mPicasso);
        rootViewHolder.setCellHolder(cellType.mCellFactory.createCellHolder(rootViewHolder.getCell(), cellType.mMe, mLayoutInflater));
        rootViewHolder.setCellHolderSpecs(new AtlasCellFactory.CellHolderSpecs());
        return rootViewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        mQueryController.updateBoundPosition(position);
        if (mFooterView != null && position == mFooterPosition) {
            // Footer
            bindFooter(viewHolder);
        } else {
            // Cell
            bindCellViewHolder((CellViewHolder) viewHolder, position);
        }
    }

    public void bindFooter(ViewHolder viewHolder) {
        viewHolder.getRoot().removeAllViews();
        if (mFooterView.getParent() != null) {
            ((ViewGroup) mFooterView.getParent()).removeView(mFooterView);
        }
        viewHolder.getRoot().addView(mFooterView);
    }

    public void bindCellViewHolder(CellViewHolder viewHolder, int position) {
        Message message = getItem(position);
        viewHolder.setMessage(message);
        CellType cellType = mCellTypesByViewType.get(viewHolder.getItemViewType());
        boolean oneOnOne = message.getConversation().getParticipants().size() == 2;

        // Clustering and dates
        Cluster cluster = getClustering(message, position);
        if (cluster.mClusterWithPrevious == null) {
            // No previous message, so no gap
            viewHolder.getClusterSpaceGap().setVisibility(View.GONE);
            viewHolder.getTimeGroup().setVisibility(View.GONE);
        } else if (cluster.mDateBoundaryWithPrevious || cluster.mClusterWithPrevious == mOptions.getGapClusterType()) {
            // Crossed into a new day, or > specified lull (default 1hr) in conversation
            Date sentAt = message.getSentAt();
            if (sentAt == null) sentAt = new Date();
            String timeBarDayText = Util.formatTimeDay(viewHolder.getCell().getContext(), sentAt);
            viewHolder.getTimeGroupDay().setText(timeBarDayText);
            String timeBarTimeText = mOptions.getInlineTimeFormat().format(sentAt.getTime());
            viewHolder.getTimeGroupTime().setText(" " + timeBarTimeText);
            viewHolder.getTimeGroup().setVisibility(View.VISIBLE);
            viewHolder.getClusterSpaceGap().setVisibility(View.GONE);
        } else if (cluster.mClusterWithPrevious == ClusterType.LESS_THAN_MINUTE) {
            // Same sender with < 1m gap
            viewHolder.getClusterSpaceGap().setVisibility(View.GONE);
            viewHolder.getTimeGroup().setVisibility(View.GONE);
        } else if (cluster.mClusterWithPrevious == ClusterType.NEW_SENDER || cluster.mClusterWithPrevious == ClusterType.MORE_THAN_MINUTE) {
            // New sender or > 1m gap
            viewHolder.getClusterSpaceGap().setVisibility(View.VISIBLE);
            viewHolder.getTimeGroup().setVisibility(View.GONE);
        }

        // Read and delivery receipts
        MessagePosition delivered = mReceiptMap.get(Message.RecipientStatus.DELIVERED);

        if (delivered != null && message == delivered.mMessage) {
            viewHolder.setDeliveredReceiptText(R.string.atlas_message_item_delivered);
        } else {
            viewHolder.setReadReceiptText(R.string.atlas_message_item_read);
        }

        // Sender-dependent elements
        if (cellType.mMe) {
            // Unsent and sent
            if (!message.isSent()) {
                viewHolder.getCell().setAlpha(0.5f);
            } else {
                viewHolder.getCell().setAlpha(1.0f);
            }
            setSentAtTime(viewHolder, message);
        } else {
            message.markAsRead();
            // Sender name, only for first message in cluster
            if (!oneOnOne && isFirstMessage(cluster)) {
                Actor sender = message.getSender();
                if (sender.getName() != null) {
                    viewHolder.getUserName().setText(sender.getName());
                } else {
                    Participant participant = mParticipantProvider.getParticipant(sender.getUserId());
                    viewHolder.getUserName().setText(participant != null ? participant.getName() : viewHolder.itemView.getResources().getString(R.string.atlas_message_item_unknown_user));
                }
                viewHolder.getUserName().setVisibility(View.VISIBLE);
                setSentAtTime(viewHolder, message);
            } else {
                viewHolder.getUserName().setVisibility(View.GONE);
                viewHolder.getSentAt().setVisibility(View.GONE);
            }

            // Avatars
            if (oneOnOne) {
                // Not in one-on-one conversations
                viewHolder.getAvatar().setVisibility(View.GONE);
            } else if (cluster.mClusterWithNext == null || cluster.mClusterWithNext != ClusterType.LESS_THAN_MINUTE) {
                // Last message in cluster
                viewHolder.getAvatar().setVisibility(View.VISIBLE);
                viewHolder.getAvatar().setParticipants(message.getSender().getUserId());
            } else {
                // Invisible for clustered messages to preserve proper spacing
                viewHolder.getAvatar().setVisibility(View.INVISIBLE);
            }
        }

        // CellHolder
        AtlasCellFactory.CellHolder cellHolder = viewHolder.getCellHolder();
        cellHolder.setMessage(message);

        // Cell dimensions
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) viewHolder.getCell().getLayoutParams();
        int maxWidth = mRecyclerView.getWidth() - viewHolder.getRoot().getPaddingLeft() - viewHolder.getRoot().getPaddingRight() - params.leftMargin - params.rightMargin;
        if (!oneOnOne && !cellType.mMe) {
            // Subtract off avatar width if needed
            ViewGroup.MarginLayoutParams avatarParams = (ViewGroup.MarginLayoutParams) viewHolder.getAvatar().getLayoutParams();
            maxWidth -= avatarParams.width + avatarParams.rightMargin + avatarParams.leftMargin;
        }
        // TODO: subtract spacing rather than multiply by 0.8 to handle screen sizes more cleanly
        int maxHeight = (int) Math.round(0.8 * mRecyclerView.getHeight());

        viewHolder.getCellHolderSpecs().isMe = cellType.mMe;
        viewHolder.getCellHolderSpecs().position = position;
        viewHolder.getCellHolderSpecs().maxWidth = maxWidth;
        viewHolder.getCellHolderSpecs().maxHeight = maxHeight;
        cellType.mCellFactory.bindCellHolder(cellHolder, cellType.mCellFactory.getParsedContent(mLayerClient, mParticipantProvider, message), message, viewHolder.getCellHolderSpecs());
    }

    private boolean isFirstMessage(com.layer.atlas.adapters.AtlasMessagesAdapter.Cluster cluster) {
        return cluster.mClusterWithPrevious == null || cluster.mClusterWithPrevious == ClusterType.NEW_SENDER;
    }

    private void setSentAtTime(CellViewHolder viewHolder, Message message) {
        if (viewHolder.getSentAt() == null) {
            return;
        }

        viewHolder.getSentAt().setVisibility(mOptions.showMessageTimes() ? View.VISIBLE : View.GONE);
        viewHolder.getSentAt().setText(mOptions.getMessageTimeFormat().format(message.getSentAt()));
    }

    @Override
    public int getItemCount() {
        return mQueryController.getItemCount() + ((mFooterView == null) ? 0 : 1);
    }

    @Override
    public Integer getPosition(Message message) {
        return mQueryController.getPosition(message);
    }

    @Override
    public Integer getPosition(Message message, int lastPosition) {
        return mQueryController.getPosition(message, lastPosition);
    }

    @Override
    public Message getItem(int position) {
        if (mFooterView != null && position == mFooterPosition) return null;
        return mQueryController.getItem(position);
    }

    @Override
    public Message getItem(RecyclerView.ViewHolder viewHolder) {
        if (!(viewHolder instanceof CellViewHolder)) return null;
        return ((CellViewHolder) viewHolder).getMessage();
    }


    //==============================================================================================
    // Clustering
    //==============================================================================================

    // TODO: optimize by limiting search to positions in- and around- visible range
    private Cluster getClustering(Message message, int position) {
        Cluster result = mClusterCache.get(message.getId());
        if (result == null) {
            result = new Cluster();
            mClusterCache.put(message.getId(), result);
        }

        int previousPosition = position - 1;
        Message previousMessage = (previousPosition >= 0) ? getItem(previousPosition) : null;
        if (previousMessage != null) {
            result.mDateBoundaryWithPrevious = isDateBoundary(previousMessage.getSentAt(), message.getSentAt());
            result.mClusterWithPrevious = ClusterType.fromMessages(previousMessage, message);

            Cluster previousCluster = mClusterCache.get(previousMessage.getId());
            if (previousCluster == null) {
                previousCluster = new Cluster();
                mClusterCache.put(previousMessage.getId(), previousCluster);
            } else {
                // does the previous need to change its clustering?
                if ((previousCluster.mClusterWithNext != result.mClusterWithPrevious) ||
                        (previousCluster.mDateBoundaryWithNext != result.mDateBoundaryWithPrevious)) {
                    requestUpdate(previousMessage, previousPosition);
                }
            }
            previousCluster.mClusterWithNext = result.mClusterWithPrevious;
            previousCluster.mDateBoundaryWithNext = result.mDateBoundaryWithPrevious;
        }

        int nextPosition = position + 1;
        Message nextMessage = (nextPosition < getItemCount()) ? getItem(nextPosition) : null;
        if (nextMessage != null) {
            result.mDateBoundaryWithNext = isDateBoundary(message.getSentAt(), nextMessage.getSentAt());
            result.mClusterWithNext = ClusterType.fromMessages(message, nextMessage);

            Cluster nextCluster = mClusterCache.get(nextMessage.getId());
            if (nextCluster == null) {
                nextCluster = new Cluster();
                mClusterCache.put(nextMessage.getId(), nextCluster);
            } else {
                // does the next need to change its clustering?
                if ((nextCluster.mClusterWithPrevious != result.mClusterWithNext) ||
                        (nextCluster.mDateBoundaryWithPrevious != result.mDateBoundaryWithNext)) {
                    requestUpdate(nextMessage, nextPosition);
                }
            }
            nextCluster.mClusterWithPrevious = result.mClusterWithNext;
            nextCluster.mDateBoundaryWithPrevious = result.mDateBoundaryWithNext;
        }

        return result;
    }

    private static boolean isDateBoundary(Date d1, Date d2) {
        if (d1 == null || d2 == null) return false;
        return (d1.getYear() != d2.getYear()) || (d1.getMonth() != d2.getMonth()) || (d1.getDay() != d2.getDay());
    }

    private void requestUpdate(final Message message, final int lastPosition) {
        mUiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(getPosition(message, lastPosition));
            }
        });
    }


    //==============================================================================================
    // Read and delivery receipts
    //==============================================================================================

    // TODO: find clever way to prevent worst-case O(n) -- perhaps based on visible positions?
    private void updateReceipts() {
        final String userId = mLayerClient.getAuthenticatedUserId();
        Map<Message.RecipientStatus, MessagePosition> receiptMap = new HashMap<Message.RecipientStatus, MessagePosition>();
        for (int position = getItemCount(); position >= 0; position--) {
            Message message = getItem(position);
            if (message == null) continue;

            // Only display receipts for our own messages
            if (!userId.equals(message.getSender().getUserId())) continue;

            for (Map.Entry<String, Message.RecipientStatus> entry : message.getRecipientStatus().entrySet()) {
                // Only show receipts for other members
                if (entry.getKey().equals(userId)) continue;

                // Only the latest entry for this RecipientStatus matters
                if (receiptMap.containsKey(entry.getValue())) continue;

                // Found the latest entry for this RecipientStatus
                receiptMap.put(entry.getValue(), new MessagePosition(message, position));
            }
            if (receiptMap.containsKey(Message.RecipientStatus.READ) && receiptMap.containsKey(Message.RecipientStatus.DELIVERED)) {
                break;
            }
        }

        // Refresh previously-marked messages
        Set<MessagePosition> previousReceiptsToRefresh = new HashSet<MessagePosition>();
        for (Message.RecipientStatus status : Message.RecipientStatus.values()) {
            MessagePosition current = (mReceiptMap == null) ? null : mReceiptMap.get(status);
            MessagePosition next = receiptMap.get(status);
            if (current != null && next != null && current.mMessage != next.mMessage) {
                previousReceiptsToRefresh.add(current);
                previousReceiptsToRefresh.add(next);
            }
            if (current != null && next == null) {
                previousReceiptsToRefresh.add(current);
            }
            if (current == null && next != null) {
                previousReceiptsToRefresh.add(next);
            }
        }
        for (MessagePosition message : previousReceiptsToRefresh) {
            notifyItemChanged(getPosition(message.mMessage, message.mPosition));
        }

        mReceiptMap = receiptMap;
    }


    //==============================================================================================
    // UI update callbacks
    //==============================================================================================

    @Override
    public void onQueryDataSetChanged(RecyclerViewController controller) {
        mFooterPosition = mQueryController.getItemCount();
        updateReceipts();
        notifyDataSetChanged();
    }

    @Override
    public void onQueryItemChanged(RecyclerViewController controller, int position) {
        updateReceipts();
        notifyItemChanged(position);
    }

    @Override
    public void onQueryItemRangeChanged(RecyclerViewController controller, int positionStart, int itemCount) {
        updateReceipts();
        notifyItemRangeChanged(positionStart, itemCount);
    }

    @Override
    public void onQueryItemInserted(RecyclerViewController controller, int position) {
        mFooterPosition++;
        updateReceipts();
        notifyItemInserted(position);
        if (mAppendListener != null && (position + 1) == getItemCount()) {
            mAppendListener.onMessageAppend(this, getItem(position));
        }
    }

    @Override
    public void onQueryItemRangeInserted(RecyclerViewController controller, int positionStart, int itemCount) {
        mFooterPosition += itemCount;
        updateReceipts();
        notifyItemRangeInserted(positionStart, itemCount);
        int positionEnd = positionStart + itemCount;
        if (mAppendListener != null && (positionEnd + 1) == getItemCount()) {
            mAppendListener.onMessageAppend(this, getItem(positionEnd));
        }
    }

    @Override
    public void onQueryItemRemoved(RecyclerViewController controller, int position) {
        mFooterPosition--;
        updateReceipts();
        notifyItemRemoved(position);
    }

    @Override
    public void onQueryItemRangeRemoved(RecyclerViewController controller, int positionStart, int itemCount) {
        mFooterPosition -= itemCount;
        updateReceipts();
        notifyItemRangeRemoved(positionStart, itemCount);
    }

    @Override
    public void onQueryItemMoved(RecyclerViewController controller, int fromPosition, int toPosition) {
        updateReceipts();
        notifyItemMoved(fromPosition, toPosition);
    }


    //==============================================================================================
    // Inner classes
    //==============================================================================================

    public enum ClusterType {
        NEW_SENDER,
        LESS_THAN_MINUTE,
        MORE_THAN_MINUTE,
        MORE_THAN_HALF_HOUR,
        MORE_THAN_HOUR;

        private static final long MILLIS_MINUTE = 60 * 1000;
        private static final long MILLIS_HALF_HOUR = 30 * MILLIS_MINUTE;
        private static final long MILLIS_HOUR = 60 * MILLIS_MINUTE;

        public static ClusterType fromMessages(Message older, Message newer) {
            // Different users?
            if (!older.getSender().equals(newer.getSender())) return NEW_SENDER;

            // Time clustering for same user?
            Date oldSentAt = older.getSentAt();
            Date newSentAt = newer.getSentAt();
            if (oldSentAt == null || newSentAt == null) return LESS_THAN_MINUTE;
            long delta = Math.abs(newSentAt.getTime() - oldSentAt.getTime());
            if (delta <= MILLIS_MINUTE) return LESS_THAN_MINUTE;
            if (delta > MILLIS_MINUTE && delta < MILLIS_HALF_HOUR) return MORE_THAN_MINUTE;
            if (delta >= MILLIS_HALF_HOUR && delta < MILLIS_HOUR) return MORE_THAN_HALF_HOUR;
            return MORE_THAN_HOUR;
        }
    }

    private static class Cluster {
        public boolean mDateBoundaryWithPrevious;
        public ClusterType mClusterWithPrevious;

        public boolean mDateBoundaryWithNext;
        public ClusterType mClusterWithNext;
    }

    private static class MessagePosition {
        public Message mMessage;
        public int mPosition;

        public MessagePosition(Message message, int position) {
            mMessage = message;
            mPosition = position;
        }
    }

    private static class CellType {
        protected final boolean mMe;
        protected final AtlasCellFactory mCellFactory;

        public CellType(boolean me, AtlasCellFactory CellFactory) {
            mMe = me;
            mCellFactory = CellFactory;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CellType cellType = (CellType) o;

            if (mMe != cellType.mMe) return false;
            return mCellFactory.equals(cellType.mCellFactory);

        }

        @Override
        public int hashCode() {
            int result = (mMe ? 1 : 0);
            result = 31 * result + mCellFactory.hashCode();
            return result;
        }
    }

    /**
     * Listens for inserts to the end of an AtlasQueryAdapter.
     */
    public interface OnMessageAppendListener {
        /**
         * Alerts the listener to inserts at the end of an AtlasQueryAdapter.  If a batch of items
         * were appended, only the last one will be alerted here.
         *
         * @param adapter The AtlasQueryAdapter which had an item appended.
         * @param message The item appended to the AtlasQueryAdapter.
         */
        void onMessageAppend(AtlasMessagesAdapter adapter, Message message);
    }

    /**
     * Options for overriding the default formatting for inline dates/times, message sent times, and
     * chat message layouts
     */
    public static class Options {

        private DateFormat inlineTimeFormat;
        private ClusterType gapClusterType;
        private DateFormat messageTimeFormat;
        private boolean showMessageTimes;
        private int layoutResourceMe;
        private int layoutResourceThem;

        public Options(Context context) {
            this(android.text.format.DateFormat.getTimeFormat(context), ClusterType.MORE_THAN_HOUR,
                 android.text.format.DateFormat.getTimeFormat(context), false,
                 R.layout.atlas_message_item_me, R.layout.atlas_message_item_them);
        }

      /**
       * Customize options for inline dates/times and message sent times
       * @param inlineTimeFormat DateFormat for inline times shown in message list
       * @param gapClusterType ClusterType to use when showing the inline time between gaps in the conversation
       * @param messageTimeFormat DateFormat for message sent times
       * @param showMessageTimes Whether to show message sent times
       * @param layoutResourceMe Layout resource id for messages sent by the current user
       * @param layoutResourceThem Layout resource id for messages sent by other users
       */
        public Options(DateFormat inlineTimeFormat, ClusterType gapClusterType,
                       DateFormat messageTimeFormat, boolean showMessageTimes,
                       @LayoutRes int layoutResourceMe, @LayoutRes int layoutResourceThem) {
            this.inlineTimeFormat = inlineTimeFormat;
            this.gapClusterType = gapClusterType;
            this.messageTimeFormat = messageTimeFormat;
            this.showMessageTimes = showMessageTimes;
            this.layoutResourceMe = layoutResourceMe;
            this.layoutResourceThem = layoutResourceThem;
        }

        public DateFormat getInlineTimeFormat() {
            return inlineTimeFormat;
        }

        public DateFormat getMessageTimeFormat() {
            return messageTimeFormat;
        }

        public boolean showMessageTimes() {
            return showMessageTimes;
        }

        @LayoutRes
        public int getLayoutResourceMe() {
            return layoutResourceMe;
        }

        @LayoutRes
        public int getLayoutResourceThem() {
            return layoutResourceThem;
        }

        public CellViewHolder getViewHolder(View itemView, ParticipantProvider participantProvider, Picasso picasso) {
            return new CellViewHolder(itemView, participantProvider, picasso);
        }

        public ClusterType getGapClusterType() {
            return gapClusterType;
        }
    }
}