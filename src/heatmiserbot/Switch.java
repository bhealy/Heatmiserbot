package heatmiserbot;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import java.util.HashMap;

public class Switch {

        class SwitchResult {

        boolean result;
    }
    /**
     * Sets the tuya switch controlling pump to on/off
     *
     * @param state
     */
    public static void set(String deviceID, String accessID, String secret, boolean state) {

        TuyaSender.log("Setting switch to " + (state ? " On" : " Off"));
        String error=null;
        try {

            TuyaSender.creds c = TuyaSender.getCreds(accessID, secret);  // request an access token
            String path="/v1.0/iot-03/devices/" + deviceID + "/commands";
            String body="{\"commands\":[{\"code\":\"switch_1\",\"value\":" + (state ? "true" : "false") + "}]}";
            LinkedTreeMap result = (LinkedTreeMap)TuyaSender.execute(accessID, secret, c.token, path, "POST", body, new HashMap<>());
            
            TuyaSender.log("Result of switch control:"+result);
            Boolean r=(Boolean)result.get("success");
            if (r!=null && !r) 
                error=String.valueOf(result.get("msg"));
            if (r==null)
                error="Unknwn (null) response from the command";
            
        } catch (Exception e) {
            e.printStackTrace();
            error=e.toString();
        }
        if (error!=null) {
            // error
            Email.send(HeatMiserBot.emailUser,HeatMiserBot.emailPassword,"Heatmiserbot error switching pump to "+state,"Check Heatmiserbot running on iMac and check pump switch is accessible via Tuya / reset Tuya switch->"+error);
        } 
    }
}
