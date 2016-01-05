/*
 * Copyright (c) 2013 The MITRE Corporation, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this work except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mitre.svmp.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.citicrowd.oval.R;
import com.google.protobuf.ByteString;
import com.oval.app.activities.OvalLoginActivity;
import com.oval.app.network.HTTPServiceHandler;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.mitre.svmp.common.AppInfo;
import org.mitre.svmp.common.Utility;
import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.Response;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Joe Portner Special activity to refresh the client's list of remote
 *         apps
 */
public class AppRTCRefreshAppsActivity extends AppRTCActivity {
	private static final String TAG = AppRTCRefreshAppsActivity.class.getName();

	private boolean fullRefresh;
	ProgressBar preparingPbar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		preparingTextView.setText("Preparing your Apps");

		Intent intent = getIntent();
		this.fullRefresh = intent.getBooleanExtra("fullRefresh", false);
	}

	@Override
	protected void startProgressDialog() {
		// not needed
	}

	@Override
	public void stopProgressDialog() {
		// TODO Auto-generated method stub
		// not needed
	}

	// MessageHandler interface method
	// Called when the client connection is established
	@Override
	public void onOpen() {
		super.onOpen();

		// send a Request message with our current list of apps
		sendAppsRequest();
	}

	// MessageHandler interface method
	// Called when a message is sent from the server, and the SessionService
	// doesn't consume it
	public boolean onMessage(Response data) {
		switch (data.getType()) {
		case APPS:
			handleAppsResponse(data);
			break;
		default:
			// any messages we don't understand, pass to our parent for
			// processing
			super.onMessage(data);
		}
		return true;
	}

	// sends a Request - containing our current apps and their info - to the VM
	// to see what needs to be
	// added, updated, or removed
	private void sendAppsRequest() {
		SVMPProtocol.AppsRequest.Builder arBuilder = SVMPProtocol.AppsRequest.newBuilder();
		arBuilder.setType(SVMPProtocol.AppsRequest.AppsRequestType.REFRESH);
		// set screen density for AppsRequest
		arBuilder.setScreenDensity(Utility.getScreenDensity(this));

		if (!fullRefresh) {
			// if we're not doing a full refresh, loop through current app info
			// list and construct an AppsRequest
			List<AppInfo> appInfoList = dbHandler.getAppInfoList_All(connectionInfo.getConnectionID());
			for (AppInfo appInfo : appInfoList) {
				SVMPProtocol.AppInfo.Builder aiBuilder = SVMPProtocol.AppInfo.newBuilder();
				aiBuilder.setAppName(appInfo.getAppName());
				aiBuilder.setPkgName(appInfo.getPackageName());
				ByteString iconHash = convertIconHash(appInfo);
				if (iconHash != null)
					aiBuilder.setIconHash(iconHash);
				arBuilder.addCurrent(aiBuilder);
			}
		}

		// package AppsRequest into a Request and send it
		SVMPProtocol.Request.Builder rBuilder = SVMPProtocol.Request.newBuilder();
		rBuilder.setType(SVMPProtocol.Request.RequestType.APPS);
		rBuilder.setApps(arBuilder);
		sendMessage(rBuilder.build());
	}

	// converts an app's icon hash to a ByteString; if the hash is null, returns
	// null
	private ByteString convertIconHash(AppInfo appInfo) {
		ByteString value = null;
		byte[] iconHash = appInfo.getIconHash();
		if (iconHash != null)
			value = ByteString.copyFrom(iconHash);
		return value;
	}

	// handles Response from VM to see what apps need to be added, updated, or
	// removed
	private void handleAppsResponse(Response response) {
		Log.v(TAG, "Received APPS response");
		SVMPProtocol.AppsResponse appsResponse = response.getApps();
		int connectionID = connectionInfo.getConnectionID();

		// get the existing (outdated) list of apps
		List<AppInfo> oldApps = dbHandler.getAppInfoList_All(connectionID);
		// create a HashMap for looping through
		HashMap<String, AppInfo> oldAppsMap = new HashMap<String, AppInfo>();
		for (AppInfo appInfo : oldApps)
			oldAppsMap.put(appInfo.getPackageName(), appInfo);

		// remove apps that don't exist on the VM anymore
		if (fullRefresh) {
			// if this was a full refresh, delete all existing apps (they will
			// be re-added)
			dbHandler.deleteAllAppInfos(connectionID);
		} else {
			// this was a partial refresh, only delete apps that the VM has
			// specified
			List<String> removedApps = appsResponse.getRemovedList();
			for (String packageName : removedApps) {
				if (oldAppsMap.containsKey(packageName))
					dbHandler.deleteAppInfo(oldAppsMap.get(packageName));
			}
		}

		// loop through the list of new AppInfos that were returned from the VM
		// and add them
		List<SVMPProtocol.AppInfo> newApps = appsResponse.getNewList();
		for (SVMPProtocol.AppInfo appInfo : newApps) {
			String packageName = appInfo.getPkgName(), appName = appInfo.getAppName();
			byte[] icon = appInfo.hasIcon() ? appInfo.getIcon().toByteArray() : null, iconHash = getIconHash(icon);
			AppInfo newAppInfo = new AppInfo(connectionID, packageName, appName, false, icon, iconHash);
			dbHandler.insertAppInfo(newAppInfo);
		}

		// loop through the list of updated AppInfos that were returned from the
		// VM and update them
		List<SVMPProtocol.AppInfo> updatedApps = appsResponse.getUpdatedList();
		for (SVMPProtocol.AppInfo appInfo : updatedApps) {
			String packageName = appInfo.getPkgName(), appName = appInfo.getAppName();
			boolean favorite = oldAppsMap.get(packageName).isFavorite();
			byte[] icon = appInfo.hasIcon() ? appInfo.getIcon().toByteArray() : null, iconHash = getIconHash(icon);
			AppInfo newAppInfo = new AppInfo(connectionID, packageName, appName, favorite, icon, iconHash);
			dbHandler.updateAppInfo(newAppInfo);
		}

		List<AppInfo> appsList = dbHandler.getAppInfoList_All(1);
		if (appsList != null) {

			for (int i = 0; i < appsList.size(); i++) {

				new IconsUrlFetchAsyncTask().execute(appsList.get(i).getPackageName(), i + "",
						appsList.size() - 1 + "");

			}

		} else {
			Log.v(TAG, "Successfully processed APPS response");
			setResult(SvmpActivity.RESULT_OK);
			disconnectAndExit();
		}
	}

	private class IconsUrlFetchAsyncTask extends AsyncTask<String, String, String> {

		String packageName;
		String appNo;
		String totalApps;

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub

			packageName = params[0];
			appNo = params[1];
			totalApps = params[2];

			HTTPServiceHandler httpServiceHandler = new HTTPServiceHandler(getApplicationContext());

			List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(2);
			nameValuePair.add(new BasicNameValuePair("key", packageName));

			String jsonStr = httpServiceHandler.makeSecureServiceCall(
					getResources().getString(R.string.fetch_app_icon_url), HTTPServiceHandler.GET, nameValuePair);

			Log.d(TAG, "Search Successful");

			return jsonStr;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			String icon = null;
			try {
				JSONObject jObj = new JSONObject(result);
				icon = jObj.getString("iconHd");
				if (icon == null || icon.isEmpty() || icon.equals("null")) {
					icon = jObj.getString("icon");
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			AppInfo appInfo = dbHandler.getAppInfo(1, packageName);
			if (icon != null) {
				if (!icon.isEmpty()) {
					if (!icon.equals("null")) {
						appInfo.setIconUrl(icon);
						dbHandler.updateAppInfo(appInfo);
					}
				}
			}

			Log.i("apps icons url", result);
			if (appNo.equals(totalApps)) {
				Log.v(TAG, "Successfully processed APPS response");
				setResult(SvmpActivity.RESULT_OK);
				disconnectAndExit();
			}

		}
	}

	// converts an app's icon into a 20-byte hash, returns null if the icon is
	// null
	private byte[] getIconHash(byte[] icon) {
		byte[] value = null;
		if (icon != null) {
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-1");
				value = md.digest(icon);
			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG, "getIconHash failed: " + e.getMessage());
			}
		}
		return value;
	}
}
