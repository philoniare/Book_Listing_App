package com.example.philoniare.booklisting2;

import android.app.SearchManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {
    private ListView booksListView;
    private List<BookItem> mBooks;
    private BookListAdapter mBookListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize variables and the main listView
        mBooks = new ArrayList<BookItem>();
        booksListView = (ListView) findViewById(R.id.booksListView);
        mBookListAdapter = new BookListAdapter(this, R.layout.book_item, mBooks);
        booksListView.setAdapter(mBookListAdapter);
        TextView empty_listView = (TextView) findViewById(R.id.empty_listView);
        booksListView.setEmptyView(empty_listView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Retrieve the SearchView and plug it into SearchManager
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                // start an asyncTask with the given search term
                new fetchNewsTask().execute(query);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private class fetchNewsTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://www.googleapis.com/books/v1/volumes?q=" + params[0] + "&maxResults=10");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                int status = conn.getResponseCode();
                switch (status) {
                    case 200:
                        // return the response as String for json parsing
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line+"\n");
                        }
                        br.close();
                        return sb.toString();
                    default:
                        // server request unsuccessful
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, "Error with HTTP request");
                }

            } catch (MalformedURLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Exception ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            // Parse the response json
            JsonParser parser = new JsonParser();
            JsonObject element = parser.parse(result).getAsJsonObject();
            JsonElement responseList = element.get("items");
            JsonArray bookList = responseList.getAsJsonArray();

            mBooks.clear();
            for(JsonElement book : bookList) {
                JsonObject bookObj = book.getAsJsonObject();
                JsonElement bookVolumeInfo = bookObj.get("volumeInfo");
                JsonObject bookVolumeInfoObj = bookVolumeInfo.getAsJsonObject();

                // Parse the title
                JsonElement bookTitle = bookVolumeInfoObj.get("title");

                // Parse the authors
                Type listType = new TypeToken<List<String>>() {}.getType();
                List<String> authorsList = new Gson().fromJson(bookVolumeInfoObj.get("authors"), listType);
                String parsedBookTitle = bookTitle.toString().substring(1, bookTitle.toString().length() - 1);
                String bookDetails;
                if (authorsList.size() > 0) {
                    bookDetails = parsedBookTitle + " by " + authorsList.get(0);
                    for(int i = 1; i < authorsList.size(); i++) {
                        bookDetails += ", " + authorsList.get(i);
                    }
                } else {
                    bookDetails = parsedBookTitle;
                }

                // Parse the thumbnail
                JsonElement imageLinks = bookVolumeInfoObj.get("imageLinks");
                if (imageLinks != null) {
                    JsonObject imageLinksObj = imageLinks.getAsJsonObject();
                    String imageUrl = imageLinksObj.get("smallThumbnail").toString();
                    mBooks.add(new BookItem(bookDetails, imageUrl.substring(1, imageUrl.length() - 1)));
                } else {
                    // no thumbnail found, imageView empty
                    mBooks.add(new BookItem(bookDetails, ""));
                }
            }
            mBookListAdapter.notifyDataSetChanged();
        }
    }
}
