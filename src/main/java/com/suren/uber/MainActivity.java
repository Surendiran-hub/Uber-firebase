package com.suren.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    Button register,login;
    RelativeLayout relativeLayout;

    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users, drivers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference("Users");
        drivers=db.getReference("Drivers");

        register = findViewById(R.id.Register);
        login = findViewById(R.id.LogIn);
        relativeLayout = findViewById(R.id.root);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowLoginDialog();
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowRegisterDialog();
            }
        });
    }

    private void ShowLoginDialog() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Login");
        dialog.setMessage("Please use your email to login");

        LayoutInflater inflater =LayoutInflater.from(this);
        View login_layout = inflater.inflate(R.layout.login_layout,null);

        final EditText email = login_layout.findViewById(R.id.mail);
        final EditText password = login_layout.findViewById(R.id.password);

        dialog.setView(login_layout);

        dialog.setPositiveButton("Login", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        login.setEnabled(false);
                        if (TextUtils.isEmpty(email.getText().toString())) {
                            Snackbar.make(relativeLayout, "Enter mail", Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        if (TextUtils.isEmpty(password.getText().toString())) {
                            Snackbar.make(relativeLayout, "Enter password", Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        final SpotsDialog waitingDialog = new SpotsDialog(MainActivity.this);
                        waitingDialog.show();

                        auth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                    @Override
                                    public void onSuccess(AuthResult authResult) {
                                        waitingDialog.dismiss();
                                        startActivity(new Intent(MainActivity.this, MapsActivity.class));
                                        finish();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                waitingDialog.dismiss();
                                Snackbar.make(relativeLayout, "" + e.getMessage(),
                                        Snackbar.LENGTH_SHORT).show();

                                login.setEnabled(true);
                            }
                        });
                    }
                });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                 dialog.dismiss();
            }
        });

        dialog.show();

    }

    private void ShowRegisterDialog() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Register");
        dialog.setMessage("Please use your email to register");

        LayoutInflater inflater =LayoutInflater.from(this);
        View register_layout = inflater.inflate(R.layout.register_layout,null);

        final EditText name = register_layout.findViewById(R.id.name);
        final EditText phone = register_layout.findViewById(R.id.phone);
        final EditText email = register_layout.findViewById(R.id.mail);
        final EditText password = register_layout.findViewById(R.id.password);

        dialog.setView(register_layout);

        dialog.setPositiveButton("Register", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                if (TextUtils.isEmpty(email.getText().toString())){
                    Snackbar.make(relativeLayout,"Enter mail",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(name.getText().toString())){
                    Snackbar.make(relativeLayout,"Enter name",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password.getText().toString())){
                    Snackbar.make(relativeLayout,"Enter password",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(phone.getText().toString())){
                    Snackbar.make(relativeLayout,"Enter phone",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                auth.createUserWithEmailAndPassword(email.getText().toString(),password.getText().toString())
                                              .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                                  @Override
                                                  public void onSuccess(AuthResult authResult) {

                                                      User user = new User();
                                                      user.setEmail(email.getText().toString());
                                                      user.setPassword(password.getText().toString());
                                                      user.setName(name.getText().toString());
                                                      user.setPhone(phone.getText().toString());

                                                      users.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(user)
                                                         .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                             @Override
                                                             public void onSuccess(Void aVoid) {
                                                                 Snackbar.make(relativeLayout,"Register Success",
                                                                         Snackbar.LENGTH_SHORT).show();
                                                             }
                                                         })
                                                          .addOnFailureListener(new OnFailureListener() {
                                                              @Override
                                                              public void onFailure(@NonNull Exception e) {
                                                                  Snackbar.make(relativeLayout,"Error"+e.getMessage(),
                                                                          Snackbar.LENGTH_SHORT).show();

                                                              }
                                                          });

                                                  }
                                              })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(relativeLayout,""+e.getMessage(),Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.dismiss();
            }
        });

        dialog.show();
    }
}
