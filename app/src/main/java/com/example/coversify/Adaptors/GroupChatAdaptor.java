package com.example.coversify.Adaptors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coversify.Models.Message;
import com.example.coversify.Models.User;
import com.example.coversify.R;
import com.example.coversify.databinding.ItemReceiveGroupBinding;
import com.example.coversify.databinding.ItemSentGroupBinding;
import com.example.coversify.databinding.ItemreceiveBinding;
import com.example.coversify.databinding.ItemsentBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class GroupChatAdaptor extends RecyclerView.Adapter{
    Context context;
    ArrayList<Message> messages;
    final int itemSent = 1;
    final int itemReceive = 2;
    public GroupChatAdaptor(Context context, ArrayList<Message> messages){
        this.context = context;
        this.messages = messages;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == itemSent){
            View view = LayoutInflater.from(context).inflate(R.layout.item_sent_group, parent, false);
            return new SentViewHolder(view);
        }

        else{
            View view = LayoutInflater.from(context).inflate(R.layout.item_receive_group, parent, false);
            return  new ReceiveViewHolder(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        super.getItemViewType(position);
        Message message = messages.get(position);
        if(FirebaseAuth.getInstance().getUid().equals(message.getSenderId())){
            return itemSent;
        }
        else return itemReceive;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if(holder.getClass() == SentViewHolder.class){
            SentViewHolder viewHolder = (SentViewHolder) holder;
            FirebaseDatabase.getInstance().getReference().child("users").child(message.getSenderId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.exists()){
                        User user = snapshot.getValue(User.class);
                        viewHolder.binding.textView8.setText(user.getName());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
            viewHolder.binding.messagesend.setText(message.getMessage());
        }

        else{
            ReceiveViewHolder viewHolder = (ReceiveViewHolder) holder;
            FirebaseDatabase.getInstance().getReference().child("users").child(message.getSenderId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.exists()){
                        User user = snapshot.getValue(User.class);
                        viewHolder.binding.messagesendergroup.setText(user.getName());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
            viewHolder.binding.message.setText(message.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class SentViewHolder extends RecyclerView.ViewHolder{
        ItemSentGroupBinding binding;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemSentGroupBinding.bind(itemView);
        }
    }

    public class ReceiveViewHolder extends RecyclerView.ViewHolder{

        ItemReceiveGroupBinding binding;
        public ReceiveViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemReceiveGroupBinding.bind(itemView);
        }
    }
}
