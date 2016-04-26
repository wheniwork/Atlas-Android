package com.layer.atlas.messagetypes.threepartimage;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.layer.atlas.R;
import com.layer.atlas.messagetypes.AtlasCellFactory;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.atlas.util.Log;
import com.layer.atlas.util.Util;
import com.layer.atlas.util.imagepopup.AtlasImagePopupActivity;
import com.layer.atlas.util.picasso.transformations.RoundedTransform;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * ThreePartImage handles image Messages with three parts: full image, preview image, and
 * image metadata.  The image metadata contains full image dimensions and rotation information used
 * for sizing and rotating images efficiently.
 */
public class ThreePartImageCellFactory extends AtlasCellFactory<ThreePartImageCellFactory.CellHolder, ThreePartImageCellFactory.Info> implements View.OnClickListener {
    private static final String PICASSO_TAG = ThreePartImageCellFactory.class.getSimpleName();

    private static final int PLACEHOLDER = R.drawable.atlas_message_item_cell_placeholder;

    private final WeakReference<Activity> mActivity;
    private final LayerClient mLayerClient;
    private final Picasso mPicasso;
    private final Transformation mTransform;

    public ThreePartImageCellFactory(Activity activity, LayerClient layerClient, Picasso picasso) {
        super(256 * 1024);
        mActivity = new WeakReference<Activity>(activity);
        mLayerClient = layerClient;
        mPicasso = picasso;
        float radius = activity.getResources().getDimension(com.layer.atlas.R.dimen.atlas_message_item_cell_radius);
        mTransform = new RoundedTransform(radius);
    }

    @Override
    public boolean isBindable(Message message) {
        return ThreePartImageCellFactory.isType(message);
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        return new CellHolder(layoutInflater.inflate(R.layout.atlas_message_item_cell_image, cellView, true));
    }

    @Override
    public Info parseContent(LayerClient layerClient, ParticipantProvider participantProvider, Message message) {
        return getInfo(message);
    }

