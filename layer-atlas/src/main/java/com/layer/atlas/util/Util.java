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
package com.layer.atlas.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;

import com.layer.atlas.BuildConfig;
import com.layer.atlas.R;
import com.layer.atlas.messagetypes.generic.GenericCellFactory;
import com.layer.atlas.messagetypes.location.LocationCellFactory;
import com.layer.atlas.messagetypes.singlepartimage.SinglePartImageCellFactory;
import com.layer.atlas.messagetypes.text.TextCellFactory;
import com.layer.atlas.messagetypes.threepartimage.ThreePartImageCellFactory;
import com.layer.atlas.provider.Participant;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.layer.sdk.query.Queryable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Util {
    private static final String METADATA_KEY_CONVERSATION_TITLE = "conversationName";
    private static final int TIME_HOURS_24 = 24 * 60 * 60 * 1000;
    private static final SimpleDateFormat DAY_OF_WEEK = new SimpleDateFormat("EEE, LLL dd,", Locale.US);

    /**
     * Returns the app version name.
     *
     * @return The app version name.
     */
    public static String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public static void copyToClipboard(Context context, int stringResId, String content) {
        copyToClipboard(context, context.getString(stringResId), content);
    }

    public static void copyToClipboard(Context context, String description, String content) {
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = new ClipData(description, new String[]{"text/plain"}, new ClipData.Item(content));
        manager.setPrimaryClip(clipData);
    }

    // TODO: base this on registered types
    public static String getLastMessageString(Context context, Message message) {
        if (TextCellFactory.isType(message)) {
            return TextCellFactory.getMessagePreview(context, message);
        }
        if (ThreePartImageCellFactory.isType(message)) {
            return ThreePartImageCellFactory.getMessagePreview(context, message);
        }
        if (LocationCellFactory.isType(message)) {
            return LocationCellFactory.getMessagePreview(context, message);
        }
        if (SinglePartImageCellFactory.isType(message)) {
            return SinglePartImageCellFactory.getMessagePreview(context, message);
        }
        return GenericCellFactory.getPreview(context, message);
    }

    public static String getConversationTitle(LayerClient client, ParticipantProvider provider, Conversation conversation) {
        String metadataTitle = getConversationMetadataTitle(conversation);
        if (metadataTitle != null) return metadataTitle.trim();

        StringBuilder sb = new StringBuilder();
        String userId = client.getAuthenticatedUserId();
        for (String participantId : conversation.getParticipants()) {
            if (participantId.equals(userId)) continue;
            Participant participant = provider.getParticipant(participantId);
            if (participant == null) continue;
            String initials = conversation.getParticipants().size() > 2 ? getInitials(participant) : participant.getName();
            if (sb.length() > 0) sb.append(", ");
            sb.append(initials);
        }
        return sb.toString().trim();
    }

    public static String getConversationMetadataTitle(Conversation conversation) {
        String metadataTitle = (String) conversation.getMetadata().get(METADATA_KEY_CONVERSATION_TITLE);
        if (metadataTitle != null && !metadataTitle.trim().isEmpty()) return metadataTitle.trim();
        return null;
    }

    public static void setConversationMetadataTitle(Conversation conversation, String title) {
        if (title == null || title.trim().isEmpty()) {
            conversation.removeMetadataAtKeyPath(METADATA_KEY_CONVERSATION_TITLE);
        } else {
            conversation.putMetadataAtKeyPath(METADATA_KEY_CONVERSATION_TITLE, title.trim());
        }
    }

    public static String getInitials(Participant p) {
        return getInitials(p.getName());
    }

    public static String getInitials(String fullName) {
        if(fullName == null || fullName.isEmpty()) return "";
        if (fullName.contains(" ")) {
            String[] names = fullName.split(" ");
            int count = 0;
            StringBuilder b = new StringBuilder();
            for (String name : names) {
                String t = name.trim();
                if (t.isEmpty()) continue;
                b.append(("" + t.charAt(0)).toUpperCase());
                if (++count >= 2) break;
            }
            return b.toString();
        } else {
            return ("" + fullName.trim().charAt(0)).toUpperCase();
        }
    }

    public static String formatTime(Context context, Date date, DateFormat timeFormat, DateFormat dateFormat) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long todayMidnight = cal.getTimeInMillis();
        long yesterMidnight = todayMidnight - TIME_HOURS_24;
        long weekAgoMidnight = todayMidnight - TIME_HOURS_24 * 7;

        String timeText;
        if (date.getTime() > todayMidnight) {
            timeText = timeFormat.format(date.getTime());
        } else if (date.getTime() > yesterMidnight) {
            timeText = context.getString(R.string.atlas_time_yesterday);
        } else if (date.getTime() > weekAgoMidnight) {
            cal.setTime(date);
            timeText = context.getResources().getStringArray(R.array.atlas_time_days_of_week)[cal.get(Calendar.DAY_OF_WEEK) - 1];
        } else {
            timeText = dateFormat.format(date);
        }
        return timeText;
    }

    /**
     * Returns Today, Yesterday, the day of the week within one week, or a date if greater.
     *
     * @param context
     * @param date
     * @return
     */
    public static String formatTimeDay(Context context, Date date) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long todayMidnight = cal.getTimeInMillis();
        long yesterdayMidnight = todayMidnight - TIME_HOURS_24;
        long weekAgoMidnight = todayMidnight - TIME_HOURS_24 * 7;

        String timeBarDayText;
        if (date.getTime() > todayMidnight) {
            timeBarDayText = context.getString(R.string.atlas_time_today);
        } else if (date.getTime() > yesterdayMidnight) {
            timeBarDayText = context.getString(R.string.atlas_time_yesterday);
        } else if (date.getTime() > weekAgoMidnight) {
            cal.setTime(date);
            timeBarDayText = context.getResources().getStringArray(R.array.atlas_time_days_of_week)[cal.get(Calendar.DAY_OF_WEEK) - 1];
        } else {
            timeBarDayText = DAY_OF_WEEK.format(date);
        }
        return timeBarDayText;
    }

    /**
     * Returns int[] {scaledWidth, scaledHeight} for dimensions that fit within the given maxWidth,
     * maxHeight at the given inWidth, inHeight aspect ratio.  If the in dimensions fit fully inside
     * the max dimensions, no scaling is applied.  Otherwise, at least one scaled dimension is set
     * to a max dimension, and the other scaled dimension is scaled to fit.
     *
     * @param inWidth
     * @param inHeight
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    public static int[] scaleDownInside(int inWidth, int inHeight, int maxWidth, int maxHeight) {
        int scaledWidth;
        int scaledHeight;
        if (inWidth <= maxWidth && inHeight <= maxHeight) {
            scaledWidth = inWidth;
            scaledHeight = inHeight;
        } else {
            double widthRatio = (double) inWidth / (double) maxWidth;
            double heightRatio = (double) inHeight / (double) maxHeight;
            if (widthRatio > heightRatio) {
                scaledWidth = maxWidth;
                scaledHeight = (int) Math.round((double) inHeight / widthRatio);
            } else {
                scaledHeight = maxHeight;
                scaledWidth = (int) Math.round((double) inWidth / heightRatio);
            }
        }
        return new int[]{scaledWidth, scaledHeight};
    }

    /**
     * Synchronously starts downloading the given MessagePart and waits for downloading to complete.
     * Returns `true` if the MessagePart downloaded successfully within the given period of time, or
     * `false` otherwise.
     *
     * @param layerClient LayerClient to download the MessagePart with.
     * @param part        MessagePart to download.
     * @param timeLength  Length of time to wait for downloading.
     * @param timeUnit    Unit of time to wait for downloading.
     * @return `true` if the MessagePart content is available, or `false` otherwise.
     */
    public static boolean downloadMessagePart(LayerClient layerClient, MessagePart part, int timeLength, TimeUnit timeUnit) {
        if (part.isContentReady()) return true;

        final CountDownLatch latch = new CountDownLatch(1);
        final LayerProgressListener listener = new LayerProgressListener.BackgroundThread.Weak() {
            @Override
            public void onProgressStart(MessagePart messagePart, Operation operation) {

            }

            @Override
            public void onProgressUpdate(MessagePart messagePart, Operation operation, long l) {

            }

            @Override
            public void onProgressComplete(MessagePart messagePart, Operation operation) {
                latch.countDown();
            }

            @Override
            public void onProgressError(MessagePart messagePart, Operation operation, Throwable throwable) {
                latch.countDown();
            }
        };
        part.download(listener);
        if (!part.isContentReady()) {
            try {
                latch.await(timeLength, timeUnit);
            } catch (InterruptedException e) {
                if (Log.isLoggable(Log.ERROR)) {
                    Log.e(e.getMessage(), e);
                }
            }
        }
        return part.isContentReady();
    }

    /**
     * Asynchronously deauthenticates with Layer.
     *
     * @param layerClient LayerClient to deauthenticate.
     * @param callback    Callback to report deauthentication success and failure.
     */
    public static void deauthenticate(LayerClient layerClient, final DeauthenticationCallback callback) {
        final AtomicBoolean alerted = new AtomicBoolean(false);
        final LayerAuthenticationListener listener = new LayerAuthenticationListener.BackgroundThread() {
            @Override
            public void onAuthenticated(LayerClient layerClient, String s) {

            }

            @Override
            public void onDeauthenticated(LayerClient layerClient) {
                if (alerted.compareAndSet(false, true)) {
                    callback.onDeauthenticationSuccess(layerClient);
                }
            }

            @Override
            public void onAuthenticationChallenge(LayerClient layerClient, String s) {

            }

            @Override
            public void onAuthenticationError(LayerClient layerClient, LayerException e) {
                if (alerted.compareAndSet(false, true)) {
                    callback.onDeauthenticationFailed(layerClient, e.getMessage());
                }
            }
        };
        layerClient.registerAuthenticationListener(listener);
        if (!layerClient.isAuthenticated()) {
            layerClient.unregisterAuthenticationListener(listener);
            if (alerted.compareAndSet(false, true)) {
                callback.onDeauthenticationSuccess(layerClient);
            }
            return;
        }
        layerClient.deauthenticate();
    }

    public interface ContentAvailableCallback {
        void onContentAvailable(LayerClient client, Queryable object);

        void onContentFailed(LayerClient client, Uri objectId, String reason);
    }


    public interface DeauthenticationCallback {
        void onDeauthenticationSuccess(LayerClient client);

        void onDeauthenticationFailed(LayerClient client, String reason);
    }
}