package com.oval.app.activities;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.mitre.svmp.activities.SvmpActivity;
import org.mitre.svmp.common.ConnectionInfo;

import com.citicrowd.oval.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.oval.app.network.CustomSSLSocketFactory;
import com.oval.app.network.HTTPServiceHandler;
import com.oval.app.vo.LoginRequestVO;
import com.oval.app.vo.LoginResultVO;
import com.oval.app.vo.PersonInfoVo;
import com.oval.app.vo.RegisterRequestVO;
import com.oval.app.vo.RegisterResultVO;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class OvalLoginActivity extends SvmpActivity
implements OnClickListener, ConnectionCallbacks, OnConnectionFailedListener {

	private static final int RC_SIGN_IN = 0;
	// Logcat tag
	private static final String TAG = "MainActivity";

	public static final String REGISTER_URL = "/auth/signup";
	public static final String LOGIN_URL = "/auth/signin";
	public static final String SEARCH_URL = "http://search.voga360.com/api/search.htm";

	public static final int STATUS_REGISTERED = 1; // UNAPPROVED
	public static final int STATUS_APPROVED = 2;
	public static final int STATUS_LOGGEDIN = 3;
	public static final int STATUS_APPSREFRESHED = 4;

	ConnectionInfo connectionInfo;

	private int sendRequestCode;

	public ProgressDialog pDialog;

	// Google client to interact with Google API
	private GoogleApiClient mGoogleApiClient;

	PersonInfoVo personInfoVo;
	/**
	 * A flag indicating that a PendingIntent is in progress and prevents us
	 * from starting further intents.
	 */
	private boolean mIntentInProgress;

	private boolean mSignInClicked;

	private ConnectionResult mConnectionResult;
	RegisterRequestVO registerRequestVo = new RegisterRequestVO();
	RegisterResultVO registerResultVo = new RegisterResultVO();

	LoginRequestVO loginRequestVo = new LoginRequestVO();
	LoginResultVO loginResultVo = new LoginResultVO();

	Gson gson = new Gson();
	// boolean alreadySignedIn = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState, R.layout.activity_splash);

		personInfoVo = new PersonInfoVo();
		// btnSignIn = (SignInButton) findViewById(R.id.btn_sign_in);

		pDialog = new ProgressDialog(this);
		pDialog.setMessage("Please wait...");
		pDialog.setCancelable(false);

		// Button click listeners
		// btnSignIn.setOnClickListener(this);

		mSignInClicked = true;

		mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).addApi(Plus.API).addScope(new Scope(Scopes.PROFILE))
				.addScope(new Scope(Scopes.EMAIL)).build();

	}

	private void showpDialog() {
		if (!pDialog.isShowing())
			pDialog.show();
	}

	private void hidepDialog() {
		if (pDialog.isShowing())
			pDialog.dismiss();
	}

	protected void onStart() {
		super.onStart();
		mGoogleApiClient.connect();
	}

	protected void onStop() {
		super.onStop();
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}

	/**
	 * Method to resolve any signin errors
	 */
	private void resolveSignInError() {
		if (mConnectionResult.hasResolution()) {
			try {
				mIntentInProgress = true;
				mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
			} catch (SendIntentException e) {
				mIntentInProgress = false;
				mGoogleApiClient.connect();
			}
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		if (!result.hasResolution()) {
			GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
			return;
		}

		// btnSignIn.setVisibility(View.VISIBLE);
		// alreadySignedIn=false;

		if (!mIntentInProgress) {
			// Store the ConnectionResult for later usage
			mConnectionResult = result;

			if (mSignInClicked) {
				// The user has already clicked 'sign-in' so we attempt to
				// resolve all
				// errors until the user is signed in, or they cancel.
				resolveSignInError();
			}

		}

	}

	@Override
	protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
		if (requestCode == RC_SIGN_IN) {
			if (responseCode != RESULT_OK) {
				mSignInClicked = false;
			}

			mIntentInProgress = false;

			if (!mGoogleApiClient.isConnecting()) {
				mGoogleApiClient.connect();
			}
		}else // fall back to superclass method
			super.onActivityResult(requestCode, responseCode, intent);

	}


	@Override
	public void onConnected(Bundle arg0) {
		mSignInClicked = false;
		// Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();

		// btnSignIn.setVisibility(View.INVISIBLE);
		// alreadySignedIn = true;

		// Get user's information

		Intent i;

		if (dbHandler.getConnectionInfoList().size() == 0) {
			// i = new Intent(OvalLoginActivity.this, OvalLoginActivity.class);

			getProfileInformation();

			doRegistration();

		} else {
			ConnectionInfo connectionInfo = dbHandler.getConnectionInfoList().get(0);
			if (connectionInfo == null) {
				// i = new Intent(OvalLoginActivity.this,
				// OvalLoginActivity.class);

				getProfileInformation();

				doRegistration();

			} else if (connectionInfo.getStatus() == OvalLoginActivity.STATUS_LOGGEDIN) {
				i = new Intent(OvalLoginActivity.this, OvalDrawerActivity.class);
				i.putExtra("connectionID", 0);
				startActivity(i);
				finish();

			} else if (connectionInfo.getStatus() == OvalLoginActivity.STATUS_REGISTERED) {
				i = new Intent(OvalLoginActivity.this, OvalAccountApprovalActivity.class);
				i.putExtra("connectionID", 0);
				startActivity(i);
				finish();

			}

		}

	}

	public void doRegistration() {
		registerRequestVo.setDevice_type("1234");
		registerRequestVo.setEmail(personInfoVo.getEmailId());
		registerRequestVo.setUsername(personInfoVo.getEmailId());
		registerRequestVo.setPassword("Hash33##");

		new RegistrationAsyncTask().execute(getString(R.string.api_end_point) + REGISTER_URL,
				gson.toJson(registerRequestVo));
	}

	private class RegistrationAsyncTask extends AsyncTask<String, String, String> {
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub

			super.onPreExecute();

			pDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub

			String response = null;

			try {

				SSLSocketFactory sslFactory = new CustomSSLSocketFactory(
						CustomSSLSocketFactory.getKeyStoreForCertificate(
								getApplicationContext().getResources().getAssets().open("new_server.crt")));
				// sslFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				// to be used in case of bypassing certificates

				// Enable HTTP parameters */
				HttpParams params4 = new BasicHttpParams();
				HttpProtocolParams.setVersion(params4, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(params4, HTTP.UTF_8);

				// Register the HTTP and HTTPS Protocols. For HTTPS, register
				// our custom SSL Factory object.
				SchemeRegistry registry = new SchemeRegistry();
				registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
				registry.register(new Scheme("https", sslFactory, 443));

				// Create a new connection manager using the newly created
				// registry and then create a new HTTP client
				// using this connection manager
				ClientConnectionManager ccm = new ThreadSafeClientConnManager(params4, registry);
				DefaultHttpClient client = new DefaultHttpClient(ccm, params4);
				HttpPost httppostreq = new HttpPost(params[0]);
				StringEntity se = new StringEntity(params[1]);
				se.setContentType("application/json;charset=UTF-8");
				se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json;charset=UTF-8"));
				httppostreq.setEntity(se);
				HttpResponse httpresponse = client.execute(httppostreq);
				httpresponse.getStatusLine().getStatusCode();

				HttpEntity httpEntity = httpresponse.getEntity();
				response = EntityUtils.toString(httpEntity);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}

			catch (SSLHandshakeException e) {
				e.printStackTrace();
			} catch (SSLException e) {
				e.printStackTrace();
			}

			catch (IOException e) {
				// e.printStackTrace();
				System.out.println(e);
			}

			catch (Exception e) {
				e.printStackTrace();
			}

			return response;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub

			super.onPostExecute(result);

			Type type = new TypeToken<RegisterResultVO>() {
			}.getType();
			registerResultVo = gson.fromJson(result, type);

			doLogin();

			pDialog.dismiss();
		}

	}

	private void doLogin() {
		loginRequestVo.setUsername(personInfoVo.getEmailId());
		loginRequestVo.setPassword("Hash33##");

		new LoginAsyncTask().execute(getString(R.string.api_end_point) + LOGIN_URL, gson.toJson(loginRequestVo));
	}

	private class LoginAsyncTask extends AsyncTask<String, String, String> {
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub

			super.onPreExecute();

			pDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub

			String response = null;

			try {

				SSLSocketFactory sslFactory = new CustomSSLSocketFactory(
						CustomSSLSocketFactory.getKeyStoreForCertificate(
								getApplicationContext().getResources().getAssets().open("new_server.crt")));
				// sslFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				// to be used in case of bypassing certificates

				// Enable HTTP parameters */
				HttpParams params4 = new BasicHttpParams();
				HttpProtocolParams.setVersion(params4, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(params4, HTTP.UTF_8);

				// Register the HTTP and HTTPS Protocols. For HTTPS, register
				// our custom SSL Factory object.
				SchemeRegistry registry = new SchemeRegistry();
				registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
				registry.register(new Scheme("https", sslFactory, 443));

				// Create a new connection manager using the newly created
				// registry and then create a new HTTP client
				// using this connection manager
				ClientConnectionManager ccm = new ThreadSafeClientConnManager(params4, registry);
				DefaultHttpClient client = new DefaultHttpClient(ccm, params4);
				HttpPost httppostreq = new HttpPost(params[0]);
				StringEntity se = new StringEntity(params[1]);
				se.setContentType("application/json;charset=UTF-8");
				se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json;charset=UTF-8"));
				httppostreq.setEntity(se);
				HttpResponse httpresponse = client.execute(httppostreq);
				httpresponse.getStatusLine().getStatusCode();

				HttpEntity httpEntity = httpresponse.getEntity();
				response = EntityUtils.toString(httpEntity);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}

			catch (SSLHandshakeException e) {
				e.printStackTrace();
			} catch (SSLException e) {
				e.printStackTrace();
			}

			catch (IOException e) {
				e.printStackTrace();
			}

			catch (Exception e) {
				e.printStackTrace();
			}

			return response;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub

			super.onPostExecute(result);
			if (result != null) {
				Type type = new TypeToken<LoginResultVO>() {
				}.getType();
				loginResultVo = gson.fromJson(result, type);
				pDialog.dismiss();
				updateUI(true);
			} else {
				Toast.makeText(OvalLoginActivity.this, "Couldnt Connect to Server. Please try again later.",
						Toast.LENGTH_LONG).show();
				finish();
			}

		}

	}

	/**
	 * Updating the UI, showing/hiding buttons and profile layout
	 */

	private void updateUI(boolean isSignedIn) {
		String status = loginResultVo.getApproved();

		if (isSignedIn) {

			if (status.equalsIgnoreCase("true")) {
				connectionInfo = new ConnectionInfo(1, "New Connection", personInfoVo.getEmailId(), "oval.co.in", 3000,
						1, 1, "", 0, OvalLoginActivity.STATUS_LOGGEDIN, personInfoVo.getName(),
						personInfoVo.getPhotoUrl(), personInfoVo.getProfileUrl());
				long result;

				result = dbHandler.insertConnectionInfo(connectionInfo);
				if (result > -1) {

					new AssignVMAsyncTask().execute();
					// refreshApps(connectionInfo);

				}
			} else {
				connectionInfo = new ConnectionInfo(1, "New Connection", personInfoVo.getEmailId(), "oval.co.in", 3000,
						1, 1, "", 0, OvalLoginActivity.STATUS_REGISTERED, personInfoVo.getName(),
						personInfoVo.getPhotoUrl(), personInfoVo.getProfileUrl());
				long result;

				result = dbHandler.insertConnectionInfo(connectionInfo);
				if (result > -1) {
					Intent i = new Intent(OvalLoginActivity.this, OvalAccountApprovalActivity.class);
					i.putExtra("connectionID", 0);
					startActivity(i);
					finish();
				}
			}

		} else {
			Intent i = new Intent(OvalLoginActivity.this, OvalDrawerActivity.class);
			i.putExtra("connectionID", 0);
			startActivity(i);
			finish();
		}
	}

