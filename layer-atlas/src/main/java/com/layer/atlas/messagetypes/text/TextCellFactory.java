package com.layer.atlas.messagetypes.text;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.R;
import com.layer.atlas.messagetypes.AtlasCellFactory;
import com.layer.atlas.provider.Participant;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.atlas.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Actor;
import com.layer.sdk.messaging.Message;

public class TextCellFactory extends AtlasCellFactory<TextCellFactory.CellHolder, TextCellFactory.TextInfo> implements View.OnLongClickListener {
    public final static String MIME_TYPE = "text/plain";

    public TextCellFactory() {
        super(256 * 1024);
    }

    public static boolean isType(Message message) {
        return message.getMessageParts().get(0).getMimeType().equals(MIME_TYPE);
    }

    public static String getMessagePreview(Context context, Message message) {
        return new String(message.getMessageParts().get(0).getData());
    }

    @Override
    public boolean isBindable(Message message) {
        return TextCellFactory.isType(message);
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        Context context = cellView.getContext();

        View v = layoutInflater.inflate(R.layout.atlas_message_item_cell_text, cellView, true);
        v.setBackgroundResource(isMe ? R.drawable.atlas_message_item_cell_me : R.drawable.atlas_message_item_cell_them);

        TextView t = (TextView) v.findViewById(R.id.cell_text);
        t.setTextColor(context.getResources().getColor(isMe ? R.color.atlas_text_white : R.color.atlas_text_black));
        return new CellHolder(v);
    }

    @Override
    public TextInfo parseContent(LayerClient layerClient, ParticipantProvider participantProvider, Message message) {
        String text = new String(message.getMessageParts().get(0).getData());
        String name;
        Actor sender = message.getSender();
        if (sender.getName() != null) {
            name = sender.getName() + ": ";
        } else {
            Participant participant = participantProvider.getParticipant(sender.getUserId());
            name = participant == null ? "" : (participant.getName() + ": ");
        }
        return new TextInfo(text, name);
    }

    @Override
    public void bindCellHolder(CellHolder cellHolder, final TextInfo parsed, Message message, CellHolderSpecs specs) {
        cellHolder.mTextView.setText(parsed.getString());
        cellHolder.mTextView.setTag(parsed);
        cellHolder.mTextView.setOnLongClickListener(this);
    }

    /**
     * Long click copies message text and sender name to clipboard
     */
    @Override
    public boolean onLongClick(View v) {
        TextInfo parsed = (TextInfo) v.getTag();
        String text = parsed.getClipboardPrefix() + parsed.getString();
        Util.copyToClipboard(v.getContext(), R.string.atlas_text_cell_factory_clipboard_description, text);
        Toast.makeText(v.getContext(), R.string.atlas_text_cell_factory_copied_to_clipboard, Toast.LENGTH_SHORT).show();
        return true;
    }

    public static class CellHolder extends AtlasCellFactory.CellHolder {
        TextView mTextView;

        public CellHolder(View view) {
            mTextView = (TextView) view.findViewById(R.id.cell_text);
        }
    }

    public static class TextInfo implements AtlasCellFactory.ParsedContent {
        private final String mString;
        private final String mClipboardPrefix;
        private final int mSize;

        public TextInfo(String string, String clipboardPrefix) {
            mString = string;
            mClipboardPrefix = clipboardPrefix;
            mSize = mString.getBytes().length + mClipboardPrefix.getBytes().length;
        }

        public String getString() {
            return mString;
        }

        public String getClipboardPrefix() {
            return mClipboardPrefix;
        }

        @Override
        public int sizeOf() {
            return mSize;
        }
    }
}
