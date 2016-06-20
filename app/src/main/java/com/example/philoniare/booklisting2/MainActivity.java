package com.example.philoniare.booklisting2;

import android.app.SearchManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {
    private ListView booksListView;
    private List<BookItem> mBooks;
    private BookListAdapter mBookListAdapter;
    private TextView empty_listView;

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
        empty_listView = (TextView) findViewById(R.id.empty_listView);
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
                String formattedParam = URLEncoder.encode(params[0]);
                URL url = new URL("https://www.googleapis.com/books/v1/volumes?q=" + formattedParam + "&maxResults=10");
                Log.d("Requested URL", "https://www.googleapis.com/books/v1/volumes?q=" + formattedParam + "&maxResults=10");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                int status = conn.getResponseCode();
                Log.d("HTTPStatus", Integer.toString(status));
                switch (status) {
                    case 200:
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        // return the response as String for json parsing
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line+"\n");
                        }
                        br.close();
                        return sb.toString();
                    default:
                        // server request unsuccessful
                        Log.d("HTTP REQUEST", "BAD RESPONSE");
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
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            mBooks.clear();
            if (result != null) {
                // Parse the response json
                JsonParser parser = new JsonParser();
                JsonObject element = parser.parse(result).getAsJsonObject();
                JsonElement responseList = element.get("items");
                if (responseList != null) {
                    JsonArray bookList = responseList.getAsJsonArray();

                    for (JsonElement book : bookList) {
                        JsonObject bookObj = book.getAsJsonObject();
                        JsonElement bookVolumeInfo = bookObj.get("volumeInfo");
                        JsonObject bookVolumeInfoObj = bookVolumeInfo.getAsJsonObject();

                        // Parse the title
                        JsonElement bookTitle = bookVolumeInfoObj.get("title");

                        // Parse the authors
                        Type listType = new TypeToken<List<String>>() {
                        }.getType();
                        List<String> authorsList = new Gson().fromJson(bookVolumeInfoObj.get("authors"), listType);
                        String parsedBookTitle = bookTitle.toString().substring(1, bookTitle.toString().length() - 1);
                        String bookDetails;
                        if (authorsList != null && authorsList.size() > 0) {
                            bookDetails = parsedBookTitle + " by " + authorsList.get(0);
                            for (int i = 1; i < authorsList.size(); i++) {
                                bookDetails += ", " + authorsList.get(i);
                            }
                        } else {
                            // No book authors were given
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
                } else {
                    empty_listView.setText(getString(R.string.no_books_found));
                }
            }
            mBookListAdapter.notifyDataSetChanged();
        }
    }
}
