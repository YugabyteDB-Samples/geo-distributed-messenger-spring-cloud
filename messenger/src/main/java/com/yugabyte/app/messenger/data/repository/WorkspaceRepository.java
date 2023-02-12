package com.yugabyte.app.messenger.data.repository;

import com.yugabyte.app.messenger.data.entity.GeoId;
import com.yugabyte.app.messenger.data.entity.Workspace;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;

public interface WorkspaceRepository extends BaseJpaRepository<Workspace, GeoId> {

}
