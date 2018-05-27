package com.example.kthrya.myapplication3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    private MqttAndroidClient client;
    static String host = "tcp://192.168.219.115:1883";
    private int qos = 2;

    TextView inputTextView, speedTextView;
    Switch routeSwitch;
    SeekBar speedSeekBar;
    ImageButton photoBtn;
    RelativeLayout joystickLayout;
    JoyStick joyStick;
    ImageView settingBtn;
    WebView webView;

    SharedPreferences sp;
    String config_move;
    boolean config_fire;

    private WebSettings webSettings;
    SharedPreferences.OnSharedPreferenceChangeListener listener;

    //수동 이동모드
    private static final int MODE_MANUAL = 0;
    //경로 저장모드
    private static final int MODE_AUTOREC = 10;
    //자동 이동모드
    private static final int MODE_AUTO = 11;
    //얼굴 인식 모드
    private static final int MODE_FACE = 20;

    //이동 모드 초기값 = 수동 조작
    private int moveMode;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //액션바 숨기기
        getSupportActionBar().hide();

        // mqtt

        try {
            String clientId = MqttClient.generateClientId();

            client = new MqttAndroidClient(this.getApplicationContext()
                    , host
                    , clientId);

            IMqttToken token = client.connect();

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("mqtt", "success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("mqtt", "fail");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        /** 초기화 **/
        //입력 값을 보여준다.(디버그 용)
        inputTextView = findViewById(R.id.inputTextView);
        //경로저장 모드 전환 스위치
        routeSwitch = findViewById(R.id.routeSwitch);
        //속도 조절바, 속도 값
        speedSeekBar = findViewById(R.id.speedSeekBar);
        speedTextView = findViewById(R.id.speedTextView);
        //사진찍기 버튼
        photoBtn = findViewById(R.id.photoBtn);
        //조이스틱 레이아웃
        joystickLayout = findViewById(R.id.joystick_layout);
        //조이스틱 입력 처리 및 표현
        joyStick = new JoyStick(getApplicationContext(), joystickLayout, R.drawable.joystick_center);
        //스트리밍 영상 뷰
        webView = findViewById(R.id.webView);

        webView.setWebViewClient(new WebViewClient());
        webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.loadUrl("http://192.168.0.7:5000");


        //설정 버튼
        settingBtn = findViewById(R.id.settingImageView);

        //설정 버튼 제어
        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });

        //설정 파일 값 불러오기
        sp = getSharedPreferences("CONFIG", Context.MODE_PRIVATE);
        loadConfig();


        joyStick.setStickSize(70, 70);
        joyStick.setStickAlpha(100);
        joyStick.setOffset(30);
        joyStick.setMinimumDistance(50);

        joystickLayout.setOnTouchListener(new JoystickListener());

        //경로저장 스위치 제어 (스위치는 수동모드일때 조작 가능, 스위치 OFF 시 수동모드로 전환)
        routeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    inputTextView.setText("switch : 경로저장 ON");
                    moveMode = MODE_AUTOREC;
                    checkMode(moveMode);
                } else {
                    inputTextView.setText("switch : 경로저장 OFF");
                    moveMode = MODE_MANUAL;
                    checkMode(moveMode);
                }
            }
        });

        //속도조절 시크바 제어
        speedSeekBar.setOnSeekBarChangeListener(new SeekBarListener());

        //사진찍기 버튼
        photoBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                checkPermission();
                inputTextView.setText("Photo");
                //영상부분 만 캡처
                webView.setDrawingCacheEnabled(true);
                Bitmap screenBitmap = webView.getDrawingCache();
                FileOutputStream fos;
                String folderName = "CCTV_PICTURES";
                String strFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"+folderName;

                try {
                    // 현재 날짜로 파일을 저장하기 (년월시분초)
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

                    Date today = new Date();
                    String dateString = formatter.format(today);
                    String picName = dateString +".png";

                    File saveFolder = new File(strFolderPath);

                    //폴더가 있는지 확인
                    if(!saveFolder.exists()) {
                        saveFolder.mkdirs(); // 없으면 폴더 생성
                    }

                    String strFilePath = strFolderPath + "/" + picName;
                    File picture = new File(strFilePath);

                    try {
                        fos = new FileOutputStream(picture);
                        screenBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos); // 캡쳐
                        fos.close();

                        // 미디어 스캐너를 통해 모든 미디어 리스트를 갱신시킨다.
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(picture)));
                        Toast.makeText(MainActivity.this, dateString + ".jpg 저장",
                                Toast.LENGTH_LONG).show();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    Log.e("Screen", "" + e.toString());

                }
                webView.setDrawingCacheEnabled(false);
            }
        });

        //설정 파일 변경 확인
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                loadConfig();
            }
        };

        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
        int progress;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            this.progress = progress;
            speedTextView.setText(String.valueOf(progress));
            inputTextView.setText("seekBar : 속도 조절 중 : " + progress);
        }

        //바 터치 시
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            inputTextView.setText("seekBar : 클릭시작");
        }

        //바에서 손을 놓았을 시
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            inputTextView.setText("seekBar : 클릭끝");
            pub("speed", Integer.toString(progress));
        }
    }

    private class JoystickListener implements View.OnTouchListener {
        int oldDirection = -1;


        @Override
        public boolean onTouch(View v, MotionEvent e) {
            joyStick.drawStick(e);

            if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE) {
                int direction = joyStick.getDirection();

                // 연속해서 같은 메시지 안 보내도록
                if(oldDirection == direction)
                    return true;

                // control direction
                if (direction == JoyStick.STICK_NONE) {

                    pub("direction", "stop");
                    inputTextView.setText("stick_none");

                } else if (direction == JoyStick.STICK_UP) {

                    pub("direction", "go");
                    inputTextView.setText("stick_up");

                } else if (direction == JoyStick.STICK_DOWN) {

                    pub("direction", "back");
                    inputTextView.setText("stick_down");

                } else if (direction == JoyStick.STICK_RIGHT) {

                    pub("direction", "right");
                    inputTextView.setText("stick_right");

                } else if (direction == JoyStick.STICK_LEFT) {

                    pub("direction", "left");
                    inputTextView.setText("stick_left");
                }

                // control speed
                int sp = speedSeekBar.getProgress();
               pub("speed", Integer.toString(sp));

                // store previous direction
                oldDirection = direction;

            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                pub("direction", "stop");
            }
            return true;
        }
    }

    public void pub(String topic, String message) {
        try {
            client.publish(topic, message.getBytes(), qos, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //설정파일 초기 시작시 불러오기
    private void loadConfig(){
        config_move = sp.getString("MOVE", "0");
        if (config_move != null) {
            if (config_move.equals("0")){
                config_move = "manual";
                moveMode = MODE_MANUAL;
            }
            else if (config_move.equals("1")){
                config_move = "auto";
                moveMode = MODE_AUTO;
            }
            else if (config_move.equals("2")){
                config_move = "face";
                moveMode = MODE_FACE;
            }
            else config_move = "error";
        }
        //설정에 따라 화면 조작가능 여부 변경
        checkMode(moveMode);

        config_fire = sp.getBoolean("FIRE", false);
        if (config_fire) config_move += " / fire true";
        else config_move += " / fire false";
        inputTextView.setText(config_move);
    }

    //moveMode에 따른 화면조작여부 설정 함수
    private void checkMode(int mode){
        if(mode == MODE_MANUAL){
            routeSwitch.setClickable(true);
            speedSeekBar.setEnabled(true);
            photoBtn.setClickable(true);
            joystickLayout.setEnabled(true);

        }else if(mode == MODE_AUTO || mode == MODE_FACE){
            routeSwitch.setClickable(false);
            speedSeekBar.setEnabled(false);
            photoBtn.setClickable(false);
            joystickLayout.setEnabled(false);

        }else if(mode == MODE_AUTOREC){
            routeSwitch.setClickable(true);
            speedSeekBar.setEnabled(true);
            photoBtn.setClickable(false);
            joystickLayout.setEnabled(true);
        }
    }

    //사진저장 관련 외부저장소 이용 퍼미션 확인
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return false;
        }
    }

}
