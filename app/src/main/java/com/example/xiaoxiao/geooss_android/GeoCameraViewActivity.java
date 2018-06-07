package com.example.xiaoxiao.geooss_android;

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
import com.vondear.rxtools.RxThreadPoolTool;
import com.vondear.rxtools.RxTimeTool;
import com.vondear.rxtools.view.RxToast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class GeoCameraViewActivity extends BaseActivity {
    private Button btn_video_time_add, btn_video_time_sub;
    private View layer_time_control;//控制视频拍摄时间的layer
    private ToggleButton tbtn_video_pic_switch;//切换拍照或摄像功能
    private TextView tv_camera_time_value;
    private CameraView mCameraView;
    private ToggleButton tbtn_camera;
    private RxThreadPoolTool threadPoolTool;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo_cameraview);

        threadPoolTool = new RxThreadPoolTool(RxThreadPoolTool.Type.SingleThread, 1);
        initView();
    }

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
                        startVideo();
                    } else {
                        stopVideo();//停止录制
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
                        File picFile = initPicOrVideoCacheFile(1);
                        if (picFile != null) {
                            saveBitmapFile(bitmap, picFile.getAbsolutePath());
                        }
                    }
                });
                tbtn_camera.setChecked(false);
            }

            @Override
            public void onVideoTaken(final File video) {
                super.onVideoTaken(video);
                threadPoolTool.execute(new Runnable() {
                    @Override
                    public void run() {
                        //将该文件剪切到视频文件夹下
                        File videoFile = initPicOrVideoCacheFile(2);
                        RxFileTool.copyOrMoveFile(video, videoFile, true);
                    }
                });

                //视频拍摄结束
                if (tbtn_camera.isChecked()) {//如果当前的录制状态仍然为正在录制，则重新开辟一块新的文件继续录制
                    startVideo();
                }
            }
        });
    }

    /**
     * @param :
     * @return :
     * @method :
     * @Author : xiaoxiao
     * @Describe : 开始拍摄视频
     * @Date : 2018/6/7
     */
    private void startVideo() {
        setVideoTimeLengthBtnVisiable(false);
        int currentTimeLength = Integer.parseInt(tv_camera_time_value.getText().toString());
        mCameraView.setVideoMaxDuration(currentTimeLength * 1000 * 60);
        File videoFile = initPicOrVideoCacheFile(0);
        if (videoFile != null) {
            if (!mCameraView.isCapturingVideo()) {
                mCameraView.stopCapturingVideo();
            }
            mCameraView.startCapturingVideo(videoFile);
        } else {
            tbtn_camera.setChecked(false);//如果无法保存到文件，则自动停止录制
            RxToast.error("无法保存录制的文件！");
        }
    }

    private void stopVideo() {
        setVideoTimeLengthBtnVisiable(true);
        if (mCameraView.isStarted()) {
            mCameraView.stopCapturingVideo();
        }
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
     * @param : 保存类型： 0-缓存目录 1-图片目录 2-视频目录
     * @return :
     * @method : initPicCacheFile
     * @Author : xiaoxiao
     * @Describe : 初始化缓存目录，用户的摄像以及拍照都是首先存储在此缓存目录中，当数据完全写入完成后才会将其转移到指定的视频或拍照目录中
     * @Date : 2018/6/7
     */
    private File initPicOrVideoCacheFile(int type) {
        if (RxFileTool.isSDCardEnable()) {
            String folderPath = RxFileTool.getSDCardPath() + SystemConstant.CACHE_VIDEO_PICTURE_PATH;
            String suffixStr = ".mp4";
            if (type == 1) {
                folderPath = RxFileTool.getSDCardPath() + SystemConstant.PICTURE_PATH;
                suffixStr = ".jpg";
            } else if (type == 2) {
                folderPath = RxFileTool.getSDCardPath() + SystemConstant.VIDEO_PATH;
                suffixStr = ".mp4";
            }
            File file = new File(folderPath, RxTimeTool.date2String(RxTimeTool.getCurTimeDate(), new SimpleDateFormat("yyyyMMddHHmmss")) + suffixStr);
            if (!file.getParentFile().exists()) {
                boolean createFolderResult = file.getParentFile().mkdirs();
                if (createFolderResult) {
                    return file;
                } else {
                    RxToast.info("无法将视频写入到存储卡中，请检查是否有存储卡或写入权限");
                }
            } else {
                return file;
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
