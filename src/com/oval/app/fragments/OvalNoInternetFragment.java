package com.oval.app.fragments;

import com.citicrowd.oval.R;
import com.oval.util.ConnectionDetector;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class OvalNoInternetFragment extends Fragment{
	
	Button retry;
	Context context;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView = inflater.inflate(R.layout.fragment_nointernet, container, false);
		context=getActivity();
		
		retry= (Button)rootView.findViewById(R.id.retryNetBtn);
		
		retry.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				if(ConnectionDetector.isConnectingToInternet(context))
				{
					getActivity().getFragmentManager().popBackStack();
				}
				
			}
		});
		
		return rootView;
	}

}
