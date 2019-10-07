package io.luda.services;


import android.content.Context;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.prof.rssparser.Article;
import com.prof.rssparser.OnTaskCompleted;
import com.prof.rssparser.engine.XMLParser;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.luda.model.Content;
import io.luda.model.UserProfile;

public class NewsProvider implements InfotainmentProvider {

    private final CompletableFuture<UserProfile> userProfile;
    private final FirebaseRemoteConfig mFirebaseRemoteConfig;
    private ArrayList<Content> contents;
    private String urlString;

    private FirebaseUtil firebaseUtil = new FirebaseUtil();

    public NewsProvider() {
         userProfile = firebaseUtil.getUserProfile();
         mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    }

    @Override
    public CompletableFuture<List<Content>> load(Context context) {
        CompletableFuture<List<Content>> future = new CompletableFuture<>();
        userProfile.thenAccept(new Consumer<UserProfile>() {
            @Override
            public void accept(UserProfile userProfile) {
                urlString = mFirebaseRemoteConfig.getString("RSS_BR");
                if(userProfile.state != null && !"".equals(userProfile.state)){
                    urlString = mFirebaseRemoteConfig.getString("RSS_"+userProfile.state);
                }

                RssParser parser = new RssParser(context);
                parser.onFinish(new OnTaskCompleted() {
                    @Override
                    public void onTaskCompleted(@NotNull List<Article> list) {
                        contents = new ArrayList<>();
                        for (Article art: list) {
                            Log.i(NewsProvider.class.getName(), art.getTitle());

                            if(art.getTitle().contains("VÍDEOS:") || art.getTitle().contains("VÍDEO:")) {
                                continue;
                            }
                            int start = occurrenceIndex(art.getDescription(),'\n', 3);
                            String desciption = start == -1? art.getDescription(): art.getDescription().substring(start);

                            contents.add(new Content(art.getTitle(), desciption.replaceAll("\n", " "), art.getImage(),null, null,  art.getPubDate(), Content.Type.NEWS));
                        }
                        future.complete(contents);
                    }

                    @Override
                    public void onError(@NotNull Exception e) {
                        future.completeExceptionally(e);
                    }
                });
                parser.execute(urlString);
            }
        });

        return future;
    }

    @Override
    public void onPause() {
        firebaseUtil.removeRegistrations();
    }

    public static int occurrenceIndex(String string, char character, int occurrence) {
        int count = 0;
        int index = 0;
        while(true) {
            index = string.indexOf(character, index) + 1; // +1 is to continue search
            if (index == 0)                               //from next character after match
                break;
            count++;
            if(count == occurrence) {
                break;
            }
        }
        return index - 1;
    }


    static class RssParser {

        private OnTaskCompleted onComplete;
        private Context context;

        RssParser(Context context) {
            this.context = context;
        }

        public void onFinish(OnTaskCompleted onComplete) {
            this.onComplete = onComplete;
        }

        public void execute(String url) {
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    ExecutorService service = Executors.newFixedThreadPool(2);
                    Future<String> future = service.submit(new XMLFetcher(context, url));
                    try {
                        String rssFeed = future.get();
                        Future<List<Article>> f2 = service.submit(new XMLParser(rssFeed));
                        onComplete.onTaskCompleted(f2.get());
                    } catch ( Exception e) {
                        onComplete.onError(e);
                    } finally {
                        service.shutdown();
                    }
                }
            });
        }

    }


    static class XMLFetcher implements Callable<String> {

        private Context context;
        private String url;

        XMLFetcher(Context context, String url) {
            this.context = context;
            this.url = url;
        }

        @Override
        public String call() throws Exception {
            CompletableFuture<String> future = new CompletableFuture<>();
            RequestQueue queue = Volley.newRequestQueue(context);
            FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
            if(queue.getCache().get(url)!=null && !queue.getCache().get(url).isExpired()){
                //response exists
                String cachedResponse = new String(queue.getCache().get(url).data);
                future.complete(cachedResponse);
            }else{
                Request<?> stringRequest = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                future.complete(response);
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                future.completeExceptionally(error);
                            }
                        }){
                    @Override
                    protected Response<String> parseNetworkResponse(NetworkResponse response) {
                        Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
                        if (cacheEntry == null) {
                            cacheEntry = new Cache.Entry();
                        }
                        final long cacheHitButRefreshed = mFirebaseRemoteConfig.getLong("HTTP_CONTENT_CACHE_HIT_MIN") * 60 * 1000; // in 3 minutes cache will be hit, but also refreshed on background
                        final long cacheExpired = mFirebaseRemoteConfig.getLong("HTTP_CONTENT_CACHE_EXPIRE_HR") * 60 * 60 * 1000; // in 24 hours this cache entry expires completely
                        long now = System.currentTimeMillis();
                        final long softExpire = now + cacheHitButRefreshed;
                        final long ttl = now + cacheExpired;
                        cacheEntry.data = response.data;
                        cacheEntry.softTtl = softExpire;
                        cacheEntry.ttl = ttl;
                        String headerValue;
                        headerValue = response.headers.get("Date");
                        if (headerValue != null) {
                            cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
                        }
                        headerValue = response.headers.get("Last-Modified");
                        if (headerValue != null) {
                            cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);
                        }
                        cacheEntry.responseHeaders = response.headers;
                        return Response.success(new String(response.data), cacheEntry);
                    }
                }.setShouldCache(true);
                stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                        (int) mFirebaseRemoteConfig.getLong("HTTP_SOCKET_TIMEOUT"),
                        (int) mFirebaseRemoteConfig.getLong("HTTP_MAX_RETRIES"),
                        (int) mFirebaseRemoteConfig.getLong("HTTP_BACKOFF")));
                queue.add(stringRequest);
            }
            return future.get();
        }
    }
}
