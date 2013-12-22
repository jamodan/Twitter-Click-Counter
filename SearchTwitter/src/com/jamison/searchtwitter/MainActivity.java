/*
 * Created By : Daniel Jamison
 */

package com.jamison.searchtwitter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import twitter4j.Query;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private Context context = null;
    private Twitter mTwitter;
	
    // List items
    private MyCursorAdapter adapter = null;
	private MatrixCursor mCursor = null;
	private ArrayList<String[]> listIDS = null;
	
	// Layout items
	private EditText searchText=null;
	private Button searchBtn=null;
	private Button cancelBtn=null;
	private Button menuBtn=null;
	private ProgressBar progress=null;
	private TextView no_results=null;
	public ListView lv = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		context = this;
		
		// Holds the twitter handles to locate tweets from the list array
		listIDS = new ArrayList<String[]>();
		
		// Sets up the twitter connection using o-auth 
        mTwitter = getTwitter();
        
        progress = (ProgressBar) findViewById(R.id.progress);
        no_results = (TextView) findViewById(R.id.no_results);
        
        searchText = (EditText) findViewById(R.id.searchText);
        // Any time the user enters text into the search bar update the search and clear buttons
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
 		// Allow user to press enter on the soft keyboard to begin the search
 		searchText.setOnKeyListener(new OnKeyListener() {
 		    @Override
 		    public boolean onKey(View v, int keyCode, KeyEvent event) {
 		        if (event.getAction()!=KeyEvent.ACTION_DOWN)
 		            return false;
 		        if(keyCode == KeyEvent.KEYCODE_ENTER ){
 		        	// Calls the search button for handling a search request
 		        	searchBtn.performClick();
 		            return true;
 		        }
 		        return false;
 		    }});
        
 		// Clears the search text
		cancelBtn = (Button) findViewById(R.id.searchCancel);
		cancelBtn.setOnClickListener(new OnClickListener(){
	    	public void onClick(View view){
	    		searchText.setText("");
	    	}
	    }
	    );
		
		// Begin the Twitter query if there is data in the search text field
		searchBtn = (Button) findViewById(R.id.search);
		searchBtn.setOnClickListener(new OnClickListener(){
	    	public void onClick(View view){
	    		if (searchText.getText().toString().length() > 0)
	    		{
	    			// Hide the soft keyboard to display the results
	    			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	    			imm.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
	    			
	    			// Get tweets and display in the list view
	    			setupListView();
	    		}
	    	}
	    }
	    );
		
		// Setup the list view
        lv = (ListView) findViewById(R.id.list);
        lv.setTextFilterEnabled(true);
        lv.setFocusable(true);
		lv.setFocusableInTouchMode(true);
		// if an item in the list view is selected load the twitter profile connected to the tweet 
		lv.setOnItemClickListener(new OnItemClickListener() 
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
			{
				String profileURL = "https://twitter.com/" + listIDS.get(position)[1];
				// Send click info to server for counting
				new UploadTask().execute(listIDS.get(position)[0], profileURL);
				
				// Let Android know the intent load a Twitter profile, let user choose to open with browser or Twitter app
				Uri uri = Uri.parse(profileURL);
				startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
			}
		});
		
		// Allow users to see the click counter web page component by opening the menu
		menuBtn = (Button) findViewById(R.id.menu_btn);
		menuBtn.setOnClickListener(new OnClickListener(){
	    	public void onClick(View view){
	    		openOptionsMenu();
	    	}
	    }
	    );
	}
	
	// Sets up the twitter connection using o-auth
	private Twitter getTwitter() {
		ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setOAuthConsumerKey(getString(R.string.consumer_key));
        cb.setOAuthConsumerSecret(getString(R.string.consumer_secret));
        cb.setOAuthAccessToken(getString(R.string.access_token));
        cb.setOAuthAccessTokenSecret(getString(R.string.access_token_secret));
        cb.setJSONStoreEnabled(true);
        return new TwitterFactory(cb.build()).getInstance();
    }
	
	// Resets the list view each time a new search is performed
	public void setupListView()
	{
		adapter = null;
		listIDS.clear();
		mCursor = new MatrixCursor(new String[] {	"_id", "pic", "user_name", "user_id", "tweet" });
		
		// Attempt to download tweets using an async task
		try {
            new DownloadTask().execute();
        } catch (Exception e) {
        	mCursor.addRow(new Object[] {"Twitter query failed: " + e.toString()});
        }
	}
	
	// Async task to download Tweets from Twitter
	private class DownloadTask extends AsyncTask<String, Integer, String> {

		// Show the progress bar before processing the download request
	    @Override
	    protected void onPreExecute() {
	        progress.setVisibility(View.VISIBLE);
	        super.onPreExecute();
	    }

	    @Override
	    protected String doInBackground(String... params) {
	    	try {
	    		// List to contain Tweets
	    		List<twitter4j.Status> statuses = mTwitter.search(new Query(searchText.getText().toString())).getTweets();
	            
	    		// Unique id for each Tweet used by the list view
	    		int i = 1;
	    		
	    		// Iterate through all the Tweets returned
	            for (twitter4j.Status s : statuses) {
	                User tweet = s.getUser();
	                
	                // Download the profile image connected to the Tweet
	                InputStream is = (InputStream) new URL(tweet.getProfileImageURL()).getContent();
			        Drawable drawable = Drawable.createFromStream(is, "User Pic");
			        
			        // Convert the profile pic from a Drawable to a Blob
	                Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
	                ByteArrayOutputStream stream = new ByteArrayOutputStream();
	                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
	                byte[] pic = stream.toByteArray();
	                
	                // Store the Twitter screen names in the list to allow the list view item to be clicked and redirect to the profile page
	                listIDS.add(new String[] {tweet.getName(), tweet.getScreenName()});
					
	                // Fill a custom cursor with all the tweet information to be displayed in the list view
					mCursor.addRow(new Object[] {
							i++,
							pic,
							tweet.getName(),
							"@" + tweet.getScreenName(),
							s.getText()
					});
	            }
	        } catch (Exception e) {
	        	mCursor.addRow(new Object[] {"Twitter query failed: " + e.toString()});
	        }
			
	    	// Call the custom adapter to prepare the Tweets for the list view
			adapter = new MyCursorAdapter(context, mCursor);
			
	        return null;
	    }

	    @Override
	    protected void onPostExecute(String result) {
	    	// Load Tweets into list view
	        lv.setAdapter(adapter);
	        
	        // If no results were found show a message, focus back on the search field, and reopen the soft keyboard
	        if(mCursor == null || mCursor.getCount() == 0)
			{
				no_results.setVisibility(View.VISIBLE);
				searchText.requestFocus();
				((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
			}
	        // Hide the no results found message
	        else
	        {
	        	no_results.setVisibility(View.GONE);
	        }
	        
	        // Hide the progress bar after processing the download request
	        progress.setVisibility(View.GONE);
	        
	        super.onPostExecute(result);
	    }
	}
	
	// Async task to upload click data to server component
	private class UploadTask extends AsyncTask<String, Integer, String> {

	    @Override
	    protected String doInBackground(String... params) {
	        HttpClient httpclient = new DefaultHttpClient();
	        
	        String name = "";
	        String URL = "";
	        // Attempt to encode non English chars
			try {
				name = URLEncoder.encode(params[0], "utf-8");
				URL = URLEncoder.encode(params[1], "utf-8");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			
			// Attempt to post data to server web component
	        HttpPost httppost = new HttpPost("http://danjamison.dyndns.org/twitterclickcounter/scripts/post_click.php?ProfileName=" + name + "&URL=" + URL);
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
	
	// Called when website menu button is pressed. Opens a browser to show the click counter
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
}
