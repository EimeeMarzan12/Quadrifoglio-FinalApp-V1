package com.surendramaran.yolov8tflite;
import static android.app.PendingIntent.getActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class Login extends AppCompatActivity {
    EditText usernameField;
    EditText passwordField;
    ImageView hiddenButton;
    String username;
    String password;
    Button login;
    DatabaseReference mUserRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        hiddenButton = (ImageView) findViewById(R.id.ic_queen);
        usernameField = (EditText) findViewById(R.id.username_editText);
        passwordField = (EditText) findViewById(R.id.password_editText);
        hiddenButton.setClickable(true);
        login = (Button) findViewById(R.id.login_button);

        hiddenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent e = new Intent(Login.this, AddUser.class);
                startActivity(e);
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent e = new Intent(Login.this, MainActivity.class);
                startActivity(e);

//                username = usernameField.getText().toString();
//                password = passwordField.getText().toString();
//                mUserRef = DatabaseManager.getUserRef();

//                DatabaseReference currentUser = mUserRef.child("Test").child(username);
//                Log.d("!!!", currentUser.getKey());
//
//                currentUser.addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        if(snapshot.exists()){
//                            HashMap <String, Object> userMap = (HashMap<String, Object>) snapshot.getValue();
//                            if(password == ((String) userMap.get("user_pass"))){
//                                Intent e = new Intent(Login.this, MainActivity.class);
//                                startActivity(e);
//                            }
//
//                            else{
////                                usernameField.setText("");
//                                passwordField.setText("");
//
//                                Toast.makeText(Login.this, "Incorrect Username/Password, try again", Toast.LENGTH_SHORT).show();
//                            }
//                        }
//
//                        else {
//                            Log.d("TAG", "No user data found");
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Log.w("TAG", "Failed to read value:", error.toException());
//                    }
//                });
            }
        });
    }
}