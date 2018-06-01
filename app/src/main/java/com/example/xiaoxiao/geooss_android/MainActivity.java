package com.example.xiaoxiao.geooss_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.alibaba.sdk.android.oss.OSS;
import com.example.xiaoxiao.geooss_android.base.BaseActivity;
import com.example.xiaoxiao.geooss_android.util.AliYunUtil;
import com.just.agentweb.AgentWeb;
import com.vondear.rxtools.RxThreadPoolTool;

import java.util.concurrent.TimeUnit;

public class MainActivity extends BaseActivity {

    private Button btn_preview_video/*视频拍摄按钮*/, btn_open_url/*打开指定url按钮*/;
    private AgentWeb mAgentWeb;
    private ViewGroup rootView;
    private static final String TAG = "MainActivity";
    private final String DEVICE_ID = "10000000000000000002";//测试用的deviceId，用来获取sts服务返回的token
    private OSS oss;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        setContentView(rootView);
        initView();
        oss= AliYunUtil.getInstance().initAliYunOSS(MainActivity.this, AliYunUtil.END_POINT_ALIYUN.CN_BEIJING, DEVICE_ID);//获取上传到aliyun的工具类，endpoint可以根据当前位置实时更新并实时修改
    }

    private void initView() {
        btn_preview_video = findViewById(R.id.btn_preview_video);
        btn_open_url = findViewById(R.id.btn_open_url);
        btn_preview_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//视频拍摄
                Intent openWebIntent = new Intent(MainActivity.this, GeoCameraViewActivity.class);
                startActivity(openWebIntent);
            }
        });

        btn_open_url.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//打开指定url
                Intent openWebIntent = new Intent(MainActivity.this, BaseWebActivity.class);
                startActivity(openWebIntent);
            }
        });
    }

    private void initUploadAliYun() {
        RxThreadPoolTool threadPoolTool = new RxThreadPoolTool(RxThreadPoolTool.Type.SingleThread, 3);
        threadPoolTool.scheduleWithFixedRate(new Runnable() {
            @Override
            public void run() {
                if (oss!=null){
                    //用RxJava背压方式上传指定数据
                    
                }
            }
        },5,5, TimeUnit.MINUTES);//没隔5分钟尝试上传一次数据
    }
}
