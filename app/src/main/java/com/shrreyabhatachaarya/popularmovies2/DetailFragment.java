package com.shrreyabhatachaarya.popularmovies2;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DetailFragment extends Fragment implements View.OnClickListener{

    private OnFragmentInteractionListener mListener;

    Integer id;
    TextView title, user_rating, release_date, synopsis;
    ImageView poster_image;
    Button favorite, share;

    //Add your api key here
    String api_key = "f20d6b74956af8e8c153eb39ac133525";

    String[] youtube_ids;
    int trailer_count;

    int review_count;

    String LOG_TAG = "DetailFragment";

    View rootView;

    SharedPreferences sharedPreferences;
    String sort_type;

    byte[] fav_poster;

    public DetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getSharedPreferences("popular_movies",getActivity().MODE_PRIVATE);
        sort_type = sharedPreferences.getString("sort_type", "popular");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        title = (TextView) rootView.findViewById(R.id.title);
        user_rating = (TextView) rootView.findViewById(R.id.user_rating);
        release_date = (TextView) rootView.findViewById(R.id.release_date);
        synopsis = (TextView) rootView.findViewById(R.id.synopsis);

        poster_image = (ImageView) rootView.findViewById(R.id.poster_image);

        favorite = (Button) rootView.findViewById(R.id.favorite);
        favorite.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                addToFavorites();
            }
        });

        share = (Button) rootView.findViewById(R.id.share);
        share.setOnClickListener(this);

        Bundle arguments = this.getArguments();
        if(arguments != null)
            id = arguments.getInt("MovieId");

        FetchMovieDetails fetchMovieDetails = new FetchMovieDetails();
        fetchMovieDetails.execute();

        // on configuration changes (screen rotation) fragment member variables to be preserved
        setRetainInstance(true);
        return rootView;
    }

    public void addToFavorites() {
        ContentValues values = new ContentValues();
        values.put(FavoritesProvider._ID, id);
        values.put(FavoritesProvider.TITLE, title.getText().toString());
        values.put(FavoritesProvider.SYNOPSIS, synopsis.getText().toString());
        values.put(FavoritesProvider.USER_RATING, user_rating.getText().toString());
        values.put(FavoritesProvider.RELEASE_DATE, release_date.getText().toString());

        BitmapDrawable drawable = (BitmapDrawable) poster_image.getDrawable();
        Bitmap bmp = drawable.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] image = stream.toByteArray();
        values.put(FavoritesProvider.POSTER, image);

        Uri uri = getContext().getContentResolver().insert(FavoritesProvider.CONTENT_URI, values);
        Log.d(LOG_TAG, uri.toString());

        if(uri.toString().equals("Duplicate"))
            Toast.makeText(getContext(), R.string.fav_exists, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getContext(), R.string.fav_success, Toast.LENGTH_SHORT).show();
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

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.share) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            if(trailer_count != 0)
                shareIntent.putExtra(Intent.EXTRA_TEXT, "http://www.youtube.com/watch?v=" + youtube_ids[0]);
            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_to)));
        }
    }

    public class FetchMovieDetails extends AsyncTask<Void, Void, Void> {

        String LOG_TAG = "FetchMovieDetails";
        String original_title, releaseDate, plotSynopsis, poster_path, rating;
        Double ratings;

        JSONArray reviews;

        @Override
        protected Void doInBackground(Void... params) {

            if(sort_type.equals("favorites")) {
                Uri favorites = Uri.parse("content://com.shrreyabhatachaarya.provider.popularmovies2/favorites");
                Cursor c = getActivity().getContentResolver().query(favorites, null, null, null, "_id");
                if (c.moveToFirst()) {
                    do {
                        if (c.getString(c.getColumnIndex(FavoritesProvider._ID)).equals(id.toString())) {
                            original_title = c.getString(c.getColumnIndex(FavoritesProvider.TITLE));
                            rating = c.getString(c.getColumnIndex(FavoritesProvider.USER_RATING));
                            plotSynopsis = c.getString(c.getColumnIndex(FavoritesProvider.SYNOPSIS));
                            releaseDate = c.getString(c.getColumnIndex(FavoritesProvider.RELEASE_DATE));
                            fav_poster = c.getBlob(c.getColumnIndex(FavoritesProvider.POSTER));
                        }
                    } while (c.moveToNext());
                }
            }
            else {
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                //get movie details
                try {
                    String base_url = "https://api.themoviedb.org/3/movie/";
                    URL url = new URL(base_url + Integer.toString(id) + "?api_key=" + api_key);
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
                    String movieJsonStr = buffer.toString();
                    Log.d(LOG_TAG, "JSON Parsed: " + movieJsonStr);

                    JSONObject main = new JSONObject(movieJsonStr);
                    original_title = main.getString("original_title");
                    releaseDate = main.getString("release_date");
                    ratings = main.getDouble("vote_average");
                    plotSynopsis = main.getString("overview");
                    poster_path = "http://image.tmdb.org/t/p/w185" + main.getString("poster_path");

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

                //get trailers
                try {
                    String base_url = "https://api.themoviedb.org/3/movie/";
                    URL url = new URL(base_url + Integer.toString(id) + "/videos" + "?api_key=" + api_key);
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
                    String trailerJsonStr = buffer.toString();
                    Log.d(LOG_TAG, "JSON Parsed: " + trailerJsonStr);

                    JSONObject main = new JSONObject(trailerJsonStr);
                    String results = main.getString("results");
                    JSONArray trailers = new JSONArray(results);
                    trailer_count = trailers.length();
                    Log.d(LOG_TAG, "Number of Trailers:" + trailer_count);

                    //Ensure there is at least one trailer
                    if (trailer_count != 0) {
                        youtube_ids = new String[trailer_count];
                        for (int i = 0; i < trailer_count; i++) {
                            JSONObject obj = trailers.getJSONObject(i);
                            youtube_ids[i] = obj.getString("key");
                        }
                    }

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

                //get reviews
                try {
                    String base_url = "https://api.themoviedb.org/3/movie/";
                    URL url = new URL(base_url + Integer.toString(id) + "/reviews" + "?api_key=" + api_key);
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
                    String reviewJsonStr = buffer.toString();
                    Log.d(LOG_TAG, "JSON Parsed: " + reviewJsonStr);

                    JSONObject main = new JSONObject(reviewJsonStr);
                    String results = main.getString("results");
                    reviews = new JSONArray(results);
                    review_count = main.getInt("total_results");
                    Log.d(LOG_TAG, "Number of Reviews:" + review_count);

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
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            title.setText(original_title);
            synopsis.setText(plotSynopsis);

            poster_image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            poster_image.setPadding(8, 8, 8, 8);

            if(sort_type.equals("favorites")) {
                user_rating.setText(rating);
                release_date.setText(releaseDate);
                Bitmap bmp = BitmapFactory.decodeByteArray(fav_poster, 0, fav_poster.length);
                poster_image.setImageBitmap(bmp);
                share.setVisibility(View.INVISIBLE);
                favorite.setVisibility(View.INVISIBLE);
            }
            else {
                user_rating.setText("User Rating: " + Double.toString(ratings) + "/10");
                release_date.setText("Release Date: " + releaseDate);
                Picasso.with(getContext()).load(poster_path).into(poster_image);

                favorite.setVisibility(View.VISIBLE);

                LinearLayout ll = (LinearLayout) rootView.findViewById(R.id.ll);

                //Ensure there is at least one trailer
                if (trailer_count != 0) {

                    share.setVisibility(View.VISIBLE); //share button visible

                    View v = createLineView();
                    ll.addView(v);

                    for (int i = 0; i < trailer_count; i++) {
                        Button b = new Button(getContext());
                        LinearLayout.LayoutParams b_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        b_params.setMargins(30, 10, 20, 20);
                        b.setLayoutParams(b_params);
                        b.setText("Watch Trailer " + Integer.toString(i + 1));
                        b.setId(i + 1001);
                        b.setBackgroundColor(getResources().getColor(R.color.indigo));
                        b.setTextColor(getResources().getColor(R.color.white));
                        b.setTextSize(18);
                        b.setPadding(20, 10, 20, 10);
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String youtube_id = youtube_ids[view.getId() - 1001];
                                try {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + youtube_id));
                                    startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + youtube_id));
                                    String title = "Watch video via";
                                    Intent chooser = Intent.createChooser(intent, title);
                                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                                        startActivity(chooser);
                                    }
                                }

                            }
                        });
                        ll.addView(b);
                    }
                } else {
                    share.setVisibility(View.INVISIBLE); //share button invisible
                }

                //Ensure there is at least one review
                if (review_count != 0) {

                    ll.addView(createLineView());

                    TextView header = new TextView(getContext());
                    LinearLayout.LayoutParams header_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    header_params.setMargins(30, 10, 20, 20);
                    header.setLayoutParams(header_params);
                    header.setText(R.string.reviews);
                    header.setTextSize(25);
                    header.setTextColor(getResources().getColor(R.color.colorPrimary));
                    ll.addView(header);

                    for (int i = 0; i < review_count; i++) {
                        TextView tv = new TextView(getContext());
                        LinearLayout.LayoutParams tv_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        tv_params.setMargins(30, 10, 20, 20);
                        tv.setLayoutParams(tv_params);
                        tv.setTextColor(getResources().getColor(R.color.black));
                        try {
                            String review = reviews.getJSONObject(i).getString("content");
                            tv.setText(review);
                            ll.addView(tv);
                            ll.addView(createLineView());
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "JSON Error", e);
                        }
                    }
                }
            }
        }

        public View createLineView() {
            View v = new View(getContext());
            v.setBackgroundColor(getResources().getColor(R.color.black));
            LinearLayout.LayoutParams v_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    3);
            v_params.topMargin = 30;
            v_params.bottomMargin = 30;
            v.setLayoutParams(v_params);
            return v;
        }
    }
}
