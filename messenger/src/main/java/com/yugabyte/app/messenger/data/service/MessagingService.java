package com.yugabyte.app.messenger.data.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yugabyte.app.messenger.data.DynamicDataSource;
import com.yugabyte.app.messenger.data.entity.Channel;
import com.yugabyte.app.messenger.data.entity.GeoId;
import com.yugabyte.app.messenger.data.entity.Message;
import com.yugabyte.app.messenger.data.entity.Profile;
import com.yugabyte.app.messenger.data.entity.Workspace;
import com.yugabyte.app.messenger.data.repository.ChannelRepository;
import com.yugabyte.app.messenger.data.repository.MessageRepository;
import com.yugabyte.app.messenger.data.repository.SessionManagementRepository;
import com.yugabyte.app.messenger.data.repository.WorkspaceRepository;

@Service
public class MessagingService {
    @Autowired
    private ChannelRepository channelsRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SessionManagementRepository sManagementRepository;

    @Autowired
    private DynamicDataSource dataSource;

    public List<Channel> getWorkspaceChannels(Workspace workspace) {
        return channelsRepository.findByWorkspaceIdAndCountryCode(workspace.getId(), workspace.getCountryCode());
    }

    public List<Message> getMessages(Channel channel) {
        return messageRepository.findByChannelIdAndCountryCodeOrderByIdAsc(channel.getId(), channel.getCountryCode());
    }

    @Transactional
    public Message addMessage(Message newMessage) {
        if (dataSource.isReplicaConnection())
            sManagementRepository.switchToReadWriteTxMode();

        Message message = messageRepository.save(newMessage);

        return message;
    }

}
