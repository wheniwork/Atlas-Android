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

import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.layer.atlas.Atlas.Tools;
import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChange;
import com.layer.sdk.changes.LayerChange.Type;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.LayerObject;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

/**
 * @author Oleg Orlov
 * @since 13 May 2015
 */
public class AtlasMessagesList extends FrameLayout implements LayerChangeEventListener.MainThread {
    private static final String TAG = AtlasMessagesList.class.getSimpleName();
    private static final boolean debug = false;
    
    private static final boolean CLUSTERED_BUBBLES = false;
    
    private final DateFormat timeFormat;
    
    private ListView messagesList;
    private BaseAdapter messagesAdapter;

    private ArrayList<Cell> viewItems = new ArrayList<Cell>();
    
    private LayerClient client;
    private Conversation conv;
    
    private ItemClickListener clickListener;
    
    //styles
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

    public void init(LayerClient layerClient, final Atlas.ParticipantProvider participantProvider) {
        if (layerClient == null) throw new IllegalArgumentException("LayerClient cannot be null");
        if (participantProvider == null) throw new IllegalArgumentException("ParticipantProvider cannot be null");
        
        
        this.client = layerClient;
        LayoutInflater.from(getContext()).inflate(R.layout.atlas_messages_list, this);
        
        // --- message view
        messagesList = (ListView) findViewById(R.id.atlas_messages_list);
        messagesList.setAdapter(messagesAdapter = new BaseAdapter() {
            
            public View getView(int position, View convertView, ViewGroup parent) {
                final Cell cell = viewItems.get(position);
                MessagePart part = cell.messagePart;
                String userId = part.getMessage().getSender().getUserId();

                boolean myMessage = client.getAuthenticatedUserId().equals(userId);
                
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
                    long yesterMidnight = todayMidnight - (24 * 60 * 60 * 1000); // 24h less
                    Date sentAt = cell.messagePart.getMessage().getSentAt();
                    if (sentAt == null) sentAt = new Date();
                    
                    String timeBarTimeText = timeFormat.format(sentAt.getTime());
                    String timeBarDayText = null;
                    if (sentAt.getTime() > todayMidnight) {
                        timeBarDayText = "Today"; 
                    } else if (sentAt.getTime() > yesterMidnight) {
                        timeBarDayText = "Yesterday";
                    } else {
                        timeBarDayText = Tools.sdfDayOfWeek.format(sentAt);
                    }
                    timeBarDay.setText(timeBarDayText);
                    timeBarTime.setText(timeBarTimeText);
                    timeBar.setVisibility(View.VISIBLE);
                } else {
                    timeBar.setVisibility(View.GONE);
                }
                
                TextView textAvatar = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_initials);
                View spacerRight = convertView.findViewById(R.id.atlas_view_messages_convert_spacer_right);
                if (myMessage) {
                    spacerRight.setVisibility(View.GONE);
                    textAvatar.setVisibility(View.INVISIBLE);
                } else {
                    spacerRight.setVisibility(View.VISIBLE);
                    Atlas.Participant participant = participantProvider.getParticipant(userId);
                    String displayText = participant != null ? Atlas.getInitials(participant) : "";
                    textAvatar.setText(displayText);
                    textAvatar.setVisibility(View.VISIBLE);
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
                
                boolean inContainer = false;
                // cleanUp container
                cellRootView.setVisibility(View.VISIBLE);
                for (int iChild = 0; iChild < cellContainer.getChildCount(); iChild++) {
                    View child = cellContainer.getChildAt(iChild);
                    if (child != cellRootView) {
                        child.setVisibility(View.GONE);
                    } else {
                        inContainer = true;
                    }
                }
                if (!inContainer) {
                    cellContainer.addView(cellRootView);
                }
            }
            
            public long getItemId(int position) {
                return position;
            }
            public Object getItem(int position) {
                return viewItems.get(position);
            }
            public int getCount() {
                return viewItems.size();
            }
            
        });
        
