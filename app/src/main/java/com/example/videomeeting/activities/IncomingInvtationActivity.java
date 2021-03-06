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
import com.example.videomeeting.network.ApiClient;
import com.example.videomeeting.network.ApiService;
import com.example.videomeeting.utilities.Constants;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingInvtationActivity extends AppCompatActivity {

    private ImageView imageMeetingType, imageAcceptInvitation, imageRejectInvitation;
    private TextView textFirstChar, textUserName, textEmail;
    private String meetingType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_invtation);
        bindViews();
        setData();
        setListeners();
    }

    private void bindViews() {
        imageMeetingType = findViewById(R.id.imageMeetingType);
        imageAcceptInvitation = findViewById(R.id.imageAcceptInvitation);
        imageRejectInvitation = findViewById(R.id.imageRejectInvitation);
        textFirstChar = findViewById(R.id.textFirstChar);
        textUserName = findViewById(R.id.textUserName);
        textEmail = findViewById(R.id.textEmail);
    }

    private void setData() {
        meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        if (meetingType != null) {
            if (meetingType.equals("video")) {
                imageMeetingType.setImageResource(R.drawable.ic_video);
            } else {
                imageMeetingType.setImageResource(R.drawable.ic_phone);
            }
        }

        String firstName = getIntent().getStringExtra(Constants.KEY_FIRST_NAME);
        if (firstName != null) {
            textFirstChar.setText(firstName.substring(0, 1));
        }
        textUserName.setText(firstName);
        textEmail.setText(getIntent().getStringExtra(Constants.KEY_LAST_NAME));
    }

    private void setListeners() {
        imageAcceptInvitation.setOnClickListener(view -> sendInvitationResponse(Constants.REMOTE_MSG_INVITATION_ACCEPTED,
                getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)));

        imageRejectInvitation.setOnClickListener(view -> sendInvitationResponse(Constants.REMOTE_MSG_INVITATION_REJECTED,
                getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)));
    }

    private void sendInvitationResponse(String type, String receiverToken) {
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);
            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();
            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, type);
            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            sendRemoteMessage(body.toString(), type);
        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
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
                            if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                                try {
                                    URL serverUrl = new URL("https://meet.jit.si");
                                    JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                                    builder.setServerURL(serverUrl);
                                    builder.setWelcomePageEnabled(false);
                                    builder.setRoom(getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM));
                                    if (meetingType.equals("audio")) {
                                        builder.setVideoMuted(true);
                                    }
                                    JitsiMeetActivity.launch(IncomingInvtationActivity.this, builder.build());
                                    finish();
                                } catch (Exception exception) {
                                    Toast.makeText(IncomingInvtationActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            } else {
                                Toast.makeText(IncomingInvtationActivity.this, "Invitation Rejected", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            Toast.makeText(IncomingInvtationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                            finish();
                        }

                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        Toast.makeText(IncomingInvtationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private final BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type != null) {
                if (type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)) {
                    Toast.makeText(IncomingInvtationActivity.this, "Invitation Cancelled", Toast.LENGTH_SHORT).show();
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