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

//<<<<<<< HEAD
import org.mitre.svmp.activities.AppRTCActivity;
import org.mitre.svmp.activities.AppRTCVideoActivity;
import org.mitre.svmp.common.AppInfo;
import org.mitre.svmp.common.ConnectionInfo;
import org.mitre.svmp.common.DatabaseHandler;
//=======
//>>>>>>> branch 'master' of https://github.com/raghavb1/oval-client-android
//import org.mitre.svmp.RemoteServerClient;
import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.IntentAction;
import org.mitre.svmp.protocol.SVMPProtocol.Request.RequestType;
import org.mitre.svmp.services.SessionService;

import com.citicrowd.oval.R;
import com.squareup.picasso.Picasso;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class SendNetIntent extends AppRTCActivity {
	private SessionService service;

	private static final String TAG = "SendNetIntent";

	private DatabaseHandler dbHandler;

	private String pkgName;
	private String apkPath;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dbHandler = new DatabaseHandler(this);

		preparingTextView.setText("Preparing your App");

		final Intent intent = getIntent();
		pkgName = intent.getStringExtra("pkgName");
		apkPath = intent.getStringExtra("apkPath");

		Picasso.with(this).load( dbHandler.getAppInfo(1, pkgName).getIconUrl())

				.into(splashIcon);

	}

	@Override
	protected void startProgressDialog() {
		// TODO Auto-generated method stub
		// super.startProgressDialog();
	}

	@Override
	public void stopProgressDialog() {
		// TODO Auto-generated method stub
		// super.stopProgressDialog();
	}

	private void sendAppsMessageToOvalAppSrvc() {

		SVMPProtocol.Request.Builder msg = SVMPProtocol.Request.newBuilder();
		SVMPProtocol.Intent.Builder intentProtoBuffer = SVMPProtocol.Intent.newBuilder();
		intentProtoBuffer.setAction(IntentAction.ACTION_VIEW);
		intentProtoBuffer.setData(apkPath);

		// Set the Request message params and send it off.
		msg.setType(RequestType.INTENT);
		msg.setIntent(intentProtoBuffer.build());

		sendMessage(msg.build());

	}

	@Override
	public void onOpen() {
		// TODO Auto-generated method stub
		super.onOpen();

		sendAppsMessageToOvalAppSrvc();
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub

		AppInfo appInfo = dbHandler.getNotInstalledAppInfo(1);

		dbHandler.deleteAppInfo(appInfo);

		super.onBackPressed();

	}
}
