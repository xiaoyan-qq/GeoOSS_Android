package com.example.xiaoxiao.geooss_android;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.xiaoxiao.geooss_android.base.BaseActivity;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.SessionType;
import com.vondear.rxtools.RxFileTool;
import com.vondear.rxtools.RxTimeTool;
import com.vondear.rxtools.view.RxToast;
import com.vondear.rxtools.view.dialog.RxDialog;
import com.vondear.rxtools.view.dialog.RxDialogSure;
import com.vondear.rxtools.view.dialog.RxDialogSureCancel;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

public class GeoCameraViewActivity extends BaseActivity {
    private Button btn_video_time_add, btn_video_time_sub;
    private TextView tv_camera_time_value;
    private CameraView mCameraView;
    private ToggleButton tbtn_camera;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo_cameraview);

        initView();
        //申请权限
        getSomePermission();
    }

    private void getSomePermission() {
        AndPermission.with(this)
                .permission(Permission.Group.CAMERA, Permission.Group.STORAGE)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        // TODO what to do.
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        if (AndPermission.hasAlwaysDeniedPermission(GeoCameraViewActivity.this, permissions)){
                            // TODO what to do
                            StringBuilder sb = null;
                            if (permissions!=null){
                                sb=new StringBuilder();
                                for (String permission:permissions){
                                    sb.append(permission+",");
                                }
                            }
                            if (sb!=null){
                                sb.substring(0,sb.length()-1);
                            }
                            final RxDialogSure rxDialogSure=new RxDialogSure(GeoCameraViewActivity.this);
                            rxDialogSure.setTitle("提示");
                            rxDialogSure.setContent("您拒绝了"+sb+"等权限，可能导致部分功能无法正常使用，您可以在系统设置-应用管理中再次打开这些权限以获取完整的应用体验");
                            rxDialogSure.setSure("确定");
                            rxDialogSure.setSureListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    rxDialogSure.dismiss();
                                }
                            });
                            rxDialogSure.show();
                        }
                    }
                })
                .rationale(mRationale)
                .start();
    }

    private Rationale mRationale = new Rationale() {
        @Override
        public void showRationale(Context context, List<String> permissions,
                                  final RequestExecutor executor) {
            StringBuilder sb = null;
            if (permissions!=null){
                sb=new StringBuilder();
                for (String permission:permissions){
                    sb.append(permission+",");
                }
            }
            // 这里使用一个Dialog询问用户是否继续授权。
            final RxDialogSureCancel rxDialogSureCancel=new RxDialogSureCancel(GeoCameraViewActivity.this);
            rxDialogSureCancel.setTitle("提示");
            rxDialogSureCancel.setContent("您拒绝了"+sb+"等权限，可能导致部分功能无法正常使用，是否重新授权");
            rxDialogSureCancel.setSure("确定");
            rxDialogSureCancel.setSureListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    rxDialogSureCancel.dismiss();
                    // 如果用户继续：
                    executor.execute();
                }
            });
            rxDialogSureCancel.setCancel("取消");
            rxDialogSureCancel.setCancelListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 如果用户中断：
                    executor.cancel();
                }
            });
            rxDialogSureCancel.show();
        }
    };

    private void initView() {
        btn_video_time_add = findViewById(R.id.btn_video_time_add);
        btn_video_time_sub = findViewById(R.id.btn_video_time_sub);
        tv_camera_time_value = findViewById(R.id.tv_camera_time_value);
        mCameraView = findViewById(R.id.camera);
        mCameraView.setSessionType(SessionType.VIDEO);
        tbtn_camera = findViewById(R.id.tbtn_camera);

        btn_video_time_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //增加每次的录制时间
                int currentTimeLength = Integer.parseInt(tv_camera_time_value.getText().toString());
                if (currentTimeLength >= 10) {
                    RxToast.info("最大时间不能超过10分钟");
                    return;
                }
                currentTimeLength = currentTimeLength + 1;
                tv_camera_time_value.setText(currentTimeLength + "");
            }
        });
        btn_video_time_sub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //增加每次的录制时间
                int currentTimeLength = Integer.parseInt(tv_camera_time_value.getText().toString());
                if (currentTimeLength <= 1) {
                    RxToast.info("最小时间不能小于1分钟");
                    return;
                }
                currentTimeLength = currentTimeLength - 1;
                tv_camera_time_value.setText(currentTimeLength + "");
            }
        });

        tbtn_camera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {//开始录制，隐藏设置时间的按钮
                    setVideoTimeLengthBtnVisiable(false);
                    int currentTimeLength = Integer.parseInt(tv_camera_time_value.getText().toString());
                    mCameraView.setVideoMaxDuration(currentTimeLength * 1000);
                    if (RxFileTool.isSDCardEnable()) {
                        File videoFile = new File(RxFileTool.getSDCardPath() + "/GeoOSS/video/" + RxTimeTool.date2String(RxTimeTool.getCurTimeDate(), new SimpleDateFormat("yyyyMMddhhmmss")) + ".mp4");
                        if (!videoFile.getParentFile().exists()) {
                            boolean createFolderResult = videoFile.getParentFile().mkdirs();
                            System.out.print(createFolderResult);
                        }
                        mCameraView.startCapturingVideo(videoFile);
                    } else {
                        RxToast.info("无法获取存储卡位置");
                        compoundButton.setChecked(false);
                    }
                } else {
                    setVideoTimeLengthBtnVisiable(true);
                    if (mCameraView.isStarted()) {
                        mCameraView.stopCapturingVideo();
                    }
                }
            }
        });
        //拍摄好的视频文件回调
        mCameraView.addCameraListener(new CameraListener() {
            @Override
            public void onVideoTaken(File video) {
                super.onVideoTaken(video);
 
            }
        });
    }

    private void setVideoTimeLengthBtnVisiable(boolean isVisiable) {
        btn_video_time_sub.setVisibility(isVisiable ? View.VISIBLE : View.GONE);
        btn_video_time_add.setVisibility(isVisiable ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraView.destroy();
    }
}
