package io.luda.services;

import android.content.Context;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.luda.model.Content;

public interface InfotainmentProvider {
    CompletableFuture<List<Content>> load(Context ctx);

    void onPause();
}
