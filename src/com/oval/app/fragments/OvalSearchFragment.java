package com.oval.app.fragments;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.mitre.svmp.common.DatabaseHandler;

import com.citicrowd.oval.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.oval.app.activities.OvalLoginActivity;
import com.oval.app.activities.OvalSearchActivity;

import com.oval.app.adapters.SearchListAdapter;
import com.oval.app.network.HTTPServiceHandler;
import com.oval.app.vo.AptoideSearchResultItemVo;
import com.oval.app.vo.RawAptoideSearchResultVo;
import com.oval.app.vo.RawSearchResultVO;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;
import com.quinny898.library.persistentsearch.SearchBox.MenuListener;
import com.quinny898.library.persistentsearch.SearchBox.SearchListener;
import com.quinny898.library.persistentsearch.SearchBox.VoiceRecognitionListener;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;

public class OvalSearchFragment extends Fragment {

	ProgressDialog pDialog;
	ListView searchResultListView;

	Gson gson = new Gson();
	//RawSearchResultVO rawSearchResultVO;
	RawAptoideSearchResultVo rawAptoideSearchResultVo;
	

	Boolean isSearch;
	private SearchBox search;

	public static final String TAG = "OvalSearchFragment";

	DatabaseHandler dbHandler;

	Context context;

	public OvalSearchFragment() {
	}
	
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.activity_search, container, false);
		  String strtext = getArguments().getString("searchText");
		 
		context = (Context)getActivity();
		dbHandler = new DatabaseHandler(context);

		search = (SearchBox) rootView.findViewById(R.id.searchbox);
		search.setLogoText("OVAL");
		search.enableVoiceRecognition(this);
		searchResultListView = (ListView) rootView.findViewById(R.id.searchResultListView);
		
		
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
				
				((OvalDrawerActivity)getActivity()).openDrawer();
				
				
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
				
				//((DrawerHomeActivity)getActivity()).startSearchFragment();

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
		
		 if(strtext!=null)
		  {
			  search.populateEditText(strtext);
		  }


		return rootView;
	}
	
	
	private void makeSearch(String searchStr) {
		// TODO Auto-generated method stub

		new SearchAsyncTask().execute(searchStr);

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
		//	nameValuePair.add(new BasicNameValuePair("q", params[0]));

			nameValuePair.add(new BasicNameValuePair("key", params[0]));
			
			String jsonStr = httpServiceHandler.makeSecureServiceCall(getResources().getString(R.string.services_prefix_url_aptoide),
					HTTPServiceHandler.GET, nameValuePair);

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

			InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(search.getWindowToken(), 0);

		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	public void updateUI() {
		// TODO Auto-generated method stub
		if (rawAptoideSearchResultVo != null) {
			if (rawAptoideSearchResultVo.getSros() != null) {
				SearchListAdapter adapter = new SearchListAdapter(getActivity(), rawAptoideSearchResultVo.getSros());
				searchResultListView.setAdapter(adapter);
			}
		}

	}
	
	

}
