package org.shepherd.recall.glass.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;


public class ConfirmAdapter extends CardScrollAdapter {

	private List<CardBuilder> mCards;
	
	public ConfirmAdapter(Context mContext, String positive, String negative) {
		super();
		mCards = new ArrayList<CardBuilder>();
		CardBuilder cbNo =new CardBuilder(mContext, CardBuilder.Layout.MENU) 		
 			.setText(negative);
	 	CardBuilder cbYes =new CardBuilder(mContext, CardBuilder.Layout.MENU) 		
			.setText(positive);	 	
	 	mCards.add(cbYes);
	 	mCards.add(cbNo);
    
 	
	}
     @Override
     public int getPosition(Object item) {
         return mCards.indexOf(item);
     }

     @Override
     public int getCount() {
         return mCards.size();
     }

     @Override
     public Object getItem(int position) {
         return mCards.get(position);
     }

     @Override
     public int getViewTypeCount() {
         return CardBuilder.getViewTypeCount();
     }

     @Override
     public int getItemViewType(int position){
         return mCards.get(position).getItemViewType();
     }

     @Override
     public View getView(int position, View convertView, ViewGroup parent) {
         return mCards.get(position).getView(convertView, parent);
     }
 }
 
