package com.layer.atlas.util;

import android.graphics.Typeface;

public final class AvatarStyle {

    private int mAvatarBackgroundColor;
    private int mAvatarBorderColor;
    private int mAvatarTextColor;
    private Typeface mAvatarTextTypeface;

    private AvatarStyle(Builder builder) {
        mAvatarBackgroundColor = builder.avatarBackgroundColor;
        mAvatarTextColor = builder.avatarTextColor;
        mAvatarTextTypeface = builder.avatarTextTypeface;
        mAvatarBorderColor = builder.avatarBorderColor;
    }

    public void setAvatarTextTypeface(Typeface avatarTextTypeface) {
        this.mAvatarTextTypeface = avatarTextTypeface;
    }

    public int getAvatarBackgroundColor() {
        return mAvatarBackgroundColor;
    }

    public int getAvatarTextColor() {
        return mAvatarTextColor;
    }

    public Typeface getAvatarTextTypeface() {
        return mAvatarTextTypeface;
    }

    public int getAvatarBorderColor() {
        return mAvatarBorderColor;
    }

    public static final class Builder {
        private int avatarBorderColor;
        private int avatarBackgroundColor;
        private int avatarTextColor;
        private Typeface avatarTextTypeface;

        public Builder() {
        }

        public Builder avatarBackgroundColor(int val) {
            avatarBackgroundColor = val;
            return this;
        }

        public Builder avatarTextColor(int val) {
            avatarTextColor = val;
            return this;
        }

        public Builder avatarTextTypeface(Typeface val) {
            avatarTextTypeface = val;
            return this;
        }

        public Builder avatarBorderColor(int val) {
            avatarBorderColor = val;
            return this;
        }

        public AvatarStyle build() {
            return new AvatarStyle(this);
        }
    }
}
