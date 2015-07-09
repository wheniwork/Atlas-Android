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
package com.layer.atlas.cells;

import android.graphics.Movie;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.layer.atlas.Atlas;
import com.layer.atlas.Atlas.ImageLoader;
import com.layer.atlas.Atlas.ImageLoader.ImageSpec;
import com.layer.atlas.Atlas.ImageLoader.StreamProvider;
import com.layer.atlas.Atlas.MessagePartBufferedStreamProvider;
import com.layer.atlas.Atlas.Tools;
import com.layer.atlas.AtlasImageView;
import com.layer.atlas.AtlasMessagesList;
import com.layer.atlas.AtlasMessagesList.Cell;
import com.layer.atlas.AtlasProgressView;
import com.layer.atlas.GIFDrawable;
import com.layer.atlas.R;
import com.layer.atlas.ShapedFrameLayout;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.MessagePart;

/**
 * @author Oleg Orlov
 * @since  21 Jun 2015
 */
public class GIFCell extends Cell implements ImageLoader.ImageLoadListener, LayerProgressListener {

    private static final String TAG = GIFCell.class.getSimpleName();
    private static final boolean debug = false;
    
    MessagePart previewPart;
    MessagePart fullPart;
    int declaredWidth;
    int declaredHeight;
    int orientation;
    ImageLoader.ImageSpec imageSpec;
    
    /** if more than 0 - download is in progress */
    volatile long downloadProgressBytes = -1;
    
    final AtlasMessagesList messagesList;
    final static long createdAt = System.currentTimeMillis();
    
