package com.example.sendotpmessage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.sendotpmessage.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PhoneAuthProvider.ForceResendingToken forceResendingToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseAuth firebaseAuth;
    //progress dialog
    private ProgressDialog pd;

    private String mVerificationId;
    private static final String TAG = "MAIN_TAG";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.LinearLayoutPhoneNumber.setVisibility(View.VISIBLE);
        binding.LinearLayoutVerificationCode.setVisibility(View.GONE);

        firebaseAuth = FirebaseAuth.getInstance();

        //init progress dialog

        pd = new ProgressDialog(this);
        pd.setTitle("Please wait....");
        pd.setCanceledOnTouchOutside(false);

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                pd.dismiss();
                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                super.onCodeSent(verificationId, forceResendingToken);
                Log.d(TAG, "onCodeSent:" + mVerificationId);

                mVerificationId = verificationId;
                forceResendingToken = token;
                pd.dismiss();

                // hide phone layout show code layout
                binding.LinearLayoutPhoneNumber.setVisibility(View.GONE);
                binding.LinearLayoutVerificationCode.setVisibility(View.VISIBLE);

                Toast.makeText(MainActivity.this, "Verification code sent", Toast.LENGTH_SHORT).show();

                binding.codeSentDescription.setText("Please type the verification code we sent \n to " + binding.editTextPhoneNumber.getText().toString().trim());
            }
        };

        binding.btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String phone = binding.editTextPhoneNumber.getText().toString().trim();
                if (TextUtils.isEmpty(phone)) {
                    Toast.makeText(MainActivity.this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                } else {
                    startPhoneNumberVerification(phone);
                }
            }
        });

        binding.btnVerificationCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = binding.editTextVerificationCode.getText().toString().trim();
                if (TextUtils.isEmpty(code)) {
                    Toast.makeText(MainActivity.this, "Please enter verification code", Toast.LENGTH_SHORT).show();
                } else {
                    verifyPhoneNumber(mVerificationId, code);
                }
            }
        });

        binding.textViewResendVerificationCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String phone = binding.editTextPhoneNumber.getText().toString().trim();
                if (TextUtils.isEmpty(phone)) {
                    Toast.makeText(MainActivity.this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                } else {
                    resendVerificationCode(phone, forceResendingToken);
                }
            }
        });
    }


    private void startPhoneNumberVerification(String phone) {
        pd.setMessage("Verifying Phone Number");
        pd.show();

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(phone)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resendVerificationCode(String phone, PhoneAuthProvider.ForceResendingToken token) {
        pd.setMessage("Resending Code");
        pd.show();

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(phone)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .setForceResendingToken(token)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);

    }

    private void verifyPhoneNumber(String verificationId, String code) {
        pd.setMessage("Verifying Code");
        pd.show();

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        pd.setMessage("Logging In");

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            pd.dismiss();
                            Log.d(TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            assert user != null;
                            String phone = user.getPhoneNumber();
                            // Update UI
                            Toast.makeText(MainActivity.this, "Logged In as" + phone, Toast.LENGTH_SHORT).show();
                            //start profile activity
                            startActivity(new Intent(MainActivity.this, ProfileActivity.class));

                        } else {
                            // Sign in failed, display a message and update the UI
                            pd.dismiss();
                            Toast.makeText(MainActivity.this, "" + task.getException(), Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(MainActivity.this, "" + task.getException(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }
}