package com.example.myapplication.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.User;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersAdapter extends RecyclerView.Adapter<ManageUsersAdapter.UserHolder> {

    private List<User> users = new ArrayList<>();
    private OnRoleChangeListener listener;

    public interface OnRoleChangeListener {
        void onRoleChanged(User user, String newRole);
    }

    public void setOnRoleChangeListener(OnRoleChangeListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_manage_row, parent, false);
        return new UserHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() { return users.size(); }

    public void setUsers(List<User> userList) {
        this.users = userList;
        notifyDataSetChanged();
    }

    static class UserHolder extends RecyclerView.ViewHolder {
        private final TextView textName, textEmail;
        private final Spinner spinnerRole;
        private boolean isInitializing = false;

        public UserHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_user_name);
            textEmail = itemView.findViewById(R.id.text_user_email);
            spinnerRole = itemView.findViewById(R.id.spinner_user_role);

            String[] roles = {"WORKER", "SHIFT_LEADER"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(itemView.getContext(), android.R.layout.simple_spinner_dropdown_item, roles);
            spinnerRole.setAdapter(adapter);
        }

        public void bind(User user, OnRoleChangeListener listener) {
            isInitializing = true;
            
            textName.setText(user.getDisplayName());
            textEmail.setText(user.getEmail());

            // B-08 Fix: Handle MANAGER role in Spinner (edge case safety)
            if ("MANAGER".equals(user.getRole())) {
                spinnerRole.setEnabled(false);
                isInitializing = false;
                return;
            } else {
                spinnerRole.setEnabled(true);
            }

            if ("SHIFT_LEADER".equals(user.getRole())) spinnerRole.setSelection(1);
            else spinnerRole.setSelection(0);

            spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (!isInitializing) {
                        String newRole = (position == 1) ? "SHIFT_LEADER" : "WORKER";
                        if (!newRole.equals(user.getRole())) {
                            listener.onRoleChanged(user, newRole);
                        }
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
            
            isInitializing = false;
        }
    }
}
