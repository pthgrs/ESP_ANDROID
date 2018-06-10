package com.example.kthrya.myapplication3;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity {

    EditText ipEdit,port1Edit, port2Edit;
    Button accessBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ipEdit = findViewById(R.id.ipText);
        port1Edit = findViewById(R.id.portText1);
        port2Edit = findViewById(R.id.portText2);
        accessBtn = findViewById(R.id.acessBtn);


        accessBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final String ip = ipEdit.getText().toString();
                final String msgPort = port1Edit.getText().toString();
                final String videoPort = port2Edit.getText().toString();

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("IP_KEY",ip);
                intent.putExtra("PORT1_KEY",msgPort);
                intent.putExtra("PORT2_KEY",videoPort);
                startActivity(intent);
            }
        });
    }

}
