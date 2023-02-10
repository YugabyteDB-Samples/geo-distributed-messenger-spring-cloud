package com.yugabyte.app.messenger.data.repository;

import java.util.List;

import com.yugabyte.app.messenger.data.entity.Channel;
import com.yugabyte.app.messenger.data.entity.GeoId;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;

public interface ChannelRepository extends BaseJpaRepository<Channel, GeoId> {
    List<Channel> findByWorkspaceIdAndCountryCode(Integer workspaceId, String countryCode);
}