        messagesList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cell item = viewItems.get(position);
                if (clickListener != null) {
                    clickListener.onItemClick(item);
                }
            }
        });
        // --- end of messageView
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

    protected void buildCellForMessage(Message msg, ArrayList<Cell> destination) {
        
        final ArrayList<MessagePart> parts = new ArrayList<MessagePart>(msg.getMessageParts());
        
        for (int partNo = 0; partNo < parts.size(); partNo++ ) {
            final MessagePart part = parts.get(partNo);
            final String mimeType = part.getMimeType();
            
            if (Atlas.MIME_TYPE_IMAGE_PNG.equals(mimeType) || Atlas.MIME_TYPE_IMAGE_JPEG.equals(mimeType)) {
                    
                // 3 parts image support
                if ((partNo + 2 < parts.size()) && Atlas.MIME_TYPE_IMAGE_DIMENSIONS.equals(parts.get(partNo + 2).getMimeType())) {
                    String jsonDimensions = new String(parts.get(partNo + 2).getData());
                    try {
                        JSONObject jo = new JSONObject(jsonDimensions);
                        int width = jo.getInt("width");
                        int height = jo.getInt("height");
                        Cell imageCell = new ImageCell(part, parts.get(partNo + 1), width, height);
                        destination.add(imageCell);
                        if (debug) Log.w(TAG, "cellForMessage() 3-image part found at partNo: " + partNo);
                        partNo++; // skip preview
                        partNo++; // skip dimensions part
                    } catch (JSONException e) {
                        Log.e(TAG, "cellForMessage() cannot parse 3-part image", e);
                    }
                } else {
                    // regular image
                    destination.add(new ImageCell(part));
                    if (debug) Log.w(TAG, "cellForMessage() single-image part found at partNo: " + partNo);
                }
            
            } else if (Atlas.MIME_TYPE_ATLAS_LOCATION.equals(part.getMimeType())){
                destination.add(new GeoCell(part));
            } else {
                Cell cellData = new TextCell(part);
                if (debug) Log.w(TAG, "cellForMessage() default item: " + cellData);
                destination.add(cellData);
            }
        }
        
    }
    
    public synchronized void updateValues() {
        if (conv == null) return;
        
        long started = System.currentTimeMillis();
        
        List<Message> messages = client.getMessages(conv);
        viewItems.clear();
        if (messages.isEmpty()) return;
        
        ArrayList<Cell> messageItems = new ArrayList<AtlasMessagesList.Cell>();
        for (Message message : messages) {
            // System messages have `null` user ID
            if (message.getSender().getUserId() == null) continue;  

            List<MessagePart> parts = message.getMessageParts();
            messageItems.clear();
            buildCellForMessage(message, messageItems);
            viewItems.addAll(messageItems);
        }
        
        // calculate heads/tails
        int currentItem = 0;
        int clusterId = currentItem;
        String currentUser = null;
        long lastMessageTime = 0;
        Calendar calLastMessage = Calendar.getInstance();
        Calendar calCurrent = Calendar.getInstance();
        long clusterTimeSpan = 60 * 1000; // 1 minute
        long oneHourSpan = 60 * 60 * 1000; // 1 hour
        for (int i = 0; i < viewItems.size(); i++) {
            Cell item = viewItems.get(i);
            boolean newCluster = false;
            if (!item.messagePart.getMessage().getSender().getUserId().equals(currentUser)) {
                newCluster = true;
            }
            Date sentAt = item.messagePart.getMessage().getSentAt();
            if (sentAt == null) sentAt = new Date();
            
            if (sentAt.getTime() - lastMessageTime > clusterTimeSpan) {
                newCluster = true;
            }
            
            if (newCluster) {
                clusterId = currentItem;
                if (i > 0) viewItems.get(i - 1).clusterTail = true;
            }
            
            // check time header is needed
            if (sentAt.getTime() - lastMessageTime > oneHourSpan) {
                item.timeHeader = true;
            }
            calCurrent.setTime(sentAt);
            if (calCurrent.get(Calendar.DAY_OF_YEAR) != calLastMessage.get(Calendar.DAY_OF_YEAR)) {
                item.timeHeader = true;
            }
            
            item.clusterHeadItemId = clusterId;
            item.clusterItemId = currentItem++;
            
            currentUser = item.messagePart.getMessage().getSender().getUserId();
            lastMessageTime = sentAt.getTime();
            calLastMessage.setTime(sentAt);
            if (debug) Log.d(TAG, "updateValues() item: " + item);
        }
            viewItems.get(viewItems.size() - 1).clusterTail = true; // last one is always a tail

        if (debug) Log.d(TAG, "updateValues() parts finished in: " + (System.currentTimeMillis() - started));
        messagesAdapter.notifyDataSetChanged();

    }
    
    @Override
    public void onEventMainThread(LayerChangeEvent event) {
        if (conv == null) return;
        boolean updateValues = false;
        boolean jumpToBottom = false;
        for (LayerChange change : event.getChanges()) {
            if (change.getObjectType() == LayerObject.Type.MESSAGE) {
                Message msg = (Message) change.getObject();
                if (msg.getConversation().getId().equals(conv.getId())) {
                    updateValues = true;
                    if (change.getChangeType() == Type.DELETE || change.getChangeType() == Type.INSERT) {
                        jumpToBottom = true;
                    }
                }
            }
        }
        if (updateValues) updateValues();
        if (jumpToBottom) messagesList.smoothScrollToPosition(messagesAdapter.getCount() - 1);
    }

    public void jumpToLastMessage() {
        messagesList.smoothScrollToPosition(viewItems.size() - 1);
    }

    public Conversation getConversation() {
        return conv;
    }

    public void setConversation(Conversation conv) {
        this.conv = conv;
        updateValues();
        jumpToLastMessage();
    }
    
    public void setItemClickListener(ItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    private class GeoCell extends TextCell {

        public GeoCell(MessagePart messagePart) {
            super(messagePart);
            String jsonLonLat = new String(messagePart.getData());
            try {
                JSONObject json = new JSONObject(jsonLonLat);
                double lon = json.getDouble("lon");
                double lat = json.getDouble("lat");
                final String text = "Location:\nlon: " + lon + "\nlat: " + lat;
                this.text = text;
            } catch (JSONException e) {
                throw new IllegalArgumentException("Wrong geoJSON format: " + jsonLonLat, e);
            }
        }

        @Override
        public View onBind(ViewGroup cellContainer) {
            return super.onBind(cellContainer);
        }
        
    }
    
    private class TextCell extends Cell {

        protected String text;
        public TextCell(MessagePart messagePart) {
            super(messagePart);
        }
        
        public TextCell(MessagePart messagePart, String text) {
            super(messagePart);
            this.text = text;
        }

        public View onBind(ViewGroup cellContainer) {
            MessagePart part = messagePart;
            Cell cell = this;
            
            View cellText = cellContainer.findViewById(R.id.atlas_view_messages_cell_text);
            if (cellText == null) {
                cellText = LayoutInflater.from(cellContainer.getContext()).inflate(R.layout.atlas_view_messages_cell_text, cellContainer, false);
            }
            
            cellText.setVisibility(View.VISIBLE);
            for (int iChild = 0; iChild < cellContainer.getChildCount(); iChild++) {
                View child = cellContainer.getChildAt(iChild);
                if (child != cellText) {
                    child.setVisibility(View.GONE);
                }
            }
            
            if (text == null) {
                if (Atlas.MIME_TYPE_TEXT.equals(part.getMimeType())) {
                    text = new String(part.getData());
                } else {
                    text = "attach, type: " + part.getMimeType() + ", size: " + part.getSize();
                }
            }
            
            boolean myMessage = client.getAuthenticatedUserId().equals(cell.messagePart.getMessage().getSender().getUserId());
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
                ((GradientDrawable)textMy.getBackground()).setColor(myBubbleColor);
                textMy.setTextColor(myTextColor);
                //textMy.setTextSize(TypedValue.COMPLEX_UNIT_DIP, myTextSize);
                textMy.setTypeface(myTextTypeface, myTextStyle);
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
                ((GradientDrawable)textOther.getBackground()).setColor(otherBubbleColor);
                textOther.setTextColor(otherTextColor);
                //textOther.setTextSize(TypedValue.COMPLEX_UNIT_DIP, otherTextSize);
                textOther.setTypeface(otherTextTypeface, otherTextStyle);
            }
            return cellText;
            
        }
    }
    
    private class ImageCell extends Cell {
        MessagePart previewPart;
        MessagePart fullPart;
        int width;
        int height;

        private ImageCell(MessagePart fullImagePart) {
            super(fullImagePart);
            this.fullPart = fullImagePart;
        }
        private ImageCell(MessagePart fullImagePart, MessagePart previewImagePart, int width, int height) {
            super(fullImagePart);
            this.fullPart = fullImagePart;
            this.previewPart = previewImagePart;
            this.width = width;
            this.height = height;
        }
        @Override
        public View onBind(final ViewGroup cellContainer) {
            View rootView = cellContainer.findViewById(R.id.atlas_view_messages_cell_custom);
            ImageView imageView = (ImageView) cellContainer.findViewById(R.id.atlas_view_messages_cell_custom_image);
             
            if (rootView == null) {
                rootView = LayoutInflater.from(cellContainer.getContext()).inflate(R.layout.atlas_view_messages_cell_image, cellContainer, false); 
                imageView = (ImageView) rootView.findViewById(R.id.atlas_view_messages_cell_custom_image);
            }

            // get BitmapDrawable
            // BitmapDrawable EMPTY_DRAWABLE = new BitmapDrawable(Bitmap.createBitmap(new int[] { Color.TRANSPARENT }, 1, 1, Bitmap.Config.ALPHA_8));
            int requiredWidth = cellContainer.getWidth() > 0 ? cellContainer.getWidth() : messagesList.getWidth();
            int requiredHeight = cellContainer.getHeight() > 0 ? cellContainer.getHeight() : messagesList.getHeight();
            MessagePart workingPart = /*previewPart != null ? previewPart :*/ fullPart;
            Bitmap bmp = imageCache.get(workingPart.getId().toString());
            if (bmp != null /*&& bmp.getWidth() >= requiredWidth / 2*/) {
                imageView.setImageBitmap(bmp);
                if (debug) Log.i(TAG, "getBitmap() returned from cache! " + bmp.getWidth() + "x" + bmp.getHeight() + " " + bmp.getByteCount() + " bytes" + " req: " + requiredWidth + "x" + requiredHeight + " for " + messagePart.getId());
            } else {
                //adjust width/height
                int stubWidth = requiredWidth;
                int stubHeight = requiredHeight;
                if (stubWidth > width) {
                    stubWidth = width;
                    stubHeight = height;
                }
                if (stubHeight > messagesList.getHeight()) {
                    stubWidth = (int)(1.0 * stubWidth * stubHeight / messagesList.getHeight());
                    stubHeight = messagesList.getHeight();
                }
                
                final RoundRectShape roundRectShape = new RoundRectShape(Atlas.Tools.getRoundRectRadii(new float[]{16, 16, 16, 16}, cellContainer.getResources().getDisplayMetrics()), null, null);
                ShapeDrawable shapeDrawable = new ShapeDrawable(roundRectShape);
                shapeDrawable.getPaint().setColor(cellContainer.getResources().getColor(R.color.atlas_background_gray));
                //shapeDrawable.setBounds(0, 0, stubWidth, stubHeight);
                shapeDrawable.setBounds(0, 0, 300, 300);
                //imageView.getLayoutParams().width = stubWidth;
                //imageView.getLayoutParams().height = stubHeight;
                imageView.setImageDrawable(shapeDrawable); //imageView.setImageResource(R.drawable.image_stub);
                imageView.requestLayout();
                
                requestBitmap(workingPart, requiredWidth, requiredHeight, new BitmapLoadListener() {
                    public void onBitmapLoaded(MessagePart part) {
                        cellContainer.post(new Runnable() {
                            public void run() {
                                messagesList.invalidateViews();
                            }
                        });
                    }
                });
            }
            
            ShapedFrameLayout cellCustom = (ShapedFrameLayout) rootView;
            Cell cell = this;
            // clustering
            cellCustom.setCornerRadiusDp(16, 16, 16, 16);
            if (!CLUSTERED_BUBBLES) return rootView;
            boolean myMessage = client.getAuthenticatedUserId().equals(cell.messagePart.getMessage().getSender().getUserId());
            if (myMessage) {
                if (cell.clusterHeadItemId == cell.clusterItemId && !cell.clusterTail) {
                    cellCustom.setCornerRadiusDp(16, 16, 2, 16);
                    //cellCustom.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_bottom_right);
                } else if (cell.clusterTail && cell.clusterHeadItemId != cell.clusterItemId) {
                    cellCustom.setCornerRadiusDp(16, 2, 16, 16);
                    //cellCustom.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_top_right);
                } else if (cell.clusterHeadItemId != cell.clusterItemId && !cell.clusterTail) {
                    cellCustom.setCornerRadiusDp(16, 2, 2, 16);
                    //cellCustom.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_right);
                }
            } else {
                if (cell.clusterHeadItemId == cell.clusterItemId && !cell.clusterTail) {
                    cellCustom.setCornerRadiusDp(16, 16, 16, 2);
                } else if (cell.clusterTail && cell.clusterHeadItemId != cell.clusterItemId) {
                    cellCustom.setCornerRadiusDp(2, 16, 16, 16);
                } else if (cell.clusterHeadItemId != cell.clusterItemId && !cell.clusterTail) {
                    cellCustom.setCornerRadiusDp(2, 16, 16, 2);
                }
            }
            return rootView;
        }

    }
    
    private static final int BITMAP_DECODE_RETRIES = 3;
    private static final Object cacheLock = new Object();
    final static Map<String, Bitmap> imageCache = new LinkedHashMap<String, Bitmap>(10, 1f, true) {
        private static final long serialVersionUID = 1L;
        protected boolean removeEldestEntry(Entry<String, Bitmap> eldest) {
            if (this.size() > 10) {
                synchronized (cacheLock) {
                    for (int i = 0; i < queue.size(); i++) {
                        final ImageSpec imageSpec = queue.get(i);
                        if (eldest.getKey().equals(imageSpec.part.getId().toString())) {
                            queue.remove(i);
                            break;
                        }
                    }
                }
                return true;
            }
            if (debug) Log.d(TAG, "cache.removeEldest() nothing, size: " + imageCache.size() + ", queue: " + queue.size());
            return false;
        }
    };
    
    final static ArrayList<ImageSpec> queue = new ArrayList<ImageSpec>();
    static volatile boolean shutdownLoader = false;
    
    /**
     * schedule messagePart at first position
     * @param loadListener TODO
     */
    private static void requestBitmap(MessagePart messagePart, int requiredWidth, int requiredHeight, BitmapLoadListener loadListener) {
        synchronized (cacheLock) {
            ImageSpec spec = null;
            for (int i = 0; i < queue.size(); i++) {
                if (queue.get(i).part.getId().equals(messagePart.getId())) {
                    spec = queue.remove(i);
                }
            }
            if (spec == null) {
                spec = new ImageSpec();
                spec.part = messagePart;
                spec.requiredHeight = requiredHeight;
                spec.requiredWidth = requiredWidth;
                spec.listener = loadListener;
            }
            queue.add(0, spec);
            messagePart.download(imageLoader);
            cacheLock.notifyAll();
        }
        if (debug) Log.w(TAG, "requestBitmap() cache: " + imageCache.size() + ", queue: " + queue.size());
    }
    
    private static Bitmap decodeBitmap(MessagePart messagePart, int requiredWidth, int requiredHeight) {
        // load
        long started = System.currentTimeMillis();
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        final InputStream dataStream = messagePart.getDataStream();
        BitmapFactory.decodeStream(dataStream, null, opts);
        int originalWidth = opts.outWidth;
        int originalHeight = opts.outHeight;
        int sampleSize = 1;
        while (opts.outWidth / (sampleSize * 2) > requiredWidth) {
            sampleSize *= 2;
        }
        
        BitmapFactory.Options opts2 = new BitmapFactory.Options();
        opts2.inSampleSize = sampleSize;
        Bitmap bmp = BitmapFactory.decodeStream(messagePart.getDataStream(), null, opts2);
        if (bmp != null) {
            if (debug) Log.d(TAG, "decodeImage() decoded " + bmp.getWidth() + "x" + bmp.getHeight() 
                    + " " + bmp.getByteCount() + " bytes" 
                    + " req: " + requiredWidth + "x" + requiredHeight 
                    + " original: " + originalWidth + "x" + originalHeight 
                    + " sampleSize: " + sampleSize
                    + " in " +(System.currentTimeMillis() - started) + "ms from: " + messagePart.getId());
        } else {
            if (debug) Log.d(TAG, "decodeImage() not decoded " + " req: " + requiredWidth + "x" + requiredHeight 
                    + " in " +(System.currentTimeMillis() - started) + "ms from: " + messagePart.getId());
        }
        return bmp;
    }
    
    private static ImageLoader imageLoader = new ImageLoader();
    static {
        if (debug) Log.w(TAG, "loaderThread.start() ");
        imageLoader.setName("AtlasImageLoader");
        imageLoader.start();
    }
    
    private static class ImageLoader extends Thread implements LayerProgressListener {
        public void run() {
            while (!shutdownLoader) {
                
                ImageSpec spec = null ;
                // search bitmap ready to inflate
                // wait for queue
                synchronized (cacheLock) {
                    while (spec == null && !shutdownLoader) {
                        try {
                            cacheLock.wait();
                            if (shutdownLoader) return;
                            // picking from queue
                            for (int i = 0; i < queue.size(); i++) {
                                if (queue.get(0).part.isContentReady()) { // ready to inflate
                                    spec = queue.remove(i);
                                    break;
                                }
                            }
                        } catch (InterruptedException e) {}
                    }
                }
                
                Bitmap bmp = decodeBitmap(spec.part, spec.requiredWidth, spec.requiredHeight);
                
                synchronized (cacheLock) {
                    if (bmp != null) {
                        imageCache.put(spec.part.getId().toString(), bmp);
                        if (spec.listener != null) spec.listener.onBitmapLoaded(spec.part);
                    } else if (spec.retries < BITMAP_DECODE_RETRIES) {
                        spec.retries++;
                        queue.add(spec);
                        cacheLock.notifyAll();
                    } /*else forget about this image, never put it back in queue */
                }
                
                if (debug) Log.w(TAG, "ImageLoader.run() cache: " + imageCache.size() + ", queue: " + queue.size());
            }
        }
        
        @Override
        public void onProgressStart(MessagePart part, Operation operation) {
        }
        @Override
        public void onProgressUpdate(MessagePart part, Operation operation, long transferredBytes) {
        }
        @Override
        public void onProgressComplete(MessagePart part, Operation operation) {
            synchronized (cacheLock) {
                cacheLock.notifyAll();
            }
        }
        @Override
        public void onProgressError(MessagePart part, Operation operation, Throwable cause) {
            synchronized (cacheLock) {
                queue.remove(part);
                cacheLock.notifyAll();
            }
        }
    }; 
    
    private static class ImageSpec {
        public MessagePart part;
        public int requiredWidth;
        public int requiredHeight;
        public int downloadProgress;
        public int retries = 0;
        public BitmapLoadListener listener;
    }
    
    public static abstract class BitmapLoadListener {
        public abstract void onBitmapLoaded(MessagePart part);
    }
    
    public abstract class Cell {
        public final MessagePart messagePart;
        private int clusterHeadItemId;
        private int clusterItemId;
        private boolean clusterTail;
        private boolean timeHeader;
        
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

        public abstract View onBind(ViewGroup cellContainer);
    }
    
    
    public interface ItemClickListener {
        void onItemClick(Cell item);
    }

}
