package com.yugabyte.app.messenger.data.service;

import com.yugabyte.app.messenger.data.DynamicDataSource;
import com.yugabyte.app.messenger.data.entity.GeoId;
import com.yugabyte.app.messenger.data.entity.Profile;
import com.yugabyte.app.messenger.data.repository.ProfileRepository;
import com.yugabyte.app.messenger.data.repository.SessionManagementRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProfileService {
    private final ProfileRepository repository;

    private final HashMap<GeoId, Profile> localCache;

    @Autowired
    private DynamicDataSource dataSource;

    @Autowired
    private SessionManagementRepository sManagementRepository;

    @Autowired
    public ProfileService(ProfileRepository repository) {
        this.repository = repository;
        localCache = new HashMap<>();
    }

    @Bean
    public CommandLineRunner preloadProfiles() {
        return args -> {
            List<Profile> users = repository.findByIdGreaterThanEqual(1);

            users.forEach(user -> {
                localCache.put(new GeoId(user.getId(), user.getCountryCode()), user);
            });

            System.out.printf("Preloaded %d Profiles to local cache\n", users.size());
        };
    }

    public Optional<Profile> get(GeoId id) {
        Profile user = localCache.get(id);

        if (user != null)
            return Optional.of(user);

        Optional<Profile> dbUser = repository.findById(id);

        if (dbUser.isPresent())
            localCache.put(id, dbUser.get());

        return dbUser;
    }

    @Transactional
    public void delete(GeoId id) {
        if (dataSource.isReplicaConnection())
            sManagementRepository.switchToReadWriteTxMode();

        localCache.remove(id);
        repository.deleteById(id);
    }

    public int count() {
        return (int) repository.count();
    }

}
