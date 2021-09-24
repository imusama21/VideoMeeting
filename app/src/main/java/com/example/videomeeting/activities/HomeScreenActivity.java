package com.example.videomeeting.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.videomeeting.R;
import com.example.videomeeting.adapter.UserAdapter;
import com.example.videomeeting.listeners.UserListeners;
import com.example.videomeeting.model.User;
import com.example.videomeeting.utilities.Constants;
import com.example.videomeeting.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HomeScreenActivity extends AppCompatActivity implements UserListeners {

    private TextView title, signOut, errorMessage;
    private RecyclerView homeRecyclerView;
    private PreferenceManager preferenceManager;
    //private KProgressHUD progress;
    private List<User> userList;
    private UserAdapter userAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        bindViews();
        setData();
        setListener();

    }

    private void bindViews() {
        title = findViewById(R.id.title);
        errorMessage = findViewById(R.id.errorMessage);
        signOut = findViewById(R.id.signOut);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        homeRecyclerView = findViewById(R.id.homeRecyclerView);
    }

    private void setData() {
        //Initializations
        preferenceManager = new PreferenceManager(getApplicationContext());
        userList = new ArrayList<>();

        //Progress Bar
        /*progress = KProgressHUD.create(HomeScreenActivity.this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please wait...")
                .setCancellable(true)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);*/

        //Set title
        title.setText(String.format("%s %s",
                preferenceManager.getString(Constants.KEY_FIRST_NAME),
                preferenceManager.getString(Constants.KEY_LAST_NAME)));

        //Token
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    sendFCMTokenToDatabase(task.getResult());
                }
            }
        });
        /*FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        sendFCMTokenToDatabase(task.getResult().getToken());
                    }
                });*/

        userAdapter = new UserAdapter(userList, this);
        homeRecyclerView.setAdapter(userAdapter);

        swipeRefreshLayout.setOnRefreshListener(this::getUser);

        getUser();
    }

    private void setListener() {
        signOut.setOnClickListener(view -> signOut());
    }

    private void sendFCMTokenToDatabase(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnSuccessListener(unused -> {
                    //    Toast.makeText(HomeScreenActivity.this, "Token updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(HomeScreenActivity.this, "Unable to send token: " + e.getMessage(), Toast.LENGTH_SHORT).show());

    }

    private void signOut() {
        Toast.makeText(HomeScreenActivity.this, "Signing out", Toast.LENGTH_SHORT).show();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clearPreferences();
                    startActivity(new Intent(HomeScreenActivity.this, SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(HomeScreenActivity.this, "Unable to sig out " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void getUser() {
        swipeRefreshLayout.setRefreshing(true);
        //progress.show();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS).get()
                .addOnCompleteListener(task -> {
                    swipeRefreshLayout.setRefreshing(false);
                    //progress.dismiss();
                    String myUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        userList.clear();
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            if (myUserId.equals(documentSnapshot.getId())) {
                                continue;
                            }
                            User user = new User();
                            user.firstName = documentSnapshot.getString(Constants.KEY_FIRST_NAME);
                            user.lastName = documentSnapshot.getString(Constants.KEY_LAST_NAME);
                            user.email = documentSnapshot.getString(Constants.KEY_EMAIL);
                            user.token = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            userList.add(user);
                        }
                        if (userList.size() > 0) {
                            userAdapter.notifyDataSetChanged();
                        } else {
                            errorMessage.setVisibility(View.VISIBLE);
                        }
                    } else {
                        //progress.dismiss();
                        swipeRefreshLayout.setRefreshing(false);
                        errorMessage.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    swipeRefreshLayout.setRefreshing(false);
                    //progress.dismiss();
                    Toast.makeText(HomeScreenActivity.this, "Unable to load user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void initiateVideoMeeting(User user) {
        if (user.token == null || user.token.trim().isEmpty()) {
            Toast.makeText(HomeScreenActivity.this, user.firstName + " " + user.lastName + " is not available for meeting", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(HomeScreenActivity.this, OutgoingInvitationActivity.class);
            intent.putExtra("user", user);
            intent.putExtra("type", "video");
            startActivity(intent);
        }
    }

    @Override
    public void initiateAudioMeeting(User user) {
        if (user.token == null || user.token.trim().isEmpty()) {
            Toast.makeText(HomeScreenActivity.this, user.firstName + " " + user.lastName + " is not available for meeting", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(HomeScreenActivity.this, "Audio meeting with " + user.firstName + " " + user.lastName, Toast.LENGTH_SHORT).show();
        }
    }
}