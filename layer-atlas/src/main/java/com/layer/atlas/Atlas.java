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

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Map;

import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

/**
 * @author Oleg Orlov
 * @since 12 May 2015
 */
public class Atlas {

    public static final String METADATA_KEY_CONVERSATION_TITLE = "conversationName";
    
    public static final String MIME_TYPE_ATLAS_LOCATION = "location/coordinate";
    public static final String MIME_TYPE_TEXT = "text/plain";
    public static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_IMAGE_JPEG_PREVIEW = "image/jpeg+preview";
    public static final String MIME_TYPE_IMAGE_PNG = "image/png";
    public static final String MIME_TYPE_IMAGE_PNG_PREVIEW = "image/png+preview";
    public static final String MIME_TYPE_IMAGE_DIMENSIONS = "application/json+imageSize";

    public static String getInitials(Participant p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.getFirstName() != null && p.getFirstName().trim().length() > 0 ? p.getFirstName().trim().charAt(0) : "");
        sb.append(p.getLastName() != null && p.getLastName().trim().length() > 0 ? p.getLastName().trim().charAt(0) : "");
        return sb.toString();
    }

    public static String getFirstNameLastInitial(Participant p) {
        StringBuilder sb = new StringBuilder();
        if (p.getFirstName() != null && p.getFirstName().trim().length() > 0) {
            sb.append(p.getFirstName().trim());
        }
        if (p.getLastName() != null && p.getLastName().trim().length() > 0) {
            sb.append(" ").append(p.getLastName().trim().charAt(0));
            sb.append(".");
        }
        return sb.toString();
    }

    public static String getFullName(Participant p) {
        StringBuilder sb = new StringBuilder();
        if (p.getFirstName() != null && p.getFirstName().trim().length() > 0) {
            sb.append(p.getFirstName().trim());
    }
        if (p.getLastName() != null && p.getLastName().trim().length() > 0) {
            sb.append(" ").append(p.getLastName().trim());
        }
        return sb.toString();
    }

    public static void setTitle(Conversation conversation, String title) {
        conversation.putMetadataAtKeyPath(Atlas.METADATA_KEY_CONVERSATION_TITLE, title);
    }

    public static String getTitle(Conversation conversation) {
        return (String) conversation.getMetadata().get(Atlas.METADATA_KEY_CONVERSATION_TITLE);
    }

    public static String getTitle(Conversation conversation, ParticipantProvider provider, String userId) {
        String conversationTitle = getTitle(conversation);
        if (conversationTitle != null && conversationTitle.trim().length() > 0) return conversationTitle.trim();

        StringBuilder sb = new StringBuilder();
        for (String participantId : conversation.getParticipants()) {
            if (participantId.equals(userId)) continue;
            Participant participant = provider.getParticipant(participantId);
            if (participant == null) continue;
            String initials = conversation.getParticipants().size() > 2 ? getFirstNameLastInitial(participant) : getFullName(participant);
            if (sb.length() > 0) sb.append(", ");
            sb.append(initials);
        }
        return sb.toString().trim();
    }

    public static final class Tools {
        public static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm a"); // TODO: localization required
        public static final SimpleDateFormat sdfDayOfWeek = new SimpleDateFormat("EEEE, LLL dd,"); // TODO: localization required
        
        public static String toString(Message msg) {
            StringBuilder sb = new StringBuilder();
            for (MessagePart mp : msg.getMessageParts()) {
                if ("text/plain".equals(mp.getMimeType())) {
                    sb.append(new String(mp.getData()));
                } else {
                    sb.append("Attachment: Image");
                    break;
                }
            }
            return sb.toString();
        }

        public static float[] getRoundRectRadii(float[] cornerRadiusDp, final DisplayMetrics displayMetrics) {
            float[] result = new float[8];
            for (int i = 0; i < cornerRadiusDp.length; i++) {
                result[i * 2] = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cornerRadiusDp[i], displayMetrics);
                result[i * 2 + 1] = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cornerRadiusDp[i], displayMetrics);
            }
            return result;
        }
    }

    /**
     * Participant allows Atlas classes to display information about users, like Message senders,
     * Conversation participants, TypingIndicator users, etc.
     */
    public interface Participant {
        /**
         * Returns the first name of this Participant.
         * 
         * @return The first name of this Participant
         */
        String getFirstName();

        /**
         * Returns the last name of this Participant.
         *
         * @return The last name of this Participant
         */
        String getLastName();
    }

    /**
     * ParticipantProvider provides Atlas classes with Participant data.
     */
    public interface ParticipantProvider {
        /**
         * Returns a map of all Participants by their unique ID who match the provided `filter`, or
         * all Participants if `filter` is `null`.  If `result` is provided, it is operated on and
         * returned.  If `result` is `null`, a new Map is created and returned.
         *
         * @param filter The filter to apply to Participants
         * @param result The Map to operate on
         * @return A Map of all matching Participants keyed by ID.
         */
        Map<String, Participant> getParticipants(String filter, Map<String, Participant> result);

        /**
         * Returns the Participant with the given ID, or `null` if the participant is not yet
         * available.
         *
         * @return The Participant with the given ID, or `null` if not available.
         */
        Atlas.Participant getParticipant(String userId);
    }

    public static final class FilteringComparator implements Comparator<Atlas.Participant> {
        private final String filter;
    
        /**
         * @param filter - the less indexOf(filter) the less order of participant
         */
        public FilteringComparator(String filter) {
            this.filter = filter;
        }
    
        @Override
        public int compare(Atlas.Participant lhs, Atlas.Participant rhs) {
            int result = subCompareCaseInsensitive(lhs.getFirstName(), rhs.getFirstName());
            if (result != 0) return result;
            return subCompareCaseInsensitive(lhs.getLastName(), rhs.getLastName());
        }
    
        private int subCompareCaseInsensitive(String lhs, String rhs) {
            int left = lhs != null ? lhs.toLowerCase().indexOf(filter) : -1;
            int right = rhs != null ? rhs.toLowerCase().indexOf(filter) : -1;
    
            if (left == -1 && right == -1) return 0;
            if (left != -1 && right == -1) return -1;
            if (left == -1 && right != -1) return 1;
            if (left - right != 0) return left - right;
            return String.CASE_INSENSITIVE_ORDER.compare(lhs, rhs);
        }
    }
    
}
