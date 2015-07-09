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
package com.layer.atlas.messenger;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.CheckBox;

import com.layer.atlas.Atlas;
import com.layer.atlas.Atlas.ImageLoader.ImageSpec;
import com.layer.atlas.Atlas.Tools;
import com.layer.atlas.AtlasImageView2;
import com.layer.atlas.AtlasProgressView;
import com.layer.atlas.cells.ImageCell;
import com.layer.sdk.internal.utils.Log;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.MessagePart;

/**
 * @author Oleg Orlov
 * @since  17 Jun 2015
 */
public class AtlasImageViewScreen extends Activity implements Atlas.ImageLoader.ImageLoadListener, LayerProgressListener {
    private static final String TAG = AtlasImageViewScreen.class.getSimpleName();
    private static final boolean debug = true;
    
    private AtlasImageView2   imageViewer;
    private AtlasProgressView progressView;
    private CheckBox hdCheck;
    private CheckBox decorCheck;
    
    private ImageCell cell;
    private Drawable previewDrawable;
    private Drawable fullDrawable;
    
    private long downloadBytesPreview = -1;
    private long downloadBytesFull = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atlas_screen_image_view);

        MessengerApp app = (MessengerApp) getApplication();
        this.cell = (ImageCell) app.getParam(getIntent()); 
        if (debug) Log.w(TAG, "onCreate() cell: " + cell);
        if (cell == null) finish();
        
        if (cell.previewPart != null && ! cell.previewPart.isContentReady() ) {
            cell.previewPart.download(this);
        }
        
        if ( ! cell.fullPart.isContentReady() ) {
            cell.fullPart.download(this);
        }
        
        this.imageViewer = (AtlasImageView2) findViewById(R.id.atlas_screen_image_view_image);
        this.imageViewer.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        
        this.imageViewer.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (debug) Log.w(TAG, "onTouch() event: " + Tools.toString(event));
                return false;
            }
        });
        
        this.progressView = (AtlasProgressView) findViewById(R.id.atlas_screen_image_view_progress);
        
        findViewById(R.id.atlas_screen_image_view_angle_minus).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                imageViewer.setAngle(imageViewer.getAngle() - 3.0f);
                imageViewer.invalidate();
            }
        });
        
        findViewById(R.id.atlas_screen_image_view_angle_plus).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                imageViewer.setAngle(imageViewer.getAngle() + 3.0f);
                imageViewer.invalidate();
            }
        });
        
        this.hdCheck = (CheckBox) findViewById(R.id.atlas_screen_image_view_hd);
        this.hdCheck.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                updateValues();
            }
        });
        
        this.decorCheck = (CheckBox) findViewById(R.id.atlas_screen_image_view_decor);
        this.decorCheck.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                updateDecor();
            }
        });
        updateDecor();
        updateValues();
    }

    private void updateDecor() {
        if (decorCheck.isChecked()) {
            if (cell.orientation == ImageCell.ORIENTATION_1_CW_180 || cell.orientation == ImageCell.ORIENTATION_2_CW_90) {
                this.imageViewer.setContentDimensions(this.cell.declaredHeight, this.cell.declaredWidth);
            } else {
                this.imageViewer.setContentDimensions(this.cell.declaredWidth, this.cell.declaredHeight);
            }
            float angle = 0;
            switch (cell.orientation) {
                case ImageCell.ORIENTATION_2_CW_90  : angle = -90; break;
                case ImageCell.ORIENTATION_3_CCW_90 : angle = 90;  break;
                case ImageCell.ORIENTATION_1_CW_180 : angle = 180; break;
            }
            this.imageViewer.setAngle(angle);
        } else {
            imageViewer.setContentDimensions(0, 0);
            imageViewer.setAngle(0);
        }
    }
    
    @Override
    public void onImageLoaded(final ImageSpec spec) {
        runOnUiThread(UPDATE_VALUES_RUNNABLE);
    }
    
    private void updateValues() {
        
        // prepare drawables
        if (fullDrawable == null) {
            // build fullDrawable
            Bitmap fullBmp = (Bitmap) Atlas.imageLoader.getBitmapFromCache(cell.fullPart.getId());
            if (fullBmp != null) {
                fullDrawable = new BitmapDrawable(fullBmp);
                if (fullBmp.getWidth() > 2048 || fullBmp.getHeight() > 2048) {
                    if (debug) Log.w(TAG, "updateValues() enabling buffering... bitmap: " + fullBmp);
                    imageViewer.setUseBitmapBuffer(true);
                }
            } else {
                DisplayMetrics dm = new DisplayMetrics();
                this.getWindowManager().getDefaultDisplay().getMetrics(dm);
                
                final int requiredWidth  = imageViewer.getWidth()  != 0 ? imageViewer.getWidth()  : dm.widthPixels;
                final int requiredHeight = imageViewer.getHeight() != 0 ? imageViewer.getHeight() : dm.heightPixels;

                Atlas.imageLoader.requestBitmap(cell.fullPart.getId(), new Atlas.MessagePartStreamProvider(cell.fullPart)
                    , requiredWidth, requiredHeight, false, this);
            }
        }
        
        if (previewDrawable == null && cell.previewPart != null) {
            Bitmap previewBmp = (Bitmap) Atlas.imageLoader.getBitmapFromCache(cell.previewPart.getId());
            if (previewBmp != null) {
                previewDrawable = new BitmapDrawable(previewBmp);
            } else { 
                Atlas.imageLoader.requestBitmap(cell.previewPart.getId(), new Atlas.MessagePartStreamProvider(cell.previewPart), this);
            }
        }
        
        if (hdCheck.isChecked()) {
            // set content if ready
            if (fullDrawable != null) {
                imageViewer.setDrawable(fullDrawable);
            } else if (previewDrawable != null){
                imageViewer.setDrawable(previewDrawable);
            }
        } else {
            if (previewDrawable != null) {
                imageViewer.setDrawable(previewDrawable);
            } else {
                imageViewer.setDrawable(Tools.EMPTY_DRAWABLE);
            }
        }

        progressView.setVisibility(fullDrawable == null ? View.VISIBLE : View.GONE);
        
        float progress = 0.0f;
        if (downloadBytesPreview > -1) progress = 1.0f * downloadBytesPreview / cell.previewPart.getSize();
        if (downloadBytesFull > -1) progress = 1.0f * downloadBytesFull / cell.fullPart.getSize();
        progressView.setProgress(progress);
    }

    long downloadStartedFull;
    long downloadStartedPreview;
    
    @Override
    public void onProgressStart(MessagePart part, Operation operation) {
        if (part == cell.fullPart) {
            downloadBytesFull = 0;
            downloadStartedFull = System.currentTimeMillis();
        } else {
            downloadBytesPreview = 0;
            downloadStartedPreview = System.currentTimeMillis();
        }
        if (debug) Log.w(TAG, "onProgressStart() started, part: " + part.getId() + ", size: " + part.getSize());
    }

    @Override
    public void onProgressUpdate(MessagePart part, Operation operation, long transferredBytes) {
        float secSpent = 0.01f;
        if (part == cell.fullPart) {
            downloadBytesFull = transferredBytes;
            secSpent = (System.currentTimeMillis() - downloadStartedFull) / 1000f;
        } else {
            downloadBytesPreview = transferredBytes;
            secSpent = (System.currentTimeMillis() - downloadStartedPreview) / 1000f;
        }
        if (debug) Log.w(TAG, "onProgressUpdate() transferred: " + transferredBytes + ", in: " + secSpent 
                + "s, speed: " + (0.001f * transferredBytes / secSpent) + "kbs");
        
        runOnUiThread(UPDATE_VALUES_RUNNABLE);
    }

    @Override
    public void onProgressComplete(MessagePart part, Operation operation) {
        float secSpent;
        if (part == cell.fullPart) {
            secSpent = (System.currentTimeMillis() - downloadStartedFull) / 1000f;
        } else {
            secSpent = (System.currentTimeMillis() - downloadStartedPreview) / 1000f;
        }
        
        if (debug) Log.w(TAG, "onProgressComplete() part: " + part.getId() + ", size: " + part.getSize() + ", in: " + secSpent
                + "s, speed: " + (0.001f * part.getSize() / secSpent) + "kbs");

        runOnUiThread(UPDATE_VALUES_RUNNABLE);
    }

    @Override
    public void onProgressError(MessagePart part, Operation operation, Throwable cause) {
        Log.e(TAG, "onProgressError() download failed. part: " + part, cause);
    }
    
    private final Runnable UPDATE_VALUES_RUNNABLE = new Runnable() {
        public void run() {
            updateValues();
        }
    };
}
