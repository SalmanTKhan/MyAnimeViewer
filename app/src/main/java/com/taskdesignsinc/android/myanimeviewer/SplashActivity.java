package com.taskdesignsinc.android.myanimeviewer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.util.BuildUtils;
import com.taskdesignsinc.android.myanimeviewer.util.ParseManager;
import com.taskdesignsinc.android.myanimeviewer.util.StorageUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildUtils.isMarshmallowOrLater()) {
            StorageUtils.requestStoragePermissions(this, new PermissionListener() {
                @Override
                public void onPermissionGranted(PermissionGrantedResponse response) {
                    StorageUtils.setStorageAllowed(true);
                    showMainActivity();
                }

                @Override
                public void onPermissionDenied(PermissionDeniedResponse response) {
                    Toast.makeText(SplashActivity.this, getString(R.string.app_name) + " will be unable to store any anime for offline viewing.", Toast.LENGTH_SHORT).show();
                    showMainActivity();
                }

                @Override
                public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                    token.continuePermissionRequest();
                }
            });
        } else
            showMainActivity();
    }

    private void showMainActivity() {
        buildSearchDatabase();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }, getResources().getInteger(R.integer.splashDuration));
    }

    private void buildSearchDatabase() {
        long cachedAnimeCount = MAVApplication.getInstance().getRepository().getAnimeCount();
        long count = ParseManager.getInstance(this) != null ? ParseManager.getInstance(this).getAnimeCount() : 0;
        if (count == 0)
            return;
        if (cachedAnimeCount <= count) {
            HashMap<String, Anime> animeMap = MAVApplication.getInstance().getRepository().getAnimeMap();
            List<Anime> animeList = ParseManager.getInstance(this).getAnimeList();
            Collections.sort(animeList, Anime.Order.ByNameAZ);
            List<Anime> nonCachedAnime = new ArrayList<>();
            for (Anime anime : animeList) {
                if (!animeMap.containsKey(anime.getUrl())) {
                    nonCachedAnime.add(anime);
                }
            }
            MAVApplication.getInstance().getRepository().updateAnimeList(nonCachedAnime);
        }
    }
}
