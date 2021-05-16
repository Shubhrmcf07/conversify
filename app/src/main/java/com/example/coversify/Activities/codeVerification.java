package com.example.coversify.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.coversify.databinding.ActivityCodeVerificationBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.mukesh.OnOtpCompletionListener;

import java.util.concurrent.TimeUnit;

public class codeVerification extends AppCompatActivity {
    ActivityCodeVerificationBinding binding;
    FirebaseAuth auth;
    String verificationId;
    ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCodeVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("Sending OTP..");
        dialog.setCancelable(false);
        dialog.show();

        auth = FirebaseAuth.getInstance();

        String phone = getIntent().getStringExtra("phone");

        binding.textView3.setText("Enter the otp sent to "+phone);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(90L, TimeUnit.SECONDS)
                .setActivity(codeVerification.this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {

                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(verificationId, forceResendingToken);
                        dialog.dismiss();
                        verificationId = s;
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);

        ProgressDialog d1 = new ProgressDialog(this);
        d1.setMessage("Verifying...");
        d1.setCancelable(false);
        binding.otpView.setOtpCompletionListener(new OnOtpCompletionListener() {
            @Override
            public void onOtpCompleted(String otp) {
                d1.show();
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);

                auth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        d1.dismiss();
                        if(task.isSuccessful()){
                            Toast.makeText(codeVerification.this, "Verified", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(codeVerification.this, profileActivity.class);
                            startActivity(intent);
                            finishAffinity();
                        }

                        else{
                            Toast.makeText(codeVerification.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}