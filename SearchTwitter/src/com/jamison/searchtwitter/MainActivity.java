/*
 * Created By : Daniel Jamison
 */

package com.jamison.searchtwitter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import twitter4j.Query;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {

	Context mContext;
    Twitter mTwitter;
    ListView mListView;
	
    private MyCursorAdapter adapter = null;
	private MatrixCursor mCursor = null;
	public List<twitter4j.Status> statuses = null;
	
	ArrayList<String[]> listIDS = null;
	Context context = null;
	ListView lv = null;
	Intent intent;
	String columnData = "";
	
	private EditText searchText=null;
	private Button searchBtn=null;
	private Button cancelBtn=null;
	private ProgressBar progress=null;
	public ConfigurationBuilder cb = new ConfigurationBuilder();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		context = this;
		
		listIDS = new ArrayList<String[]>();
        mTwitter = getTwitter();
        
        progress = (ProgressBar) findViewById(R.id.progress);
        searchText = (EditText) findViewById(R.id.searchText);
        // any time the user enters text into the search bar update the search and clear buttons
 		searchText.addTextChangedListener(new TextWatcher() {
             public void onTextChanged(CharSequence s, int start, int before, int count) {
             }

             public void beforeTextChanged(CharSequence s, int start, int count, int after) {
             }

             public void afterTextChanged(Editable s) {
            	 if (searchText.getText().toString().length() == 0)
            	 {
            		 cancelBtn.setVisibility(View.GONE);
            		 searchBtn.setBackgroundResource(R.drawable.forward_dark);
            	 }
            	 else
            	 {
            		 cancelBtn.setVisibility(View.VISIBLE);
            		 searchBtn.setBackgroundResource(R.drawable.forward_light);
            	 }
             }
 		});
        
		cancelBtn = (Button) findViewById(R.id.searchCancel);
		cancelBtn.setOnClickListener(new OnClickListener(){
	    	public void onClick(View view){
	    		searchText.setText("");
	    	}
	    }
	    );
		searchBtn = (Button) findViewById(R.id.search);
		searchBtn.setOnClickListener(new OnClickListener(){
	    	public void onClick(View view){
	    		if (searchText.getText().toString().length() > 0)
	    		{
	    			InputMethodManager imm = (InputMethodManager)getSystemService(
	    				      Context.INPUT_METHOD_SERVICE);
	    				imm.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
	    			setupListView();
	    		}
	    	}
	    }
	    );
		
		// Displays the information in the list
        lv = (ListView) findViewById(R.id.list);

        lv.setTextFilterEnabled(true);
        lv.setFocusable(true);
		lv.setFocusableInTouchMode(true);
		lv.setOnItemClickListener(new OnItemClickListener() 
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
			{
				String profileURL = "https://twitter.com/" + listIDS.get(position)[1];
				// send click info to server for counting
				new UploadTask().execute(listIDS.get(position)[0], profileURL);
					
				Uri uri = Uri.parse(profileURL);
				startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
			}
		});
	}
	
	private Twitter getTwitter() {
        
        cb.setOAuthConsumerKey(getString(R.string.consumer_key));
        cb.setOAuthConsumerSecret(getString(R.string.consumer_secret));
        cb.setOAuthAccessToken(getString(R.string.access_token));
        cb.setOAuthAccessTokenSecret(getString(R.string.access_token_secret));
        cb.setJSONStoreEnabled(true);
        return new TwitterFactory(cb.build()).getInstance();
    }
	
	public void setupListView()
	{
		adapter = null;
    	
    	String[] allColumns = null;
    	List<Status> statuses = new ArrayList<Status>();
		try {
            listIDS.clear();
            allColumns = new String[] {	"_id", "pic", "user_id", "tweet" };
            mCursor = new MatrixCursor(allColumns);
            columnData = "";
            new DownloadTask().execute();
        } catch (Exception e) {
        	mCursor.addRow(new Object[] {"Twitter query failed: " + e.toString()});
        }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
		switch (item.getItemId()) {
	    	case R.id.menu_webpage:
	    		website(super.getCurrentFocus());
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
    	}
    }
	
	// Called when the Other Apps by Us button is pressed. Launches a search in Google Play for our developer name.
	public void website(View v)
	{
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("http://danjamison.dyndns.org/twitterclickcounter/"));

		try {
			startActivity(intent);
		}
		catch (ActivityNotFoundException e){
			Toast.makeText(context, "NO Viewer", Toast.LENGTH_SHORT).show();
		}
	}
	
	private class UploadTask extends AsyncTask<String, Integer, String> {

	    @Override
	    protected String doInBackground(String... params) {
	        HttpClient httpclient = new DefaultHttpClient();
	        
	        String name = "";
	        String URL = "";
			try {
				name = URLEncoder.encode(params[0], "utf-8");
				URL = URLEncoder.encode(params[1], "utf-8");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			
	        HttpPost httppost = new HttpPost(
	                "http://danjamison.dyndns.org/twitterclickcounter/scripts/post_click.php?ProfileName=" + name + "&URL=" + URL);
	        try {
				httpclient.execute(httppost);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	        return null;
	    }
	}
	
	private class DownloadTask extends AsyncTask<String, Integer, String> {

	    @Override
	    protected void onPreExecute() {
	        progress.setVisibility(View.VISIBLE);
	        super.onPreExecute();
	    }

	    @Override
	    protected String doInBackground(String... params) {
			try {
				statuses = mTwitter.search(new Query(searchText.getText().toString())).getTweets();
	            int i = 1;
	            for (twitter4j.Status s : statuses) {
	                User tweet = s.getUser();
	                String user_pic = tweet.getProfileImageURL();
	                
	                listIDS.add(new String[] {tweet.getName(), tweet.getScreenName()});
					
					mCursor.addRow(new Object[] {
							i++,
							user_pic,
							tweet.getName(),
							s.getText()
					});
	            }
	        } catch (Exception e) {
	        	mCursor.addRow(new Object[] {"Twitter query failed: " + e.toString()});
	        }
			adapter = new MyCursorAdapter(context, mCursor);

	        return null;
	    }

	    @Override
	    protected void onPostExecute(String result) {
	        lv.setAdapter(adapter);
	        progress.setVisibility(View.GONE);
	        // process the result
	        super.onPostExecute(result);
	    }
	}
}
