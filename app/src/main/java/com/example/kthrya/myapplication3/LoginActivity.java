package com.example.kthrya.myapplication3;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity {

    EditText ipEdit,ipEdit2;
    Button accessBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ipEdit = findViewById(R.id.ipText);
        ipEdit2 = findViewById(R.id.ipText2);
        accessBtn = findViewById(R.id.acessBtn);


        accessBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final String ip = ipEdit.getText().toString();
                final String ip2 = ipEdit2.getText().toString();

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("IP_KEY",ip);
                intent.putExtra("IP_KEY2",ip2);
                startActivity(intent);
            }
        });
    }

}
