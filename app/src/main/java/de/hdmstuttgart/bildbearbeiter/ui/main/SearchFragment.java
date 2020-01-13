package de.hdmstuttgart.bildbearbeiter.ui.main;

import androidx.lifecycle.ViewModelProviders;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hdmstuttgart.bildbearbeiter.UnsplashAPI;
import de.hdmstuttgart.bildbearbeiter.SearchResponseResult;
import de.hdmstuttgart.bildbearbeiter.R;
import de.hdmstuttgart.bildbearbeiter.utilities.UIUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import de.hdmstuttgart.bildbearbeiter.utilities.Constants;
import de.hdmstuttgart.bildbearbeiter.utilities.FileIndexer;
import de.hdmstuttgart.bildbearbeiter.utilities.ImageFileHandler;

public class SearchFragment extends Fragment {

    private SearchViewModel mViewModel;
    private Button searchButton;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private ImageAdapter searchAdapter;
    private List<SearchResponseResult.Photo> searchResponseResultList;
    private List<Bitmap> bitmapList;

    private EditText editText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        bitmapList = new ArrayList<>();

        //inflate view to work with it
        final View view = inflater.inflate(R.layout.search_fragment, container, false);
        //setting onClick on SearchButton
        searchButton = view.findViewById(R.id.buttonSearchPictures);
        searchButton.setOnClickListener(v -> {

            UIUtil.hideKeyboard(getActivity());

            bitmapList.clear();
            view.findViewById(R.id.progress_search).setVisibility(View.VISIBLE);
            searchPicturesOnline();
        });

        //assign recycle
        recyclerView = view.findViewById(R.id.recyclerSearch);
        linearLayoutManager = new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);

        //adapter
        searchAdapter = new ImageAdapter(bitmapList);
        recyclerView.setAdapter(searchAdapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(SearchViewModel.class);

        // TODO: Use the ViewModel
    }

    private void searchPicturesOnline() {

        editText = Objects.requireNonNull(getActivity()).findViewById(R.id.editTextSearchPictures);

        if (editText.getText().toString().equals("")) {
            String snackBarString;
            snackBarString = "Please enter something in the search field";
            showSnackbar(snackBarString);

            Objects.requireNonNull(getActivity()).findViewById(R.id.progress_search).setVisibility(View.GONE);

            return;
        }

        searchResponseResultList = new ArrayList<>();

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.UNSPLASH_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        UnsplashAPI unsplashAPI = retrofit.create(UnsplashAPI.class);

        //get Search String from User
        String query = editText.getText().toString();
        editText.setText("");


        Call<SearchResponseResult> call = unsplashAPI.getSearchResults(query, Constants.UNSPLASH_PAGE, Constants.UNSPLASH_PER_PAGE, Constants.UNSPLASH_ACCESS_TOKEN);
        call.enqueue(new Callback<SearchResponseResult>() {
            @Override
            public void onResponse(Call<SearchResponseResult> call, Response<SearchResponseResult> response) {
                //get urls
                if (response.isSuccessful()) {
                    searchResponseResultList = response.body().getResults();
                    if (checkIfResponseIsEmpty(searchResponseResultList, query)) {
                        hideProgressCircle();
                        return;
                    }
                } else {
                    //escape if response is invalid
                    return;
                }
                handleResponse();
            }

            @Override
            public void onFailure(Call<SearchResponseResult> call, Throwable t) {
                Objects.requireNonNull(getActivity()).findViewById(R.id.progress_search).setVisibility(View.GONE);
            }
        });
    }

    private void showSnackbar(String snackBarString) {
        Snackbar.make(getView(), snackBarString, Snackbar.LENGTH_SHORT).show();
    }

    private boolean checkIfResponseIsEmpty(List<SearchResponseResult.Photo> searchResponseResultList, String query) {
        if (searchResponseResultList.size() < 1) {
            String snackBarString;
            snackBarString = "No results were found for \""  + query + "\". Try again!";
            showSnackbar(snackBarString);
            return true;
        }

        return false;
    }

    private void handleResponse() {
        //download all images in regular size
        new DownloadFilesTask().execute();
    }

    private class DownloadFilesTask extends AsyncTask<URL, Integer, Boolean> {
        ImageFileHandler imageFileHandler;
        Bitmap downloadedImage;
        FileIndexer fileIndexer;

        protected Boolean doInBackground(URL... urls) {
            //used for saving images locally
            imageFileHandler = new ImageFileHandler(getContext().getFilesDir(), ImageFileHandler.IMAGE_DIR_LIB);
            fileIndexer = new FileIndexer();
            //getting urls
            searchResponseResultList.forEach(image -> {
                try {
                    URL url = new URL(image.getUrls().getSmall());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    downloadedImage = BitmapFactory.decodeStream(input);
                } catch (IOException e) {
                    // Log exception
                    String snackBarString;
                    showSnackbar("Failed to establish a connection to the Internet, Please try again!");
                    return;

                }
                publishProgress();

            });
            return true;
        }

        protected void onProgressUpdate(Integer... progress) {
            bitmapList.add(downloadedImage);
            searchAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            hideProgressCircle();
        }
    }

    private void hideProgressCircle() {
        getView().findViewById(R.id.progress_search).setVisibility(View.GONE);

    }
}

