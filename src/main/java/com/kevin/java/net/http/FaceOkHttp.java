package com.kevin.java.net.http;

import com.kevin.common.utils.json.JsonUtil;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * @author dinghaifeng
 * @date 2021-09-02 15:10:02
 * @desc
 */
public class FaceOkHttp {

    public static void main(String[] args) throws IOException {
        get();
//        post();
    }

    public static void get() {
        try {
            request("http://gtw.qa.enmonster.com/product-eboss-ms/shop/price/lock/type/list", RequestMethod.GET, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void post() {
        try {
            request("http://gtw.qa.enmonster.com/product-eboss-ms/shop/price/check/lock/record", RequestMethod.POST, new RequestBodyParam(172451L));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void request(String urlStr, RequestMethod requestMethod, Object requestBody) throws IOException {
        request(urlStr, requestMethod, requestBody, null, null);
    }


    public static void request(String urlStr, RequestMethod requestMethod, Object requestBodyParam, Map<String, String> reqHeaders, Map<String, String> params) throws IOException {
        OkHttpClient client = new OkHttpClient();

        String content = "";
        if (Objects.nonNull(requestBodyParam)) {
            content = JsonUtil.toString(requestBodyParam);
        }

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"), content);
        Request.Builder reqBuilder = new Request.Builder().url(urlStr);
        if (RequestMethod.POST.equals(requestMethod)) {
            reqBuilder.post(requestBody);
        } else {
            reqBuilder.get();
        }
        Request request = reqBuilder.build();
        Call call = client.newCall(request);
        Response response = call.execute();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(response.body().byteStream())))) {
            System.out.println(reader.readLine());
        }

    }

}
