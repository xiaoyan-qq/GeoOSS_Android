package com.example.xiaoxiao.geooss_android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.example.xiaoxiao.geooss_android.base.BaseActivity;
import com.example.xiaoxiao.geooss_android.util.AliYunUtil;
import com.example.xiaoxiao.geooss_android.util.SystemConstant;
import com.just.agentweb.AgentWeb;
import com.vondear.rxtools.RxDeviceTool;
import com.vondear.rxtools.RxFileTool;
import com.vondear.rxtools.RxThreadPoolTool;
import com.vondear.rxtools.view.RxToast;
import com.vondear.rxtools.view.dialog.RxDialogSure;
import com.vondear.rxtools.view.dialog.RxDialogSureCancel;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;

import org.json.JSONObject;
import org.reactivestreams.Subscription;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.FlowableSubscriber;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends BaseActivity {

    private Button btn_preview_video/*视频拍摄按钮*/, btn_open_url/*打开指定url按钮*/;
    private AgentWeb mAgentWeb;
    private ViewGroup rootView;
    private static final String TAG = "MainActivity";
    private final String DEVICE_ID = "10000000000000000002";//测试用的deviceId，用来获取sts服务返回的token
    private OSS oss;//阿里云的上传接口调用类
    private boolean isUploading = false;//是否正在上传

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        setContentView(rootView);
        initView();
        oss = AliYunUtil.getInstance().initAliYunOSS(MainActivity.this, AliYunUtil.END_POINT_ALIYUN.CN_BEIJING, DEVICE_ID);//获取上传到aliyun的工具类，endpoint可以根据当前位置实时更新并实时修改
        getSomePermission();
        initUploadAliYun();
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
        RxThreadPoolTool threadPoolTool = new RxThreadPoolTool(RxThreadPoolTool.Type.SingleThread, 1);
        threadPoolTool.scheduleWithFixedRate(new Runnable() {
            @Override
            public void run() {
                if (oss != null && !isUploading/*如果当前已经正在上传，则不再继续开启线程上传数据了*/) {
                    Flowable flowable = Flowable.create(new FlowableOnSubscribe<File>() {
                        @Override
                        public void subscribe(FlowableEmitter<File> emitter) throws Exception {
                            //遍历缓存文件中的所有照片和视频文件，自动上传到阿里云
                            isUploading = true;
                            File videoCacheFolder = new File(RxFileTool.getSDCardPath() + SystemConstant.VIDEO_PATH);
                            File picCacheFolder = new File(RxFileTool.getSDCardPath() + SystemConstant.PICTURE_PATH);
                            List<File> videoAndPicFileList = new ArrayList<>();
                            videoAndPicFileList.addAll(RxFileTool.listFilesInDirWithFilter(videoCacheFolder, "mp4"));
                            videoAndPicFileList.addAll(RxFileTool.listFilesInDirWithFilter(picCacheFolder, "jpg"));
                            if (videoAndPicFileList != null && !videoAndPicFileList.isEmpty()) {
                                Iterator fileIterator = videoAndPicFileList.iterator();
                                while (fileIterator.hasNext()) {
                                    emitter.onNext((File) fileIterator.next());
                                }
                            }
                            emitter.onComplete();
                        }
                    }, BackpressureStrategy.BUFFER);

                    FlowableSubscriber<File> subscriber = new FlowableSubscriber<File>() {
                        Subscription subscription;

                        @Override
                        public void onSubscribe(Subscription s) {
                            subscription = s;
                            s.request(1);
                        }

                        @Override
                        public void onNext(File file) {
                            //处理
                            if (file != null) {
                                PutObjectRequest request = new PutObjectRequest("xiaoxiao-test", file.getName(), file.getAbsolutePath());
                                PutObjectResult result = AliYunUtil.getInstance().uploadData(oss, request);
                                if (result.getStatusCode() == 200 && file != null && file.exists()) {
                                    file.delete();
                                }
                                subscription.request(1);
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            isUploading = false;
                        }

                        @Override
                        public void onComplete() {
                            isUploading = false;
                        }
                    };

                    flowable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(subscriber);
                }
            }
        }, 0, 5, TimeUnit.MINUTES);//每隔5分钟尝试上传一次数据
    }

    /**
     * @param :
     * @return :
     * @method : uploadDeviceInfo
     * @Author : xiaoxiao
     * @Describe : 打开程序后首先上传当前的设备信息
     * @Date : 2018/6/7
     */
    private void uploadDeviceInfo() {
        Map<String, String> deviceInfo = new HashMap<>();
        deviceInfo.put("SN", RxDeviceTool.getUniqueSerialNumber());
        deviceInfo.put("IMEI", RxDeviceTool.getIMEI(this));
        deviceInfo.put("IMSI", RxDeviceTool.getIMSI(this));
        deviceInfo.put("SoftVersion", RxDeviceTool.getDeviceSoftwareVersion(this));
        deviceInfo.put("PhoneNum", RxDeviceTool.getLine1Number(this));
        deviceInfo.put("NetworkType", String.valueOf(RxDeviceTool.getNetworkType(this)));
        deviceInfo.put("PhoneType", String.valueOf(RxDeviceTool.getPhoneType(this)));
        deviceInfo.put("SimOperatorName", String.valueOf(RxDeviceTool.getSimOperatorName(this)));
        deviceInfo.put("SimSerialNumber", String.valueOf(RxDeviceTool.getSimSerialNumber(this)));
        deviceInfo.put("SubscriberId", String.valueOf(RxDeviceTool.getSubscriberId(this)));
        deviceInfo.put("BuildBrand", String.valueOf(RxDeviceTool.getBuildBrand()));
        deviceInfo.put("BuildBrandModel", String.valueOf(RxDeviceTool.getBuildBrandModel()));
        deviceInfo.put("BuildMANUFACTURER", String.valueOf(RxDeviceTool.getBuildMANUFACTURER()));
        deviceInfo.put("AppVersionNo", String.valueOf(RxDeviceTool.getAppVersionNo(this)));
        deviceInfo.put("AppVersionName", String.valueOf(RxDeviceTool.getAppVersionName(this)));
        deviceInfo.put("MacAddress", String.valueOf(RxDeviceTool.getMacAddress(this)));
        deviceInfo.put("SerialNumber", String.valueOf(RxDeviceTool.getSerialNumber()));
        if (oss != null) {
            JSONObject jsonObject = new JSONObject(deviceInfo);
            PutObjectRequest put = new PutObjectRequest("xiaoxiao-test", String.valueOf(RxDeviceTool.getSerialNumber()), jsonObject.toString().getBytes());
            PutObjectResult result = AliYunUtil.getInstance().uploadData(oss, put);
            if (result.getStatusCode() != 200) {
                RxToast.error("设备信息没有正常上传!");
            } else {

            }
        }
    }

    private void getSomePermission() {
        AndPermission.with(this)
                .permission(Permission.CAMERA, Permission.READ_EXTERNAL_STORAGE, Permission.READ_PHONE_STATE, Permission.ACCESS_COARSE_LOCATION, Permission.ACCESS_FINE_LOCATION, Permission.WRITE_EXTERNAL_STORAGE)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        // TODO what to do.
                        uploadDeviceInfo();
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        if (AndPermission.hasAlwaysDeniedPermission(MainActivity.this, permissions)) {
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
                            final RxDialogSure rxDialogSure = new RxDialogSure(MainActivity.this);
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
            final RxDialogSureCancel rxDialogSureCancel = new RxDialogSureCancel(MainActivity.this);
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
}
