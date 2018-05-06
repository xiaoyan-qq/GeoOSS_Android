package com.example.xiaoxiao.geooss_android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button btn_preview_video/*视频拍摄按钮*/, btn_open_url/*打开指定url按钮*/;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView(){
        btn_preview_video=findViewById(R.id.btn_preview_video);
        btn_open_url=findViewById(R.id.btn_open_url);
        btn_preview_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//视频拍摄

            }
        });

        btn_open_url.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//打开指定url

            }
        });
    }
}
