package heatmiserbot;

import heatmiserbot.TuyaSender;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;

import org.json.JSONObject;

public class Switch {

    /**
     * Sets the tuya switch controlling pump to on/off
     *
     * @param state
     */
    public static void set(String deviceID, String accessID, String secret, boolean state) {

        TuyaSender.log("Setting switch to " + (state ? " On" : " Off"));
        try {

            TuyaSender.creds c = TuyaSender.getCreds(accessID, secret);  // request an access token
            String path="/v1.0/iot-03/devices/" + deviceID + "/commands";
            String body="{\"commands\":[{\"code\":\"switch_1\",\"value\":" + (state ? "true" : "false") + "}]}";
            Object result = TuyaSender.execute(accessID, secret, c.token, path, "POST", body, new HashMap<>());
            TuyaSender.log("Result of switch control:"+result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
