package com.oval.app.fragments;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.mitre.svmp.activities.ConnectionList;
import org.mitre.svmp.common.AppInfo;
import org.mitre.svmp.common.ConnectionInfo;
import org.mitre.svmp.common.Constants;
import org.mitre.svmp.common.DatabaseHandler;

import com.citicrowd.oval.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.oval.app.activities.OvalDrawerActivity;
import com.oval.app.activities.OvalLoginActivity;
import com.oval.app.activities.OvalSearchActivity;

import com.oval.app.adapters.SearchListAdapter;

import com.oval.app.network.HTTPServiceHandler;
import com.oval.app.vo.AptoideSearchResultItemVo;
import com.oval.app.vo.RawAptoideSearchResultVo;
import com.oval.app.vo.RawSearchResultVO;
import com.oval.util.ConnectionDetector;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;
import com.quinny898.library.persistentsearch.SearchBox.MenuListener;
import com.quinny898.library.persistentsearch.SearchBox.SearchListener;
import com.quinny898.library.persistentsearch.SearchBox.VoiceRecognitionListener;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;

public class OvalSearchFragment extends Fragment {

	ProgressDialog pDialog;
	ListView searchResultListView;

	Gson gson = new Gson();
	// RawSearchResultVO rawSearchResultVO;
	RawAptoideSearchResultVo rawAptoideSearchResultVo;

	ArrayList<AptoideSearchResultItemVo> searchResultsList;
	Boolean isSearch;
	private SearchBox search;

	public static final String TAG = "OvalSearchFragment";

	DatabaseHandler dbHandler;

	Context context;
	ConnectionInfo connectionInfo;
	int pos;

