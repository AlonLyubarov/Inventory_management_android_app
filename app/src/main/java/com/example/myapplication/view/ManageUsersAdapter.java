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
    private final String[] roleLabels = {"עובד מחסן", "ראש משמרת", "מנהל מחסן"};
    private final String[] roleValues = {"WORKER", "SHIFT_LEADER", "MANAGER"};

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
        holder.textName.setText(user.getDisplayName());
        holder.textEmail.setText(user.getEmail());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(holder.itemView.getContext(), 
                android.R.layout.simple_spinner_item, roleLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.spinnerRole.setAdapter(adapter);

        // Set current role
        for (int i = 0; i < roleValues.length; i++) {
            if (roleValues[i].equals(user.getRole())) {
                holder.spinnerRole.setSelection(i);
                break;
            }
        }

        holder.spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String newRole = roleValues[pos];
                if (!newRole.equals(user.getRole()) && listener != null) {
                    listener.onRoleChanged(user, newRole);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    static class UserHolder extends RecyclerView.ViewHolder {
        TextView textName, textEmail;
        Spinner spinnerRole;

        public UserHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_user_name);
            textEmail = itemView.findViewById(R.id.text_user_email);
            spinnerRole = itemView.findViewById(R.id.spinner_user_role);
        }
    }
}
