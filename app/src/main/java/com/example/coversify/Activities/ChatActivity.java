package com.example.coversify.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.coversify.Adaptors.MessageAdaptor;
import com.example.coversify.Models.Message;
import com.example.coversify.R;
import com.example.coversify.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

@RequiresApi(api = Build.VERSION_CODES.O)
public class ChatActivity extends AppCompatActivity {
    public ChatActivity() throws NoSuchAlgorithmException, IOException {
    }

    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    public static SecretKey getKeyFromPassword(String password, String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec)
                .getEncoded(), "AES");
        return secret;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String encrypt(String algorithm, String input, SecretKey key,
                                 IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder()
                .encodeToString(cipherText);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String decrypt(String algorithm, String cipherText, SecretKey key,
                                 IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] plainText = cipher.doFinal(Base64.getDecoder()
                .decode(cipherText));
        return new String(plainText);
    }
    SecretKey key;
    IvParameterSpec ivParameterSpec;
    String algo = "AES/CBC/PKCS5Padding";

    ActivityChatBinding binding;
    MessageAdaptor adaptor;
    ArrayList<Message> messages;
    String senderRoom, receiverRoom;
    FirebaseDatabase database;
    FirebaseStorage storage;
    ProgressDialog dialog;
    String senderuid, receiveruid;
    String randomKey;
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InputStream is = getResources().openRawResource(R.raw.key);
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

        InputStream ivs = getResources().openRawResource(R.raw.iv);
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

        System.out.println(KEY);
        String pls = new String(KEY);
        byte[] decodedKey = Base64.getDecoder().decode(pls);
        key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        ivParameterSpec = new IvParameterSpec(ivFinal);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        messages = new ArrayList<>();
        try {
            adaptor = new MessageAdaptor(this, messages);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        finally {

            dialog = new ProgressDialog(this);
            dialog.setMessage("Uploading..");
            dialog.setCancelable(false);
            LinearLayoutManager llm = new LinearLayoutManager(this);
            llm.setStackFromEnd(true);
            binding.recyclerview.setLayoutManager(llm);
            binding.recyclerview.setAdapter(adaptor);
            System.out.println(adaptor.getItemCount());

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            senderuid = FirebaseAuth.getInstance().getUid();
            String name = getIntent().getStringExtra("name");
            receiveruid = getIntent().getStringExtra("uid");
            String img = getIntent().getStringExtra("img");

            senderRoom = senderuid+receiveruid;
            receiverRoom = receiveruid + senderuid;


            binding.recyclerview.scrollToPosition(adaptor.getItemCount() - 1);

            database.getReference().child("status").child(receiveruid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    System.out.println(snapshot);
                    if(snapshot.exists()){
                        String status = snapshot.getValue(String.class);
                        if(!status.isEmpty()) {
                            if (status.equals("Offline")) {
                                binding.onlinestatus.setVisibility(View.GONE);
                            } else {
                                binding.onlinestatus.setText(status);
                                binding.onlinestatus.setVisibility(View.VISIBLE);
                            }
                        }

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            ImageView imageView = binding.imageView;

            System.out.println(img);
            Uri uri = Uri.parse(img);


            Glide.with(this).load(uri).placeholder(R.drawable.profile_picture).into(imageView);

            TextView textView = binding.toolbartitle;
            textView.setText(name);

            getSupportActionBar().setDisplayShowTitleEnabled(false);

            database.getReference().child("chats").child(senderRoom).child("messages").addValueEventListener(new ValueEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    messages.clear();
                    LinearLayoutManager llm = new LinearLayoutManager(ChatActivity.this);
                    llm.setStackFromEnd(true);
                    binding.recyclerview.setLayoutManager(llm);
                    binding.recyclerview.setAdapter(adaptor);
                    for(DataSnapshot snapshot1 : snapshot.getChildren()){
                        Message message = snapshot1.getValue(Message.class);
                        messages.add(message);

                    }

                    adaptor.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            binding.sendButton.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onClick(View v) {
                    String messageTxt = binding.messagetext.getText().toString();
                    Date date = new Date();
                    String msg = messageTxt;
                    try {
                        msg = encrypt(algo, messageTxt, key, ivParameterSpec);
                    }
                    catch (Exception e){
                        System.out.println(e);
                        msg = messageTxt;
                    }
                    finally {
                        System.out.println(msg);
                        Message message = new Message(msg, senderuid, date.getTime());
                        binding.messagetext.setText("");
                        randomKey = database.getReference().push().getKey();
                        HashMap<String, Object> lastmessage = new HashMap<>();
                        lastmessage.put("lastmessage", message.getMessage());
                        lastmessage.put("lastmessagetime", date.getTime());

                        database.getReference().child("chats").child(senderRoom).updateChildren(lastmessage);
                        database.getReference().child("chats").child(receiverRoom).updateChildren(lastmessage);

                        database.getReference().child("chats").child(senderRoom).child("messages").child(randomKey).setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                database.getReference().child("chats").child(receiverRoom).child("messages").child(randomKey).setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                    }
                                });
                            }
                        });
                    }

                }

            });
            binding.attachment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, 25);
                }
            });

            final Handler handler = new Handler();

            binding.messagetext.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                    database.getReference().child("status").child(senderuid).setValue("Typing...");
                    handler.removeCallbacksAndMessages(null);
                    handler.postDelayed(userStoppedTyping, 1000);
                }

                Runnable userStoppedTyping = new Runnable() {
                    @Override
                    public void run() {
                        database.getReference().child("status").child(senderuid).setValue("Online");
                    }
                };
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        dialog.show();
        if (requestCode == 25){
            if(data!=null){
                if(data.getData() != null){
                    Uri selectedImage = data.getData();
                    Calendar calendar = Calendar.getInstance();
                    StorageReference reference = storage.getReference().child("chats").child(calendar.getTimeInMillis() + "");
                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            dialog.dismiss();
                            if(task.isSuccessful()){
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String filepath = uri.toString();

                                        String messageTxt = binding.messagetext.getText().toString();
                                        Date date = new Date();
                                        Message message = new Message(messageTxt, senderuid, date.getTime());
                                        message.setImageurl(filepath);
                                        message.setMessage("Photo");
                                        binding.messagetext.setText("");
                                        String rk = database.getReference().push().getKey();
                                        HashMap<String, Object> lastmessage = new HashMap<>();
                                        lastmessage.put("lastmessage", message.getMessage());
                                        lastmessage.put("lastmessagetime", date.getTime());

                                        database.getReference().child("chats").child(senderRoom).updateChildren(lastmessage);
                                        database.getReference().child("chats").child(receiverRoom).updateChildren(lastmessage);

                                        database.getReference().child("chats").child(senderRoom).child("messages").child(rk).setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                database.getReference().child("chats").child(receiverRoom).child("messages").child(rk).setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {

                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    });


                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String userId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("status").child(userId).setValue("Online");
    }

    @Override
    protected void onPause() {

        String userId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("status").child(userId).setValue("Offline");
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        String userId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("status").child(userId).setValue("Offline");
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chatmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}