    @Override
    public void bindCellHolder(final CellHolder cellHolder, final Info info, final Message message, CellHolderSpecs specs) {
        cellHolder.mImageView.setTag(info);
        cellHolder.mImageView.setOnClickListener(this);
        MessagePart preview = ThreePartImageUtils.getPreviewPart(message);

        // Info width and height are the rotated width and height, though the content is not pre-rotated.
        int[] cellDims = Util.scaleDownInside(info.width, info.height, specs.maxWidth, specs.maxHeight);
        ViewGroup.LayoutParams params = cellHolder.mImageView.getLayoutParams();
        params.width = cellDims[0];
        params.height = cellDims[1];
        cellHolder.mProgressBar.show();
        RequestCreator creator = mPicasso.load(preview.getId()).tag(PICASSO_TAG).placeholder(PLACEHOLDER);
        switch (info.orientation) {
            case ThreePartImageUtils.ORIENTATION_0:
                creator.resize(cellDims[0], cellDims[1]);
                break;
            case ThreePartImageUtils.ORIENTATION_90:
                creator.resize(cellDims[1], cellDims[0]).rotate(-90);
                break;
            case ThreePartImageUtils.ORIENTATION_180:
                creator.resize(cellDims[0], cellDims[1]).rotate(180);
                break;
            default:
                creator.resize(cellDims[1], cellDims[0]).rotate(90);
                break;
        }
        creator.centerCrop().transform(mTransform).into(cellHolder.mImageView, new Callback() {
            @Override
            public void onSuccess() {
                cellHolder.mProgressBar.hide();
            }

            @Override
            public void onError() {
                cellHolder.mProgressBar.hide();
            }
        });

        cellHolder.mImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                MessagePart full = ThreePartImageUtils.getFullPart(message);
                MessagePart preview = ThreePartImageUtils.getPreviewPart(message);
                MessagePart info = ThreePartImageUtils.getInfoPart(message);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                BitmapFactory.decodeStream(full.getDataStream(), null, options);
                Log.v("Full size: " + options.outWidth + "x" + options.outHeight);

                BitmapFactory.decodeStream(preview.getDataStream(), null, options);
                Log.v("Preview size: " + options.outWidth + "x" + options.outHeight);

                Log.v("Info: " + new String(info.getData()));

                return false;
            }
        });
    }

    @Override
    public void onClick(View v) {
        AtlasImagePopupActivity.init(mLayerClient);
        Activity activity = mActivity.get();
        if (activity == null) return;
        Info info = (Info) v.getTag();
        Intent intent = new Intent(activity, AtlasImagePopupActivity.class);
        intent.putExtra("previewId", info.previewPartId);
        intent.putExtra("fullId", info.fullPartId);
        intent.putExtra("info", info);

        if (Build.VERSION.SDK_INT >= 21) {
            activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity, v, "image").toBundle());
        } else {
            activity.startActivity(intent);
        }
    }

    @Override
    public void onScrollStateChanged(int newState) {
        switch (newState) {
            case RecyclerView.SCROLL_STATE_DRAGGING:
                mPicasso.pauseTag(PICASSO_TAG);
                break;
            case RecyclerView.SCROLL_STATE_IDLE:
            case RecyclerView.SCROLL_STATE_SETTLING:
                mPicasso.resumeTag(PICASSO_TAG);
                break;
        }
    }


    //==============================================================================================
    // Static utilities
    //==============================================================================================

    public static boolean isType(Message message) {
        List<MessagePart> parts = message.getMessageParts();
        return parts.size() == 3 &&
                parts.get(ThreePartImageUtils.PART_INDEX_FULL).getMimeType().startsWith("image/") &&
                parts.get(ThreePartImageUtils.PART_INDEX_PREVIEW).getMimeType().equals(ThreePartImageUtils.MIME_TYPE_PREVIEW) &&
                parts.get(ThreePartImageUtils.PART_INDEX_INFO).getMimeType().equals(ThreePartImageUtils.MIME_TYPE_INFO);
    }

    public static String getMessagePreview(Context context, Message message) {
        return context.getString(R.string.atlas_message_preview_image);
    }

    public static Info getInfo(Message message) {
        try {
            Info info = new Info();
            JSONObject infoObject = new JSONObject(new String(ThreePartImageUtils.getInfoPart(message).getData()));
            info.orientation = infoObject.getInt("orientation");
            info.width = infoObject.getInt("width");
            info.height = infoObject.getInt("height");
            info.previewPartId = ThreePartImageUtils.getPreviewPart(message).getId();
            info.fullPartId = ThreePartImageUtils.getFullPart(message).getId();
            return info;
        } catch (JSONException e) {
            if (Log.isLoggable(Log.ERROR)) {
                Log.e(e.getMessage(), e);
            }
        }
        return null;
    }


    //==============================================================================================
    // Inner classes
    //==============================================================================================

    public static class Info implements AtlasCellFactory.ParsedContent, Parcelable {
        public int orientation;
        public int width;
        public int height;
        public Uri fullPartId;
        public Uri previewPartId;

        @Override
        public int sizeOf() {
            return ((Integer.SIZE + Integer.SIZE + Integer.SIZE) / Byte.SIZE) + fullPartId.toString().getBytes().length + previewPartId.toString().getBytes().length;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(orientation);
            dest.writeInt(width);
            dest.writeInt(height);
        }

        public static final Parcelable.Creator<Info> CREATOR
                = new Parcelable.Creator<Info>() {
            public Info createFromParcel(Parcel in) {
                Info info = new Info();
                info.orientation = in.readInt();
                info.width = in.readInt();
                info.height = in.readInt();
                return info;
            }

            public Info[] newArray(int size) {
                return new Info[size];
            }
        };
    }

    public static class CellHolder extends AtlasCellFactory.CellHolder {
        ImageView mImageView;
        ContentLoadingProgressBar mProgressBar;

        public CellHolder(View view) {
            mImageView = (ImageView) view.findViewById(R.id.cell_image);
            mProgressBar = (ContentLoadingProgressBar) view.findViewById(R.id.cell_progress);
        }
    }
}
