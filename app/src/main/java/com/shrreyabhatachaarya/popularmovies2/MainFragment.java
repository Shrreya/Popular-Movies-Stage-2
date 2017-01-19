package com.shrreyabhatachaarya.popularmovies2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class MainFragment extends Fragment implements ConnectivityReceiver.ConnectivityReceiverListener{

    private OnFragmentInteractionListener mListener;

    View mMainView;

    ArrayList<String> posters;
    ArrayList<Integer> ids;

    ArrayList<byte[]> fav_posters;
    ArrayList<String> fav_ids;

    ImageAdapter imageAdapter;
    GridView gridView;

    MoviesFetchTask mTask;

    SharedPreferences sharedPreferences;
    String sort_type;
    Boolean twoPane;

    //add your api key here
    String api_key = "f20d6b74956af8e8c153eb39ac133525";

    String LOG_TAG = "MainFragment";

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication.getInstance().setConnectivityListener(this);
        Bundle args = getArguments();
        if(args != null)
            twoPane = args.getBoolean("twoPane");
        else
            twoPane = Boolean.FALSE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mMainView = inflater.inflate(R.layout.fragment_main, container, false);
        gridView = (GridView) mMainView.findViewById(R.id.gridview);
        //handle clicks on posters
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boolean isConnected = ConnectivityReceiver.isConnected();
                if(isConnected) {
                    if(!twoPane) {
                        Intent intent = new Intent(getContext(), DetailActivity.class);
                        if (sort_type.equals("favorites")) {
                            intent.putExtra("Movie ID", Integer.parseInt(fav_ids.get(position)));
                        } else {
                            intent.putExtra("Movie ID", ids.get(position));
                            intent.putExtra("Poster", posters.get(position));
                        }
                        startActivity(intent);
                    } else {
                        DetailFragment detailFragment = new DetailFragment();
                        Bundle args = new Bundle();
                        args.putBoolean("twoPane", Boolean.TRUE);
                        if (sort_type.equals("favorites")) {
                            args.putInt("MovieId", Integer.parseInt(fav_ids.get(position)));
                        } else {
                            args.putInt("MovieId", ids.get(position));
                            args.putString("Poster", posters.get(position));
                        }
                        detailFragment.setArguments(args);
                        ((MainActivity)getActivity()).showDetailTwoPane(detailFragment);
                    }
                } else {
                    Toast.makeText(getActivity(), "Sorry! No internet connection detected.", Toast.LENGTH_LONG).show();
                }
            }
        });

        Log.d(LOG_TAG, "Starting fetch");

        sharedPreferences = getActivity().getSharedPreferences("popular_movies",getActivity().MODE_PRIVATE);
        sort_type = sharedPreferences.getString("sort_type", "popular");

        mTask = new MoviesFetchTask(this);
        mTask.execute(sort_type);

        // on configuration changes (screen rotation) we want fragment member variables to be preserved
        setRetainInstance(true);
        return mMainView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {

    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public class MoviesFetchTask extends AsyncTask<String, Void, String> {

        String LOG_TAG = "MoviesFetchTask";

        MainFragment container;
        public MoviesFetchTask(MainFragment f) {
            this.container = f;
        }


        @Override
        protected String doInBackground(String... params) {

            if(params[0].equals("favorites")) {
                Uri favorites = Uri.parse("content://com.shrreyabhatachaarya.provider.popularmovies2/favorites");
                Cursor c = getContext().getContentResolver().query(favorites, null, null, null, "_id");
                fav_ids = new ArrayList<String>();
                fav_posters = new ArrayList<byte[]>();
                if(c.moveToFirst()) {
                    do{
                        fav_ids.add(c.getString(c.getColumnIndex(FavoritesProvider._ID)));
                        fav_posters.add(c.getBlob(c.getColumnIndex(FavoritesProvider.POSTER)));
                    } while(c.moveToNext());
                }
            }

            else {
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;
                String moviesJsonStr = null;

                posters = new ArrayList<String>();
                ids = new ArrayList<Integer>();

                try {
                    String base_url = "https://api.themoviedb.org/3/movie/";
                    URL url = new URL(base_url + params[0] + "?api_key=" + api_key);
                    Log.d(LOG_TAG, "URL: " + url.toString());

                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line + "\n");
                    }
                    if (buffer.length() == 0) {
                        return null;
                    }
                    moviesJsonStr = buffer.toString();
                    Log.d(LOG_TAG, "JSON Parsed: " + moviesJsonStr);

                    JSONObject main = new JSONObject(moviesJsonStr);
                    JSONArray arr = main.getJSONArray("results");
                    JSONObject movie;
                    for (int i = 0; i < arr.length(); i++) {
                        movie = arr.getJSONObject(i);
                        ids.add(movie.getInt("id"));
                        posters.add(movie.getString("poster_path"));
                    }
                    Log.d(LOG_TAG, "Posters:" + posters);
                    Log.d(LOG_TAG, "IDs:" + ids);

                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error", e);
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (final IOException e) {
                            Log.e(LOG_TAG, "Error closing stream", e);
                        }
                    }
                }
            }

            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {

            if(result.equals("favorites")) {
                imageAdapter = new ImageAdapter(getActivity(), fav_posters, result);
            }

            else {
                imageAdapter = new ImageAdapter(getActivity(), posters);
            }
            try {
                gridView.setAdapter(imageAdapter);
            } catch (NullPointerException e) {
                Log.d(LOG_TAG, "Error", e);
            }
        }
    }
}
