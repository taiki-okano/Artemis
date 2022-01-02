package de.tum.in.www1.artemis.service.dto;

import java.io.Serializable;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.tum.in.www1.artemis.domain.User;

/**
 * DTO containing user data and sets of removed and added groups.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserGroupDTO implements Serializable {

    private User user;

    private Set<String> removedGroups;

    private Set<String> addedGroups;

    public UserGroupDTO() {
        /* Needed from the object mapper in order to construct the object */}

    public UserGroupDTO(User user, Set<String> removedGroups, Set<String> addedGroups) {
        this.user = user;
        this.removedGroups = removedGroups;
        this.addedGroups = addedGroups;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<String> getRemovedGroups() {
        return removedGroups;
    }

    public void setRemovedGroups(Set<String> removedGroups) {
        this.removedGroups = removedGroups;
    }

    public Set<String> getAddedGroups() {
        return addedGroups;
    }

    public void setAddedGroups(Set<String> addedGroups) {
        this.addedGroups = addedGroups;
    }

    @Override
    public String toString() {
        return "UserGroupDTO{" + "user=" + user + ", removedGroups=" + removedGroups + ", addedGroups=" + addedGroups + '}';
    }
}
