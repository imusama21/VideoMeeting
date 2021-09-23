package com.example.videomeeting.listeners;

import com.example.videomeeting.model.User;

public interface UserListeners {

    void initiateVideoMeeting(User user);

    void initiateAudioMeeting(User user);
}
