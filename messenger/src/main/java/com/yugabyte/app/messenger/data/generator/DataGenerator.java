package com.yugabyte.app.messenger.data.generator;

import com.vaadin.exampledata.DataType;
import com.vaadin.exampledata.ExampleDataGenerator;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.yugabyte.app.messenger.data.entity.Channel;
import com.yugabyte.app.messenger.data.entity.Message;
import com.yugabyte.app.messenger.data.entity.Profile;
import com.yugabyte.app.messenger.data.entity.Workspace;
import com.yugabyte.app.messenger.data.repository.ChannelRepository;
import com.yugabyte.app.messenger.data.repository.MessageRepository;
import com.yugabyte.app.messenger.data.repository.ProfileRepository;
import com.yugabyte.app.messenger.data.repository.WorkspaceRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringComponent
public class DataGenerator {

        @Bean
        public CommandLineRunner loadData(PasswordEncoder passwordEncoder, ProfileRepository userRepository,
                        WorkspaceRepository workspaceRepository,
                        ChannelRepository channelRepository,
                        MessageRepository messageRepository) {
                return args -> {
                        Logger logger = LoggerFactory.getLogger(getClass());

                        if (userRepository.count() != 0L) {
                                logger.info("Using existing database");
                                logger.info("Number of entities " + userRepository.count());
                                return;
                        }

                        logger.info("Generating demo data");

                        int seed = 1024;
                        Random rand = new Random(seed);

                        String usCode = "USA";
                        String germanyCode = "DEU";
                        String taiwanCode = "TWN";

                        List<String> countryCodes = Stream.of(usCode, germanyCode, taiwanCode).toList();

                        // Generating Workspaces
                        logger.info("Generating Workspaces");

                        ExampleDataGenerator<Workspace> wGenerator = new ExampleDataGenerator<>(Workspace.class,
                                        LocalDateTime.now());
                        wGenerator.setData(Workspace::setName, DataType.COMPANY_NAME);

                        AtomicInteger counter = new AtomicInteger();

                        List<Workspace> workspacesInit = wGenerator.create(10, seed).stream().map(
                                        workspace -> {
                                                int idx = counter.incrementAndGet() % countryCodes.size();
                                                workspace.setCountryCode(
                                                                countryCodes.get(idx));
                                                return workspace;
                                        }).collect(Collectors.toList());
                        workspaceRepository.saveAll(workspacesInit);

                        // Generating Channels
                        logger.info("Generating Channels");

                        // Reading workspaces back with IDs generated by the database sequence generator
                        final List<Workspace> workspacesWithIds = workspaceRepository.findAll();

                        ExampleDataGenerator<Channel> cGenerator = new ExampleDataGenerator<>(Channel.class,
                                        LocalDateTime.now());
                        cGenerator.setData(Channel::setName, DataType.WORD);
                        List<Channel> channelsInit = cGenerator.create(30, seed).stream().map(
                                        channel -> {
                                                Workspace workspace = workspacesWithIds
                                                                .get(rand.nextInt(workspacesWithIds.size()));
                                                channel.setCountryCode(workspace.getCountryCode());
                                                channel.setWorkspaceId(workspace.getId());
                                                return channel;
                                        }).collect(Collectors.toList());
                        channelRepository.saveAll(channelsInit);

                        // Generating Users

                        logger.info("Generating Users");

                        ExampleDataGenerator<Profile> pGenerator = new ExampleDataGenerator<>(Profile.class,
                                        LocalDateTime.now());
                        pGenerator.setData(Profile::setFullName, DataType.FULL_NAME);
                        pGenerator.setData(Profile::setEmail, DataType.EMAIL);
                        pGenerator.setData(Profile::setPhone, DataType.PHONE_NUMBER);
                        pGenerator.setData(Profile::setUserPictureUrl, DataType.PROFILE_PICTURE_URL);

                        Set<Integer> setProfileWorkspaces = new HashSet<>();

                        List<Profile> profilesInit = pGenerator.create(100, seed).stream().map(
                                        profile -> {
                                                // User's personal data is stored in the country of residency. It
                                                // doesn't need to be similar to a workspace country.
                                                profile.setCountryCode(
                                                                countryCodes.get(rand.nextInt(countryCodes.size())));
                                                profile.setHashedPassword(passwordEncoder.encode("password"));

                                                setProfileWorkspaces.clear();

                                                for (int i = 0; i < 10; i++) {
                                                        Workspace workspace = workspacesWithIds.get(
                                                                        rand.nextInt(workspacesWithIds.size()));

                                                        if (setProfileWorkspaces.contains(workspace.getId()))
                                                                continue;

                                                        profile.getWorkspaces().add(workspace);
                                                        setProfileWorkspaces.add(workspace.getId());
                                                }

                                                return profile;
                                        }).collect(Collectors.toList());

                        profilesInit.get(0).setEmail("test@gmail.com");

                        profilesInit.get(1).setEmail("test-europe@gmail.com");
                        profilesInit.get(1).setCountryCode("DEU");

                        profilesInit.get(2).setEmail("test-asia@gmail.com");
                        profilesInit.get(2).setCountryCode("TWN");

                        userRepository.saveAll(profilesInit);

                        // Generating messages
                        logger.info("Generating Messages");

                        for (Workspace workspace : workspacesWithIds) {
                                List<Channel> workspaceChannels = channelRepository
                                                .findByWorkspaceIdAndCountryCode(workspace.getId(),
                                                                workspace.getCountryCode());

                                List<Profile> workspaceProfiles = userRepository.findByWorkspaceIdAndCountryCode(
                                                workspace.getId(), workspace.getCountryCode());

                                ExampleDataGenerator<Message> mGenerator = new ExampleDataGenerator<>(Message.class,
                                                LocalDateTime.now());
                                mGenerator.setData(Message::setMessage, DataType.SENTENCE);

                                List<Message> messages = mGenerator.create(100, seed).stream().map(
                                                message -> {
                                                        message.setCountryCode(workspace.getCountryCode());
                                                        message.setChannelId(workspaceChannels
                                                                        .get(rand.nextInt(workspaceChannels.size()))
                                                                        .getId());
                                                        Profile wProfile = workspaceProfiles
                                                                        .get(rand.nextInt(workspaceProfiles.size()));

                                                        message.setSenderId(wProfile.getId());
                                                        message.setSenderCountryCode(wProfile.getCountryCode());

                                                        return message;
                                                }).collect(Collectors.toList());

                                messageRepository.saveAll(messages);
                        }

                        logger.info("Finished data generation");
                };
        }

}