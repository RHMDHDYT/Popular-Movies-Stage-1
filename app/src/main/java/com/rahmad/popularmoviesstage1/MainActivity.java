package com.rahmad.popularmoviesstage1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.rahmad.popularmoviesstage1.models.movielist.MovieResponse;
import com.rahmad.popularmoviesstage1.models.movielist.MovieResultsItem;
import com.rahmad.popularmoviesstage1.util.ApiClient;
import com.rahmad.popularmoviesstage1.util.ApiInterface;
import com.rahmad.popularmoviesstage1.util.AppSharedPref;
import com.rahmad.popularmoviesstage1.util.NetworkUtil;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * The type Main activity.
 */
public class MainActivity extends AppCompatActivity {

  private TextView textInfoCaption;
  private ProgressBar progressBar;
  private static final int COLUMN_SIZE = 2;
  private Call<MovieResponse> callMovies;
  private static final String KEY_SAVED_INSTANCE_STATE = "movie_poster_key";
  private final List<MovieResultsItem> moviesList = new ArrayList<>();
  private MoviesAdapter moviesAdapter;
  private SwipeRefreshLayout swipeContainer;
  private Boolean isCurrentPopular;
  private AppSharedPref appSharedPref;
  private ApiInterface apiService;

  @Override protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    textInfoCaption = (TextView) findViewById(R.id.text_caption);
    progressBar = (ProgressBar) findViewById(R.id.progress_bar);
    RecyclerView listMovies = (RecyclerView) findViewById(R.id.grid_poster_movies);
    swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);

    appSharedPref = new AppSharedPref(this);
    //get default state
    isCurrentPopular = appSharedPref.getIsPopularState();
    //set toolbar title
    setToolbarTitle();

    moviesAdapter =
        new MoviesAdapter(moviesList, getApplicationContext(), new MoviesAdapter.MoviesAdapterOnClickHandler() {
          @Override public void onClick(String data) {
            Intent intent = new Intent(getApplicationContext(), DetailActivity.class);
            intent.putExtra(ConstantData.MOVIE_ID_KEY, data);
            startActivity(intent);
          }
        });
    //set list config
    listMovies.setHasFixedSize(true);
    listMovies.setLayoutManager(new GridLayoutManager(this, COLUMN_SIZE));
    listMovies.setAdapter(moviesAdapter);

    //set api caller
    apiService = ApiClient.getClient().create(ApiInterface.class);

    //get movies data
    getMoviesData(savedInstanceState);

    //set on refresh listener
    swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override public void onRefresh() {
        getMoviesData(null, false);
      }
    });
  }

  private void setToolbarTitle() {
    if (isCurrentPopular) {
      setTitle(getString(R.string.most_popular_movies_caption));
    } else {
      setTitle(getString(R.string.highest_rated_movies_caption));
    }
  }

  private void getMoviesData(Bundle savedInstanceState) {
    getMoviesData(savedInstanceState, true);
  }

  private void getMoviesData(Bundle savedInstanceState, Boolean enableLoadingIndicatorCenter) {

    //if there is connection then continue
    if (NetworkUtil.isOnline(this)) {
      hideTextCaption();

      //null and check saved instance state
      if (savedInstanceState == null || !savedInstanceState.containsKey(KEY_SAVED_INSTANCE_STATE)) {

        //if flag true then call popular movies, else of that then call top rated movies
        if (isCurrentPopular) {
          getPopularMovies(enableLoadingIndicatorCenter);
        } else {
          getTopRatedMovies(enableLoadingIndicatorCenter);
        }
      } else {
        //if instance not null and contain key bundle then add the data to list
        List<MovieResultsItem> parcelableData = savedInstanceState.getParcelableArrayList(KEY_SAVED_INSTANCE_STATE);
        //add data from parcelable
        assert parcelableData != null;
        moviesList.addAll(parcelableData);

        //if movielist null or size = 0 then show no data caption and clear list
        if (moviesList.size() == 0) {
          showNoDataCaption();
          clearListData();
        } else {
          //else of that refresh the adapter
          hideTextCaption();
          moviesAdapter.notifyDataSetChanged();
        }
      }
    } else {
      //if didn't have connection then show the UI configuration to show the error
      showNoConnectivityCaption();
      clearListData();
    }
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    //put list and cast to arraylist
    outState.putParcelableArrayList(KEY_SAVED_INSTANCE_STATE, (ArrayList<? extends Parcelable>) moviesList);
    //save current flag to shared preferences
    appSharedPref.setIsPopularState(isCurrentPopular);
    appSharedPref.commit();
    super.onSaveInstanceState(outState);
  }

  private void getTopRatedMovies(Boolean withLoading) {
    hideTextCaption();

    if (withLoading) {
      showProgressBar();
    }

    //call the api
    callMovies = apiService.getMovies(ConstantData.SORT_BY_TOP_RATED, BuildConfig.API_KEY);
    callMovies.clone().enqueue(new Callback<MovieResponse>() {
      @Override public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
        processSuccessData(response);
      }

      @Override public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
        processFailedData();
      }
    });
  }

  private void getPopularMovies(Boolean withLoading) {
    hideTextCaption();

    if (withLoading) {
      showProgressBar();
    }

    //call the api
    callMovies = apiService.getMovies(ConstantData.SORT_BY_POPULAR, BuildConfig.API_KEY);
    callMovies.clone().enqueue(new Callback<MovieResponse>() {
      @Override public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
        processSuccessData(response);
      }

      @Override public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
        processFailedData();
      }
    });
  }

  private void processSuccessData(Response<MovieResponse> response) {
    swipeContainer.setRefreshing(false);
    hideProgressBar();

    //check null and empty data
    //noinspection ConstantConditions
    if (response.body() == null || response.body().getResults().size() == 0) {
      showNoDataCaption();
    } else {
      hideTextCaption();
      moviesList.clear();
      //noinspection ConstantConditions
      moviesList.addAll(response.body().getResults());
    }

    moviesAdapter.notifyDataSetChanged();
  }

  private void processFailedData() {
    //show UI error state
    swipeContainer.setRefreshing(false);
    hideProgressBar();
    showFailedCaption();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.option_menu, menu);

    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    //options for switching to highest rated movies
    if (id == R.id.action_highest_rated) {
      setTitle(getString(R.string.highest_rated_movies_caption));
      isCurrentPopular = false;
      clearListData();
      getMoviesData(null);
      return true;
      //options for switching to popular movies
    } else if (id == R.id.action_most_popular) {
      isCurrentPopular = true;
      setTitle(getString(R.string.most_popular_movies_caption));
      clearListData();
      getMoviesData(null);
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  private void clearListData() {
    moviesList.clear();
    moviesAdapter.notifyDataSetChanged();
  }

  private void showProgressBar() {
    progressBar.setVisibility(View.VISIBLE);
  }

  private void hideProgressBar() {
    progressBar.setVisibility(View.GONE);
  }

  private void showNoConnectivityCaption() {
    textInfoCaption.setText(getString(R.string.no_connectivity_caption));
    textInfoCaption.setVisibility(View.VISIBLE);
  }

  private void showNoDataCaption() {
    textInfoCaption.setText(getString(R.string.no_data_caption));
    textInfoCaption.setVisibility(View.VISIBLE);
  }

  private void showFailedCaption() {
    textInfoCaption.setText(getString(R.string.failed_getting_data_caption));
    textInfoCaption.setVisibility(View.VISIBLE);
  }

  private void hideTextCaption() {
    textInfoCaption.setVisibility(View.GONE);
  }
}
