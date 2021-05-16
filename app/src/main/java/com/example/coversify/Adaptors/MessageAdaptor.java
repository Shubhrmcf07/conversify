package com.example.coversify.Adaptors;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.coversify.Activities.ChatActivity;
import com.example.coversify.Activities.MainActivity;
import com.example.coversify.Models.Message;
import com.example.coversify.R;
import com.example.coversify.databinding.ItemreceiveBinding;
import com.example.coversify.databinding.ItemsentBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MessageAdaptor extends RecyclerView.Adapter{
    SecretKey key;
    IvParameterSpec ivParameterSpec;
    String algo = "AES/CBC/PKCS5Padding";
    Context context;
    ArrayList<Message> messages;
    final int itemSent = 1;
    final int itemReceive = 2;
    public MessageAdaptor(Context context, ArrayList<Message> messages) throws NoSuchAlgorithmException {
        this.context = context;
        this.messages = messages;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        if(viewType == itemSent){
            View view = LayoutInflater.from(context).inflate(R.layout.itemsent, parent, false);
            return new SentViewHolder(view);
        }

        else{
            View view = LayoutInflater.from(context).inflate(R.layout.itemreceive, parent, false);
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if(holder.getClass() == SentViewHolder.class){
            SentViewHolder viewHolder = (SentViewHolder) holder;

            if(message.getImageurl() != null){
                viewHolder.binding.attachmentimage.setVisibility(View.VISIBLE);
                viewHolder.binding.messagesend.setVisibility(View.GONE);
                Glide.with(context).load(message.getImageurl()).into(viewHolder.binding.attachmentimage);
            }
            try {
                String msgDec = ChatActivity.decrypt(algo, message.getMessage(), key, ivParameterSpec);
                message.setMessage(msgDec);
            }
            catch (Exception e){

            }
            finally {
                viewHolder.binding.messagesend.setText(message.getMessage());
            }
        }

        else{
            ReceiveViewHolder viewHolder = (ReceiveViewHolder) holder;
            if(message.getImageurl() != null){
                viewHolder.binding.imagereceived.setVisibility(View.VISIBLE);
                viewHolder.binding.message.setVisibility(View.GONE);
                Glide.with(context).load(message.getImageurl()).into(viewHolder.binding.imagereceived);
            }
            try {
                String msgDec = ChatActivity.decrypt(algo, message.getMessage(), key, ivParameterSpec);
                message.setMessage(msgDec);
            }
            catch (Exception e){

            }
            finally {
                viewHolder.binding.message.setText(message.getMessage());
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class SentViewHolder extends RecyclerView.ViewHolder{
        ItemsentBinding binding;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemsentBinding.bind(itemView);
        }
    }

    public class ReceiveViewHolder extends RecyclerView.ViewHolder{

        ItemreceiveBinding binding;
        public ReceiveViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemreceiveBinding.bind(itemView);
        }
    }
}
