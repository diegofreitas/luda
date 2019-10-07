package io.luda.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.common.eventbus.Subscribe;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.jeasy.states.api.Event;


import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.luda.FdActivity;
import io.luda.model.UserProfile;
import io.luda.services.FirebaseUtil;
import io.luda.services.InfotainmentSession;
import io.luda.services.InfotainmentStatesListener;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;


import io.luda.houseads.HouseAdsInterstitial;
import io.luda.houseads.listener.AdListener;
import io.luda.model.Content;
import io.luda.R;
import io.luda.ViewPagerAdapter;
import io.luda.services.InfotainmentProvider;
import io.luda.services.NewsProvider;


public class ContentActivity extends FdActivity implements InfotainmentStatesListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener  {

    private static final String TAG = "ContentActivity";

    private RequestQueue queue;

    private ViewPagerAdapter adapter;
    private ViewPager viewPager;
    private long lastDisplayedContent = 0;
    private List<Content> contents;
    private Timer timer;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private InfotainmentProvider infotainmentProvider;
    private View logoWait;
    private HouseAdsInterstitial interstitial;
    private InfotainmentSession session;
    FirebaseAnalytics mFirebaseAnalytics;
    private GoogleApiClient googleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private LocationRequest locationRequest;
    private Location location;
    private static final long UPDATE_INTERVAL = 10000, FASTEST_INTERVAL = 10000;
    private UserProfile userProfile;
    private View adContainer;

    public Prediction lastPrediction = new Prediction();

    public ContentActivity() {

    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_content);
        adContainer = findViewById(R.id.ad_fragment_container);
        super.onCreate(savedInstanceState);
        this.viewPager = findViewById(R.id.view_pager);
        this.logoWait = findViewById(R.id.logo_wait);

        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            public void onPageSelected(int position) {

            }
        });
        viewPager.setOffscreenPageLimit(3);
        Log.d(TAG, "View Adapter Done");

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();


        session = new InfotainmentSession(this);


        infotainmentProvider = new NewsProvider();

