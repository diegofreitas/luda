package io.luda.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import io.luda.R;
import io.luda.model.UserProfile;
import io.luda.services.FirebaseUtil;

public class ProfileActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private Spinner spinner;

    private List<String> states = Arrays.asList("", "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
                                                "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
                                                "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    private UserProfile currentProfile = new UserProfile();
    private FirebaseFirestore fs;
    private FirebaseUtil firebaseUtil;
    private TextView txtEmail;
    private TextView txtName;
    private ImageView imvPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        spinner = findViewById(R.id.spState);
        txtEmail = findViewById(R.id.user_profile_email);
        txtName = findViewById(R.id.user_profile_name);
        imvPhoto = findViewById(R.id.user_profile_photo);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ProfileActivity.this,
                android.R.layout.simple_spinner_item, states);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        fs = FirebaseFirestore.getInstance();

    }

    @Override
    protected void onResume() {
        super.onResume();
        firebaseUtil = new FirebaseUtil();

        firebaseUtil.getUserProfile().thenAccept(new Consumer<UserProfile>() {
            @Override
            public void accept(UserProfile userProfile) {
                ProfileActivity.this.currentProfile = userProfile;
                if(userProfile != null) {
                    int indexState = states.indexOf(userProfile.state);
                    spinner.setSelection(indexState != -1 ? indexState: 0);

                    txtEmail.setText(userProfile.email);
                    txtName.setText(userProfile.displayName);
                    Picasso.get().load(userProfile.photoUrl).into(imvPhoto);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        firebaseUtil.removeRegistrations();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(position != 0 && (currentProfile != null && states.indexOf(currentProfile.state) != position )) {
            Task<Void> op = null;
            DocumentReference ref = fs.collection("profiles").document(FirebaseUtil.getCurrentUserId());
            if(currentProfile == null) {
                currentProfile = new UserProfile();
                currentProfile.state = states.get(position);
                op = ref.set(Collections.singletonMap("state", currentProfile.state));
            } else {
                currentProfile.state = states.get(position);
                op = ref.update(Collections.singletonMap("state", currentProfile.state));
            }
            op.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i(ProfileActivity.class.getName(), "Country State of profile saved");
                    Toast.makeText(ProfileActivity.this,"Cadastro atualizado", Toast.LENGTH_LONG).show();
                }
            });
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
