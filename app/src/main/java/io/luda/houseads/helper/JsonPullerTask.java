/*
 * Created by Darshan Pandya.
 * @itznotabug
 * Copyright (c) 2018.
 */

package io.luda.houseads.helper;

import android.os.AsyncTask;
import android.util.Log;

public class JsonPullerTask extends AsyncTask<String, String, String> {
    private String jsonUrl;
    private JsonPullerListener listener;

    public JsonPullerTask(String url, JsonPullerListener jsonPullerListener) {
        this.jsonUrl = url;
        this.listener = jsonPullerListener;
    }

    @Override
    protected String doInBackground(String... p1) {
        return HouseAdsHelper.parseJsonObject(jsonUrl);
    }

    @Override
    protected void onPostExecute(String result) {
        listener.onPostExecute(result);
        Log.d("Response", result);
    }

    public interface JsonPullerListener{
        void onPostExecute(String result);
    }
}
