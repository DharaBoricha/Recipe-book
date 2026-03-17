package com.example.myrecipebook.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.example.myrecipebook.DataClass;
import com.example.myrecipebook.R;
import com.example.myrecipebook.models.DetailRecipeModel;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

//allows users to upload recipe details and images to a Firebase Realtime Database
public class UploadActivity extends AppCompatActivity {
    ImageView uploadImage;
    Button saveButton;
    EditText uploadName, uploadIngre;
    String imageURL;
    NumberPicker uploadTotalTime;
    Uri uri;
    private CheckBox breakfastCheckBox, lunchCheckBox, dinnerCheckBox, dessertCheckBox;
    private CheckBox veganCB, vegetarianCB, kosherCB, glutenCB, dairyCB;

    // Default image URL if no image is selected
    private static final String DEFAULT_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/recipe-7d56f.firebasestorage.app/o/recipe_images%2Fdefault_recipe.png?alt=media";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upload);

        uploadImage = findViewById(R.id.uploadImage);
        uploadName = findViewById(R.id.upload_name);
        uploadIngre = findViewById(R.id.upload_ingredients);
        uploadTotalTime = findViewById(R.id.uploadTotalTime);
        uploadTotalTime.setMinValue(0);
        uploadTotalTime.setMaxValue(120);
        saveButton = findViewById(R.id.saveButton);

        // Registers a photo picker activity launcher in single-select mode.
        ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), selectedUri -> {
                    // Callback is invoked after the user selects a media item or closes the
                    // photo picker.
                    if (selectedUri != null) {
                        Log.d("PhotoPicker", "Selected URI: " + selectedUri);
                        uri = selectedUri;
                        uploadImage.setImageURI(uri);
                    } else {
                        Log.d("PhotoPicker", "No media selected");
                    }
                });

        //Upload recipe image button
        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch the photo picker and let the user choose only images.
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            }
        });

        //Save data button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveData();
            }
        });
    }

    public void saveData() {
        List<String> category = new ArrayList<>();
        breakfastCheckBox = findViewById(R.id.breakfast_checkbox);
        lunchCheckBox = findViewById(R.id.lunch_checkbox);
        dinnerCheckBox = findViewById(R.id.dinner_checkbox);
        dessertCheckBox = findViewById(R.id.dessert_checkbox);
        if (breakfastCheckBox.isChecked()) category.add("breakfast");
        if (lunchCheckBox.isChecked()) category.add("lunch");
        if (dinnerCheckBox.isChecked()) category.add("dinner");
        if (dessertCheckBox.isChecked()) category.add("dessert");

        List<String> healthLabels = new ArrayList<>();
        veganCB = findViewById(R.id.vegan_checkbox);
        vegetarianCB = findViewById(R.id.vegetarian_checkbox);
        kosherCB = findViewById(R.id.kosher_checkbox);
        glutenCB = findViewById(R.id.gluten_checkbox);
        dairyCB = findViewById(R.id.dairy_checkbox);
        healthLabels.add("healthLabels");
        if (vegetarianCB.isChecked()) healthLabels.add("Vegetarian");
        if (veganCB.isChecked()) healthLabels.add("Vegan");
        if (kosherCB.isChecked()) healthLabels.add("Kosher");
        if (glutenCB.isChecked()) healthLabels.add("Gluten-Free");
        if (dairyCB.isChecked()) healthLabels.add("Dairy-Free");

        String name = uploadName.getText().toString().trim();
        String ingredients = uploadIngre.getText().toString().trim();
        int int_totalTime = uploadTotalTime.getValue();

        String totalTime = Integer.toString(int_totalTime) + " min";

        // Validation
        if (name.isEmpty()) {
            Toast.makeText(this, "Please set recipe name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ingredients.isEmpty()) {
            Toast.makeText(this, "Please set ingredients", Toast.LENGTH_SHORT).show();
            return;
        }
        if (int_totalTime == 0) {
            Toast.makeText(this, "Please set total time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (category.isEmpty()) {
            Toast.makeText(this, "Please choose at least 1 category", Toast.LENGTH_SHORT).show();
            return;
        }

        //retrieves the current user's ID using Firebase Authentication
        String curUser = FirebaseAuth.getInstance().getUid();
        //creates a DetailRecipeModel object with including the current user's ID and recipe details
        DetailRecipeModel detailRecipeModel = new DetailRecipeModel(curUser, name, category, healthLabels, ingredients, "", totalTime, DEFAULT_IMAGE_URL);

        if (uri != null) {
            // Initialize Firebase Storage with full path to avoid location errors
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReference();
            StorageReference imageRef = storageReference.child("recipe_images/" + name + "_" + System.currentTimeMillis() + ".jpg");

            UploadTask uploadTask = imageRef.putFile(uri);

            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        if (downloadUri != null) {
                            detailRecipeModel.imageUrl = downloadUri.toString();
                            uploadRecipeToDatabase(detailRecipeModel);
                        }
                    } else {
                        String errorMessage = "Failed to upload image";
                        if (task.getException() != null) {
                            errorMessage += ": " + task.getException().getMessage();
                            Log.e("UploadActivity", "Upload failed", task.getException());
                        }
                        Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                        // Still try to upload recipe with default image if upload fails
                        uploadRecipeToDatabase(detailRecipeModel);
                    }
                }
            });
        } else {
            // No image selected, use default image URL and upload directly
            uploadRecipeToDatabase(detailRecipeModel);
        }
    }

    private void uploadRecipeToDatabase(DetailRecipeModel detailRecipeModel) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference recipeRef = database.getReference("Recipes");

        recipeRef.child(detailRecipeModel.getName()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    recipeRef.child(detailRecipeModel.getName()).setValue(detailRecipeModel);
                    Toast.makeText(getApplicationContext(), "Recipe uploaded successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "Recipe name is already taken", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("UploadActivity", "Database error: " + error.getMessage());
                Toast.makeText(UploadActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
