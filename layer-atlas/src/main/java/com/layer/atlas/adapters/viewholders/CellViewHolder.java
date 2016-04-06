package com.layer.atlas.adapters.viewholders;

import android.support.annotation.IdRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Space;
import android.widget.TextView;

import com.layer.atlas.AtlasAvatar;
import com.layer.atlas.R;
import com.layer.atlas.adapters.AtlasMessagesAdapter;
import com.layer.atlas.messagetypes.AtlasCellFactory;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.sdk.messaging.Message;
import com.squareup.picasso.Picasso;

/**
 * {@link android.support.v7.widget.RecyclerView.ViewHolder} for displaying {@link Message}s in
 * {@link AtlasMessagesAdapter}. Can be extended to customize Atlas with your own message layouts
 */
public class CellViewHolder extends ViewHolder {

  private Message mMessage;

  // View cache
  private TextView mUserName;
  private TextView mSentAt;
  private View mTimeGroup;
  private TextView mTimeGroupDay;
  private TextView mTimeGroupTime;
  private Space mClusterSpaceGap;
  private AtlasAvatar mAvatar;
  private ViewGroup mCell;
  private TextView mReceipt;

  // Cell
  private AtlasCellFactory.CellHolder mCellHolder;
  private AtlasCellFactory.CellHolderSpecs mCellHolderSpecs;

  public CellViewHolder(View itemView, ParticipantProvider participantProvider, Picasso picasso) {
    this(itemView, participantProvider, picasso, R.id.sender, R.id.sent_at, R.id.time_group,
         R.id.time_group_day, R.id.time_group_time, R.id.cluster_space, R.id.cell, R.id.receipt, R.id.avatar);
  }

  /**
   * Override in subclasses to specify view ids specific to your custom cell layouts
   */
  protected CellViewHolder(View itemView, ParticipantProvider participantProvider, Picasso picasso,
                           @IdRes int senderId, @IdRes int sentAtId,
                           @IdRes int timeGroupId, @IdRes int timeGroupDayId,
                           @IdRes int timeGroupTimeId, @IdRes int clusterSpaceGapId,
                           @IdRes int cellId, @IdRes int receiptId,
                           @IdRes int avatarId) {
    super(itemView);
    mUserName = (TextView) itemView.findViewById(senderId);
    mSentAt = (TextView) itemView.findViewById(sentAtId);
    mTimeGroup = itemView.findViewById(timeGroupId);
    mTimeGroupDay = (TextView) itemView.findViewById(timeGroupDayId);
    mTimeGroupTime = (TextView) itemView.findViewById(timeGroupTimeId);
    mClusterSpaceGap = (Space) itemView.findViewById(clusterSpaceGapId);
    mCell = (ViewGroup) itemView.findViewById(cellId);
    mReceipt = (TextView) itemView.findViewById(receiptId);

    mAvatar = ((AtlasAvatar) itemView.findViewById(avatarId));
    if (mAvatar != null) mAvatar.init(participantProvider, picasso);
  }

  public Message getMessage() {
    return mMessage;
  }

  public void setMessage(Message message) {
    this.mMessage = message;
  }

  public TextView getUserName() {
    return mUserName;
  }

  public TextView getSentAt() {
    return mSentAt;
  }

  public View getTimeGroup() {
    return mTimeGroup;
  }

  public TextView getTimeGroupDay() {
    return mTimeGroupDay;
  }

  public TextView getTimeGroupTime() {
    return mTimeGroupTime;
  }

  public Space getClusterSpaceGap() {
    return mClusterSpaceGap;
  }

  public AtlasAvatar getAvatar() {
    return mAvatar;
  }

  public ViewGroup getCell() {
    return mCell;
  }

  public TextView getReceipt() {
    return mReceipt;
  }

  public AtlasCellFactory.CellHolder getCellHolder() {
    return mCellHolder;
  }

  public void setCellHolder(AtlasCellFactory.CellHolder cellHolder) {
    this.mCellHolder = cellHolder;
  }

  public AtlasCellFactory.CellHolderSpecs getCellHolderSpecs() {
    return mCellHolderSpecs;
  }

  public void setCellHolderSpecs(AtlasCellFactory.CellHolderSpecs cellHolderSpecs) {
    this.mCellHolderSpecs = cellHolderSpecs;
  }
}
