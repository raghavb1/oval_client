package com.oval.app.adapters;

import java.util.List;

import org.mitre.svmp.common.AppInfo;

import com.citicrowd.oval.R;
import com.squareup.picasso.Picasso;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class HomeGridAdapter extends BaseAdapter {

	Context context;
	List<AppInfo> appsList;

	public HomeGridAdapter(Context context, List<AppInfo> appsList) {
		super();
		this.context = context;
		this.appsList = appsList;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return appsList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return appsList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View gridView;

		if (convertView == null) {

			gridView = new View(context);

			// get layout from mobile.xml
			gridView = inflater.inflate(R.layout.grid_item_home, null);

			// set value into textview
			
			AppInfo gridItem= appsList.get(position);
			
			Holder holder = new Holder();
			holder.appIconImageView = (ImageView) gridView.findViewById(R.id.appIconImageView);
			holder.appName = (TextView) gridView.findViewById(R.id.appName);
			holder.appRating = (TextView) gridView.findViewById(R.id.appRating);

			holder.appName.setText(gridItem.getAppName());
			
			Picasso.with(context).load(gridItem.getIconUrl())

			.into(holder.appIconImageView);

		} else {
			gridView = (View) convertView;
		}

		

		return gridView;
	}

	class Holder {
		public ImageView appIconImageView;
		public TextView appName;
		public TextView appRating;
	}

}
