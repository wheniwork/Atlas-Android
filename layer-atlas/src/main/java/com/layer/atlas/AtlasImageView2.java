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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.layer.atlas.Atlas.Tools;

/**
 * @author Oleg Orlov
 * @since  15 Jun 2015
 */
public class AtlasImageView2 extends View {
    private static final String TAG = AtlasImageView2.class.getSimpleName();
    private static final boolean debug = true;
    
    public static final int ORIENTATION_NORMAL = 0;
    public static final int ORIENTATION_90_CW = 1;
    public static final int ORIENTATION_180 = 2;
    public static final int ORIENTATION_90_CCW = 3;
    
    private int defaultLayerType;
    
    private Drawable drawable;
    
    private int contentWidth;
    private int contentHeight;
    public int orientation;
    private float angle;
    
    // TODO: 
    // - support contentDimensions: 0x0
    // - support contentDimensions + MeasureSpec.EXACT sizes 
    // - support boundaries + drawable instead of contentDimensions + drawable 
    
    //----------------------------------------------------------------------------
    public AtlasImageView2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupPaints();
    }

    public AtlasImageView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaints();
    }

    public AtlasImageView2(Context context) {
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
            if (debug) Log.w(TAG, "onMeasure() exact dimensions, skipping " + Tools.toStringSpec(widthSpec, heightSpec)); 
        } else if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            if (debug) Log.w(TAG, "onMeasure() first pass, skipping " + Tools.toStringSpec(widthSpec, heightSpec));
        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
            if (widthMode == MeasureSpec.EXACTLY) {
                setMeasuredDimension(w, (int)(1.0 * w * contentHeight / contentWidth));
            }
            if (widthMode == MeasureSpec.AT_MOST) {
                if (contentWidth >= w) {
                    setMeasuredDimension(w, (int)(1.0 * w * contentHeight / contentWidth));
                } else {
                    setMeasuredDimension(contentWidth, contentHeight);
                }
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
        this.defaultLayerType = getLayerType();
    }
    
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        
        if (getWidth() != getMeasuredWidth() || getHeight() != getMeasuredHeight()) {
            if (debug) Log.w(TAG, "onDraw() actual: " + getWidth() + "x" + getHeight()
                    + ", measured: " + getMeasuredWidth() + "x" + getMeasuredHeight());
        }
        
        if (drawable == null) return;
        
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bmpDrw = (BitmapDrawable) drawable;
            if (bmpDrw.getBitmap() != null) {
                if (debug) Log.w(TAG, "onDraw() bitmap: " + bmpDrw.getBitmap().getWidth() + "x" + bmpDrw.getBitmap().getHeight());
            } else {
                if (debug) Log.w(TAG, "onDraw() bitmap: null");
            }
        }
        
        if (debug) {
            Log.w(TAG, 
                    "onDraw() bounds: " + drawable.getBounds() + ", content: " + contentWidth + "x" + contentHeight 
                    + ", orientation: " + orientation + ", angle: " + angle 
                    + "          min: " + drawable.getMinimumWidth() + "x" + drawable.getMinimumHeight()
                    + "   instrinsic: " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight()
            );
        }
        
        int viewWidth  = getWidth();
        int viewHeight = getHeight();
        int imgWidth  = contentWidth;
        int imgHeight = contentHeight;

        if (contentWidth == 0 && contentHeight == 0) {
            imgWidth = drawable.getIntrinsicWidth();
            imgHeight = drawable.getIntrinsicHeight();
        }
        
        // fit in width
        if (imgWidth > viewWidth) {
            int newHeight = (int) (1.0 * imgHeight * viewWidth / imgWidth);
            int newWidth  = viewWidth;
            if (debug) Log.w(TAG, "onDraw() fit in width:  " + imgWidth + "x" + imgHeight + " -> " + newWidth + "x" + newHeight);
            imgWidth = newWidth;
            imgHeight = newHeight;
        }
        if (imgHeight > viewHeight) {
            int newWidth = (int) (1.0 * imgWidth * viewHeight / imgHeight);
            int newHeight = viewHeight;
            if (debug) Log.w(TAG, "onDraw() fit in height: " + imgWidth + "x" + imgHeight + " -> " + newWidth + "x" + newHeight);
            imgWidth = newWidth;
            imgHeight = newHeight;
        }
        
        int left = (viewWidth  - imgWidth)  / 2;
        int top  = (viewHeight - imgHeight) / 2;
        int right = left + imgWidth;
        int bottom = top + imgHeight;
        if (debug) Log.w(TAG, "onDraw() left: " + left + ", top: " + top + ", right: " + right + ", bottom: " + bottom);
        drawable.setBounds(left, top, right, bottom);

        
        if (!useBitmapBuffer && buffer != null) {
            buffer = null;
            bufferCanvas = null;
        }
        if (useBitmapBuffer && (buffer == null || buffer.getWidth() != viewWidth || buffer.getHeight() != viewHeight)) {
            buffer = Bitmap.createBitmap(viewWidth, viewHeight, Config.ARGB_8888);
            bufferCanvas = new Canvas(buffer);
        }
        
        Canvas workCanvas = useBitmapBuffer ? bufferCanvas : canvas;
        if (useBitmapBuffer) {          
            workCanvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);            // clean before using
        }
        
        if (debug) Log.w(TAG, "onDraw() useBitmapBuffer: " + useBitmapBuffer + ", buffer: " + (buffer == null ? "null" : buffer.getWidth() + "x" + buffer.getHeight()) );
        int saved = workCanvas.save();
        boolean iOSBug = true;
        if (iOSBug) {
            switch (orientation) {
                case ORIENTATION_90_CW  : workCanvas.rotate(180, 0.5f * drawable.getBounds().width() , 0.5f * drawable.getBounds().height()); break;
                case ORIENTATION_180    : 
                    if (false) {
                        drawable.setBounds(-viewHeight / 2, -viewWidth / 2, viewHeight / 2, viewWidth / 2);
                        workCanvas.rotate(-90);
                        workCanvas.translate(viewWidth / 2, viewHeight /2 );
                    } else {
                        drawable.setBounds(0, 0, viewHeight, viewWidth);
                        workCanvas.translate(0, viewHeight);
                        workCanvas.rotate(-90); 
                    }
                    break;
                case ORIENTATION_90_CCW : 
                    drawable.setBounds(0, 0, viewHeight, viewWidth);
                    workCanvas.translate(viewWidth, 0);
                    workCanvas.rotate(90);
                    break;
                default: workCanvas.rotate(angle, 0.5f * viewWidth , 0.5f * viewHeight);
            }
        } else {
            if (orientation == ORIENTATION_90_CCW || orientation == ORIENTATION_90_CW) {
                drawable.setBounds(0,0, viewHeight, viewWidth);
            }
            switch (orientation) {
                case ORIENTATION_90_CW  : workCanvas.rotate(-90, 0.5f * drawable.getBounds().width() , 0.5f * drawable.getBounds().height()); break;
                case ORIENTATION_180    : workCanvas.rotate(180, 0.5f * viewWidth , 0.5f * viewHeight); break;
                case ORIENTATION_90_CCW : workCanvas.rotate(90,  0.5f * drawable.getBounds().width() , 0.5f * drawable.getBounds().height()); break;
                default: workCanvas.rotate(angle, 0.5f * viewWidth , 0.5f * viewHeight);
            }
        }
        drawable.draw(workCanvas);
        workCanvas.restoreToCount(saved);
        if (useBitmapBuffer) {
            canvas.drawBitmap(buffer, 0, 0, bitmapPaint);
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (debug) Log.w(TAG, "onTouch() event: " + Tools.toString(event));
        return super.onTouchEvent(event);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        boolean result = super.verifyDrawable(who);
        if (who == this.drawable) result = true;
        return result;
    }

    public void setBitmap(Bitmap bmp) {
        setDrawable(new BitmapDrawable(bmp));
    }
    
    public void setDrawable(Drawable drawable) {
        if (this.drawable != null) {
            this.drawable.setCallback(null);
        }
        this.drawable = drawable;
        if (drawable != null) {
            this.drawable.setCallback(this);
        }
        if (drawable instanceof GIFDrawable) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        } else {
            setLayerType(defaultLayerType, null);
        }
        invalidate();
    }
    
    public void setContentDimensions(int contentWidth, int contentHeight) {
        boolean requestLayout = false;
        if (this.contentWidth != contentWidth || this.contentHeight != contentHeight) {
            requestLayout = true;
        }
        if (debug) Log.w(TAG, "setContentDimensions() new: " + contentWidth + "x" + contentHeight + ", old: " + this.contentWidth + "x" + this.contentHeight);
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
        
        if (requestLayout) {
            requestLayout();
        }
        invalidate();
    }
    
    boolean useBitmapBuffer;
    private Bitmap buffer;
    private Canvas bufferCanvas;
    private static final Paint bitmapPaint = new Paint();

    /** 
     * Big bitmaps may not fit into GL_MAX_TEXTURE_SIZE boundaries (2048x2048 for Nexus S).
     * To draw such images, buffer bitmap needs to be created
     * TODO: understand it automatically
     */
    public void setUseBitmapBuffer(boolean useBitmapBuffer) {
        this.useBitmapBuffer = useBitmapBuffer;
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
        invalidate();
    }

}
