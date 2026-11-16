
package com.tripsync.interfaces;

import com.tripsync.model.Traveler;

public interface Collaboratable {

    void addMember(Traveler traveler);

    void removeMember(String userId);

    void displayCollaborators();
}
