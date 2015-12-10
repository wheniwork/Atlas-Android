package com.layer.atlas.util;

import android.graphics.Typeface;

public final class AvatarStyle {

    private int avatarBackgroundColor;
    private int avatarTextColor;
    private Typeface avatarTextTypeface;

    private AvatarStyle(Builder builder) {
        avatarBackgroundColor = builder.avatarBackgroundColor;
        avatarTextColor = builder.avatarTextColor;
        avatarTextTypeface = builder.avatarTextTypeface;
    }

    public void setAvatarTextTypeface(Typeface avatarTextTypeface) {
        this.avatarTextTypeface = avatarTextTypeface;
    }

    public int getAvatarBackgroundColor() {
        return avatarBackgroundColor;
    }

    public int getAvatarTextColor() {
        return avatarTextColor;
    }

    public Typeface getAvatarTextTypeface() {
        return avatarTextTypeface;
    }

    public static final class Builder {
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

        public AvatarStyle build() {
            return new AvatarStyle(this);
        }
    }
}
