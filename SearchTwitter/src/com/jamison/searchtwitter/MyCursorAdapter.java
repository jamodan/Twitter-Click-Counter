/*
 * Created By : Daniel Jamison
 */

package com.jamison.searchtwitter;

import java.io.InputStream;
import java.net.URL;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class MyCursorAdapter extends CursorAdapter {
   private Context mContext;
   private final LayoutInflater mInflater;
   int layout = 0;
   int instanceAccessType = 0;


    public MyCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
        this.mContext = context;
        mInflater=LayoutInflater.from(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

    	ImageView pic=(ImageView)view.findViewById(R.id.pic);
		String imagefile = cursor.getString(cursor.getColumnIndex("pic"));
		if (imagefile.length() > 0)
		{
			new DownloadTask().execute(pic, cursor.getString(cursor.getColumnIndex("pic")));
		}
		
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
    
    private class DownloadTask extends AsyncTask<Object, Integer, String> {

	    @Override
	    protected String doInBackground(Object... params) {
	    	try {
		        InputStream is = (InputStream) new URL((String) params[1]).getContent();
		        ImageView pic = (ImageView) params[0];
		        pic.setImageDrawable(Drawable.createFromStream(is, "User Pic"));
		    } catch (Exception e) {
		        return null;
		    }

	        return null;
	    }

	    @Override
	    protected void onPostExecute(String result) {
	    	// process the result
	        super.onPostExecute(result);
	    }
	}
}