package com.layer.atlas.messagetypes;

import android.graphics.Typeface;

public class MessageStyle {
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

    private MessageStyle(Builder builder) {
        myBubbleColor = builder.myBubbleColor;
        myTextColor = builder.myTextColor;
        myTextStyle = builder.myTextStyle;
        myTextSize = builder.myTextSize;
        setMyTextTypeface(builder.myTextTypeface);
        otherBubbleColor = builder.otherBubbleColor;
        otherTextColor = builder.otherTextColor;
        otherTextStyle = builder.otherTextStyle;
        otherTextSize = builder.otherTextSize;
        setOtherTextTypeface(builder.otherTextTypeface);
    }

    public void setMyTextTypeface(Typeface myTextTypeface) {
        this.myTextTypeface = myTextTypeface;
    }

    public void setOtherTextTypeface(Typeface otherTextTypeface) {
        this.otherTextTypeface = otherTextTypeface;
    }

    public int getMyBubbleColor() {
        return myBubbleColor;
    }

    public int getMyTextColor() {
        return myTextColor;
    }

    public int getMyTextStyle() {
        return myTextStyle;
    }

    public float getMyTextSize() {
        return myTextSize;
    }

    public Typeface getMyTextTypeface() {
        return myTextTypeface;
    }

    public int getOtherBubbleColor() {
        return otherBubbleColor;
    }

    public int getOtherTextColor() {
        return otherTextColor;
    }

    public int getOtherTextStyle() {
        return otherTextStyle;
    }

    public float getOtherTextSize() {
        return otherTextSize;
    }

    public Typeface getOtherTextTypeface() {
        return otherTextTypeface;
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
