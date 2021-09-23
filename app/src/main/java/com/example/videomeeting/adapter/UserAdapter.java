package com.example.videomeeting.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.videomeeting.R;
import com.example.videomeeting.listeners.UserListeners;
import com.example.videomeeting.model.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<User> users;
    private UserListeners userListeners;

    public UserAdapter(List<User> users, UserListeners userListeners) {
        this.users = users;
        this.userListeners = userListeners;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textFirstChar, textUserName, textLastName;
        private ImageView phoneIm, videoCam;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textFirstChar = itemView.findViewById(R.id.textFirstChar);
            textUserName = itemView.findViewById(R.id.textUserName);
            textLastName = itemView.findViewById(R.id.textLastName);
            phoneIm = itemView.findViewById(R.id.phoneIm);
            videoCam = itemView.findViewById(R.id.videoCam);
        }

        private void setUserData(User user) {
            textFirstChar.setText(user.firstName.substring(0, 1));
            textUserName.setText(String.format("%s %s", user.firstName, user.lastName));
            textLastName.setText(user.email);
            phoneIm.setOnClickListener(view -> userListeners.initiateAudioMeeting(user));
            videoCam.setOnClickListener(view -> userListeners.initiateVideoMeeting(user));
        }
    }
}
