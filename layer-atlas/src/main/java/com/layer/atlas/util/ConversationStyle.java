package com.layer.atlas.util;

import android.graphics.Typeface;

public final class ConversationStyle {

    private int mTitleTextColor;
    private int mTitleTextStyle;
    private Typeface mTitleTextTypeface;
    private int mTitleUnreadTextColor;
    private int mTitleUnreadTextStyle;
    private Typeface mTitleUnreadTextTypeface;
    private int mSubtitleTextColor;
    private int mSubtitleTextStyle;
    private Typeface mSubtitleTextTypeface;
    private int mSubtitleUnreadTextColor;
    private int mSubtitleUnreadTextStyle;
    private Typeface mSubtitleUnreadTextTypeface;
    private int mCellBackgroundColor;
    private int mCellUnreadBackgroundColor;
    private Typeface mDateTextTypeface;
    private int mDateTextColor;
    private AvatarStyle mAvatarStyle;

    private ConversationStyle(Builder builder) {
        mTitleTextColor = builder.titleTextColor;
        mTitleTextStyle = builder.titleTextStyle;
        setTitleTextTypeface(builder.titleTextTypeface);
        mTitleUnreadTextColor = builder.titleUnreadTextColor;
        mTitleUnreadTextStyle = builder.titleUnreadTextStyle;
        setTitleUnreadTextTypeface(builder.titleUnreadTextTypeface);
        mSubtitleTextColor = builder.subtitleTextColor;
        mSubtitleTextStyle = builder.subtitleTextStyle;
        setSubtitleTextTypeface(builder.subtitleTextTypeface);
        mSubtitleUnreadTextColor = builder.subtitleUnreadTextColor;
        mSubtitleUnreadTextStyle = builder.subtitleUnreadTextStyle;
        setSubtitleUnreadTextTypeface(builder.subtitleUnreadTextTypeface);
        mCellBackgroundColor = builder.cellBackgroundColor;
        mCellUnreadBackgroundColor = builder.cellUnreadBackgroundColor;
        setDateTextTypeface(builder.dateTextTypeface);
        mDateTextColor = builder.dateTextColor;
        mAvatarStyle = builder.avatarStyle;
    }

    public void setTitleTextTypeface(Typeface titleTextTypeface) {
        this.mTitleTextTypeface = titleTextTypeface;
    }

    public void setTitleUnreadTextTypeface(Typeface titleUnreadTextTypeface) {
        this.mTitleUnreadTextTypeface = titleUnreadTextTypeface;
    }

    public void setSubtitleTextTypeface(Typeface subtitleTextTypeface) {
        this.mSubtitleTextTypeface = subtitleTextTypeface;
    }

    public void setSubtitleUnreadTextTypeface(Typeface subtitleUnreadTextTypeface) {
        this.mSubtitleUnreadTextTypeface = subtitleUnreadTextTypeface;
    }

    public void setDateTextTypeface(Typeface dateTextTypeface) {
        this.mDateTextTypeface = dateTextTypeface;
    }

    public int getTitleTextColor() {
        return mTitleTextColor;
    }

    public int getTitleTextStyle() {
        return mTitleTextStyle;
    }

    public Typeface getTitleTextTypeface() {
        return mTitleTextTypeface;
    }

    public int getTitleUnreadTextColor() {
        return mTitleUnreadTextColor;
    }

    public int getTitleUnreadTextStyle() {
        return mTitleUnreadTextStyle;
    }

    public Typeface getTitleUnreadTextTypeface() {
        return mTitleUnreadTextTypeface;
    }

    public int getSubtitleTextColor() {
        return mSubtitleTextColor;
    }

    public int getSubtitleTextStyle() {
        return mSubtitleTextStyle;
    }

    public Typeface getSubtitleTextTypeface() {
        return mSubtitleTextTypeface;
    }

    public int getSubtitleUnreadTextColor() {
        return mSubtitleUnreadTextColor;
    }

    public int getSubtitleUnreadTextStyle() {
        return mSubtitleUnreadTextStyle;
    }

    public Typeface getSubtitleUnreadTextTypeface() {
        return mSubtitleUnreadTextTypeface;
    }

    public int getCellBackgroundColor() {
        return mCellBackgroundColor;
    }

    public int getCellUnreadBackgroundColor() {
        return mCellUnreadBackgroundColor;
    }

    public Typeface getDateTextTypeface() {
        return mDateTextTypeface;
    }

    public int getDateTextColor() {
        return mDateTextColor;
    }

    public AvatarStyle getAvatarStyle() {
        return mAvatarStyle;
    }

    public static final class Builder {
        private int titleTextColor;
        private int titleTextStyle;
        private Typeface titleTextTypeface;
        private int titleUnreadTextColor;
        private int titleUnreadTextStyle;
        private Typeface titleUnreadTextTypeface;
        private int subtitleTextColor;
        private int subtitleTextStyle;
        private Typeface subtitleTextTypeface;
        private int subtitleUnreadTextColor;
        private int subtitleUnreadTextStyle;
        private Typeface subtitleUnreadTextTypeface;
        private int cellBackgroundColor;
        private int cellUnreadBackgroundColor;
        private Typeface dateTextTypeface;
        private int dateTextColor;
        private AvatarStyle avatarStyle;

        public Builder() {
        }

        public Builder titleTextColor(int val) {
            titleTextColor = val;
            return this;
        }

        public Builder titleTextStyle(int val) {
            titleTextStyle = val;
            return this;
        }

        public Builder titleTextTypeface(Typeface val) {
            titleTextTypeface = val;
            return this;
        }

        public Builder titleUnreadTextColor(int val) {
            titleUnreadTextColor = val;
            return this;
        }

        public Builder titleUnreadTextStyle(int val) {
            titleUnreadTextStyle = val;
            return this;
        }

        public Builder titleUnreadTextTypeface(Typeface val) {
            titleUnreadTextTypeface = val;
            return this;
        }

        public Builder subtitleTextColor(int val) {
            subtitleTextColor = val;
            return this;
        }

        public Builder subtitleTextStyle(int val) {
            subtitleTextStyle = val;
            return this;
        }

        public Builder subtitleTextTypeface(Typeface val) {
            subtitleTextTypeface = val;
            return this;
        }

        public Builder subtitleUnreadTextColor(int val) {
            subtitleUnreadTextColor = val;
            return this;
        }

        public Builder subtitleUnreadTextStyle(int val) {
            subtitleUnreadTextStyle = val;
            return this;
        }

        public Builder subtitleUnreadTextTypeface(Typeface val) {
            subtitleUnreadTextTypeface = val;
            return this;
        }

        public Builder cellBackgroundColor(int val) {
            cellBackgroundColor = val;
            return this;
        }

        public Builder cellUnreadBackgroundColor(int val) {
            cellUnreadBackgroundColor = val;
            return this;
        }

        public Builder dateTextTypeface(Typeface val) {
            dateTextTypeface = val;
            return this;
        }

        public Builder dateTextColor(int val) {
            dateTextColor = val;
            return this;
        }

        public Builder avatarStyle(AvatarStyle val) {
            avatarStyle = val;
            return this;
        }

        public ConversationStyle build() {
            return new ConversationStyle(this);
        }
    }
}
