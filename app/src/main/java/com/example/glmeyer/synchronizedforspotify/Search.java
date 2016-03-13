package com.example.glmeyer.synchronizedforspotify;

import android.content.Context;
import android.graphics.Color;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class Search extends AppCompatActivity {
    // Building my own version of ArrayAdapter
    private class songAdapter extends ArrayAdapter<Item> {
        // Generic overriding code (Super, etc.)
        // plus some local global variables
        int resource;
        Context context;

        public songAdapter(Context _context, int _resource, List<Item> tracks) {
            super(_context, _resource, tracks);
            resource = _resource;
            context = _context;
            this.context = _context;

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            LinearLayout newView;
            Item w = getItem(position);
            Log.d("TrackListing: ",w.name);

            // Inflate if one does not currently exist.
            if(convertView == null){
                newView = new LinearLayout(getContext());
                String inflater = Context.LAYOUT_INFLATER_SERVICE;
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
                vi.inflate(resource, newView, true);
            } else {
                newView = (LinearLayout) convertView;
            }
            TextView songTitle = (TextView) newView.findViewById(R.id.songTitle);
            songTitle.setText(w.name);

            return newView;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        String searchString;

        Bundle extras = getIntent().getExtras();
        if(extras == null){
            searchString = "";
        }else {
            searchString = extras.getString("searchText");
            TextView tv = (TextView)findViewById(R.id.searchActivitybar);
            tv.setText(searchString);
            songSearch(searchString);
        }

        // Look up song in spotify.
    }
    private songAdapter myAdapter;
    private ArrayList<Item> songList;

    public void songSearch(String searchText){
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        spotifyService service = retrofit.create(spotifyService.class);
        songList = new ArrayList<Item>();
        myAdapter = new songAdapter(this, R.layout.list_song,songList);
        ListView songListView = (ListView) findViewById(R.id.listView);
        songListView.setAdapter(myAdapter);

        Call<SpotifySearch> spotifySearchCall = service.searchSong(searchText, "track", 10, 0);
        spotifySearchCall.enqueue(new Callback<SpotifySearch>() {
            @Override
            public void onResponse(Response<SpotifySearch> response) {
                Log.d("Album Name: ", response.body().tracks.items.get(0).album.name.toString());
                for (int i = 0; i < (response.body().tracks.items.size() -1); i++){
                    myAdapter.add(response.body().tracks.items.get(i));
                }


            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }
    public interface spotifyService{
        @GET("v1/search")
        Call<SpotifySearch> searchSong(@Query("q") String query,
                                       @Query("type") String type,
                                       @Query("limit") int limit,
                                       @Query("offset") int offset);
    }
}
