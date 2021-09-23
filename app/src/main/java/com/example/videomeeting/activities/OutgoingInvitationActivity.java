package com.example.videomeeting.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.videomeeting.R;
import com.example.videomeeting.model.User;

public class OutgoingInvitationActivity extends AppCompatActivity {

    private ImageView imageMeetingType, imageRejectInvitation;
    private TextView textFirstChar, textUserName, textEmail;
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
        meetingType = getIntent().getStringExtra("type");
        if (meetingType != null) {
            if (meetingType.equals("video")) {
                imageMeetingType.setImageResource(R.drawable.ic_video);
            } else {
                imageMeetingType.setImageResource(R.drawable.ic_phone);
            }
        }
        user = (User) getIntent().getSerializableExtra("user");
        if(user != null){
            textFirstChar.setText(user.firstName.substring(0, 1));
            textUserName.setText(user.firstName);
            textEmail.setText(user.lastName);
        }
        imageRejectInvitation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void setListeners() {
    }

}