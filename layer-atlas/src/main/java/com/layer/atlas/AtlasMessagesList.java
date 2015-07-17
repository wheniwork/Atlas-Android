/*
 * Copyright (c) 2015 Layer. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.layer.atlas;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.layer.atlas.Atlas.Tools;
import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChange;
import com.layer.sdk.changes.LayerChange.Type;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.LayerObject;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.Message.RecipientStatus;
import com.layer.sdk.messaging.MessagePart;
import com.layer.sdk.query.Query;

/**
 * @author Oleg Orlov
 * @since 13 May 2015
 */
public class AtlasMessagesList extends FrameLayout implements LayerChangeEventListener.MainThread {
    private static final String TAG = AtlasMessagesList.class.getSimpleName();
    private static final boolean debug = true;
    
    public static final boolean CLUSTERED_BUBBLES = false;
    
    private static final int MESSAGE_TYPE_UPDATE_VALUES = 0;
    private static final int MESSAGE_REFRESH_UPDATE_ALL = 0;
    private static final int MESSAGE_REFRESH_UPDATE_DELIVERY = 1;

    private final DateFormat timeFormat;
    
    private ListView messagesList;
    private BaseAdapter messagesAdapter;

    /** 
     * Message Data main container. Holds messagesData for AtlasMessagesList. 
     * Particular MessageData could accessed by message.id   
     */
    private HashMap<Uri, MessageData> messagesData = new HashMap<Uri, MessageData>();
    
    /** 
     * Ordered list of messages from Query/Conversation (used to calculate delivery status) 
     */
    private ArrayList<MessageData> messagesOrder = new ArrayList<AtlasMessagesList.MessageData>();
    
    /** 
     *  Collects ids of messages that was changed to rebuild cells at next {@link #updateValues()} <p>
     * (see {@link #onEventMainThread(LayerChangeEvent)} 
     * */
    private HashSet<Uri> messagesToUpdate = new HashSet<Uri>();
    
    /**
     * Cells to render messages (generated in {@link #updateValues()}
     */
    private ArrayList<Cell> cells = new ArrayList<Cell>();
    
    /** Where cells comes from */
    private CellFactory cellFactory;
    
    private LayerClient client;
    private Conversation conv;
    private Query<Message> query;
    /** if query is set instead of conversation - participants needs to be precalculated somewhere else */
    private final Set<String> participants = new HashSet<String>(); 
    
    private Message latestReadMessage = null;
    private Message latestDeliveredMessage = null;
    
    private ItemClickListener clickListener;
    
    //styles
    private static final float CELL_CONTAINER_ALPHA_UNSENT  = 0.5f;
    private static final float CELL_CONTAINER_ALPHA_SENT    = 1.0f;
    private int myBubbleColor;
    private int myTextColor;
    private int myTextStyle;
    private float myTextSize;
    private Typeface myTextTypeface;
    
    private int otherBubbleColor;
    private int otherTextColor;
    private int otherTextStyle;
    private float otherTextSize;
    private Typeface otherTextTypeface;

    private int dateTextColor;
    private int avatarTextColor;
    private int avatarBackgroundColor;
    
