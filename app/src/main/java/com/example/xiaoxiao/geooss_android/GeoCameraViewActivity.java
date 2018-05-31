package com.example.xiaoxiao.geooss_android;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSFederationCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSFederationToken;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.example.xiaoxiao.geooss_android.base.BaseActivity;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.SessionType;
import com.vondear.rxtools.RxDataTool;
import com.vondear.rxtools.RxFileTool;
import com.vondear.rxtools.RxTimeTool;
import com.vondear.rxtools.RxTool;
import com.vondear.rxtools.view.RxToast;
import com.vondear.rxtools.view.dialog.RxDialogSure;
import com.vondear.rxtools.view.dialog.RxDialogSureCancel;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GeoCameraViewActivity extends BaseActivity {
    private Button btn_video_time_add, btn_video_time_sub;
    private TextView tv_camera_time_value;
    private CameraView mCameraView;
    private ToggleButton tbtn_camera;
    private final String DEVICE_ID = "10000000000000000002";//测试用的deviceId，用来获取sts服务返回的token

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
                    initAliYunOSS();//测试上传数据
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

    private void initAliYunOSS() {
        String endpoint = "http://oss-cn-beijing.aliyuncs.com";
        // 在移动端建议使用STS方式初始化OSSClient。
        // 更多信息可查看sample 中 sts 使用方式(https://github.com/aliyun/aliyun-oss-android-sdk/tree/master/app/src/main/java/com/alibaba/sdk/android/oss/app)
//        OSSCredentialProvider credentialProvider = new OSSStsTokenCredentialProvider("LTAI8WtDDODaqxEI", "3REJHb4GcDGvSF5ZyVl4otoUYzbv8I", "acs:ram::1453306301353415:role/xiaoxiao-role");
        OSSCredentialProvider credentialProvider = new OSSFederationCredentialProvider() {
            @Override
            public OSSFederationToken getFederationToken() {
                try {
                    String aaaMd5 = RxTool.Md5("aaa");
                    System.out.print("Md5------------" + RxTool.Md5("aaa"));
                    OkHttpClient httpClient = new OkHttpClient();
                    String deviceId = DEVICE_ID;
                    String currentTimeTamp = String.valueOf(new Date().getTime());
//                    RequestBody body = new FormBody.Builder().add("deviceId", deviceId).add("timestamp", currentTimeTamp).add("sign", RxTool.Md5(deviceId + currentTimeTamp)).build();
                    HttpUrl url = new HttpUrl.Builder()
                            .scheme("http")
                            .host("47.104.153.134")
                            .addPathSegments("/sts/auth")
                            .addQueryParameter("deviceId", deviceId).addQueryParameter("timestamp", currentTimeTamp).addQueryParameter("sign", RxTool.Md5(deviceId + currentTimeTamp))
                            .build();
                    Request ossCredentialRequest = new Request.Builder().url(url).build();
                    Response response = httpClient.newCall(ossCredentialRequest).execute();
                    if (response != null) {
                        String responseStr = response.body().string();
                        if (!RxDataTool.isEmpty(responseStr)) {
                            System.out.print(responseStr);
                            JSONObject jsonObjs = new JSONObject(responseStr);
                            String ak = jsonObjs.getString("AccessKeyId");
                            String sk = jsonObjs.getString("AccessKeySecret");
                            String token = jsonObjs.getString("SecurityToken");
                            String expiration = jsonObjs.getString("Expiration");
                            return new OSSFederationToken(ak, sk, token, expiration);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        //该配置类如果不设置，会有默认配置，具体可看该类
        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
        conf.setMaxConcurrentRequest(5); // 最大并发请求数，默认5个
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
        OSSLog.enableLog();
        OSS oss = new OSSClient(getApplicationContext(), endpoint, credentialProvider);
        uploadData(oss);
    }

    /**
     * @param :
     * @return :
     * @method :
     * @Author : xiaoxiao
     * @Describe :
     * @Date : 2018/5/18
     */
    private void uploadData(OSS oss) {
        // 构造上传请求
        PutObjectRequest put = new PutObjectRequest("xiaoxiao-test-1", "name", new String("测试name").getBytes());
        // 异步上传时可以设置进度回调
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
            }
        });
        OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.d("PutObject", "UploadSuccess");
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });
    }
}
