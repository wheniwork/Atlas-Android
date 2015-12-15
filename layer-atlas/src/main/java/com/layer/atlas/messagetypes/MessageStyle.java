package com.layer.atlas.messagetypes;

import android.graphics.Typeface;

public class MessageStyle {
    private int mMyBubbleColor;
    private int mMyTextColor;
    private int mMyTextStyle;
    private float mMyTextSize;
    private Typeface mMyTextTypeface;

    private int mOtherBubbleColor;
    private int mOtherTextColor;
    private int mOtherTextStyle;
    private float mOtherTextSize;
    private Typeface mOtherTextTypeface;

    private MessageStyle(Builder builder) {
        mMyBubbleColor = builder.myBubbleColor;
        mMyTextColor = builder.myTextColor;
        mMyTextStyle = builder.myTextStyle;
        mMyTextSize = builder.myTextSize;
        setMyTextTypeface(builder.myTextTypeface);
        mOtherBubbleColor = builder.otherBubbleColor;
        mOtherTextColor = builder.otherTextColor;
        mOtherTextStyle = builder.otherTextStyle;
        mOtherTextSize = builder.otherTextSize;
        setOtherTextTypeface(builder.otherTextTypeface);
    }

    public void setMyTextTypeface(Typeface myTextTypeface) {
        this.mMyTextTypeface = myTextTypeface;
    }

    public void setOtherTextTypeface(Typeface otherTextTypeface) {
        this.mOtherTextTypeface = otherTextTypeface;
    }

    public int getMyBubbleColor() {
        return mMyBubbleColor;
    }

    public int getMyTextColor() {
        return mMyTextColor;
    }

    public int getMyTextStyle() {
        return mMyTextStyle;
    }

    public float getMyTextSize() {
        return mMyTextSize;
    }

    public Typeface getMyTextTypeface() {
        return mMyTextTypeface;
    }

    public int getOtherBubbleColor() {
        return mOtherBubbleColor;
    }

    public int getOtherTextColor() {
        return mOtherTextColor;
    }

    public int getOtherTextStyle() {
        return mOtherTextStyle;
    }

    public float getOtherTextSize() {
        return mOtherTextSize;
    }

    public Typeface getOtherTextTypeface() {
        return mOtherTextTypeface;
    }

    public static final class Builder {
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

        public Builder() {
        }

        public Builder myBubbleColor(int val) {
            myBubbleColor = val;
            return this;
        }

        public Builder myTextColor(int val) {
            myTextColor = val;
            return this;
        }

        public Builder myTextStyle(int val) {
            myTextStyle = val;
            return this;
        }

        public Builder myTextSize(float val) {
            myTextSize = val;
            return this;
        }

        public Builder myTextTypeface(Typeface val) {
            myTextTypeface = val;
            return this;
        }

        public Builder otherBubbleColor(int val) {
            otherBubbleColor = val;
            return this;
        }

        public Builder otherTextColor(int val) {
            otherTextColor = val;
            return this;
        }

        public Builder otherTextStyle(int val) {
            otherTextStyle = val;
            return this;
        }

        public Builder otherTextSize(float val) {
            otherTextSize = val;
            return this;
        }

        public Builder otherTextTypeface(Typeface val) {
            otherTextTypeface = val;
            return this;
        }

        public MessageStyle build() {
            return new MessageStyle(this);
        }
    }
}