// we build google api client
        googleApiClient = new GoogleApiClient.Builder(this).
        addApi(LocationServices.API).
        addConnectionCallbacks(this).
        addOnConnectionFailedListener(this).build();
        Log.d(TAG, "Init Location API");


    }

    @Override
    protected void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
            Log.d(TAG, "Connect gclient api");
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        session.sendEvent(new InfotainmentSession.UserDetectedEvent());
        if (!checkPlayServices()) {
            Log.i(ContentActivity.TAG,"You need to install Google Play Services to use the App properly");
        }

        new FirebaseUtil().getUserProfile().thenAccept(new Consumer<UserProfile>() {




            @Override
            public void accept(UserProfile userProfile) {
                if(userProfile != null) {
                    ContentActivity.this.userProfile = userProfile;
                    Log.d(TAG, "User profile loaded");
                }
            }
        });
    }

    @Subscribe()
    public void onMessageEvent(Prediction event) {
        this.lastPrediction = event;
        if(event.state.equals(Prediction.PredictionState.FACE_DETECTED)) {
            session.sendEvent(new InfotainmentSession.UserDetectedEvent());
        }
    }

    private void startTransitionEvaluation() {
        timer = new Timer(); // This will create a new Thread
        timer.schedule(new TimerTask() { // task to be scheduled

            @Override
            public void run() {
                if(getCurrentContent().duration + lastDisplayedContent <  System.currentTimeMillis()
                        && session.machine.getCurrentState().equals(InfotainmentSession.CONSUME)) {
                    lastDisplayedContent = System.currentTimeMillis();

                    runOnUiThread(() -> {
                        if(shouldShowAd()){
                            Log.d(TAG, "Should show ad");
                            if( interstitial.isAdLoaded()) {
                                interstitial.show();
                                Log.d(TAG, "Request interstitial to show");
                                session.sendEvent(new InfotainmentSession.AdsLoadedEvent());
                            } else {
                                Log.d(TAG, "interstitial not ready. Loading Again");
                                interstitial.loadAd();
                            }

                        } else {
                            nextContent();
                        }


                    });
                } else if (!session.machine.getCurrentState().equals(InfotainmentSession.CONSUME)){
                    lastDisplayedContent = System.currentTimeMillis();//adiar até que esteja no estado correto
                    Log.i(TAG,"content not changed");
                }
            }

        },mFirebaseRemoteConfig.getLong("evaluate_content_ms"), mFirebaseRemoteConfig.getLong("evaluate_content_ms"));
    }

    private void nextContent() {
        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1 == adapter.getCount()? 0: viewPager.getCurrentItem() + 1 , true);
        ContentActivity.this.adapter.notifyDataSetChanged();
        Log.d(TAG, "Content changed");
    }

    private boolean shouldShowAd() {
        return false;
        //return (int)(Math.random() * mFirebaseRemoteConfig.getLong("show_ad_rate")) == 0;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(timer != null) {
            timer.cancel();
        }
        infotainmentProvider.onPause();

        if (googleApiClient != null  &&  googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
            Log.d(TAG, " stop location updates");
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                finish();
            }

            return false;
        }

        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Permissions ok, we get last location
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location != null) {
            Log.i(TAG,"Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude());
        }

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        if (location != null) {
            Log.d(TAG, "Latitude : " + location.getLatitude() + " Longitude : " + location.getLongitude());
        }
    }

    private Content getCurrentContent() {
        return adapter.getContents().get(viewPager.getCurrentItem());
    }

    private void loadViewPagerAdapter() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logoWait.setVisibility(View.GONE);
                adapter.updateFeedContent(contents);
                adapter.notifyDataSetChanged();
                viewPager.setCurrentItem(0);
                viewPager.setVisibility(View.VISIBLE);
            }
        });

    }


    @Override
    public void onStandBy(Event event) {
       logoWait.setVisibility(View.VISIBLE);
    }

    @Override
    public void onInitialize(Event event) {
        //carregar conteudo baseado nos dados contextuais do usuário
        CompletableFuture<List<Content>> result = infotainmentProvider.load(this);

        result.thenAccept(new Consumer<List<Content>>() {
            @Override
            public void accept(List<Content> o) {
                contents = o;
                loadViewPagerAdapter();
                session.sendEvent(new InfotainmentSession.ContentLoadedEvent());
                Log.d(TAG, "Content loaded form provider");

            }
        }).handle(new BiFunction<Void, Throwable, Object>() {
            @Override
            public Object apply(Void aVoid, Throwable throwable) {
                Crashlytics.logException(throwable);
                return null;
            }
        });
    }

    @Override
    public void onConsume(Event source) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Start show content to passanger");
                startTransitionEvaluation();
            }
        });

            interstitial = new HouseAdsInterstitial(ContentActivity.this, "https://us-central1-luda-ec6e9.cloudfunctions.net/loadads", findViewById(R.id.ad_fragment_container));
            interstitial.setAdListener(new AdListener() {
                @Override
                public void onAdLoadFailed(Exception e) {
                    Log.d(TAG, "AD load failed", e);
                }

                @Override
                public void onAdLoaded() {
                    Log.d(TAG, "AD loaded");
                }

                @Override
                public void onAdClosed() {
                    session.sendEvent(new InfotainmentSession.AdsClosedEvent());
                    Log.d(TAG, "Ad Closed");
                    runOnUiThread( () -> {
                        adContainer.setVisibility(View.GONE);
                        nextContent();
                    });

                }

                @Override
                public void onAdShown() {
                    if(interstitial.getModal() == null) {
                        Crashlytics.log("Intertistial modal data is null");
                        return;
                    }
                    adContainer.setVisibility(View.VISIBLE);
                    Log.d(TAG, "AD shown");
                    Bundle bundle = new Bundle();

                    double latitude= 0;
                    double longitude = 0;


                    if(location != null){
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    }
                    bundle.putString("ad_id", interstitial.getModal().getAdId() );
                    bundle.putString("client_id", interstitial.getModal().getClientId() );
                    bundle.putString("prediction_state", lastPrediction.state.toString());
                    bundle.putString("age", lastPrediction.age );
                    bundle.putString("gender", lastPrediction.gender );
                    bundle.putString("prediction_timestamp", Long.toString(lastPrediction.timestamp));
                    bundle.putString("latitude", Double.toString(latitude ));
                    bundle.putString("longitude", Double.toString(longitude) );
                    bundle.putString("driver_email", userProfile.email);
                    bundle.putString("driver_uid", userProfile.uid);
                    mFirebaseAnalytics.logEvent("IMPRESSION", bundle);
                }

                @Override
                public void onApplicationLeft() {

                }
            });


        interstitial.loadAd();
    }

    @Override
    public void onAdvertisement(Event source) {

    }
}
