package heatmiserbot;



import com.google.gson.Gson;


import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;


import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.util.StringUtils;

/**
 * @author gongtai.yin
 * @since 2021/08/18
 */
public class TuyaSender {

    // Tuya could endpoint
    private static String endpoint = "https://openapi-weaz.tuyaeu.com";//https://openapi.tuyaeu.com";

    static public class creds {
        public String token;
        public long t;
    }

    public static void log (Object o) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
   LocalDateTime now = LocalDateTime.now();  
        System.out.println(dtf.format(now)+"--->"+o);
    }
    public static creds getCreds(String accessID,String secret) {

        String getTokenPath = "/v1.0/token?grant_type=1";
        Object result = TuyaSender.execute(accessID,secret,getTokenPath, "GET", "", new HashMap<>());
       // System.out.println(result);
        String accessToken=((Map)((Map)result).get("result")).get("access_token").toString();
        Double t=(Double)((Map)result).get("t");
        log("Response from token request:"+gson.toJson(result));

        log("Access token:"+accessToken);
        //System.out.println((long)t.longValue());
        creds c=new creds();
        c.t=t.longValue();
        c.token=accessToken;
        return c;
    }

    private static final MediaType CONTENT_TYPE = MediaType.parse("application/json");
    private static final String EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final Gson gson = new Gson();


    /**
     * Used to obtain tokens, refresh tokens: no token request
     */
    public static Object execute(String accessID,String secret,String path, String method, String body, Map<String, String> customHeaders) {
        return TuyaSender.execute(accessID,secret,"", path, method, body, customHeaders);
    }

    /**
     * 
     * @param accessID  provided by tuya cloud dash
     * @param secret    provided by tuya cloud dash
     * @param accessToken dynamically requested by this code in credential get
     * @param path API to execute
     * @param method usually POST but sometimes GET - check Tuya docs
     * @param body params for the command or empty for some commands
     * @param customHeaders
     * @return 
     */
    public static Object execute(String accessID,String secret,String accessToken, String path, String method, String body, Map<String, String> customHeaders) {
        try {
            String url = endpoint + path;

            Request.Builder request;
            if ("GET".equals(method)) {
                request = getRequest(url);
            } else if ("POST".equals(method)) {
                request = postRequest(url, body);
            } else if ("PUT".equals(method)) {
                request = putRequest(url, body);
            } else if ("DELETE".equals(method)) {
                request = deleteRequest(url, body);
            } else {
                throw new TuyaCloudSDKException("Method only support GET, POST, PUT, DELETE");
            }
            if (customHeaders.isEmpty()) {
                customHeaders = new HashMap<>();
            }
            Headers headers = getHeader(accessID,secret,accessToken, request.build(), body, customHeaders);
            request.headers(headers);
            request.url(endpoint+ getPathAndSortParam(new URL(url)));
            Response response = doRequest(request.build());
            return gson.fromJson(response.body().string(), Object.class);
        } catch (Exception e) {
            throw new TuyaCloudSDKException(e.getMessage());
        }
    }

    /**
     * 
     * @param accessID
     * @param secret
     * @param accessToken
     * @param request
     * @param body
     * @param headerMap
     * @return
     * @throws Exception 
     */ 
    public static Headers getHeader(String accessID,String secret,String accessToken, Request request, String body, Map<String, String> headerMap) throws Exception {
        Headers.Builder hb = new Headers.Builder();

        Map<String, String> flattenHeaders = flattenHeaders(headerMap);
        String t = flattenHeaders.get("t");
        if (StringUtils.isEmpty(t)) {
            t = System.currentTimeMillis() + "";
        }

        hb.add("client_id", accessID);
        hb.add("t", t);
        hb.add("sign_method", "HMAC-SHA256");
        hb.add("lang", "zh");
        hb.add("Signature-Headers", flattenHeaders.getOrDefault("Signature-Headers", ""));
        String nonceStr = flattenHeaders.getOrDefault("nonce", "");
        hb.add("nonce", flattenHeaders.getOrDefault("nonce", ""));
        String stringToSign = stringToSign(request, body, flattenHeaders);
        if (!StringUtils.isEmpty(accessToken)) {
            hb.add("access_token", accessToken);
            hb.add("sign", sign(accessID, secret, t, accessToken, nonceStr, stringToSign));
        } else {
            hb.add("sign", sign(accessID, secret, t, nonceStr, stringToSign));
        }
        return hb.build();
    }

    public static String getPathAndSortParam(URL url) {
        try {
            // supported the query contains zh-Han char
            String query = URLDecoder.decode(url.getQuery(), "UTF-8");
            String path = url.getPath();
            if (StringUtils.isEmpty(query)) {
                return path;
            }
            Map<String, String> kvMap = new TreeMap<>();
            String[] kvs = query.split("\\&");
            for (String kv : kvs) {
                String[] kvArr = kv.split("=");
                if (kvArr.length > 1) {
                    kvMap.put(kvArr[0], kvArr[1]);
                } else {
                    kvMap.put(kvArr[0], "");
                }
            }
            return path + "?" + kvMap.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue())
                    .collect(Collectors.joining("&"));
        } catch (Exception e) {
            return url.getPath();
        }
    }

    private static String stringToSign(Request request, String body, Map<String, String> headers) throws Exception {
        List<String> lines = new ArrayList<>(16);
        lines.add(request.method().toUpperCase());
        String bodyHash = EMPTY_HASH;
        if (request.body() != null && request.body().contentLength() > 0) {
            bodyHash = Sha256Util.encryption(body);
        }
        String signHeaders = headers.get("Signature-Headers");
        String headerLine = "";
        if (signHeaders != null) {
            String[] sighHeaderNames = signHeaders.split("\\s*:\\s*");
            headerLine = Arrays.stream(sighHeaderNames).map(String::trim)
                    .filter(it -> it.length() > 0)
                    .map(it -> it + ":" + headers.get(it))
                    .collect(Collectors.joining("\n"));
        }
        lines.add(bodyHash);
        lines.add(headerLine);
        String paramSortedPath = getPathAndSortParam(request.url().url());
        lines.add(paramSortedPath);
        return String.join("\n", lines);
    }

    private static Map<String, String> flattenHeaders(Map<String, String> headers) {
        Map<String, String> newHeaders = new HashMap<>();
        headers.forEach((name, values) -> {
            if (values == null || values.isEmpty()) {
                newHeaders.put(name, "");
            } else {
                newHeaders.put(name, values);
            }
        });
        return newHeaders;
    }

    /**
     * Calculate sign
     */
    private static String sign(String accessId, String secret, String t, String accessToken, String nonce, String stringToSign) {
        StringBuilder sb = new StringBuilder();
        sb.append(accessId);
        if (!StringUtils.isEmpty(accessToken)) {
            sb.append(accessToken);
        }
        sb.append(t);
        if (!StringUtils.isEmpty(nonce)) {
            sb.append(nonce);
        }
        sb.append(stringToSign);
        return Sha256Util.sha256HMAC(sb.toString(), secret);
    }

    private static String sign(String accessId, String secret, String t, String nonce, String stringToSign) {
        return sign(accessId, secret, t, "", nonce, stringToSign);
    }

    /**
     * Handle get request
     */
    public static Request.Builder getRequest(String url) {
        Request.Builder request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .get();
        } catch (IllegalArgumentException e) {
            throw new TuyaCloudSDKException(e.getMessage());
        }
        return request;
    }

    /**
     * Handle post request
     */
    public static Request.Builder postRequest(String url, String body) {
        Request.Builder request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(CONTENT_TYPE, body));
        } catch (IllegalArgumentException e) {
            throw new TuyaCloudSDKException(e.getMessage());
        }

        return request;
    }

    /**
     * Handle put request
     */
    public static Request.Builder putRequest(String url, String body) {
        Request.Builder request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create(CONTENT_TYPE, body));
        } catch (IllegalArgumentException e) {
            throw new TuyaCloudSDKException(e.getMessage());
        }
        return request;
    }


    /**
     * Handle delete request
     */
    public static Request.Builder deleteRequest(String url, String body) {
        Request.Builder request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .delete(RequestBody.create(CONTENT_TYPE, body));
        } catch (IllegalArgumentException e) {
            throw new TuyaCloudSDKException(e.getMessage());
        }
        return request;
    }

    /**
     * Execute request
     */
    public static Response doRequest(Request request) {
        Response response;
        try {
            response = getHttpClient().newCall(request).execute();
        } catch (IOException e) {
            throw new TuyaCloudSDKException(e.getMessage());
        }
        return response;
    }


    // Get http client
    private static OkHttpClient getHttpClient() {
        OkHttpClient client = new OkHttpClient();
        return client;
    }



    static class Sha256Util {

        public static String encryption(String str) throws Exception {
            return encryption(str.getBytes(StandardCharsets.UTF_8));
        }

        public static String encryption(byte[] buf) throws Exception {
            MessageDigest messageDigest;
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(buf);
            return byte2Hex(messageDigest.digest());
        }

        private static String byte2Hex(byte[] bytes) {
            StringBuilder stringBuffer = new StringBuilder();
            String temp;
            for (byte aByte : bytes) {
                temp = Integer.toHexString(aByte & 0xFF);
                if (temp.length() == 1) {
                    stringBuffer.append("0");
                }
                stringBuffer.append(temp);
            }
            return stringBuffer.toString();
        }

        public static String sha256HMAC(String content, String secret) {
            Mac sha256HMAC = null;
            try {
                sha256HMAC = Mac.getInstance("HmacSHA256");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            try {
                sha256HMAC.init(secretKey);
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
            byte[] digest = sha256HMAC.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return new HexBinaryAdapter().marshal(digest).toUpperCase();
        }
    }


    static class TuyaCloudSDKException extends RuntimeException {

        private Integer code;

        public TuyaCloudSDKException(String message) {
            super(message);
        }

        public TuyaCloudSDKException(Integer code, String message) {
            super(message);
            this.code = code;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        @Override
        public String toString() {
            if (code != null) {
                return "TuyaCloudSDKException: " +
                        "[" + code + "] " + getMessage();
            }

            return "TuyaCloudSDKException: " + getMessage();
        }
    }

}
