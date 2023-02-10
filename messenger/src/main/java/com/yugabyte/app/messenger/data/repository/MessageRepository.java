package com.yugabyte.app.messenger.data.repository;

import java.util.List;

import com.yugabyte.app.messenger.data.entity.GeoId;
import com.yugabyte.app.messenger.data.entity.Message;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;

public interface MessageRepository extends BaseJpaRepository<Message, GeoId> {

    public List<Message> findByChannelIdAndCountryCodeOrderByIdAsc(Integer channelId, String countryCode);
}
