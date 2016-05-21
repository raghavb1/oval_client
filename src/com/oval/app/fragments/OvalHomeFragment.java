package com.oval.app.fragments;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.mitre.svmp.common.AppInfo;
import org.mitre.svmp.common.ConnectionInfo;
import org.mitre.svmp.common.Constants;
import org.mitre.svmp.common.DatabaseHandler;

import com.citicrowd.oval.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.oval.app.activities.OvalDrawerActivity;
import com.oval.app.activities.VideoStreamingActivity;
import com.oval.app.adapters.HomeGridAdapter;
import com.oval.app.adapters.SearchListAdapter;
import com.oval.app.network.HTTPServiceHandler;
import com.oval.app.vo.AptoideSearchResultItemVo;
import com.oval.app.vo.RawAptoideSearchResultVo;
import com.oval.util.ConnectionDetector;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchBox.MenuListener;
import com.quinny898.library.persistentsearch.SearchBox.SearchListener;
import com.quinny898.library.persistentsearch.SearchBox.VoiceRecognitionListener;
import com.quinny898.library.persistentsearch.SearchResult;

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
import android.widget.GridView;

public class OvalHomeFragment extends Fragment {

	private SearchBox search;

	public static final String TAG = "OvalHomeFragment";

	DatabaseHandler dbHandler;

	Context context;

	GridView gridView;
	List<AppInfo> appsList;

	ProgressDialog pDialog;
	ConnectionInfo connectionInfo;

	int pos;

	public OvalHomeFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		dbHandler = new DatabaseHandler(getActivity());
		connectionInfo = dbHandler.getConnectionInfo(1);

	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		search.hideResults();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_find_people, container, false);

		context = (Context) getActivity();
		dbHandler = new DatabaseHandler(context);

		gridView = (GridView) rootView.findViewById(R.id.gridView1);

		new SearchAsyncTask().execute("");


		search = (SearchBox) rootView.findViewById(R.id.searchbox);
		search.setLogoText("Search App");
		search.enableVoiceRecognition(this);

		ArrayList<String> searchHistory = (ArrayList<String>) dbHandler.getAllSearchHistory();
		Collections.reverse(searchHistory);

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

		//		search.setMenuListener(new MenuListener() {
		//
		//			@Override
		//			public void onMenuClick() {
		//				// Hamburger has been clicked
		//				// Toast.makeText(OvalSearchActivity.this, "Menu click",
		//				// Toast.LENGTH_LONG).show();
		//
		//				((OvalDrawerActivity) getActivity()).openDrawer();
		//
		//			}
		//
		//		});

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

				((OvalDrawerActivity) getActivity()).startSearchFragment(searchTerm);

			}

			@Override
			public void onResultClick(SearchResult result) {
				// React to a result being clicked

				// makeSearch(result.title);
				((OvalDrawerActivity) getActivity()).startSearchFragment(result.title);
			}

			@Override
			public void onSearchCleared() {
				// Called when the clear button is clicked

				// Toast.makeText(OvalSearchActivity.this, "Searched Closed",
				// Toast.LENGTH_LONG).show();
			}

		});

		return rootView;
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

	public void openApp(boolean isNew) {
		// TODO Auto-generated method stub
		AppInfo appinfo = appsList.get(pos);
		/*	if (!isNew) {
			Intent i = new Intent(context, ConnectionList.class);
			i.setAction(Constants.ACTION_LAUNCH_APP);
			i.putExtra("connectionID", 1);

			i.putExtra("pkgName", appinfo.getPackageName());
			startActivity(i);
		} else {*/
		Intent intent = new Intent(context, VideoStreamingActivity.class);
		intent.setAction(Constants.ACTION_LAUNCH_APP);
		intent.putExtra("pkgName", appinfo.getPackageName());
		intent.putExtra("apkPath", appinfo.getPackageName());
		intent.putExtra("connectionID", 1);
		context.startActivity(intent);
		//		}

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
			Gson gson = new Gson();
			RawAptoideSearchResultVo rawAptoideSearchResultVo;
			rawAptoideSearchResultVo = gson.fromJson(result, type);

			pDialog.dismiss();
			updateUI(rawAptoideSearchResultVo);

		}
	}

	public void updateUI(RawAptoideSearchResultVo rawAptoideSearchResultVo) {

		if (rawAptoideSearchResultVo != null) {
			if (rawAptoideSearchResultVo.getSros() != null) {
				for(int i=0; i< rawAptoideSearchResultVo.getSros().size(); i++){
					AptoideSearchResultItemVo searchItem = rawAptoideSearchResultVo.getSros().get(i);

					AppInfo appInfoTemp = dbHandler.getAppInfo(1, searchItem.getApkid());

					AppInfo appinfo = new AppInfo(1, searchItem.getApkid(), searchItem.getName(), false, null, null, 0,
							searchItem.getIconHd(), 1, searchItem.getAlternateUrl() != null ? searchItem.getAlternateUrl() : searchItem.getPath());
					if(appInfoTemp == null){
						dbHandler.insertAppInfo(appinfo);
					}else{
						dbHandler.updateAppInfo(appinfo);
					}

				}
			}
		}
		updateGridUI();
	}
	
	private void updateGridUI(){
		appsList = dbHandler.getAppInfoList_All(1);

		HomeGridAdapter adapter = new HomeGridAdapter(context, appsList);

		gridView.setAdapter(adapter);
		//adapter.notifyDataSetChanged();

		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub

				if (ConnectionDetector.isConnectingToInternet(context)) {

					pos = position;
					openApp(true);
					//					new AssignVMAsyncTask().execute();
				} else {
					Fragment fragment = new OvalNoInternetFragment();
					FragmentManager fragmentManager = getFragmentManager();
					fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).addToBackStack(OvalHomeFragment.TAG).commit();

				}

			}
		});
	}
}
