package com.oval.app.fragments;

import java.util.ArrayList;
import java.util.List;

import org.mitre.svmp.activities.ConnectionList;
import org.mitre.svmp.common.AppInfo;
import org.mitre.svmp.common.Constants;
import org.mitre.svmp.common.DatabaseHandler;

import com.citicrowd.oval.R;
import com.oval.app.adapters.HomeGridAdapter;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;
import com.quinny898.library.persistentsearch.SearchBox.MenuListener;
import com.quinny898.library.persistentsearch.SearchBox.SearchListener;
import com.quinny898.library.persistentsearch.SearchBox.VoiceRecognitionListener;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

public class OvalHomeFragment extends Fragment {

	private SearchBox search;

	public static final String TAG = "OvalHomeFragment";

	DatabaseHandler dbHandler;

	Context context;

	GridView gridView;
	List<AppInfo> appsList;

	public OvalHomeFragment() {
	}

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_find_people, container, false);

		context = (Context) getActivity();
		dbHandler = new DatabaseHandler(context);

		gridView = (GridView) rootView.findViewById(R.id.gridView1);

		appsList = dbHandler.getAppInfoList_areUsed(1);
		
		HomeGridAdapter adapter = new HomeGridAdapter(context, appsList);

		gridView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
		
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				
				Intent i = new Intent(context, ConnectionList.class);
				i.setAction(Constants.ACTION_LAUNCH_APP);
				i.putExtra("connectionID", 1);
				
				i.putExtra("pkgName", appsList.get(position).getPackageName());
				startActivity(i);
				
			}
		});

		search = (SearchBox) rootView.findViewById(R.id.searchbox);
		search.setLogoText("OVAL");
		search.enableVoiceRecognition(this);

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
}
