package com.example.xiaoxiao.geooss_android.util;

import android.content.Context;

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
import com.vondear.rxtools.RxDataTool;
import com.vondear.rxtools.RxTool;

import org.json.JSONObject;

import java.util.Date;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by xiaoxiao on 2018/6/1.
 */

public class AliYunUtil {
    private static AliYunUtil instance;

    public static AliYunUtil getInstance() {
        if (instance == null) {
            instance = new AliYunUtil();
        }
        return instance;
    }

    public OSS initAliYunOSS(Context mContext, END_POINT_ALIYUN end_point_aliyun, final String deviceId) {
        String endpoint = end_point_aliyun.url;
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
                            String ak = jsonObjs.getString("accessKeyId");
                            String sk = jsonObjs.getString("accessKeySecret");
                            String token = jsonObjs.getString("securityToken");
                            String expiration = jsonObjs.getString("expiration");
                            return new OSSFederationToken(ak, sk, token, expiration);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        OSSLog.enableLog();
        OSS oss = new OSSClient(mContext, endpoint, credentialProvider);
        return oss;
    }

    /**
     * @param :
     * @return :
     * @method :
     * @Author : xiaoxiao
     * @Describe : 异步方式上传文件
     * @Date : 2018/5/18
     */
    public OSSAsyncTask uploadDataAsync(OSS oss, PutObjectRequest put, OSSProgressCallback progressCallback, OSSCompletedCallback completedCallback) {
        // 构造上传请求
//        PutObjectRequest put = new PutObjectRequest("xiaoxiao-test", "name", new String("测试name").getBytes());
        // 异步上传时可以设置进度回调
//        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
//            @Override
//            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
//                Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
//            }
//        });
        put.setProgressCallback(progressCallback);
        OSSAsyncTask task = oss.asyncPutObject(put, completedCallback
//                new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
//            @Override
//            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
//                Log.d("PutObject", "UploadSuccess");
//            }
//
//            @Override
//            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
//                // 请求异常
//                if (clientExcepion != null) {
//                    // 本地异常如网络异常等
//                    clientExcepion.printStackTrace();
//                }
//                if (serviceException != null) {
//                    // 服务异常
//                    Log.e("ErrorCode", serviceException.getErrorCode());
//                    Log.e("RequestId", serviceException.getRequestId());
//                    Log.e("HostId", serviceException.getHostId());
//                    Log.e("RawMessage", serviceException.getRawMessage());
//                }
//            }
//        }
        );
        return task;
    }

    /**
     * @param :
     * @return :
     * @method : uploadData
     * @Author : xiaoxiao
     * @Describe : 同步方式上传数据
     * @Date : 2018/6/7
     */
    public PutObjectResult uploadData(OSS oss, PutObjectRequest put) {
        try {
            if (put != null && oss != null) {
                return oss.putObject(put);
            }
        } catch (ClientException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @author : xiaoxiao
     * @version V1.0
     * @ClassName : AliYunUtil
     * @Date : 2018/6/1
     * @Description: 定义aliyun的接入点
     */
    public enum END_POINT_ALIYUN {
        CN_HANGZHOU("oss-cn-hangzhou", "华东1", "oss-cn-hangzhou.aliyuncs.com"),
        CN_SHANGHAI("oss-cn-shanghai", "华东2", "oss-cn-shanghai.aliyuncs.com"),
        CN_QINGDAO("oss-cn-qingdao", "华北1", "oss-cn-qingdao.aliyuncs.com"),
        CN_BEIJING("oss-cn-beijing", "华北2", "oss-cn-beijing.aliyuncs.com"),
        CN_ZHANGJIAKOU("oss-cn-zhangjiakou", "华北3", "oss-cn-zhangjiakou.aliyuncs.com"),
        CN_HUHEHAOTE("oss-cn-huhehaote", "华北5", "oss-cn-huhehaote.aliyuncs.com"),
        CN_SHENZHEN("oss-cn-shenzhen", "华南1", "oss-cn-shenzhen.aliyuncs.com");

        END_POINT_ALIYUN(String region_en, String region_cn, String url) {
            this.region_en = region_en;
            this.region_cn = region_cn;
            this.url = url;
        }

        public String region_en;//region的英文名
        public String region_cn;//region的中文名
        public String url;//endpoint对应的url-外网
    }
}
