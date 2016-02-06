package com.oval.app.adapters;

import java.util.List;
import java.util.StringTokenizer;

import org.mitre.svmp.activities.ConnectionList;
import org.mitre.svmp.client.SendNetIntent;
import org.mitre.svmp.common.AppInfo;
import org.mitre.svmp.common.Constants;
import org.mitre.svmp.common.DatabaseHandler;

import com.citicrowd.oval.R;
import com.oval.app.vo.AptoideSearchResultItemVo;
import com.oval.app.vo.SearchResultItemVO;
import com.squareup.picasso.Picasso;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchListAdapter extends BaseAdapter {

	private Activity activity;
	private LayoutInflater inflater;
	private List<AptoideSearchResultItemVo> items;
	private DatabaseHandler dbHandler;

	public SearchListAdapter(Activity activity, List<AptoideSearchResultItemVo> items) {
		// TODO Auto-generated constructor stub

		this.activity = activity;
		this.items = items;
		this.dbHandler = new DatabaseHandler(activity);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return items.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub

		if (inflater == null)
			inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null)
			convertView = inflater.inflate(R.layout.list_item_search, null);

		Holder holder = new Holder();
		holder.appNameTextView = (TextView) convertView.findViewById(R.id.appNameTextView);
		holder.appCategoryTextView = (TextView) convertView.findViewById(R.id.appCategoryTextView);
		holder.appIconImageView = (ImageView) convertView.findViewById(R.id.appIconImageView);
		holder.compatibilityView=(View) convertView.findViewById(R.id.compatibilityView);

		if (items != null) {
			AptoideSearchResultItemVo searchItem = items.get(position);

			if (searchItem != null) {
				holder.appCategoryTextView.setText(searchItem.getCategory());
				holder.appNameTextView.setText(searchItem.getName());
				String icon = searchItem.getIconHd();

				if (icon == null || icon.isEmpty() || icon.equals("null")) {

					icon = searchItem.getIcon();

				}

				Picasso.with(activity).load( icon)

						.into(holder.appIconImageView);
				
				if(searchItem.getCompatibility()!=null)
				{
					if(searchItem.getCompatibility().equals("true"))
					{
						holder.compatibilityView.setBackgroundColor(activity.getResources().getColor(R.color.compatiblity_green));
					}
					else
					{
						holder.compatibilityView.setBackgroundColor(activity.getResources().getColor(R.color.compatibility_false));
					}
				}
				
				
			}
		}
		return convertView;
	}

	class Holder {
		public ImageView appIconImageView;
		public TextView appNameTextView;
		public TextView appCategoryTextView;
		public View compatibilityView;

	}

}
