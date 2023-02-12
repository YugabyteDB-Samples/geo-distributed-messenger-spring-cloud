package com.yugabyte.app.messenger.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.yugabyte.app.messenger.data.entity.Channel;
import com.yugabyte.app.messenger.data.entity.Workspace;
import com.yugabyte.app.messenger.data.entity.Message;
import com.yugabyte.app.messenger.data.entity.Profile;
import com.yugabyte.app.messenger.data.repository.ChannelRepository;
import com.yugabyte.app.messenger.data.repository.WorkspaceRepository;
import com.yugabyte.app.messenger.data.repository.MessageRepository;
import com.yugabyte.app.messenger.data.repository.ProfileRepository;

@Service
@Transactional(readOnly = true)
public class DataGenerationService {
    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ProfileRepository userRepository;

    @Transactional
    public List<Workspace> storeWorkspaces(List<Workspace> workspaces) {
        return workspaceRepository.persistAll(workspaces);
    }

    @Transactional
    public List<Channel> storeChannels(List<Channel> channels) {
        return channelRepository.persistAll(channels);
    }

    @Transactional
    public List<Message> storeMessages(List<Message> messages) {
        return messageRepository.persistAll(messages);
    }

    @Transactional
    public List<Profile> storeUsers(List<Profile> users) {
        return userRepository.persistAll(users);
    }

    public List<Channel> findWorkspaceChannels(Workspace workspace) {
        return channelRepository.findByWorkspaceIdAndCountryCode(workspace.getId(),
                workspace.getCountryCode());
    }
}
