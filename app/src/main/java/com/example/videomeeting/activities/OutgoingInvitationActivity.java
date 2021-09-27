package com.example.videomeeting.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.videomeeting.R;
import com.example.videomeeting.model.User;
import com.example.videomeeting.network.ApiClient;
import com.example.videomeeting.network.ApiService;
import com.example.videomeeting.utilities.Constants;
import com.example.videomeeting.utilities.PreferenceManager;
import com.google.firebase.messaging.FirebaseMessaging;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutgoingInvitationActivity extends AppCompatActivity {

    private ImageView imageMeetingType, imageRejectInvitation;
    private TextView textFirstChar, textUserName, textEmail;
    private PreferenceManager preferenceManager;
    private String invitationToken = null;
    private String meetingRoom = null;
    private String meetingType;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_invitation);
        bindViews();
        setData();
        setListeners();
    }

    private void bindViews() {
        imageMeetingType = findViewById(R.id.imageMeetingType);
        imageRejectInvitation = findViewById(R.id.imageRejectInvitation);
        textFirstChar = findViewById(R.id.textFirstChar);
        textUserName = findViewById(R.id.textUserName);
        textEmail = findViewById(R.id.textEmail);
    }

    private void setData() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        meetingType = getIntent().getStringExtra("type");
        if (meetingType != null) {
            if (meetingType.equals("video")) {
                imageMeetingType.setImageResource(R.drawable.ic_video);
            } else {
                imageMeetingType.setImageResource(R.drawable.ic_phone);
            }
        }
        user = (User) getIntent().getSerializableExtra("user");
        if (user != null) {
            textFirstChar.setText(user.firstName.substring(0, 1));
            textUserName.setText(user.firstName);
            textEmail.setText(user.lastName);
        }
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                invitationToken = task.getResult();
                if (meetingType != null && user != null) {
                    initiateMeeting(meetingType, user.token);
                }
            }
        });

    }

    private void setListeners() {
        imageRejectInvitation.setOnClickListener(view -> {
            if (user != null) {
                cancelInvitation(user.token);
            }
        });
    }

    private void initiateMeeting(String meetingType, String receiverToken) {
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, meetingType);
            data.put(Constants.KEY_FIRST_NAME, preferenceManager.getString(Constants.KEY_FIRST_NAME));
            data.put(Constants.KEY_LAST_NAME, preferenceManager.getString(Constants.KEY_LAST_NAME));
            data.put(Constants.KEY_EMAIL, preferenceManager.getString(Constants.KEY_EMAIL));
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN, invitationToken);

            meetingRoom = preferenceManager.getString(Constants.KEY_USER_ID) + "_" + UUID.randomUUID().toString().substring(0, 5);
            data.put(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoom);


            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION);

        } catch (Exception exception) {
            Toast.makeText(OutgoingInvitationActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void sendRemoteMessage(String remoteMessageBody, String type) {
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
                Constants.getRemoteMessageHeaders(), remoteMessageBody)
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.isSuccessful()) {
                            if (type.equals(Constants.REMOTE_MSG_INVITATION)) {
                                try {
                                    URL serverUrl = new URL("https://meet.jit.si");
                                    JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                                    builder.setServerURL(serverUrl);
                                    builder.setWelcomePageEnabled(false);
                                    builder.setRoom(meetingRoom);
                                    if (meetingType.equals("audio")) {
                                        builder.setVideoMuted(true);
                                    }
                                    JitsiMeetActivity.launch(OutgoingInvitationActivity.this, builder.build());
                                    finish();
                                } catch (Exception exception) {
                                    Toast.makeText(OutgoingInvitationActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            } else if (type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)) {
                                Toast.makeText(OutgoingInvitationActivity.this, "Invitation cancelled", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            Toast.makeText(OutgoingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        Toast.makeText(OutgoingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void cancelInvitation(String receiverToken) {
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, Constants.REMOTE_MSG_INVITATION_CANCELLED);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_RESPONSE);


        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private final BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type != null) {
                if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                    Toast.makeText(OutgoingInvitationActivity.this, "Invitation Accepted", Toast.LENGTH_SHORT).show();
                } else if (type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)) {
                    Toast.makeText(OutgoingInvitationActivity.this, "Invitation Rejected    ", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );

    }
}