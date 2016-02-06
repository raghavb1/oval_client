package com.oval.app.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.mitre.svmp.activities.AppList;
import org.mitre.svmp.activities.AppRTCRefreshAppsActivity;
import org.mitre.svmp.activities.SvmpActivity;
import org.mitre.svmp.common.ConnectionInfo;
import org.mitre.svmp.services.SessionService;

import com.citicrowd.oval.R;
import com.oval.app.fragments.OvalHomeFragment;
import com.oval.app.fragments.OvalSearchFragment;
import com.oval.slidingmenu.adapter.NavDrawerListAdapter;
import com.oval.slidingmenu.model.NavDrawerItem;
import com.squareup.picasso.Picasso;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class OvalDrawerActivity extends SvmpActivity {
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;

	// nav drawer title
	private CharSequence mDrawerTitle;

	// used to store app title
	private CharSequence mTitle;

	// slide menu items
	private String[] navMenuTitles;
	private TypedArray navMenuIcons;

	private ArrayList<NavDrawerItem> navDrawerItems;
	private NavDrawerListAdapter adapter;
	ImageView icProfile;

	ConnectionInfo connectionInfo;
	TextView emailTv;

	private int sendRequestCode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.layout.activity_drawer);

		connectionInfo = dbHandler.getConnectionInfo(1);

		mTitle = mDrawerTitle = getTitle();

		// load slide menu items
		navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items);

		// nav drawer icons from resources
		navMenuIcons = getResources().obtainTypedArray(R.array.nav_drawer_icons);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.list_slidermenu);
		icProfile = (ImageView) findViewById(R.id.icProfile);
		emailTv = (TextView) findViewById(R.id.emailTv);

		emailTv.setText(connectionInfo.getUsername());

		StringTokenizer strToken = new StringTokenizer(connectionInfo.getPhotoUrl(), "=");
		String icon = strToken.nextToken() + "=200";
		Picasso.with(this).load(icon).placeholder(R.drawable.ic_profile).into(icProfile);

		navDrawerItems = new ArrayList<NavDrawerItem>();

		// adding nav drawer items to array
		// Home
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1)));
		// Find People
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[1], navMenuIcons.getResourceId(1, -1)));
		// Photos
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1)));
		// Communities, Will add a counter here
		/*
		 * navDrawerItems.add(new NavDrawerItem(navMenuTitles[3],
		 * navMenuIcons.getResourceId(3, -1), true, "22")); // Pages
		 * navDrawerItems.add(new NavDrawerItem(navMenuTitles[4],
		 * navMenuIcons.getResourceId(4, -1))); // What's hot, We will add a
		 * counter here navDrawerItems.add(new NavDrawerItem(navMenuTitles[5],
		 * navMenuIcons.getResourceId(5, -1), true, "50+"));
		 */

		// Recycle the typed array
		navMenuIcons.recycle();

		mDrawerList.setOnItemClickListener(new SlideMenuClickListener());

		// setting the nav drawer list adapter
		adapter = new NavDrawerListAdapter(getApplicationContext(), navDrawerItems);
		mDrawerList.setAdapter(adapter);

		// enabling action bar app icon and behaving it as toggle button
		/*
		 * getActionBar().setDisplayHomeAsUpEnabled(true);
		 * getActionBar().setHomeButtonEnabled(true);
		 */

		// mDrawerLayout.openDrawer(mDrawerList);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_launcher, // nav
																								// menu
																								// toggle
																								// icon
				R.string.app_name, // nav drawer open - description for
									// accessibility
				R.string.app_name // nav drawer close - description for
									// accessibility
		) {
			public void onDrawerClosed(View view) {
				// getActionBar().setTitle(mTitle);
				// calling onPrepareOptionsMenu() to show action bar icons
				invalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView) {
				// getActionBar().setTitle(mDrawerTitle);
				// calling onPrepareOptionsMenu() to hide action bar icons
				invalidateOptionsMenu();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null) {

			Fragment fragment = new OvalHomeFragment();

			if (fragment != null) {
				FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit();

				// update selected item and title, then close the drawer
				mDrawerLayout.closeDrawer(Gravity.START);
			}
		}
	}

	public void openDrawer() {
		mDrawerLayout.openDrawer(Gravity.START);
	}

	/**
	 * Slide menu item click listener
	 */
	private class SlideMenuClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// display view for selected nav drawer item
			displayView(position);
		}
	}

	public void clearApplicationData() {
		File cache = getCacheDir();
		File appDir = new File(cache.getParent());
		if (appDir.exists()) {
			String[] children = appDir.list();
			for (String s : children) {
				if (!s.equals("lib")) {
					deleteDir(new File(appDir, s));
					Log.i("TAG", "**************** File /data/data/APP_PACKAGE/" + s + " DELETED *******************");
				}
			}
		}
	}

	public static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		return dir.delete();
	}

	/*
	 * * Called when invalidateOptionsMenu() is triggered
	 */

	/**
	 * Diplaying fragment view for selected nav drawer list item
	 */
	private void displayView(int position) {
		// update the main content by replacing fragments
		Fragment fragment = null;
		switch (position) {
		case 0:

			fragment = new OvalHomeFragment();

			break;
		case 1:
			// fragment = new OvalSearchFragment();
			refreshApps(connectionInfo);

			// clearApplicationData();
			break;

		default:
			break;
		}

		mDrawerLayout.closeDrawer(Gravity.START);

		if (fragment != null) {
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit();

			// update selected item and title, then close the drawer
			mDrawerList.setItemChecked(position, true);
			mDrawerList.setSelection(position);
			// setTitle(navMenuTitles[position]);
			mDrawerLayout.closeDrawer(Gravity.START);
		} else {
			// error in creating fragment
			Log.e("MainActivity", "Error in creating fragment");
		}
	}

	/*
	 * @Override public void setTitle(CharSequence title) { mTitle = title;
	 * getActionBar().setTitle(mTitle); }
	 */

	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */

	private void refreshApps(ConnectionInfo connectionInfo) {
		// TODO Auto-generated method stub

		this.sendRequestCode = AppList.REQUEST_REFRESHAPPS_FULL;
		authPrompt(connectionInfo); // utilizes "startActivityForResult", which
									// uses this.sendRequestCode

	}

	@Override
	protected void afterStartAppRTC(ConnectionInfo connectionInfo) {
		// after we have handled the auth prompt and made sure the service is
		// started...

		// create explicit intent
		Intent intent = new Intent();
		if (this.sendRequestCode == AppList.REQUEST_REFRESHAPPS_QUICK
				|| this.sendRequestCode == AppList.REQUEST_REFRESHAPPS_FULL) {
			// we're refreshing our cached list of apps that reside on the VM
			intent.setClass(OvalDrawerActivity.this, AppRTCRefreshAppsActivity.class);
			if (this.sendRequestCode == AppList.REQUEST_REFRESHAPPS_FULL)
				intent.putExtra("fullRefresh", true);
		}

		intent.putExtra("connectionID", connectionInfo.getConnectionID());

		// start the AppRTCActivity
		startActivityForResult(intent, this.sendRequestCode);

	}

	public void startSearchFragment(String searchStr) {

		Bundle bundle = new Bundle();
		bundle.putString("searchText", searchStr);
		// set Fragmentclass Arguments

		Fragment fragment = new OvalSearchFragment();

		if (fragment != null) {
			fragment.setArguments(bundle);
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.frame_container, fragment)
					.addToBackStack(OvalSearchFragment.TAG).commit();

			// update selected item and title, then close the drawer

		}
	}

	@Override
	protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
		// TODO Auto-generated method stub

		busy = false;
		if (requestCode == AppList.REQUEST_REFRESHAPPS_QUICK || requestCode == AppList.REQUEST_REFRESHAPPS_FULL) {
			if (responseCode == RESULT_CANCELED) {
				// the activity ended before processing the Apps response
				toastShort(R.string.appList_toast_refreshFail);
			} else if (responseCode == RESULT_OK) {
				toastShort(R.string.appList_toast_refreshSuccess);

				super.onActivityResult(requestCode, RESULT_REPOPULATE, intent);

				/*
				 * Intent i = new Intent(DrawerHomeActivity.this,
				 * DrawerHomeActivity.class); i.putExtra("connectionID", 0);
				 * startActivity(i); finish();
				 */
			} else {
				// this is probably a result of an AUTH_FAIL, let superclass
				// handle it
				super.onActivityResult(requestCode, responseCode, intent);
			}
		} else if (responseCode == RESULT_CANCELED && requestCode == AppList.REQUEST_STARTAPP_FINISH) {
			// the user intentionally canceled the activity, and we are
			// supposed to finish this activity after resuming
			finish();
		} else // fall back to superclass method
			super.onActivityResult(requestCode, responseCode, intent);

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Intent stopServiceIntent = new Intent(ACTION_STOP_SERVICE, null, this, SessionService.class);
		stopService(stopServiceIntent);
	}

}
