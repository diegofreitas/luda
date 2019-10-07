package io.luda.services;



import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import io.luda.model.UserProfile;

public class FirebaseUtil {

    private static Gson gson = new Gson();
    private List<ListenerRegistration> regtistrations = new ArrayList<>();

    public static FirebaseFirestore getBaseRef() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
        return db;
    }

    public static String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        return null;
    }

    public void removeRegistrations() {
        for(ListenerRegistration lr: regtistrations) {
            lr.remove();
        }
    }

    public CompletableFuture<UserProfile> getUserProfile() {
        CompletableFuture cf = new CompletableFuture<UserProfile>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        regtistrations.add(getBaseRef().collection("profiles").document(user.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                JsonElement jsonElement = gson.toJsonTree(documentSnapshot.getData());
                UserProfile pojo = gson.fromJson(jsonElement, UserProfile.class);
                if(pojo == null) {
                    pojo = new UserProfile();
                }
                pojo.displayName = user.getDisplayName();
                pojo.email = user.getEmail();
                pojo.photoUrl = user.getPhotoUrl();
                cf.complete(pojo);
            }
        }));

        return cf;
    }

}