package com.layer.atlas.adapters.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.layer.atlas.R;

/**
 * Base {@link com.layer.atlas.adapters.AtlasMessagesAdapter} view holder
 */
public class ViewHolder extends RecyclerView.ViewHolder {
    public final static int RESOURCE_ID_FOOTER = R.layout.atlas_message_item_footer;

    // View cache
    protected ViewGroup mRoot;

    public ViewHolder(View itemView) {
        super(itemView);
        mRoot = (ViewGroup) itemView.findViewById(R.id.swipeable);
    }

    public ViewGroup getRoot() {
        return mRoot;
    }
}
