package com.example.coversify.Adaptors;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.coversify.Activities.ChatActivity;
import com.example.coversify.R;
import com.example.coversify.Models.User;
import com.example.coversify.databinding.ConversationRowBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class UsersAdaptor extends RecyclerView.Adapter<UsersAdaptor.UsersViewHolder> {
    Context context;
    ArrayList<User> users;
    SecretKey key;
    IvParameterSpec ivParameterSpec;
    String algo = "AES/CBC/PKCS5Padding";
    public UsersAdaptor(Context context, ArrayList<User> users){
        this.context = context;
        this.users = users;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull
    @Override
    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        InputStream is = parent.getContext().getResources().openRawResource(R.raw.key);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        char[] KEY = new char[24];
        try {
            int i = is.read();
            for(int p = 0 ; p < 24 ; p++){
                KEY[p] = (char)i;
                i = is.read();
            }
            is.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(KEY);
        String pls = new String(KEY);
        byte[] decodedKey = Base64.getDecoder().decode(pls);
        key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        InputStream ivs = parent.getContext().getResources().openRawResource(R.raw.iv);
        char[] IV = new char[16];

        try{
            int i = ivs.read();
            for(int p = 0 ; p < 16 ; p++) {
                IV[p] = (char) i;
                i = ivs.read();
            }

            ivs.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        String byteConv = new String(IV);

        byte[] ivFinal = byteConv.getBytes();
        ivParameterSpec = new IvParameterSpec(ivFinal);

        View view = LayoutInflater.from(context).inflate(R.layout.conversation_row, parent, false);
        return new UsersViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {

        User user = users.get(position);

        String senderId = FirebaseAuth.getInstance().getUid();
        String senderRoom = senderId + user.getUid();

        FirebaseDatabase.getInstance().getReference().child("chats").child(senderRoom).addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    String lastmessage = snapshot.child("lastmessage").getValue(String.class);
                    long time = snapshot.child("lastmessagetime").getValue(Long.class);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
                    String lastmsgPT = lastmessage;
                    try{
                        System.out.println(key);
                        lastmsgPT = ChatActivity.decrypt(algo, lastmessage, key, ivParameterSpec);
                    }
                    catch (Exception e){
                        System.out.println(e);
                    }

                    finally {
                        holder.binding.LastMessageTime.setText(dateFormat.format(new Date(time)));
                        holder.binding.lastmessage.setText(lastmsgPT);
                    }
                }

                else{
                    holder.binding.lastmessage.setText("Tap to Chat...");
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        holder.binding.username.setText(user.getName());
        Glide.with(context).load(user.getProfileImage()).placeholder(R.drawable.profile_picture).into(holder.binding.imageView3);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("name", user.getName());
                intent.putExtra("uid", user.getUid());
                intent.putExtra("img", user.getProfileImage());
                context.startActivity(intent);
            }
        });
    }


    @Override
    public int getItemCount() {
        return users.size();
    }

    public class UsersViewHolder extends RecyclerView.ViewHolder{
        ConversationRowBinding binding;
        public UsersViewHolder(@NonNull View itemView ){
            super(itemView);
            binding = ConversationRowBinding.bind(itemView);
        }

    }
}
