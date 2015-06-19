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
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.layer.atlas.Atlas.Tools;
import com.layer.sdk.internal.utils.Log;

/**
 * @author Oleg Orlov
 * @since  15 Jun 2015
 */
public class AtlasImageView extends View {
    private static final String TAG = AtlasImageView.class.getSimpleName();
    private static final boolean debug = false;
    
    public static final int ORIENTATION_NORMAL = 0;
    public static final int ORIENTATION_90_CW = 1;
    public static final int ORIENTATION_180 = 2;
    public static final int ORIENTATION_90_CCW = 3;
    
    public Drawable drawable;
    public Movie    movie;
    
    private int contentWidth;
    private int contentHeight;
    public int orientation;
    public float angle;
    
    // TODO: 
    // - support contentDimensions: 0x0
    // - support contentDimensions + MeasureSpec.EXACT sizes 
    // - support boundaries + drawable instead of contentDimensions + drawable 
    
    //----------------------------------------------------------------------------
    public AtlasImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupPaints();
    }

    public AtlasImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaints();
    }

    public AtlasImageView(Context context) {
        super(context);
        setupPaints();
    }
    
    protected void onMeasure(int widthSpec, int heightSpec) {
        int mWidthBefore  = getMeasuredWidth();
        int mHeightBefore = getMeasuredHeight();
        super.onMeasure(widthSpec, heightSpec);
        int mWidthAfter = getMeasuredWidth();
        int mHeightAfter = getMeasuredHeight();

        if (debug) Log.w(TAG, "onMeasure() before: " + mWidthBefore + "x" + mHeightBefore
                + ", spec: " + Tools.toStringSpec(widthSpec, heightSpec)
                + ", after: " + mWidthAfter + "x" + mHeightAfter
                + ", content: " + contentWidth + "x" + contentHeight + " h/w: " + (1.0f * contentHeight / contentWidth)
                );

        int widthMode = MeasureSpec.getMode(widthSpec);
        int heightMode = MeasureSpec.getMode(heightSpec);
        int w = MeasureSpec.getSize(widthSpec);
        int h = MeasureSpec.getSize(heightSpec);
        
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            if (debug) Log.w(TAG, "onMeasure() exact dimenstions, skipping " + Tools.toStringSpec(widthSpec, heightSpec)); 
        } else if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            if (debug) Log.w(TAG, "onMeasure() first pass, skipping " + Tools.toStringSpec(widthSpec, heightSpec));
        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
            if (widthMode == MeasureSpec.EXACTLY) {
                setMeasuredDimension(w, (int)(w * (1.0 * contentHeight / contentWidth)));
            }
            if (widthMode == MeasureSpec.AT_MOST) {
                setMeasuredDimension(w, (int)(w * (1.0 * contentHeight / contentWidth)));
            }
        } else {
            if (debug) Log.w(TAG, "onMeasure() unchanged. " + Tools.toStringSpec(widthSpec, heightSpec));
        }
                
        if (debug) Log.w(TAG, "onMeasure() final: " + getMeasuredWidth() + "x" + getMeasuredHeight());
    }
    
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (debug) Log.d(TAG, "onLayout() changed: " + changed+ " left: " + left+ " top: " + top+ " right: " + right+ " bottom: " + bottom);
    }
    
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (debug) Log.w(TAG, "onSizeChanged() w: " + w + " h: " + h+ " oldw: " + oldw+ " oldh: " + oldh);
    }

    private void setupPaints() {
    }
    
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (getWidth() != getMeasuredWidth() || getHeight() != getMeasuredHeight()) {
            if (debug) Log.w(TAG, "onDraw() actual: " + getWidth() + "x" + getHeight()
                    + ", measured: " + getMeasuredWidth() + "x" + getMeasuredHeight());
        }
        
        if (drawable == null) return;
        
        int viewWidth  = getWidth();
        int viewHeight = getHeight();
        
        drawable.setBounds(0, 0, viewWidth, viewHeight);
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bmpDrw = (BitmapDrawable) drawable;
            if (debug) Log.w(TAG, "onDraw() bitmap: " + bmpDrw.getBitmap().getWidth() + "x" + bmpDrw.getBitmap().getHeight());
        }
        
        if (debug) Log.w(TAG, 
                  "onDraw() bounds: " + drawable.getBounds() + ", orientation: " + orientation 
                + "            min: " + drawable.getMinimumWidth() + "x" + drawable.getMinimumHeight()
                + "     instrinsic: " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight()
        );
        
        int saved = canvas.save();
        boolean iOSBug = true;
        if (iOSBug) {
            switch (orientation) {
                case ORIENTATION_90_CW  : canvas.rotate(180, 0.5f * drawable.getBounds().width() , 0.5f * drawable.getBounds().height()); break;
                case ORIENTATION_180    : 
                    if (false) {
                        drawable.setBounds(-viewHeight / 2, -viewWidth / 2, viewHeight / 2, viewWidth / 2);
                        canvas.rotate(-90);
                        canvas.translate(viewWidth / 2, viewHeight /2 );
                    } else {
                        drawable.setBounds(0, 0, viewHeight, viewWidth);
                        canvas.translate(0, viewHeight);
                        canvas.rotate(-90); 
                    }
                    break;
                case ORIENTATION_90_CCW : 
                    drawable.setBounds(0, 0, viewHeight, viewWidth);
                    canvas.translate(viewWidth, 0);
                    canvas.rotate(90);
                    break;
                default: canvas.rotate(angle, 0.5f * viewWidth , 0.5f * viewHeight);
            }
        } else {
            if (orientation == ORIENTATION_90_CCW || orientation == ORIENTATION_90_CW) {
                drawable.setBounds(0,0, viewHeight, viewWidth);
            }
            switch (orientation) {
                case ORIENTATION_90_CW  : canvas.rotate(-90, 0.5f * drawable.getBounds().width() , 0.5f * drawable.getBounds().height()); break;
                case ORIENTATION_180    : canvas.rotate(180, 0.5f * viewWidth , 0.5f * viewHeight); break;
                case ORIENTATION_90_CCW : canvas.rotate(90,  0.5f * drawable.getBounds().width() , 0.5f * drawable.getBounds().height()); break;
                default: canvas.rotate(angle, 0.5f * viewWidth , 0.5f * viewHeight);
            }
        }
        drawable.draw(canvas);
        canvas.restoreToCount(saved);
    }

    public void setImageBitmap(Bitmap bmp) {
        this.drawable = new BitmapDrawable(bmp);
        invalidate();
    }
    
    public void setGifMovie(Movie gif) {
        Bitmap bmp = Bitmap.createBitmap(gif.width(), gif.height(), Config.ARGB_8888);
        Canvas cnv = new Canvas(bmp);
        gif.draw(cnv, 0, 0);
        this.drawable = new BitmapDrawable(bmp);
        invalidate();
    }

    public void setImageDrawable(Drawable drawable) {
        this.drawable = drawable;
        invalidate();
    }
    
    public void setContentDimensions(int contentWidth, int contentHeight) {
        boolean requestLayout = false;
        if (this.contentWidth != contentWidth || this.contentHeight != contentHeight) {
            requestLayout = true;
        }
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
        
        if (requestLayout) {
            requestLayout();
        }
    }
}
