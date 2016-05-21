package com.oval.app.activities;

import org.mitre.svmp.activities.SvmpActivity;
import org.mitre.svmp.apprtc.AppRTCClient;
import org.mitre.svmp.common.ConnectionInfo;
import org.mitre.svmp.protocol.SVMPProtocol.AppsRequest;
import org.mitre.svmp.protocol.SVMPProtocol.Request;

import com.citicrowd.oval.R;

import android.content.Intent;
import android.os.Bundle;

public class VideoStreamingActivity  extends SvmpActivity {

	protected AppRTCClient appRtcClient;
	private static final int REQUEST_CONNECTIONAPPLIST = 101;
	
	private String pkgName;
	private String apkPath;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.layout.connection_list);

		ConnectionInfo connectionInfo = dbHandler.getConnectionInfo(1);
		if (connectionInfo != null) {
			Intent intent = getIntent();
			pkgName = intent.getStringExtra("pkgName");
			apkPath = intent.getStringExtra("apkPath");
			authPrompt(connectionInfo);
		}
	}
	
	private void startConnectionApp(ConnectionInfo connectionInfo) {
		Intent intent = new Intent(VideoStreamingActivity.this, RTCActivity.class);
		intent.putExtra("connectionID", connectionInfo.getConnectionID());
		intent.putExtra("description", connectionInfo.getDescription());
		intent.putExtra("pkgName", pkgName);
		intent.putExtra("apkPath", apkPath);
		intent.setAction(ACTION_LAUNCH_APP);
		// start the activity and expect a result intent when it is finished
		startActivityForResult(intent, REQUEST_CONNECTIONAPPLIST);
		finish();
	}
	
	@Override
	protected void afterStartAppRTC(ConnectionInfo connectionInfo) {
		startConnectionApp(connectionInfo);

	}


}
