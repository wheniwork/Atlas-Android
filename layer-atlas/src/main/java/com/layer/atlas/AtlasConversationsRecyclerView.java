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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;

import com.layer.atlas.adapters.AtlasConversationsAdapter;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.atlas.util.AvatarStyle;
import com.layer.atlas.util.ConversationStyle;
import com.layer.atlas.util.itemanimators.NoChangeAnimator;
import com.layer.atlas.util.views.SwipeableItem;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.query.Predicate;
import com.squareup.picasso.Picasso;

public class AtlasConversationsRecyclerView extends RecyclerView {
    AtlasConversationsAdapter mAdapter;
    private ItemTouchHelper mSwipeItemTouchHelper;

    private ConversationStyle conversationStyle;

    public AtlasConversationsRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseStyle(context, attrs, defStyle);
    }

    public AtlasConversationsRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtlasConversationsRecyclerView(Context context) {
        super(context);
    }

    public AtlasConversationsRecyclerView init(LayerClient layerClient, ParticipantProvider participantProvider, Picasso picasso, Predicate... predicates) {
        // Linear layout manager
        LinearLayoutManager manager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        manager.setStackFromEnd(false);
        setLayoutManager(manager);

        // Don't flash items when changing content
        setItemAnimator(new NoChangeAnimator());

        mAdapter = new AtlasConversationsAdapter(getContext(), layerClient, participantProvider, picasso, predicates);
        mAdapter.setStyle(conversationStyle);
        super.setAdapter(mAdapter);
        refresh();

        return this;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        throw new RuntimeException("AtlasConversationsRecyclerView sets its own Adapter");
    }

    /**
     * Automatically refresh on resume
     */
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != View.VISIBLE) return;
        refresh();
    }

    public AtlasConversationsRecyclerView refresh() {
        if (mAdapter != null) mAdapter.refresh();
        return this;
    }

    /**
     * Convenience pass-through to this list's AtlasConversationsAdapter.
     *
     * @see AtlasConversationsAdapter#setOnConversationClickListener(AtlasConversationsAdapter.OnConversationClickListener)
     */
    public AtlasConversationsRecyclerView setOnConversationClickListener(AtlasConversationsAdapter.OnConversationClickListener listener) {
        mAdapter.setOnConversationClickListener(listener);
        return this;
    }

    public AtlasConversationsRecyclerView setOnConversationSwipeListener(SwipeableItem.OnSwipeListener<Conversation> listener) {
        if (mSwipeItemTouchHelper != null) {
            mSwipeItemTouchHelper.attachToRecyclerView(null);
        }
        if (listener == null) {
            mSwipeItemTouchHelper = null;
        } else {
            listener.setBaseAdapter((AtlasConversationsAdapter) getAdapter());
            mSwipeItemTouchHelper = new ItemTouchHelper(listener);
            mSwipeItemTouchHelper.attachToRecyclerView(this);
        }
        return this;
    }

    /**
     * Convenience pass-through to this list's AtlasConversationsAdapter.
     *
     * @see AtlasConversationsAdapter#setInitialHistoricMessagesToFetch(long)
     */
    public AtlasConversationsRecyclerView setInitialHistoricMessagesToFetch(long count) {
        mAdapter.setInitialHistoricMessagesToFetch(count);
        return this;
    }

    public AtlasConversationsRecyclerView setTypeface(Typeface titleTypeface, Typeface titleUnreadTypeface, Typeface subtitleTypeface, Typeface subtitleUnreadTypeface, Typeface dateTypeface) {
        conversationStyle.setTitleTextTypeface(titleTypeface);
        conversationStyle.setTitleUnreadTextTypeface(titleUnreadTypeface);
        conversationStyle.setSubtitleTextTypeface(subtitleTypeface);
        conversationStyle.setSubtitleUnreadTextTypeface(subtitleUnreadTypeface);
        conversationStyle.setDateTextTypeface(dateTypeface);
        return this;
    }

    private void parseStyle(Context context, AttributeSet attrs, int defStyle) {
        ConversationStyle.Builder styleBuilder = new ConversationStyle.Builder();
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AtlasConversationsRecyclerView, R.attr.AtlasConversationsRecyclerView, defStyle);
        styleBuilder.titleTextColor(ta.getColor(R.styleable.AtlasConversationsRecyclerView_cellTitleTextColor, context.getResources().getColor(R.color.atlas_text_gray)));
        int titleTextStyle = ta.getInt(R.styleable.AtlasConversationsRecyclerView_cellTitleTextStyle, Typeface.NORMAL);
        styleBuilder.titleTextStyle(titleTextStyle);
        String titleTextTypefaceName = ta.getString(R.styleable.AtlasConversationsRecyclerView_cellTitleTextTypeface);
        styleBuilder.titleTextTypeface(titleTextTypefaceName != null ? Typeface.create(titleTextTypefaceName, titleTextStyle) : null);

        styleBuilder.titleUnreadTextColor(ta.getColor(R.styleable.AtlasConversationsRecyclerView_cellTitleUnreadTextColor, context.getResources().getColor(R.color.atlas_text_black)));
        int titleUnreadTextStyle = ta.getInt(R.styleable.AtlasConversationsRecyclerView_cellTitleUnreadTextStyle, Typeface.BOLD);
        styleBuilder.titleUnreadTextStyle(titleUnreadTextStyle);
        String titleUnreadTextTypefaceName = ta.getString(R.styleable.AtlasConversationsRecyclerView_cellTitleUnreadTextTypeface);
        styleBuilder.titleUnreadTextTypeface(titleUnreadTextTypefaceName != null ? Typeface.create(titleUnreadTextTypefaceName, titleUnreadTextStyle) : null);

        styleBuilder.subtitleTextColor(ta.getColor(R.styleable.AtlasConversationsRecyclerView_cellSubtitleTextColor, context.getResources().getColor(R.color.atlas_text_gray)));
        int subtitleTextStyle = ta.getInt(R.styleable.AtlasConversationsRecyclerView_cellSubtitleTextStyle, Typeface.NORMAL);
        styleBuilder.subtitleTextStyle(subtitleTextStyle);
        String subtitleTextTypefaceName = ta.getString(R.styleable.AtlasConversationsRecyclerView_cellSubtitleTextTypeface);
        styleBuilder.subtitleTextTypeface(subtitleTextTypefaceName != null ? Typeface.create(subtitleTextTypefaceName, subtitleTextStyle) : null);

        styleBuilder.subtitleUnreadTextColor(ta.getColor(R.styleable.AtlasConversationsRecyclerView_cellSubtitleUnreadTextColor, context.getResources().getColor(R.color.atlas_text_black)));
        int subtitleUnreadTextStyle = ta.getInt(R.styleable.AtlasConversationsRecyclerView_cellSubtitleUnreadTextStyle, Typeface.NORMAL);
        styleBuilder.subtitleUnreadTextStyle(subtitleUnreadTextStyle);
        String subtitleUnreadTextTypefaceName = ta.getString(R.styleable.AtlasConversationsRecyclerView_cellSubtitleUnreadTextTypeface);
        styleBuilder.subtitleUnreadTextTypeface(subtitleUnreadTextTypefaceName != null ? Typeface.create(subtitleUnreadTextTypefaceName, subtitleUnreadTextStyle) : null);

        styleBuilder.cellBackgroundColor(ta.getColor(R.styleable.AtlasConversationsRecyclerView_cellBackgroundColor, Color.TRANSPARENT));
        styleBuilder.cellUnreadBackgroundColor(ta.getColor(R.styleable.AtlasConversationsRecyclerView_cellUnreadBackgroundColor, Color.TRANSPARENT));
        styleBuilder.dateTextColor(ta.getColor(R.styleable.AtlasConversationsRecyclerView_dateTextColor, context.getResources().getColor(R.color.atlas_color_primary_blue)));

        AvatarStyle.Builder avatarStyleBuilder = new AvatarStyle.Builder();
        avatarStyleBuilder.avatarTextColor(ta.getColor(R.styleable.AtlasConversationsRecyclerView_avatarTextColor, context.getResources().getColor(R.color.atlas_avatar_text)));
        avatarStyleBuilder.avatarBackgroundColor(ta.getColor(R.styleable.AtlasConversationsRecyclerView_avatarBackgroundColor, context.getResources().getColor(R.color.atlas_avatar_background)));
        avatarStyleBuilder.avatarBorderColor(ta.getColor(R.styleable.AtlasConversationsRecyclerView_avatarBorderColor, context.getResources().getColor(R.color.atlas_avatar_border)));
        styleBuilder.avatarStyle(avatarStyleBuilder.build());
        ta.recycle();
        conversationStyle = styleBuilder.build();
    }
}