//	private void refreshApps(ConnectionInfo connectionInfo) {
//		// TODO Auto-generated method stub
//
//		this.sendRequestCode = AppList.REQUEST_REFRESHAPPS_FULL;
//		authPrompt(connectionInfo); // utilizes "startActivityForResult", which
//		// uses this.sendRequestCode
//
//	}

	@Override
	protected void afterStartAppRTC(ConnectionInfo connectionInfo) {
		// after we have handled the auth prompt and made sure the service is
		// started...

		// create explicit intent
		Intent intent = new Intent();
//		if (this.sendRequestCode == AppList.REQUEST_REFRESHAPPS_QUICK
//				|| this.sendRequestCode == AppList.REQUEST_REFRESHAPPS_FULL) {
//			// we're refreshing our cached list of apps that reside on the VM
//			intent.setClass(OvalLoginActivity.this, AppRTCRefreshAppsActivity.class);
//			if (this.sendRequestCode == AppList.REQUEST_REFRESHAPPS_FULL)
//				intent.putExtra("fullRefresh", true);
//		}

		intent.putExtra("connectionID", connectionInfo.getConnectionID());

		// start the AppRTCActivity
		startActivityForResult(intent, this.sendRequestCode);

	}

	/**
	 * Fetching user's information name, email, profile pic
	 */
	@SuppressWarnings("deprecation")
	private void getProfileInformation() {
		try {
			if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {

				// accessToken = GoogleAuthUtil.getToken(
				// getApplicationContext(),
				// mPlusClient.getAccountName(), "oauth2:"
				// + Scopes.PLUS_LOGIN + " "
				// + Scopes.PLUS_PROFILE+"
				// https://www.googleapis.com/auth/plus.profile.emails.read");
				//

				Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
				personInfoVo.setName(currentPerson.getDisplayName());
				personInfoVo.setPhotoUrl(currentPerson.getImage().getUrl());
				personInfoVo.setProfileUrl(currentPerson.getUrl());
				personInfoVo.setEmailId(Plus.AccountApi.getAccountName(mGoogleApiClient));

				/*
				 * Log.e(TAG, "Name: " + personName + ", plusProfile: " +
				 * personGooglePlusProfile + ", email: " + email + ", Image: " +
				 * personPhotoUrl);
				 */

			} else {
				Toast.makeText(getApplicationContext(), "Person information is null", Toast.LENGTH_LONG).show();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		mGoogleApiClient.connect();
		updateUI(false);
	}

	/**
	 * Button on click listener
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_sign_in:
			// Signin button clicked
			signInWithGplus();
			break;
			/*
			 * case R.id.btn_sign_out: // Signout button clicked signOutFromGplus();
			 * break; case R.id.btn_revoke_access: // Revoke access button clicked
			 * revokeGplusAccess(); break;
			 */
		}
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		signInWithGplus();
	}

	/**
	 * Sign-in into google
	 */
	private void signInWithGplus() {
		if (!mGoogleApiClient.isConnecting()) {
			mSignInClicked = true;
			resolveSignInError();
		}
	}

	/**
	 * Sign-out from google
	 */
	private void signOutFromGplus() {
		if (mGoogleApiClient.isConnected()) {
			Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
			mGoogleApiClient.disconnect();
			mGoogleApiClient.connect();
			updateUI(false);
		}
	}

	/**
	 * Revoking access from google
	 */
	private void revokeGplusAccess() {
		if (mGoogleApiClient.isConnected()) {
			Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
			Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
				@Override
				public void onResult(Status arg0) {
					Log.e(TAG, "User access revoked!");
					mGoogleApiClient.connect();
					updateUI(false);
				}

			});
		}
	}

	private class AssignVMAsyncTask extends AsyncTask<String, String, String> {
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub

			super.onPreExecute();
			pDialog = new ProgressDialog(OvalLoginActivity.this);
			pDialog.setMessage("Please wait...");
			pDialog.setCancelable(false);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {

			HTTPServiceHandler httpServiceHandler = new HTTPServiceHandler(OvalLoginActivity.this);

			String jsonStr = httpServiceHandler.makeSecureServiceCall(
					getResources().getString(R.string.assign_vm_url) + connectionInfo.getUsername(),
					HTTPServiceHandler.GET, null);

			Log.d(TAG, "Search Successful");
			return jsonStr;

		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub

			super.onPostExecute(result);

			pDialog.dismiss();
			try {
				JSONObject jObj = new JSONObject(result);
				String success = jObj.getString("success");
				if (success.equals("true")) {
					// refreshApps(connectionInfo);
					Intent i = new Intent(OvalLoginActivity.this, OvalDrawerActivity.class);
					i.putExtra("connectionID", 0);
					startActivity(i);
					finish();
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

}