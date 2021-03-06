/*
Copyright 2013 The MITRE Corporation, All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.mitre.svmp.client;

import org.json.JSONException;
import org.json.JSONObject;
import org.mitre.svmp.protocol.SVMPProtocol;

import com.citicrowd.oval.R;
import com.oval.app.activities.RTCActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

/**
 * @author Joe Portner Receives Intents from the server to act upon on the
 *         client-side
 */
public class IntentHandler {
	private static final String TAG = IntentHandler.class.getName();

	public static void inspect(SVMPProtocol.Response response, Context context) {
		SVMPProtocol.Intent intent = response.getIntent();
		switch (intent.getAction()) {
		case ACTION_DIAL:
			Log.d(TAG, String.format("Received 'call' Intent for number '%s'", intent.getData()));
			int telephonyEnabled = isTelephonyEnabled(context);
			if (telephonyEnabled == 0) {
				Intent call = new Intent(Intent.ACTION_CALL);
				call.setData(Uri.parse(intent.getData()));
				context.startActivity(call);
			} else {
				// phone calls are not supported on this device; send a Toast to
				// the user to let them know
				Toast toast = Toast.makeText(context, telephonyEnabled, Toast.LENGTH_LONG);
				toast.show();
			}
			break;
		case ACTION_VIEW:
			String jsonStr = intent.getData().toString();
			JSONObject jObj = null;
			String message = null;
			try {
				jObj = new JSONObject(jsonStr);
				message = jObj.getString("message");

				if (jObj != null && message != null) {
					switch (message) {
					case "keyboardStarted":

						InputMethodManager imm = (InputMethodManager) context
								.getSystemService(context.INPUT_METHOD_SERVICE);
						imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
						break;

					case "appInstalled":

						String success = jObj.getString("success");
						String packageName = jObj.getString("packageId");

						if (success != null) {
							if (success.equals("true"))
								startApp(packageName, context);
						}
						break;

					default:
						break;
					}
				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			break;
		default:
			break;
		}
	}

	public static void startApp(String packageName, Context context) {
		/*
		 * DatabaseHandler dbHandler = new DatabaseHandler(context);
		 * 
		 * AppInfo appInfo = dbHandler.getNotInstalledAppInfo(1); if (appInfo !=
		 * null) { appInfo.setIsInstalled(1); dbHandler.updateAppInfo(appInfo);
		 */

		Intent i = new Intent(context, RTCActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra("pkgName", packageName);
		
		context.startActivity(i);

		// }
	}

	// returns an error message if telephony is not enabled
	private static int isTelephonyEnabled(Context context) {
		int resId = 0;
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		if (tm != null) {
			if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_GSM
					&& !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
				resId = R.string.intentHandler_toast_noTelephonyCDMA;
			else if (tm.getSimState() != TelephonyManager.SIM_STATE_READY)
				resId = R.string.intentHandler_toast_noTelephonyGSM;
		}
		return resId;
	}
}
