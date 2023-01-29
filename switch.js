
import { TuyaContext } from '@tuya/tuya-connector-nodejs';

const context = new TuyaContext({
	baseUrl: 'https://openapi.tuyaeu.com',
	accessKey: '7mrtv4jsc9yx58t9ncek',
	secretKey: '9ac8c1eacc7a42f68eaf555dbfcb2d95',
    
});

const main = async () => {
 // Define the device ID
  const device_id = "9b555620cbd15b0f05ofe4";
  // Query device details
  const devicedetail  = await context.device.detail({
    device_id: device_id,
  });
  if(!devicedetail.success) {
    new Error();
  }
  console.log("Device details:",devicedetail);
// Send commands
  const commands = await context.request({
    path: `/v1.0/iot-03/devices/${device_id}/commands`,
    method: 'POST',
    body: {
      "commands":[{"code":"switch_led","value":true}]
    }
  });
  if(!commands.success) {
    new Error();
  }
  console.log("Execution result:",commands);
};
main().catch(err => {
  console.log(err);
});