    public AtlasMessagesList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseStyle(context, attrs, defStyle);
        this.timeFormat = android.text.format.DateFormat.getTimeFormat(context);
    }

    public AtlasMessagesList(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtlasMessagesList(Context context) {
        super(context);
        this.timeFormat = android.text.format.DateFormat.getTimeFormat(context);
    }

    /** Setup with {@link Atlas.DefaultCellFactory}  */
    public void init(final LayerClient layerClient, final Atlas.ParticipantProvider participantProvider) {
        init(layerClient, participantProvider, new Atlas.DefaultCellFactory(this));
    }
    
    /** @param cellFactory - use or extend {@link DefaultCellFactory} to get basic cells support */
    public void init(final LayerClient layerClient, final Atlas.ParticipantProvider participantProvider, CellFactory cellFactory) {
        if (layerClient == null) throw new IllegalArgumentException("LayerClient cannot be null");
        if (participantProvider == null) throw new IllegalArgumentException("ParticipantProvider cannot be null");
        if (messagesList != null) throw new IllegalStateException("AtlasMessagesList is already initialized!");
        
        this.client = layerClient;
        this.cellFactory = cellFactory;
        LayoutInflater.from(getContext()).inflate(R.layout.atlas_messages_list, this);
        
        // --- message view
        messagesList = (ListView) findViewById(R.id.atlas_messages_list);
        messagesList.setAdapter(messagesAdapter = new BaseAdapter() {
            
            public View getView(int position, View convertView, ViewGroup parent) {
                final Cell cell = cells.get(position);
                MessagePart part = cell.messagePart;
                String userId = part.getMessage().getSender().getUserId();

                boolean myMessage = client.getAuthenticatedUserId().equals(userId);
                boolean showTheirDecor = participants.size() > 2;
                
                if (convertView == null) { 
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_view_messages_convert, parent, false);
                }
                
                View spacerTop = convertView.findViewById(R.id.atlas_view_messages_convert_spacer_top);
                spacerTop.setVisibility(cell.clusterItemId == cell.clusterHeadItemId && !cell.timeHeader ? View.VISIBLE : View.GONE); 
                
                View spacerBottom = convertView.findViewById(R.id.atlas_view_messages_convert_spacer_bottom);
                spacerBottom.setVisibility(cell.clusterTail ? View.VISIBLE : View.GONE); 
                
                // format date
                View timeBar = convertView.findViewById(R.id.atlas_view_messages_convert_timebar);
                TextView timeBarDay = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_timebar_day);
                TextView timeBarTime = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_timebar_time);
                if (cell.timeHeader) {

                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    long todayMidnight = cal.getTimeInMillis();
                    long yesterMidnight = todayMidnight - Tools.TIME_HOURS_24;
                    long weekAgoMidnight = todayMidnight - Tools.TIME_HOURS_24 * 7;
                    Date sentAt = cell.messagePart.getMessage().getSentAt();
                    if (sentAt == null) sentAt = new Date();
                    
                    String timeBarTimeText = timeFormat.format(sentAt.getTime());
                    String timeBarDayText = null;
                    if (sentAt.getTime() > todayMidnight) {
                        timeBarDayText = "Today"; 
                    } else if (sentAt.getTime() > yesterMidnight) {
                        timeBarDayText = "Yesterday";
                    } else if (sentAt.getTime() > weekAgoMidnight) {
                        cal.setTime(sentAt);
                        timeBarDayText = Tools.TIME_WEEKDAYS_NAMES[cal.get(Calendar.DAY_OF_WEEK) - 1];
                    } else {
                        timeBarDayText = Tools.sdfDayOfWeek.format(sentAt);
                    }
                    timeBarDay.setText(timeBarDayText);
                    timeBarTime.setText(timeBarTimeText);
                    timeBar.setVisibility(View.VISIBLE);
                } else {
                    timeBar.setVisibility(View.GONE);
                }
                
                View avatarContainer = convertView.findViewById(R.id.atlas_view_messages_convert_avatar_container);
                TextView textAvatar = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_initials);
                View spacerRight = convertView.findViewById(R.id.atlas_view_messages_convert_spacer_right);
                TextView userNameHeader = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_user_name);
                if (myMessage) {
                    spacerRight.setVisibility(View.GONE);
                    textAvatar.setVisibility(View.INVISIBLE);
                    userNameHeader.setVisibility(View.GONE);
                } else {
                    spacerRight.setVisibility(View.VISIBLE);
                    Atlas.Participant participant = participantProvider.getParticipant(userId);
                    if (cell.firstUserMsg && showTheirDecor) {
                        userNameHeader.setVisibility(View.VISIBLE);
                        String fullName = participant == null ? "Unknown User" : participant.getFirstName() + " " + participant.getLastName();
                        userNameHeader.setText(fullName);
                    } else {
                        userNameHeader.setVisibility(View.GONE);
                    }
                    
                    if (cell.lastUserMsg && participant != null) {
                        textAvatar.setVisibility(View.VISIBLE);
                        textAvatar.setText(Atlas.getInitials(participant));
                    } else {
                        textAvatar.setVisibility(View.INVISIBLE);
                    }
                    avatarContainer.setVisibility(showTheirDecor ? View.VISIBLE : View.GONE);
                }
                
                // mark unsent messages
                View cellContainer = convertView.findViewById(R.id.atlas_view_messages_cell_container);
                cellContainer.setAlpha((myMessage && !cell.messagePart.getMessage().isSent()) 
                        ? CELL_CONTAINER_ALPHA_UNSENT : CELL_CONTAINER_ALPHA_SENT);
                
                // delivery receipt check
                TextView receiptView = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_delivery_receipt);
                receiptView.setVisibility(View.GONE);
                if (latestDeliveredMessage != null && latestDeliveredMessage.getId().equals(cell.messagePart.getMessage().getId())) {
                    receiptView.setVisibility(View.VISIBLE);
                    receiptView.setText("Delivered");
                }
                if (latestReadMessage != null && latestReadMessage.getId().equals(cell.messagePart.getMessage().getId())) {
                    receiptView.setVisibility(View.VISIBLE);
                    receiptView.setText("Read");
                }
                
                // processing cell
                bindCell(convertView, cell);

                // mark displayed message as read
                Message msg = part.getMessage();
                if (!msg.getSender().getUserId().equals(client.getAuthenticatedUserId())) {
                    msg.markAsRead();
                }
                
                timeBarDay.setTextColor(dateTextColor);
                timeBarTime.setTextColor(dateTextColor);
                textAvatar.setTextColor(avatarTextColor);
                ((GradientDrawable)textAvatar.getBackground()).setColor(avatarBackgroundColor);
                
                return convertView;
            }
            
            private void bindCell(View convertView, final Cell cell) {
                
                ViewGroup cellContainer = (ViewGroup) convertView.findViewById(R.id.atlas_view_messages_cell_container);
                
                View cellRootView = cell.onBind(cellContainer);
                boolean alreadyInContainer = false;
                // cleanUp container
                cellRootView.setVisibility(View.VISIBLE);
                for (int iChild = 0; iChild < cellContainer.getChildCount(); iChild++) {
                    View child = cellContainer.getChildAt(iChild);
                    if (child != cellRootView) {
                        child.setVisibility(View.GONE);
                    } else {
                        alreadyInContainer = true;
                    }
                }
                if (!alreadyInContainer) {
                    cellContainer.addView(cellRootView);
                }
            }
            
            public long getItemId(int position) {
                return position;
            }
            public Object getItem(int position) {
                return cells.get(position);
            }
            public int getCount() {
                return cells.size();
            }
            
        });
        
        messagesList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cell item = cells.get(position);
                if (clickListener != null) {
                    clickListener.onItemClick(item);
                }
            }
        });
        // --- end of messageView
        
        updateValues();
    }
    
    public void parseStyle(Context context, AttributeSet attrs, int defStyle) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AtlasMessageList, R.attr.AtlasMessageList, defStyle);
        this.myTextColor = ta.getColor(R.styleable.AtlasMessageList_myTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.myTextStyle = ta.getInt(R.styleable.AtlasMessageList_myTextStyle, Typeface.NORMAL);
        String myTextTypefaceName = ta.getString(R.styleable.AtlasMessageList_myTextTypeface); 
        this.myTextTypeface  = myTextTypefaceName != null ? Typeface.create(myTextTypefaceName, myTextStyle) : null;
        //this.myTextSize = ta.getDimension(R.styleable.AtlasMessageList_myTextSize, context.getResources().getDimension(R.dimen.atlas_text_size_general));

        this.otherTextColor = ta.getColor(R.styleable.AtlasMessageList_theirTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.otherTextStyle = ta.getInt(R.styleable.AtlasMessageList_theirTextStyle, Typeface.NORMAL);
        String otherTextTypefaceName = ta.getString(R.styleable.AtlasMessageList_theirTextTypeface); 
        this.otherTextTypeface  = otherTextTypefaceName != null ? Typeface.create(otherTextTypefaceName, otherTextStyle) : null;
        //this.otherTextSize = ta.getDimension(R.styleable.AtlasMessageList_theirTextSize, context.getResources().getDimension(R.dimen.atlas_text_size_general));
        
        this.myBubbleColor  = ta.getColor(R.styleable.AtlasMessageList_myBubbleColor, context.getResources().getColor(R.color.atlas_bubble_blue));
        this.otherBubbleColor = ta.getColor(R.styleable.AtlasMessageList_theirBubbleColor, context.getResources().getColor(R.color.atlas_background_gray));

        this.dateTextColor = ta.getColor(R.styleable.AtlasMessageList_dateTextColor, context.getResources().getColor(R.color.atlas_text_gray)); 
        this.avatarTextColor = ta.getColor(R.styleable.AtlasMessageList_avatarTextColor, context.getResources().getColor(R.color.atlas_text_black)); 
        this.avatarBackgroundColor = ta.getColor(R.styleable.AtlasMessageList_avatarBackgroundColor, context.getResources().getColor(R.color.atlas_background_gray));
        ta.recycle();
    }
    
    private void applyStyle() {
        messagesAdapter.notifyDataSetChanged();
    }

    public static abstract class CellFactory {
        public abstract void buildCellForMessage(Message msg, List<Cell> result);
    }
    
    protected void buildCellForMessage(Message msg, List<Cell> result) {
        cellFactory.buildCellForMessage(msg, result);
    }
    
    public void updateValues() {
        long started = System.currentTimeMillis();
        
        // cells are just order keeper. order is going to be changed while new info arrives 
        cells.clear();
        messagesOrder.clear();
        
        List<Uri> msgIds = null;
        if (conv != null) {
            msgIds = client.getMessageIds(conv);
        } else if (query != null) {
            msgIds = client.executeQueryForIds(query);
        }
        
        if (msgIds == null || msgIds.size() == 0) {
            this.messagesData.clear();
            this.participants.clear();
            this.messagesAdapter.notifyDataSetChanged();
            return;
        };
        
        // check last message is added
        boolean jumpToTheEnd = false;
        Uri lastMessageId = msgIds.get(msgIds.size() - 1);
        if ( ! messagesData.containsKey(lastMessageId) ) {
            jumpToTheEnd = true;
        }
        if (debug) Log.w(TAG, "updateValues() jump: " + jumpToTheEnd + ", lastMessage: " + lastMessageId);
        
        // consider each cached messageData to remove
        HashSet<Uri> ids2Delete = new HashSet<Uri>(messagesData.keySet());
        
        ArrayList<Message> messagesUpdated = new ArrayList<Message>();
        // rebuild messageOrder and cells with cached data
        for (Uri msgId : msgIds) {
            MessageData msgData = messagesData.get(msgId);
            // rebuild messageData if new or updated
            if (msgData == null || messagesToUpdate.contains(msgId)) {
                Message msg = client.getMessage(msgId);
                msgData = new MessageData(msg);
                buildCellForMessage(msg, msgData.cells);
                messagesData.put(msgId, msgData);
                messagesUpdated.add(msg);
            }
            
            if (msgData.msg.getSender().getUserId() == null) continue;
            
            messagesOrder.add(msgData);
            cells.addAll(msgData.cells);
            // message appears in query results? Keep them in cache
            ids2Delete.remove(msgId);
        }
        
        // remove all cached messages that absents in current query results
        messagesData.keySet().removeAll(ids2Delete);
        messagesToUpdate.clear();
        if (debug) Log.w(TAG, "updateValues() change applied in: " + (System.currentTimeMillis() - started) + " ms, messageData removed: " + ids2Delete.size());
        
        // rebuild participants list (for conv - pick from conversation, from query - scan for everyone)
        participants.clear();
        if (conv != null) {
            participants.addAll(conv.getParticipants());
        } else { /* query != null */
            for (MessageData msgData : messagesOrder) {
                String userId = msgData.msg.getSender().getUserId();
                if (userId != null) participants.add(userId);
            }
        }

        updateDeliveryStatus(messagesOrder);
        
        // calculate heads/tails
        int currentItem = 0;
        int clusterId = currentItem;
        String currentUser = null;
        long lastMessageTime = 0;
        Calendar calLastMessage = Calendar.getInstance();
        Calendar calCurrent = Calendar.getInstance();
        long clusterTimeSpan = 60 * 1000; // 1 minute
        long oneHourSpan = 60 * 60 * 1000; // 1 hour
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            cell.reset();
            boolean newCluster = false;
            if (!cell.messagePart.getMessage().getSender().getUserId().equals(currentUser)) {
                newCluster = true;
            }
            Date sentAt = cell.messagePart.getMessage().getSentAt();
            if (sentAt == null) sentAt = new Date();
            
            if (sentAt.getTime() - lastMessageTime > clusterTimeSpan) {
                newCluster = true;
            }
            
            if (newCluster) {
                clusterId = currentItem;
                if (i > 0) cells.get(i - 1).clusterTail = true;
            }
            
            // last message from user
            if ( ! cell.messagePart.getMessage().getSender().getUserId().equals(currentUser)) {
                cell.firstUserMsg = true;
                if (i > 0) cells.get(i - 1).lastUserMsg = true;
            }
            
            // check time header is needed
            if (sentAt.getTime() - lastMessageTime > oneHourSpan) {
                cell.timeHeader = true;
            }
            calCurrent.setTime(sentAt);
            if (calCurrent.get(Calendar.DAY_OF_YEAR) != calLastMessage.get(Calendar.DAY_OF_YEAR)) {
                cell.timeHeader = true;
            }
            
            cell.clusterHeadItemId = clusterId;
            cell.clusterItemId = currentItem++;
            
            currentUser = cell.messagePart.getMessage().getSender().getUserId();
            lastMessageTime = sentAt.getTime();
            calLastMessage.setTime(sentAt);
            if (false && debug) Log.d(TAG, "updateValues() item: " + cell);
        }
        
        cells.get(cells.size() - 1).lastUserMsg = true; // last one is always a last message from user
        cells.get(cells.size() - 1).clusterTail = true; // last one is always a tail

        if (debug) Log.d(TAG, "updateValues() parts finished in: " + (System.currentTimeMillis() - started) + " ms");
        messagesAdapter.notifyDataSetChanged();
        
        if (jumpToTheEnd) jumpToLastMessage();
    }

    private boolean updateDeliveryStatus(List<MessageData> messagesData) {
        if (debug) Log.w(TAG, "updateDeliveryStatus() checking messages:   " + messagesData.size());
        Message oldLatestDeliveredMessage = latestDeliveredMessage;
        Message oldLatestReadMessage = latestReadMessage;
        // reset before scan
        latestDeliveredMessage = null;
        latestReadMessage = null;
        
        for (MessageData msgData : messagesData) {
            // only our messages
            Message message = msgData.msg;
            if (client.getAuthenticatedUserId().equals(message.getSender().getUserId())){
                if (!message.isSent()) continue;
                Map<String, RecipientStatus> statuses = message.getRecipientStatus();
                if (statuses == null || statuses.size() == 0) continue;
                for (Map.Entry<String, RecipientStatus> entry : statuses.entrySet()) {
                    // our read-status doesn't matter 
                    if (entry.getKey().equals(client.getAuthenticatedUserId())) continue;
                    
                    if (entry.getValue() == RecipientStatus.READ) {
                        latestDeliveredMessage = message;
                        latestReadMessage = message;
                        break;
                    }
                    if (entry.getValue() == RecipientStatus.DELIVERED) {
                        latestDeliveredMessage = message;
                    }
                }
            }
        }
        boolean changed = false;
        if      (oldLatestDeliveredMessage == null && latestDeliveredMessage != null) changed = true;
        else if (oldLatestDeliveredMessage != null && latestDeliveredMessage == null) changed = true;
        else if (oldLatestDeliveredMessage != null && latestDeliveredMessage != null 
                && !oldLatestDeliveredMessage.getId().equals(latestDeliveredMessage.getId())) changed = true;
        
        if      (oldLatestReadMessage == null && latestReadMessage != null) changed = true;
        else if (oldLatestReadMessage != null && latestReadMessage == null) changed = true;
        else if (oldLatestReadMessage != null && latestReadMessage != null 
                && !oldLatestReadMessage.getId().equals(latestReadMessage.getId())) changed = true;
        
        if (debug) Log.w(TAG, "updateDeliveryStatus() read status changed: " + (changed ? "yes" : "no"));
        if (debug) Log.w(TAG, "updateDeliveryStatus() latestRead:          " + (latestReadMessage != null ? latestReadMessage.getSentAt() + ", id: " + latestReadMessage.getId() : "null"));
        if (debug) Log.w(TAG, "updateDeliveryStatus() latestDelivered:     " + (latestDeliveredMessage != null ? latestDeliveredMessage.getSentAt()+ ", id: " + latestDeliveredMessage.getId() : "null"));
        
        return changed;
    }
    
    private long messageUpdateSentAt = 0;
    
    private final Handler refreshHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            long started = System.currentTimeMillis();
            if (msg.what ==  MESSAGE_TYPE_UPDATE_VALUES) {
                if (msg.arg1 == MESSAGE_REFRESH_UPDATE_ALL) {
                    updateValues();
                } else if (msg.arg1 == MESSAGE_REFRESH_UPDATE_DELIVERY) {
                    boolean changed = updateDeliveryStatus(messagesOrder);
                    if (changed) messagesAdapter.notifyDataSetInvalidated();
                    if (debug) Log.w(TAG, "refreshHandler() delivery status changed: " + changed);
                }
                if (msg.arg2 > 0) {
                    jumpToLastMessage();
                }
            }
            final long currentTimeMillis = System.currentTimeMillis();
            if (debug) Log.w(TAG, "refreshHandler() delay: " + (currentTimeMillis - messageUpdateSentAt) + " ms, handled in: " + (currentTimeMillis - started) + "ms"); 
            messageUpdateSentAt = 0;
        }
        
    };
    
    @Override
    public void onEventMainThread(LayerChangeEvent event) {
        boolean updateValues = false;
        boolean jumpToBottom = false;
        boolean updateDeliveryStatus = false;
        
        for (LayerChange change : event.getChanges()) {
            
            if (change.getObjectType() == LayerObject.Type.MESSAGE) {
                Message msg = (Message) change.getObject();
                messagesToUpdate.add(msg.getId());
            } else if (change.getObjectType() == LayerObject.Type.MESSAGE_PART) {
                MessagePart part = (MessagePart) change.getObject();
                messagesToUpdate.add(part.getMessage().getId());
            }
            
            if (query != null) {
                updateValues = true;
                
            } else if (conv != null) {
                if (change.getObjectType() == LayerObject.Type.MESSAGE) {
                    Message msg = (Message) change.getObject();
                    if ( ! msg.getConversation().getId().equals(conv.getId())) continue;
                    
                    if (change.getChangeType() == Type.UPDATE && "recipientStatus".equals(change.getAttributeName())) {
                        updateDeliveryStatus = true;
                    }
                    
                    if (change.getChangeType() == Type.DELETE || change.getChangeType() == Type.INSERT) {
                        updateValues = true;
                        jumpToBottom = true;
                    }
                }
            }  
        }
        
        if (updateValues || updateDeliveryStatus) {
            requestRefreshValues(updateValues, jumpToBottom);
        }
    }
    
    public void requestRefreshValues(boolean updateValues, boolean jumpToBottom) {
        if (messageUpdateSentAt == 0) messageUpdateSentAt = System.currentTimeMillis();
        refreshHandler.removeMessages(MESSAGE_TYPE_UPDATE_VALUES);
        final android.os.Message message = refreshHandler.obtainMessage();
        message.arg1 = updateValues ? MESSAGE_REFRESH_UPDATE_ALL : MESSAGE_REFRESH_UPDATE_DELIVERY;
        message.arg2 = jumpToBottom ? 1 : 0; 
        message.obj  = client;
        refreshHandler.sendMessage(message);
    }
    
    public void requestRefresh() {
        messagesList.post(INVALIDATE_VIEW);
    }
    
    private final Runnable INVALIDATE_VIEW = new Runnable() {
        public void run() {
            messagesList.invalidateViews();
        }
    }; 
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        
        if (debug) Log.d(TAG, "onDetachedFromWindow() clean cells and views... ");
        cells.clear();
        messagesAdapter.notifyDataSetChanged();
        messagesList.removeAllViewsInLayout();
    }
    
    public void jumpToLastMessage() {
        messagesList.smoothScrollToPosition(cells.size() - 1);
    }

    public Conversation getConversation() {
        return conv;
    }

    public void setConversation(Conversation conv) {
        this.conv = conv;
        this.query = null;
        updateValues();
        jumpToLastMessage();
    }
    
    public void setQuery(Query<Message> query) {
        // check
        if ( ! Message.class.equals(query.getQueryClass())) {
            throw new IllegalArgumentException("Query must return Message object. Actual class: " + query.getQueryClass());
        }
        // 
        this.query = query;
        this.conv = null;
        updateValues();
        jumpToLastMessage();
    }
    
    public LayerClient getLayerClient() {
        if (client == null) throw new IllegalStateException("AtlasMessagesList has not been initialized yet. Please call .init() first");
        return client;
    }
    
    public void setItemClickListener(ItemClickListener clickListener) {
        this.clickListener = clickListener;
    }
    
    /** Cells per message container */
    private static class MessageData {
        final Message msg;
        final List<Cell> cells;
        public MessageData(Message msg) {
            if (msg == null) throw new IllegalArgumentException("Message cannot be null");
            this.msg = msg;
            this.cells = new ArrayList<Cell>();
        }
    }
    
    public static class TextCell extends Cell {

        protected String text;
        AtlasMessagesList messagesList;
        
        public TextCell(MessagePart messagePart, AtlasMessagesList messagesList) {
            super(messagePart);
            this.messagesList = messagesList;
        }
        
        public TextCell(MessagePart messagePart, String text, AtlasMessagesList messagesList) {
            super(messagePart);
            this.text = text;
        }

        public View onBind(ViewGroup cellContainer) {
            MessagePart part = messagePart;
            Cell cell = this;
            
            View cellText = Tools.findChildById(cellContainer, R.id.atlas_view_messages_cell_text);
            if (cellText == null) {
                cellText = LayoutInflater.from(cellContainer.getContext()).inflate(R.layout.atlas_view_messages_cell_text, cellContainer, false);
            }
            
            if (text == null) {
                if (Atlas.MIME_TYPE_TEXT.equals(part.getMimeType())) {
                    text = new String(part.getData());
                } else {
                    text = "attach, type: " + part.getMimeType() + ", size: " + part.getSize();
                }
            }
            
            boolean myMessage = messagesList.client.getAuthenticatedUserId().equals(cell.messagePart.getMessage().getSender().getUserId());
            TextView textMy = (TextView) cellText.findViewById(R.id.atlas_view_messages_convert_text);
            TextView textOther = (TextView) cellText.findViewById(R.id.atlas_view_messages_convert_text_counterparty);
            if (myMessage) {
                textMy.setVisibility(View.VISIBLE);
                textMy.setText(text);
                textOther.setVisibility(View.GONE);
                
                textMy.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue);
                
                if (CLUSTERED_BUBBLES) {
                    if (cell.clusterHeadItemId == cell.clusterItemId && !cell.clusterTail) {
                        textMy.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_bottom_right);
                    } else if (cell.clusterTail && cell.clusterHeadItemId != cell.clusterItemId) {
                        textMy.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_top_right);
                    } else if (cell.clusterHeadItemId != cell.clusterItemId && !cell.clusterTail) {
                        textMy.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_right);
                    }
                }
                ((GradientDrawable)textMy.getBackground()).setColor(messagesList.myBubbleColor);
                textMy.setTextColor(messagesList.myTextColor);
                //textMy.setTextSize(TypedValue.COMPLEX_UNIT_DIP, myTextSize);
                textMy.setTypeface(messagesList.myTextTypeface, messagesList.myTextStyle);
            } else {
                textOther.setVisibility(View.VISIBLE);
                textOther.setText(text);
                textMy.setVisibility(View.GONE);
                
                textOther.setBackgroundResource(R.drawable.atlas_shape_rounded16_gray);
                if (CLUSTERED_BUBBLES) {
                    if (cell.clusterHeadItemId == cell.clusterItemId && !cell.clusterTail) {
                        textOther.setBackgroundResource(R.drawable.atlas_shape_rounded16_gray_no_bottom_left);
                    } else if (cell.clusterTail && cell.clusterHeadItemId != cell.clusterItemId) {
                        textOther.setBackgroundResource(R.drawable.atlas_shape_rounded16_gray_no_top_left);
                    } else if (cell.clusterHeadItemId != cell.clusterItemId && !cell.clusterTail) {
                        textOther.setBackgroundResource(R.drawable.atlas_shape_rounded16_gray_no_left);
                    }
                }
                ((GradientDrawable)textOther.getBackground()).setColor(messagesList.otherBubbleColor);
                textOther.setTextColor(messagesList.otherTextColor);
                //textOther.setTextSize(TypedValue.COMPLEX_UNIT_DIP, otherTextSize);
                textOther.setTypeface(messagesList.otherTextTypeface, messagesList.otherTextStyle);
            }
            return cellText;
        }
    }
    
    public static abstract class Cell {
        public final MessagePart messagePart;
        protected int clusterHeadItemId;
        protected int clusterItemId;
        protected boolean clusterTail;
        private boolean timeHeader;
        
        /** if true, than previous message was from different user*/
        private boolean firstUserMsg;
        /** if true, than next message is from different user */
        private boolean lastUserMsg; 
        
        public Cell(MessagePart messagePart) {
            this.messagePart = messagePart;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[ ")
                .append("messagePart: ").append(messagePart.getMimeType())
                .append(": ").append(messagePart.getSize() < 2048 ? new String(messagePart.getData()) : messagePart.getSize() + " bytes" )
                .append(", clusterId: ").append(clusterHeadItemId)
                .append(", clusterItem: ").append(clusterItemId)
                .append(", clusterTail: ").append(clusterTail)
                .append(", timeHeader: ").append(timeHeader).append(" ]");
            return builder.toString();
        }
        
        private void reset() {
            clusterHeadItemId = 0;
            clusterItemId     = 0;
            clusterTail       = false;
            timeHeader        = false; 
            firstUserMsg      = false;
            lastUserMsg       = false; 
        }

        /** 
         * Start with inflating your own cell.xml
         * <pre>
            View rootView = Tools.findChildById(cellContainer, R.id.atlas_view_messages_cell_image);
            if (rootView == null) {
                rootView = LayoutInflater.from(cellContainer.getContext()).inflate(R.layout.atlas_view_messages_cell_image, cellContainer, false); 
            }
            // ...
            return rootView;
            </pre>
         */
        public abstract View onBind(ViewGroup cellContainer);
    }
    
    
    public interface ItemClickListener {
        void onItemClick(Cell item);
    }

}
