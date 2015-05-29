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

import com.layer.atlas.Atlas;

public class Participant implements Atlas.Participant {
    public String userId;
    public String firstName;
    public String lastName;

    public String getId() {
        return userId;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }
    
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Contact [userId: ").append(userId).append(", firstName: ").append(firstName).append(", lastName: ").append(lastName).append("]");
        return builder.toString();
    }
}
