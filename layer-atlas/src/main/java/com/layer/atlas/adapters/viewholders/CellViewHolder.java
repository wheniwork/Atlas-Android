package com.layer.atlas.adapters.viewholders;

import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
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

  protected Message mMessage;

  // View cache
  protected TextView mUserName;
  protected TextView mSentAt;
  protected View mTimeGroup;
  protected TextView mTimeGroupDay;
  protected TextView mTimeGroupTime;
  protected Space mClusterSpaceGap;
  protected AtlasAvatar mAvatar;
  protected ViewGroup mCell;
  protected TextView mReceipt;

  // Cell
  protected AtlasCellFactory.CellHolder mCellHolder;
  protected AtlasCellFactory.CellHolderSpecs mCellHolderSpecs;

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

  /**
   * Override to customize the read receipt message. By default, this will only show read receipts
   * for messages sent by the current user.
   * @param resource String resource to use for receipt message
   */
  public void setReadReceiptText(@StringRes int resource) {
    if (mCellHolderSpecs.isMe) {
      mReceipt.setVisibility(View.VISIBLE);
      mReceipt.setText(resource);
    } else {
      mReceipt.setVisibility(View.GONE);
    }
  }

  /**
   * Override to customize the delivered receipt message. By default, this will only show delivered
   * receipts for messages sent by the current user.
   * @param resource String resource to use for delivered message
   */
  public void setDeliveredReceiptText(@StringRes int resource) {
    if (mCellHolderSpecs.isMe) {
      mReceipt.setVisibility(View.VISIBLE);
      mReceipt.setText(resource);
    } else {
      mReceipt.setVisibility(View.GONE);
    }
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

  protected int getReadCount() {
    int readCount = 0;
    for (Message.RecipientStatus status : mMessage.getRecipientStatus().values()) {
      if (status == Message.RecipientStatus.READ) {
        readCount++;
      }
    }
    return readCount;
  }
}
