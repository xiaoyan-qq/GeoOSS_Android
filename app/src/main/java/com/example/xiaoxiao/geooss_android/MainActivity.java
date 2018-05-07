package com.example.xiaoxiao.geooss_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.xiaoxiao.geooss_android.base.BaseActivity;
import com.just.agentweb.AgentWeb;

public class MainActivity extends BaseActivity {

    private Button btn_preview_video/*视频拍摄按钮*/, btn_open_url/*打开指定url按钮*/;
    private AgentWeb mAgentWeb;
    private ViewGroup rootView;
    private static final String TAG="MainActivity" ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView= (ViewGroup) LayoutInflater.from(this).inflate(R.layout.activity_main,null);
        setContentView(rootView);
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
                Intent openWebIntent=new Intent(MainActivity.this,BaseWebActivity.class);
                startActivity(openWebIntent);
            }
        });
    }
}
