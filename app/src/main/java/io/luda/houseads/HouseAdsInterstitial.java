/*
 * Created by Darshan Pandya.
 * @itznotabug
 * Copyright (c) 2018.
 */

package io.luda.houseads;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.luda.houseads.helper.JsonPullerTask;
import io.luda.houseads.listener.AdListener;
import io.luda.houseads.modal.InterstitialModal;


public class HouseAdsInterstitial {
    private final FragmentActivity mContext;
    private static AdListener mAdListener;
    private ArrayList<InterstitialModal> modalArrayList;
    private InterstitialModal modal;

    private View container;

    public String getUrl() {
        return url;
    }

    private String url;
    private int lastLoaded = 0;

    private static boolean isAdLoaded = false;
    private static Bitmap bitmap;
    private static String packageName;

    public HouseAdsInterstitial(FragmentActivity context, String url, View container) {
        this.mContext = context;
        this.url = url;
        this.container = container;
    }

    public void setAdListener(AdListener adListener) {
        mAdListener = adListener;
    }

    public void loadAd() {
        if (url.trim().isEmpty()) throw new IllegalArgumentException("Url is Blank!");
        else {
            new JsonPullerTask(url, result -> {
                if (!result.trim().isEmpty()) setUp(result);
                else {
                    if (mAdListener != null) mAdListener.onAdLoadFailed(new Exception("Null Response"));
                }
            }).execute();
        }
    }

    public boolean isAdLoaded() {
        return isAdLoaded;
    }

    public InterstitialModal getModal() {
        return modal;
    }


    private void setUp(String val) {
        modalArrayList = new ArrayList<>();
        String x = new String(new StringBuilder().append(val));

        try {
            JSONObject rootObject = new JSONObject(x);
            JSONArray array = rootObject.optJSONArray("apps");

            for (int object = 0; object < array.length(); object++) {
                JSONObject jsonObject = array.getJSONObject(object);

                if (jsonObject.optString("app_adType").equals("interstitial")) {
                    InterstitialModal interstitialModal = new InterstitialModal();
                    interstitialModal.setInterstitialImageUrl(jsonObject.optString("app_interstitial_url"));
                    interstitialModal.setPackageOrUrl(jsonObject.optString("app_uri"));
                    interstitialModal.setAdId(jsonObject.optString("ad_id"));
                    interstitialModal.setClientId(jsonObject.optString("client_id"));
                    interstitialModal.setDuration(jsonObject.optLong("duration"));
                    modalArrayList.add(interstitialModal);
                }
            }

        } catch (JSONException e) { e.printStackTrace(); }

        if (modalArrayList.size() > 0) {
            modal = modalArrayList.get(lastLoaded);
            if (lastLoaded == modalArrayList.size() - 1) lastLoaded = 0;
            else lastLoaded++;
            new Picasso.Builder(mContext)
                    //.downloader(new OkHttp3Downloader(mContext, Integer.MAX_VALUE))
                    .build().load(modal.getInterstitialImageUrl()).into(new com.squareup.picasso.Target() {
                @Override
                public void onBitmapLoaded(Bitmap resource, Picasso.LoadedFrom from) {
                    bitmap = resource;
                    if (mAdListener != null) mAdListener.onAdLoaded();
                    isAdLoaded = true;
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    if (mAdListener != null) mAdListener.onAdLoadFailed(e);
                    isAdLoaded = false;
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            });
            packageName = modal.getPackageOrUrl();
        }
    }

    public void show() {
        this.container.setVisibility(View.VISIBLE);
        InterstitialFragment fragment = new InterstitialFragment();

        Bundle bundle = new Bundle();
        bundle.putSerializable("ad_data", modal);
        fragment.setArguments(bundle);
        mContext.getSupportFragmentManager().beginTransaction().add(this.container.getId(),fragment).commit();

    }

    public static class InterstitialFragment extends Fragment {
        private InterstitialModal adData;

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (mAdListener != null) mAdListener.onAdShown();
            if (getArguments() != null) {
                adData = (InterstitialModal) getArguments().getSerializable("ad_data");
            }

        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View inflate = inflater.inflate(io.luda.R.layout.house_ads_interstitial_layout, container, false);
            ImageView imageView = inflate.findViewById(io.luda.R.id.image);
            ImageButton button = inflate.findViewById(io.luda.R.id.button_close);

            imageView.setImageBitmap(bitmap);


            button.setOnClickListener(view -> {
                closeAd();
            });
            return inflate;
        }

        private void closeAd() {
            getFragmentManager().beginTransaction().remove(this).commit();
            isAdLoaded = false;
            if (mAdListener != null) mAdListener.onAdClosed();
        }

        @Override
        public void onResume() {
            super.onResume();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {

                    new CountDownTimer(adData.duration, adData.duration) {

                        public void onTick(long millisUntilFinished) {

                        }

                        public void onFinish() {
                            closeAd();
                        }
                    }.start();
                }
            });
        }
    }


    public static class InterstitialActivity extends AppCompatActivity {

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (mAdListener != null) mAdListener.onAdShown();

            setContentView(io.luda.R.layout.house_ads_interstitial_layout);
            ImageView imageView = findViewById(io.luda.R.id.image);
            ImageButton button = findViewById(io.luda.R.id.button_close);

            imageView.setImageBitmap(bitmap);

            /*imageView.setOnClickListener(view -> {
                isAdLoaded = false;
                if (packageName.startsWith("http")) {
                    Intent val = new Intent(Intent.ACTION_VIEW, Uri.parse(packageName));
                    val.setPackage("com.android.chrome");
                    if (val.resolveActivity(getPackageManager()) != null) startActivity(val);
                    else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(packageName)));

                    if (mAdListener != null) mAdListener.onApplicationLeft();
                    finish();
                }
                else {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
                        if (mAdListener != null) mAdListener.onApplicationLeft();
                        finish();
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + packageName)));
                        if (mAdListener != null) mAdListener.onApplicationLeft();
                        finish();
                    }
                }
            });*/
            button.setOnClickListener(view -> {
                finish();
                isAdLoaded = false;
                if (mAdListener != null) mAdListener.onAdClosed();
            });

            overridePendingTransition(io.luda.R.anim.enter, io.luda.R.anim.exit);
        }

        @Override
        protected void onResume() {
            super.onResume();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    new CountDownTimer(10000, 1000) {

                        public void onTick(long millisUntilFinished) {
                            Log.i(HouseAdsInterstitial.class.getName(), "FECHANDO EM: "+ millisUntilFinished /1000);
                        }

                        public void onFinish() {
                            onBackPressed();
                        }
                    }.start();
                }
            });
        }

        @Override
        public void onBackPressed() {
            isAdLoaded = false;
            if (mAdListener != null) mAdListener.onAdClosed();
            finish();
        }
    }
}
