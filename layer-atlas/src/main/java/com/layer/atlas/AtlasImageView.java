package com.layer.atlas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
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
    private static final boolean debug = true;

    public Drawable drawable;
    
    public int width;
    public int height;
    public int orientation;
    
    private float progress = 0.6f;
    private int colorMain = Color.argb(0xA0, 0xFF, 0xFF, 0xFF);
    
    private float pieRadiusDp = 20;
    private float spacingWidthDp = 5;
    
    // working stuff
    private Paint ringPaint;
    private Paint piePaint;
    private RectF pieBounds = new RectF();
    private float defaultWidthDp = pieRadiusDp * 2 + spacingWidthDp * 2;
    private float defaultHeightDp = defaultWidthDp;
    
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
    
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mWidthBefore  = getMeasuredWidth();
        int mHeightBefore = getMeasuredHeight();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        int mWidthAfter = getMeasuredWidth();
        int mHeightAfter = getMeasuredHeight();
        
        int measuredWidth = getMeasuredWidth();
        
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED && measuredWidth == 0) {
            measuredWidth = (int) Tools.getPxFromDp(defaultWidthDp, getContext());
        }
        int measuredHeight = getMeasuredHeight();
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED && measuredHeight == 0) {
            measuredHeight = (int) Tools.getPxFromDp(defaultHeightDp, getContext());
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
        if (debug) Log.w(TAG, "onMeasure() before: " + mWidthBefore + "x" + mHeightBefore
                + ", spec: " + Tools.toStringSpec(widthMeasureSpec) + "x" + Tools.toStringSpec(heightMeasureSpec)
                + ", after: " + mWidthAfter + "x" + mHeightAfter
                + ", final: " + getMeasuredWidth() + "x" + getMeasuredHeight());
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
        ringPaint = new Paint();
        ringPaint.setColor(colorMain);
        ringPaint.setStyle(Style.STROKE);
        ringPaint.setAntiAlias(true);
        
        piePaint = new Paint();
        piePaint.setStyle(Style.FILL);
        piePaint.setColor(colorMain);
        piePaint.setAntiAlias(true);
    }
    
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (progress < 0.001f || progress > 0.999f) return;
        
        if (getWidth() != getMeasuredWidth() || getHeight() != getMeasuredHeight()) {
            if (debug) Log.w(TAG, "onDraw() actual: " + getWidth() + "x" + getHeight()
                    + ", measured: " + getMeasuredWidth() + "x" + getMeasuredHeight());
        }
        
        float viewWidth  = getWidth();
        float viewHeight = getHeight();
        
        float outerRadiusPx = (viewWidth + viewHeight) / 2;// + viewHeight;
        float innerRadiusPx = Tools.getPxFromDp(pieRadiusDp, getContext());
        float centerX = viewWidth / 2;
        float centerY = viewHeight / 2;

        pieBounds.set(centerX - innerRadiusPx, centerY - innerRadiusPx, centerX + innerRadiusPx, centerY + innerRadiusPx);
        canvas.drawArc(pieBounds, -90 + (360 * progress), 360 - (360 * progress) , true, piePaint);   // sweepAngle is a diff, not absolute value
        
        float ringInnerRadiusPx = innerRadiusPx + Tools.getPxFromDp(spacingWidthDp, getContext());
        
        // calculate ring parameters
        float ringRadiusPx = 0.5f * (ringInnerRadiusPx + outerRadiusPx);
        float strokeWidth = outerRadiusPx - ringInnerRadiusPx;
        
        ringPaint.setStrokeWidth(strokeWidth);
        
        canvas.drawCircle(viewWidth / 2, viewHeight / 2, ringRadiusPx, ringPaint);
        
        if (debug) Log.w(TAG, 
                    "onDraw() instrinsic: " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight()
                + "\n                min: " + drawable.getMinimumWidth() + "x" + drawable.getMinimumHeight()
                + "\n             bounds: " + drawable.getBounds());
        int saved = canvas.save();
        canvas.rotate(90 * orientation);
        drawable.draw(canvas);
        canvas.restoreToCount(saved);
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }

    public float getProgress() {
        return progress;
    }

    public void setImageBitmap(Bitmap bmp) {
        this.drawable = new BitmapDrawable(bmp);
        invalidate();
    }

    public void setImageDrawable(Drawable drawable) {
        this.drawable = drawable;
        invalidate();
    }
}