	public OvalSearchFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.activity_search, container, false);
		String strtext = getArguments().getString("searchText");

		context = (Context) getActivity();
		dbHandler = new DatabaseHandler(context);

		connectionInfo = dbHandler.getConnectionInfo(1);

		search = (SearchBox) rootView.findViewById(R.id.searchbox);
		search.setLogoText("OVAL");
		search.enableVoiceRecognition(this);
		searchResultListView = (ListView) rootView.findViewById(R.id.searchResultListView);

		searchResultListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				if (searchResultsList != null) {

					if (ConnectionDetector.isConnectingToInternet(context)) {

						pos = position;

						new AssignVMAsyncTask().execute();
					} else {
						launchNointernetFragment();
					}
				}

			}

		});

		ArrayList<String> searchHistory = (ArrayList<String>) dbHandler.getAllSearchHistory();

		for (String searchString : searchHistory) {

			SearchResult option = new SearchResult(searchString, getResources().getDrawable(R.drawable.ic_history));
			search.addSearchable(option);

		}

		search.setVoiceRecognitionListener(new VoiceRecognitionListener() {

			@Override
			public void onClick() {
				// TODO Auto-generated method stub

				InputMethodManager inputMethodManager = (InputMethodManager) context
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				inputMethodManager.hideSoftInputFromWindow(search.getWindowToken(), 0);

				search.micClick();

			}
		});

		search.setMenuListener(new MenuListener() {

			@Override
			public void onMenuClick() {
				// Hamburger has been clicked
				// Toast.makeText(OvalSearchActivity.this, "Menu click",
				// Toast.LENGTH_LONG).show();

				((OvalDrawerActivity) getActivity()).openDrawer();

			}

		});

		search.setSearchListener(new SearchListener() {

			@Override
			public void onSearchOpened() {
				// Use this to tint the screen
			}

			@Override
			public void onSearchClosed() {
				// Use this to un-tint the screen

				// Toast.makeText(OvalSearchActivity.this, "Searched Closed",
				// Toast.LENGTH_LONG).show();
			}

			@Override
			public void onSearchTermChanged(String term) {
				// React to the search term changing
				// Called after it has updated results
			}

			@Override
			public void onSearch(String searchTerm) {
				// Toast.makeText(OvalSearchActivity.this, searchTerm +"
				// Searched", Toast.LENGTH_LONG).show();

				if (!searchTerm.isEmpty()) {
					dbHandler.insertSearchHistory(searchTerm);
					makeSearch(searchTerm);
				}

				// ((DrawerHomeActivity)getActivity()).startSearchFragment();

			}

			@Override
			public void onResultClick(SearchResult result) {
				// React to a result being clicked

				makeSearch(result.title);
			}

			@Override
			public void onSearchCleared() {
				// Called when the clear button is clicked

				// Toast.makeText(OvalSearchActivity.this, "Searched Closed",
				// Toast.LENGTH_LONG).show();
			}

		});

		if (strtext != null) {
			search.populateEditText(strtext);
		}

		return rootView;
	}

	public void launchNointernetFragment() {
		Fragment fragment = new OvalNoInternetFragment();
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).addToBackStack(OvalHomeFragment.TAG)
				.commit();

	}

	private void openApp(boolean isNew) {

		// TODO Auto-generated method stub

		AptoideSearchResultItemVo searchItem = searchResultsList.get(pos);
		if (searchItem != null) {
			/*
			 * Intent i = new Intent(context, ConnectionList.class);
			 * i.setAction(Constants.ACTION_LAUNCH_APP);
			 * i.putExtra("connectionID", 1);
			 */

			AppInfo appInfoTemp = dbHandler.getAppInfo(1, searchItem.getApkid());
			String downloadUrl;

			if (appInfoTemp != null) {

				downloadUrl = appInfoTemp.getDownloadUrl();
				if (appInfoTemp.getIsUsed() == 0) {
					appInfoTemp.setIsUsed(1);
					dbHandler.updateAppInfo(appInfoTemp);

				}
			} else {

				String iconPath = searchItem.getIconHd();
				if (iconPath == null || iconPath.isEmpty() || iconPath.equals("null")) {
					iconPath = searchItem.getIcon();
				}

				String apkDownloadUrl = searchItem.getAlternateUrl();
				if (apkDownloadUrl == null) {
					apkDownloadUrl = context.getString(R.string.aptoide_apk_path) + searchItem.getPath();
				}
				AppInfo appinfo = new AppInfo(1, searchItem.getApkid(), searchItem.getName(), false, null, null, 0,
						iconPath, 1, apkDownloadUrl);
				dbHandler.insertAppInfo(appinfo);

				downloadUrl = apkDownloadUrl;

			}

			Intent intent = new Intent(context, ConnectionList.class);
			intent.setAction(Constants.ACTION_LAUNCH_APP);
			intent.putExtra("pkgName", searchItem.getApkid());
			intent.putExtra("connectionID", 1);
			Uri.Builder builder = new Uri.Builder();
			builder.scheme("http").authority("oval.co.in");
			builder.appendQueryParameter("type", "downloadAndInstall");
			builder.appendQueryParameter("packageName", searchItem.getApkid());
			builder.appendQueryParameter("url", downloadUrl);
			intent.setData(builder.build());
			context.startActivity(intent);

		}

	}

	private void makeSearch(String searchStr) {
		// TODO Auto-generated method stub
		if (ConnectionDetector.isConnectingToInternet(context)) {
			new SearchAsyncTask().execute(searchStr);
		} else {
			launchNointernetFragment();
		}

	}

	private class SearchAsyncTask extends AsyncTask<String, String, String> {
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub

			super.onPreExecute();
			pDialog = new ProgressDialog(context);
			pDialog.setMessage("Please wait...");
			pDialog.setCancelable(false);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {

			HTTPServiceHandler httpServiceHandler = new HTTPServiceHandler(context);

			List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(2);

			nameValuePair.add(new BasicNameValuePair("key", params[0]));

			String jsonStr = httpServiceHandler.makeSecureServiceCall(
					getResources().getString(R.string.services_prefix_url_aptoide), HTTPServiceHandler.GET,
					nameValuePair);

			Log.d(TAG, "Search Successful");
			return jsonStr;

		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub

			super.onPostExecute(result);

			Type type = new TypeToken<RawAptoideSearchResultVo>() {
			}.getType();
			rawAptoideSearchResultVo = gson.fromJson(result, type);

			pDialog.dismiss();
			updateUI();

		}

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1234 && resultCode == Activity.RESULT_OK) {
			ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			search.populateEditText(matches.get(0));
			// search.setLogoText(matches.get(0));

			if (search.isSearchOpen()) {

				search.closeSearch();
			}

			InputMethodManager inputMethodManager = (InputMethodManager) context
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(search.getWindowToken(), 0);

		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void updateUI() {
		// TODO Auto-generated method stub
		if (rawAptoideSearchResultVo != null) {
			if (rawAptoideSearchResultVo.getSros() != null) {
				searchResultsList = rawAptoideSearchResultVo.getSros();
				SearchListAdapter adapter = new SearchListAdapter(getActivity(), searchResultsList);
				searchResultListView.setAdapter(adapter);
			}
		}

	}

	private class AssignVMAsyncTask extends AsyncTask<String, String, String> {
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub

			super.onPreExecute();
			pDialog = new ProgressDialog(context);
			pDialog.setMessage("Please wait...");
			pDialog.setCancelable(false);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {

			HTTPServiceHandler httpServiceHandler = new HTTPServiceHandler(context);

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
				boolean isNew = jObj.getBoolean("new");
				if (success.equals("true")) {
					openApp(isNew);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

}
