package com.layer.atlas.util;

import android.graphics.Typeface;

public final class ConversationStyle {

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

    private ConversationStyle(Builder builder) {
        titleTextColor = builder.titleTextColor;
        titleTextStyle = builder.titleTextStyle;
        setTitleTextTypeface(builder.titleTextTypeface);
        titleUnreadTextColor = builder.titleUnreadTextColor;
        titleUnreadTextStyle = builder.titleUnreadTextStyle;
        setTitleUnreadTextTypeface(builder.titleUnreadTextTypeface);
        subtitleTextColor = builder.subtitleTextColor;
        subtitleTextStyle = builder.subtitleTextStyle;
        setSubtitleTextTypeface(builder.subtitleTextTypeface);
        subtitleUnreadTextColor = builder.subtitleUnreadTextColor;
        subtitleUnreadTextStyle = builder.subtitleUnreadTextStyle;
        setSubtitleUnreadTextTypeface(builder.subtitleUnreadTextTypeface);
        cellBackgroundColor = builder.cellBackgroundColor;
        cellUnreadBackgroundColor = builder.cellUnreadBackgroundColor;
        setDateTextTypeface(builder.dateTextTypeface);
        dateTextColor = builder.dateTextColor;
        avatarStyle = builder.avatarStyle;
    }

    public void setTitleTextTypeface(Typeface titleTextTypeface) {
        this.titleTextTypeface = titleTextTypeface;
    }

    public void setTitleUnreadTextTypeface(Typeface titleUnreadTextTypeface) {
        this.titleUnreadTextTypeface = titleUnreadTextTypeface;
    }

    public void setSubtitleTextTypeface(Typeface subtitleTextTypeface) {
        this.subtitleTextTypeface = subtitleTextTypeface;
    }

    public void setSubtitleUnreadTextTypeface(Typeface subtitleUnreadTextTypeface) {
        this.subtitleUnreadTextTypeface = subtitleUnreadTextTypeface;
    }

    public void setDateTextTypeface(Typeface dateTextTypeface) {
        this.dateTextTypeface = dateTextTypeface;
    }

    public int getTitleTextColor() {
        return titleTextColor;
    }

    public int getTitleTextStyle() {
        return titleTextStyle;
    }

    public Typeface getTitleTextTypeface() {
        return titleTextTypeface;
    }

    public int getTitleUnreadTextColor() {
        return titleUnreadTextColor;
    }

    public int getTitleUnreadTextStyle() {
        return titleUnreadTextStyle;
    }

    public Typeface getTitleUnreadTextTypeface() {
        return titleUnreadTextTypeface;
    }

    public int getSubtitleTextColor() {
        return subtitleTextColor;
    }

    public int getSubtitleTextStyle() {
        return subtitleTextStyle;
    }

    public Typeface getSubtitleTextTypeface() {
        return subtitleTextTypeface;
    }

    public int getSubtitleUnreadTextColor() {
        return subtitleUnreadTextColor;
    }

    public int getSubtitleUnreadTextStyle() {
        return subtitleUnreadTextStyle;
    }

    public Typeface getSubtitleUnreadTextTypeface() {
        return subtitleUnreadTextTypeface;
    }

    public int getCellBackgroundColor() {
        return cellBackgroundColor;
    }

    public int getCellUnreadBackgroundColor() {
        return cellUnreadBackgroundColor;
    }

    public Typeface getDateTextTypeface() {
        return dateTextTypeface;
    }

    public int getDateTextColor() {
        return dateTextColor;
    }

    public AvatarStyle getAvatarStyle() {
        return avatarStyle;
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