    public GIFCell(MessagePart fullImagePart, AtlasMessagesList messagesList) {
        super(fullImagePart);
        this.fullPart = fullImagePart;
        this.messagesList = messagesList;
        if (debug) Log.w(TAG, "GIFCell() created from full part: " + fullImagePart.getId());
    }
    public GIFCell(MessagePart fullImagePart, MessagePart previewImagePart, int width, int height, int orientation, AtlasMessagesList messagesList) {
        super(fullImagePart);
        this.fullPart = fullImagePart;
        this.previewPart = previewImagePart;
        this.declaredWidth = width;
        this.declaredHeight = height;
        this.orientation = orientation;
        this.messagesList = messagesList;
        if (debug) Log.w(TAG, "GIFCell() created from 3-part: " + width + "x" + height + "@" + orientation + ", id: " + fullImagePart.getId() + ", preview: " + previewImagePart.getId() );
    }
    @Override
    public View onBind(final ViewGroup cellContainer) {
        View rootView = Tools.findChildById(cellContainer, R.id.atlas_view_messages_cell_image);
        if (rootView == null) {
            rootView = LayoutInflater.from(cellContainer.getContext()).inflate(R.layout.atlas_view_messages_cell_image, cellContainer, false); 
        }
        
        boolean myMessage = messagesList.getLayerClient().getAuthenticatedUserId().equals(messagePart.getMessage().getSender().getUserId());
        
        View imageContainerMy = rootView.findViewById(R.id.atlas_view_messages_cell_image_container_my);
        View imageContainerTheir = rootView.findViewById(R.id.atlas_view_messages_cell_image_container_their);
        AtlasImageView imageViewMy = (AtlasImageView) imageContainerMy.findViewById(R.id.atlas_view_messages_cell_image_my);
        AtlasImageView imageViewTheir = (AtlasImageView) imageContainerTheir.findViewById(R.id.atlas_view_messages_cell_image_their);
        AtlasImageView imageView = myMessage ? imageViewMy : imageViewTheir;
        View imageContainer = myMessage ? imageContainerMy : imageContainerTheir;
        
        if (myMessage) {
            imageContainerMy.setVisibility(View.VISIBLE);
            imageContainerTheir.setVisibility(View.GONE);
        } else {
            imageContainerMy.setVisibility(View.GONE);
            imageContainerTheir.setVisibility(View.VISIBLE);
        }

        MessagePart workingPart = fullPart;
        if (debug) Log.w(TAG, "onBind() GIF part: " + workingPart.getMimeType() + ", id: " + workingPart.getId());
        
        Movie img  = (Movie) Atlas.imageLoader.getBitmapFromCache(workingPart.getId());
             
        // understanging image's dimensions
        int imgWidth  = this.declaredWidth;
        int imgHeight = this.declaredHeight;
        if (debug) Log.w(TAG, "gif.onBind() declared image: " + declaredWidth + "x" + declaredHeight);
        if ((imgWidth == 0 || imgHeight == 0) && imageSpec != null && imageSpec.originalWidth != 0) {
            if (debug) Log.w(TAG, "gif.onBind() using imgSize from spec:   " + imageSpec.originalWidth + "x" + imageSpec.originalHeight);
            imgWidth  = imageSpec.originalWidth;
            imgHeight = imageSpec.originalHeight;
        }
        if ((imgWidth == 0 || imgHeight == 0) && img != null) {
            if (debug) Log.w(TAG, "gif.onBind() using imgSize from bitmap: " + img.width()+ "x" + img.height());
            imgWidth  = img.width();
            imgHeight = img.height();
        }

        // calculate appropriate View size
        // if image dimensions are unknown, use default size 192dp
        int viewWidth  = (int) (imgWidth  != 0 ? imgWidth  : Tools.getPxFromDp(192, imageContainer.getContext()));
        int viewHeight = (int) (imgHeight != 0 ? imgHeight : Tools.getPxFromDp(192, imageContainer.getContext()));
        if (orientation == ImageCell.ORIENTATION_1_CW_180 || orientation == ImageCell.ORIENTATION_3_CCW_90) {
             int oldWidth = viewWidth;
             viewWidth = viewHeight;
             viewHeight = oldWidth;
        }
        
        if (debug) Log.w(TAG, "gif.onBind() image: " + imgWidth + "x" + imgHeight + " into view: " + viewWidth + "x" + viewHeight + ", orientation: " + orientation
                + ", container: " + (myMessage ? "my " : "their ") + imageContainer.getWidth() + "x" + imageContainer.getHeight() 
                + ", cell: " + cellContainer.getWidth() + "x" + cellContainer.getHeight());
        
        int widthToFit;
        if (cellContainer.getWidth() != 0) {
            if (debug) Log.w(TAG, "gif.onBind() widthToFit from cellContainer: " + cellContainer.getWidth());
            widthToFit = cellContainer.getWidth();
        } else {
            if (debug) Log.w(TAG, "gif.onBind() widthToFit from  messagesList:  " + messagesList.getWidth());
            widthToFit = messagesList.getWidth();
        }
        
        if (viewWidth > widthToFit) {
            int oldWidth  = viewWidth;
            viewHeight = (int) (1.0 * viewHeight * widthToFit / viewWidth);
            viewWidth = widthToFit;
            if (debug) Log.w(TAG, "gif.onBind() viewWidth > widthToFit: " + oldWidth + " > " + widthToFit + " -> view: " + viewWidth + "x" + viewHeight);
        }
        
        if (viewHeight > messagesList.getHeight() && messagesList.getHeight() > 0) {
            int oldHeight = viewHeight;
            viewWidth = (int)(1.0 * viewWidth * messagesList.getHeight() / viewHeight);
            viewHeight = messagesList.getHeight();
            if (debug) Log.w(TAG, "gif.onBind() viewHeight > messagesList.height: " + oldHeight + " > " + messagesList.getHeight() + " -> view: " + viewWidth + "x" + viewHeight);
        }
        
        if (debug) Log.w(TAG, "gif.onBind() image: " + imgWidth + "x" + imgHeight + " set"
                + "  view: " + viewWidth + "x" + viewHeight + ", h/w: " + (1.0f * viewHeight / viewWidth) 
                );
        
        imageView.setContentDimensions(viewWidth, viewHeight);
        imageView.orientation = orientation;

        // TODO: calculate properly with rotation
        int requiredWidth  = messagesList.getWidth();
        int requiredHeight = messagesList.getHeight();

        if (img != null){
            GIFDrawable gifDrawable = new GIFDrawable(img, createdAt);
            imageView.setDrawable(gifDrawable);
            if (debug) Log.i(TAG, "gif.onBind() returned from cache! GIF: " + img.width() + "x" + img.height() + "@" + img.duration() 
                    + ", req: " + requiredWidth + "x" + requiredHeight + " for " + workingPart.getId());
        } else {
            imageView.setDrawable(Tools.EMPTY_DRAWABLE);
            final Uri id = workingPart.getId();
            StreamProvider streamProvider = new MessagePartBufferedStreamProvider(workingPart);
            if (workingPart.isContentReady()) {
                imageSpec = Atlas.imageLoader.requestBitmap(id, streamProvider, requiredWidth, requiredHeight, true, this);
            } else if (downloadProgressBytes == -1){
                workingPart.download(this);
            }
        }
        
        AtlasProgressView progressMy = (AtlasProgressView) rootView.findViewById(R.id.atlas_view_messages_cell_image_my_progress);
        AtlasProgressView progressTheir = (AtlasProgressView) rootView.findViewById(R.id.atlas_view_messages_cell_image_their_progress);
        AtlasProgressView progressView = myMessage ? progressMy : progressTheir;
        if (downloadProgressBytes > 0) {
            float progress = 1.0f * downloadProgressBytes / workingPart.getSize();
            if (debug) Log.w(TAG, "gif.onBind() showing progress: " + progress);
            progressView.setVisibility(View.VISIBLE);
            progressView.setProgress(progress);
        } else {
            if (debug) Log.w(TAG, "gif.onBind() no progressView. bytes: " + downloadProgressBytes);
            progressView.setVisibility(View.GONE);
        }
        
        ShapedFrameLayout cellCustom = (ShapedFrameLayout) (myMessage ? imageContainerMy : imageContainerTheir);
        // clustering
        cellCustom.setCornerRadiusDp(16, 16, 16, 16);
        if (!AtlasMessagesList.CLUSTERED_BUBBLES) return rootView;
        if (myMessage) {
            if (this.clusterHeadItemId == this.clusterItemId && !this.clusterTail) {
                cellCustom.setCornerRadiusDp(16, 16, 2, 16);
            } else if (this.clusterTail && this.clusterHeadItemId != this.clusterItemId) {
                cellCustom.setCornerRadiusDp(16, 2, 16, 16);
            } else if (this.clusterHeadItemId != this.clusterItemId && !this.clusterTail) {
                cellCustom.setCornerRadiusDp(16, 2, 2, 16);
            }
        } else {
            if (this.clusterHeadItemId == this.clusterItemId && !this.clusterTail) {
                cellCustom.setCornerRadiusDp(16, 16, 16, 2);
            } else if (this.clusterTail && this.clusterHeadItemId != this.clusterItemId) {
                cellCustom.setCornerRadiusDp(2, 16, 16, 16);
            } else if (this.clusterHeadItemId != this.clusterItemId && !this.clusterTail) {
                cellCustom.setCornerRadiusDp(2, 16, 16, 2);
            }
        }
        return rootView;
    }
    
    // LayerDownloadListener (when downloading part)
    public void onProgressStart(MessagePart part, Operation operation) {
    }
    public void onProgressUpdate(MessagePart part, Operation operation, long transferredBytes) {
        MessagePart workingPart = previewPart != null ? previewPart : fullPart;
        if (debug) Log.w(TAG, "onProgressUpdate() transferred: " + transferredBytes + " of " + workingPart.getSize() + ", progress: " + (1.0f * transferredBytes / workingPart.getSize()));
        downloadProgressBytes = transferredBytes;
        messagesList.requestRefresh();
    }
    public void onProgressError(MessagePart part, Operation operation, Throwable cause) {
        downloadProgressBytes = -1;
        messagesList.requestRefresh();
    }
    public void onProgressComplete(MessagePart part, Operation operation) {
        downloadProgressBytes = -1;
        messagesList.requestRefresh();
    }
    
    @Override
    public void onImageLoaded(ImageSpec spec) {
        messagesList.requestRefresh();
    }
}
