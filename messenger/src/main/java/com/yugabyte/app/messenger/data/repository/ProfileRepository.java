package com.yugabyte.app.messenger.data.repository;

import com.yugabyte.app.messenger.data.entity.GeoId;
import com.yugabyte.app.messenger.data.entity.Profile;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProfileRepository extends JpaRepository<Profile, GeoId> {
    public Profile findByEmail(String email);

    @Query(value = "SELECT p.* FROM Profile p JOIN Workspace_Profile wp " +
            "ON wp.profile_id = p.id WHERE wp.workspace_id = ?1 and wp.workspace_country = ?2", nativeQuery = true)
    public List<Profile> findByWorkspaceIdAndCountryCode(Integer workspaceId, String countryCode);
}