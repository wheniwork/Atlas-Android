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
package com.layer.atlas.messenger.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.layer.atlas.Atlas;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ParticipantProvider implements Atlas.ParticipantProvider {
    private final static String TAG = ParticipantProvider.class.getSimpleName();
    private final Map<String, Participant> mParticipantMap = new HashMap<String, Participant>();
    private final Callback mCallback;
    private final SharedPreferences mSharedPreferences;
    private final AtomicInteger mRefreshRequestCounter = new AtomicInteger(0);

    public ParticipantProvider(Context context, Callback callback) {
        mCallback = callback;
        mSharedPreferences = context.getSharedPreferences("contacts", Context.MODE_PRIVATE);
        load();
    }

    /**
     * Refreshes all Contacts from the ContactProvider.Callback.
     */
    public synchronized void refresh() {
        // TODO: consider a time-based limiter to prevent rapid requests, or intelligent refresh
        int requests = mRefreshRequestCounter.get();
        if (requests > 1) return;   // Two covers race conditions.
        mRefreshRequestCounter.incrementAndGet();


        (new AsyncTask<Void, Void, List<Participant>>() {
            @Override
            protected List<Participant> doInBackground(Void... params) {
                List<Participant> participants = mCallback.getAllParticipants();
                if (participants != null) set(participants);
                return participants;
            }

            @Override
            protected void onPostExecute(List<Participant> participant) {
                mRefreshRequestCounter.decrementAndGet();
            }
        }).execute();
    }

    /**
     * Overwrites the current list of Contacts with the provided list.
     *
     * @param participants New list of Contacts to apply.
     */
    public void set(List<Participant> participants) {
        synchronized (mParticipantMap) {
            mParticipantMap.clear();
            for (Participant participant : participants) {
                mParticipantMap.put(participant.userId, participant);
            }
        }
        save();
    }

    /**
     * Returns all cached Contacts.
     *
     * @return All cachec Contacts.
     */
    private List<Participant> getAll() {
        synchronized (mParticipantMap) {
            return new ArrayList<Participant>(mParticipantMap.values());
        }
    }

    /**
     * Returns the cached Contact with the given ID, or `null`.  If the contact is not cached,
     * refresh() is called.
     *
     * @param id The ID of the Contact to get or fetch.
     * @return The Contact with the given ID if it is cached, or null.
     */
    public Participant get(String id) {
        synchronized (mParticipantMap) {
            Participant participant = mParticipantMap.get(id);
            if (participant != null) {
                return participant;
            }
        }
        refresh();
        return null;
    }

    private boolean load() {
        String jsonString = mSharedPreferences.getString("json", null);
        if (jsonString == null) return false;

        List<Participant> participants;
        try {
            JSONArray contactsJson = new JSONArray(jsonString);
            participants = new ArrayList<Participant>(contactsJson.length());
            for (int i = 0; i < contactsJson.length(); i++) {
                JSONObject contactJson = contactsJson.getJSONObject(i);
                Participant participant = new Participant();
                participant.userId = contactJson.optString("id");
                participant.firstName = contactJson.optString("first_name");
                participant.lastName = contactJson.optString("last_name");
                participants.add(participant);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error while saving", e);
            return false;
        }

        synchronized (mParticipantMap) {
            mParticipantMap.clear();
            for (Participant participant : participants) {
                mParticipantMap.put(participant.userId, participant);
            }
        }

        return true;
    }

    private boolean save() {
        Collection<Participant> participants;
        synchronized (mParticipantMap) {
            participants = mParticipantMap.values();
        }

        JSONArray contactsJson;
        try {
            contactsJson = new JSONArray();
            for (Participant participant : participants) {
                JSONObject contactJson = new JSONObject();
                contactJson.put("id", participant.userId);
                contactJson.put("first_name", participant.firstName);
                contactJson.put("last_name", participant.lastName);
                contactsJson.put(contactJson);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error while saving", e);
            return false;
        }

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("json", contactsJson.toString());
        editor.apply();

        return true;
    }

    @Override
    public Map<String, Atlas.Participant> getParticipants(String filter, Map<String, Atlas.Participant> result) {
        if (result == null) {
            result = new HashMap<String, Atlas.Participant>();
        }

        // With no filter, return all Participants
        if (filter == null) {
            for (Participant p : getAll()) {
                result.put(p.getId(), p);
            }
            return result;
        }

        // Filter participants by substring matching first- and last- names
        for (Participant p : getAll()) {
            boolean matches = false;
            if (p.firstName != null && p.firstName.toLowerCase().contains(filter)) matches = true;
            if (!matches && p.lastName != null && p.lastName.toLowerCase().contains(filter)) matches = true;
            if (matches) {
                result.put(p.getId(), p);
            } else {
                result.remove(p.getId());
            }
        }
        return result;
    }

    @Override
    public Atlas.Participant getParticipant(String userId) {
        return get(userId);
    }

    /**
     * ContactProvider.Callback provides the mechanism for refreshing Contacts from an external
     * provider, such as a backend API.
     */
    public interface Callback {
        /**
         * Returns a list of all Contacts.  Called on a background thread.
         *
         * @return The complete list of Contacts.
         */
        List<Participant> getAllParticipants();
    }

}
