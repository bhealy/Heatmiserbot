/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package heatmiserbot;

/**
 *
 * @author bhealy
 */
import java.io.*;
import java.net.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Automates a water pump using a TUYA smart relay. Switches the pump on if ANY of a list of Heatmiser NeoStat thermostats are calling for heat.
 * This allows a wireless control of a pump based on conditional logic across the various stats.
 * I wrote this because my own Nibe heatpump and monifold was wired incorrectly and the pump for 1 floor was permanently ON. This was a complicated optimisation!
 * 
 * @author bhealy
 */
public class HeatMiserBot {

    class DeviceInfoResult {

        Boolean AWAY;
        Boolean COOLING;
        Boolean COOLING_ENABLED;
        float COOLING_TEMPERATURE_IN_WHOLE_DEGREE;
        float CURRENT_TEMPERATURE;
        float CURRENT_SET_TEMPERATURE;
        Boolean DEMAND;
        String device;
        Boolean HEATING; // calling for heat
        Boolean OFFLINE;

    }

    class InfoResult {

        DeviceInfoResult[] devices;
    }

    class StatItem {

        String name;
        Integer id;
    }

    private static final String neoStatIP = "192.168.2.219"; // Replace with the IP address of your thermostat
    private static final int neoStatPort = 4242;
     static String tuyaDeviceID;
     static String tuyaAccessID;
     static String tuyaSecret;

    public static void main(String[] args) throws IOException {

        if (args.length != 4) {
            System.out.println("Usage: COMMAND deviceID accessID secret stat1,stat2,statn    ");
            System.out.println("eg. COMMAND devid accessid secret Esther,Lily  (uses startsWith to natch tokens to actual device names)");
            System.exit(0);
        }
        String[] list = args[3].split(",");
        tuyaDeviceID=args[0];
        tuyaSecret=args[2];
        Socket socket = new Socket(neoStatIP, neoStatPort);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        Gson g = new Gson();

        out.println("{\"GET_ZONES\":0}" + '\0' + '\r');
        String response = in.readLine();

        //System.out.println("Response: " + response);
        Type mapType = new TypeToken<Map<String, Integer>>() {
        }.getType();
        Map<String, Integer> r = new Gson().fromJson(response, mapType);

        boolean allOff=true; // just need one stat to be kooking for juice and we turn pump on
        for (Entry e : r.entrySet()) {
            // Send a command to the thermostat
            in.skip(1);
            out.println("{\"INFO\":\"" + e.getKey().toString() + "\"}" + '\0' + '\r');

            // Read the response from the thermostat
            response = in.readLine();
           // System.out.println("Response: " + response);
            
            // is this a stat of interest

            InfoResult d = g.fromJson(response, InfoResult.class);
            String deviceName=d.devices[0].device.toUpperCase();
            for (String s : list) {
                if (deviceName.toUpperCase().startsWith(s.toUpperCase())) {
                    // we are interested in this stat
                    TuyaSender.log("Found stat of interest: "+deviceName);
                    allOff=allOff && !d.devices[0].HEATING;
                    if (!allOff) {
                        TuyaSender.log("Stat "+deviceName+" calling for heat");
                    }
                    break;
                }
            }
            if (!allOff)
                break;
        }
        socket.close();
                            
        // now set pump based on allOff state - only need one stat to be asking or heat and we run the pump
        Switch.set(tuyaDeviceID,tuyaAccessID,tuyaSecret,!allOff);

    }
}
