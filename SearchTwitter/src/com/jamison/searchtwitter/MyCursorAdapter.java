/*
 * Created By : Daniel Jamison
 */

package com.jamison.searchtwitter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class MyCursorAdapter extends CursorAdapter {
   private final LayoutInflater mInflater;

    public MyCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
        mInflater=LayoutInflater.from(context);
    }

    // Load the data into each list view item
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    	// Load the profile image after converting to from a blob to a bitmap
    	ImageView pic=(ImageView)view.findViewById(R.id.pic);
		BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = false;
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[1024 *32];
        pic.setImageBitmap(BitmapFactory.decodeByteArray(cursor.getBlob(cursor.getColumnIndex("pic")) , 0, cursor.getBlob(cursor.getColumnIndex("pic")).length, options));
        
		TextView user_name = (TextView)view.findViewById(R.id.user_name);
		user_name.setText(cursor.getString(cursor.getColumnIndex("user_name")));
		
		TextView user_id = (TextView)view.findViewById(R.id.user_id);
		user_id.setText(cursor.getString(cursor.getColumnIndex("user_id")));
		
		TextView tweet=(TextView)view.findViewById(R.id.tweet);
		tweet.setText(cursor.getString(cursor.getColumnIndex("tweet")));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
    	final View view=mInflater.inflate(R.layout.list_item_result,parent,false); 
    	return view;
    }
}