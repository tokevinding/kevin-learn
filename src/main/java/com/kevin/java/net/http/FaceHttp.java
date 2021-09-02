package com.kevin.java.net.http;

import com.kevin.common.utils.json.JsonUtil;
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
public class FaceHttp {

    public static void main(String[] args) throws IOException {
//        get();
        post();
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
            request("http://gtw.qa.enmonster.com/product-eboss-ms/shop/price/check/lock/record", RequestMethod.GET, new RequestBodyParam(172451L));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void request(String urlStr, RequestMethod requestMethod, Object requestBody) throws IOException {
        request(urlStr, requestMethod, requestBody, null, null);
    }


    public static void request(String urlStr, RequestMethod requestMethod, Object requestBody, Map<String, String> reqHeaders, Map<String, String> params) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod(requestMethod.name());
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("Content-type", "application/json");
        connection.connect();

        if (Objects.nonNull(requestBody)) {
            //写入参（requestBody）
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(JsonUtil.toString(requestBody).getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())))) {
            System.out.println(reader.readLine());
        }
    }

}
