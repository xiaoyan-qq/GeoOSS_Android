package com.example.xiaoxiao.geooss_android;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.xiaoxiao.geooss_android.base.BaseActivity;
import com.example.xiaoxiao.geooss_android.util.SystemConstant;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.SessionType;
import com.vondear.rxtools.RxFileTool;
import com.vondear.rxtools.RxTimeTool;
import com.vondear.rxtools.view.RxToast;
import com.vondear.rxtools.view.dialog.RxDialogSure;
import com.vondear.rxtools.view.dialog.RxDialogSureCancel;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class GeoCameraViewActivity extends BaseActivity {
    private Button btn_video_time_add, btn_video_time_sub;
    private View layer_time_control;//控制视频拍摄时间的layer
    private ToggleButton tbtn_video_pic_switch;//切换拍照或摄像功能
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
                        if (AndPermission.hasAlwaysDeniedPermission(GeoCameraViewActivity.this, permissions)) {
                            // TODO what to do
                            StringBuilder sb = null;
                            if (permissions != null) {
                                sb = new StringBuilder();
                                for (String permission : permissions) {
                                    sb.append(permission + ",");
                                }
                            }
                            if (sb != null) {
                                sb.substring(0, sb.length() - 1);
                            }
                            final RxDialogSure rxDialogSure = new RxDialogSure(GeoCameraViewActivity.this);
                            rxDialogSure.setTitle("提示");
                            rxDialogSure.setContent("您拒绝了" + sb + "等权限，可能导致部分功能无法正常使用，您可以在系统设置-应用管理中再次打开这些权限以获取完整的应用体验");
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
            if (permissions != null) {
                sb = new StringBuilder();
                for (String permission : permissions) {
                    sb.append(permission + ",");
                }
            }
            // 这里使用一个Dialog询问用户是否继续授权。
            final RxDialogSureCancel rxDialogSureCancel = new RxDialogSureCancel(GeoCameraViewActivity.this);
            rxDialogSureCancel.setTitle("提示");
            rxDialogSureCancel.setContent("您拒绝了" + sb + "等权限，可能导致部分功能无法正常使用，是否重新授权");
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
        layer_time_control = findViewById(R.id.layer_video_time_control);
        btn_video_time_add = findViewById(R.id.btn_video_time_add);
        btn_video_time_sub = findViewById(R.id.btn_video_time_sub);
        tv_camera_time_value = findViewById(R.id.tv_camera_time_value);
        tbtn_video_pic_switch = findViewById(R.id.tbtn_video_pic_switch);

        mCameraView = findViewById(R.id.camera);
        mCameraView.setSessionType(SessionType.VIDEO);//默认为摄像
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
                //减少每次的录制时间
                int currentTimeLength = Integer.parseInt(tv_camera_time_value.getText().toString());
                if (currentTimeLength <= 1) {
                    RxToast.info("最小时间不能小于1分钟");
                    return;
                }
                currentTimeLength = currentTimeLength - 1;
                tv_camera_time_value.setText(currentTimeLength + "");
            }
        });

        tbtn_video_pic_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {//选中状态下为拍照
                    mCameraView.setSessionType(SessionType.PICTURE);
                } else {//非选中状态下为摄像
                    mCameraView.setSessionType(SessionType.VIDEO);
                }
            }
        });

        tbtn_camera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!tbtn_video_pic_switch.isChecked()) {//摄像模式
                    if (b) {//开始录制，隐藏设置时间的按钮
                        setVideoTimeLengthBtnVisiable(false);
                        int currentTimeLength = Integer.parseInt(tv_camera_time_value.getText().toString());
                        mCameraView.setVideoMaxDuration(currentTimeLength * 1000);
                        File videoFile = initVideoOrPicFile(1);
                        if (videoFile != null) {
                            mCameraView.startCapturingVideo(videoFile);
                        } else {
                            tbtn_camera.setChecked(false);
                        }
                    } else {
                        setVideoTimeLengthBtnVisiable(true);
                        if (mCameraView.isStarted()) {
                            mCameraView.stopCapturingVideo();
                        }
                    }
                } else {//拍照模式
                    if (b) {//拍照
                        mCameraView.capturePicture();
                    }
                }
            }
        });
        //拍摄好的视频或照片文件回调
        mCameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] jpeg) {
                super.onPictureTaken(jpeg);
                CameraUtils.decodeBitmap(jpeg, new CameraUtils.BitmapCallback() {
                    @Override
                    public void onBitmapReady(Bitmap bitmap) {
                        File picFile = initVideoOrPicFile(1);
                        if (picFile != null) {
                            saveBitmapFile(bitmap, picFile.getAbsolutePath());
                        }
                    }
                });
                tbtn_camera.setChecked(false);
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

    /**
     * @param : type-0：视频，1：照片
     * @return :
     * @method : initVideoOrPicFile
     * @Author : xiaoxiao
     * @Describe : 初始化要存储的视频或照片文件夹
     * @Date : 2018/6/1
     */
    private File initVideoOrPicFile(int type) {
        if (RxFileTool.isSDCardEnable()) {
            String folderPath = RxFileTool.getSDCardPath() + SystemConstant.VIDEO_PATH;
            String suffixStr = ".mp4";
            if (type == 1) {
                folderPath = RxFileTool.getSDCardPath() + SystemConstant.PICTURE_PATH;
                suffixStr = ".jpg";
            }
            File file = new File(folderPath, RxTimeTool.date2String(RxTimeTool.getCurTimeDate(), new SimpleDateFormat("yyyyMMddhhmmss")) + suffixStr);
            if (!file.getParentFile().exists()) {
                boolean createFolderResult = file.getParentFile().mkdirs();
                if (createFolderResult) {
                    return file;
                } else {
                    RxToast.info("无法将视频写入到存储卡中，请检查是否有存储卡或写入权限");
                }
            }
        } else {
            RxToast.info("无法获取存储卡位置");
        }
        return null;
    }

    /**
     * 把batmap 转file
     *
     * @param bitmap
     * @param filepath
     */
    public static File saveBitmapFile(Bitmap bitmap, String filepath) {
        File file = new File(filepath);//将要保存图片的路径
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
