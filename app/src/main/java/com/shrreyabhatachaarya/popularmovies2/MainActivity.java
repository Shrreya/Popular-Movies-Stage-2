package com.shrreyabhatachaarya.popularmovies2;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements MainFragment.OnFragmentInteractionListener,
        DetailFragment.OnFragmentInteractionListener, ConnectivityReceiver.ConnectivityReceiverListener{

    SharedPreferences sharedPreferences;
    String sort_type;
    Boolean twoPane = Boolean.FALSE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(findViewById(R.id.movie_detail_container) != null) {
            twoPane = Boolean.TRUE;
            if (savedInstanceState == null) {
                MainFragment mainFragment = new MainFragment();
                Bundle args = new Bundle();
                args.putBoolean("twoPane", twoPane);
                mainFragment.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.movies_container, mainFragment)
                        .commit();
            }
        } else {
            twoPane = Boolean.FALSE;
        }

        sharedPreferences = getSharedPreferences("popular_movies",MODE_PRIVATE);
        sort_type = sharedPreferences.getString("sort_type", "popular");

        if(sort_type.equals("popular"))
            getSupportActionBar().setTitle(R.string.app_name);
        else if(sort_type.equals("top_rated"))
            getSupportActionBar().setTitle(R.string.top_rated);
        else if(sort_type.equals("favorites"))
            getSupportActionBar().setTitle(R.string.favorites);

        // register connection status listener and check connection
        MyApplication.getInstance().setConnectivityListener(this);
        checkConnection();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //handle click on sort settings

        if (id == R.id.action_sort_settings) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final SharedPreferences.Editor editor=sharedPreferences.edit();
            int selected = 0;
            sort_type = sharedPreferences.getString("sort_type", "popular");
            if(sort_type.equals("popular"))
                selected = 0;
            else if(sort_type.equals("top_rated"))
                selected = 1;
            else if(sort_type.equals("favorites"))
                selected = 2;
            builder.setTitle(R.string.dialog_title);
            builder.setSingleChoiceItems(R.array.sort_types, selected,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0)
                                editor.putString("sort_type", "popular");
                            else if (which == 1)
                                editor.putString("sort_type", "top_rated");
                            else if (which == 2)
                                editor.putString("sort_type", "favorites");
                        }
                    });
            builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //user clicked save
                    editor.commit();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //user clicked cancel
                }
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    //refresh activity
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    startActivity(intent);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkConnection() {
        boolean isConnected = ConnectivityReceiver.isConnected();
        showMessage(isConnected);
    }

    private void showMessage(boolean isConnected) {
        if(!isConnected && !sort_type.equals("favorites"))
            Toast.makeText(MainActivity.this, "Sorry! No internet connection detected.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
            showMessage(isConnected);
    }

    public void showDetailTwoPane(DetailFragment detailFragment){
        getSupportFragmentManager().beginTransaction()
                        .replace(R.id.movie_detail_container, detailFragment)
                        .commit();
    }
}
