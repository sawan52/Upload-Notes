package com.sawankumarsingh.uploadnotes;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final int READ_WRITE_PERMISSION = 9;
    private static final int FILE_SELECTION_CONFIRMED = 86;

    private String subjects[] = {"Computer Graphics", "Compiler Design", "Design and Development of Applications", "Computer Network", "Sociology", "Industrial Management"};

    private Button selectFile, uploadFile;
    private TextView notification;
    private EditText savedFileName;
    private Uri pdfUri;
    private Spinner spinner;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private FirebaseStorage storage;

    private String downloadUrl;
    private int subjectPosition = 0;
    private FirebaseUser currentUser;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        progressDialog = new ProgressDialog(this);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        storage = FirebaseStorage.getInstance();

        spinner = findViewById(R.id.spinner);
        selectFile = findViewById(R.id.button_select_file);
        uploadFile = findViewById(R.id.button_upload);

        notification = findViewById(R.id.textView_notification);
        savedFileName = findViewById(R.id.editText_file_name);

        // Create an ArrayAdapter Instance having Subjects list
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, subjects);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);

        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    selectPdf();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_WRITE_PERMISSION);
                }

            }
        });

        uploadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (pdfUri != null) { // the user has selected a file...
                    uploadFile(pdfUri);
                    savedFileName.setText("");

                } else {
                    Toast.makeText(MainActivity.this, "Select a File first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                Toast.makeText(MainActivity.this, subjects[position], Toast.LENGTH_SHORT).show();
                subjectPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void uploadFile(Uri pdfUri) {

        final String fileName = savedFileName.getText().toString();

        if (TextUtils.isEmpty(fileName)){

            databaseReference.child("Uploaded Files").child(subjects[subjectPosition]).child("fileExist").setValue("false");
            Toast.makeText(this, "Please enter a File name before Uploading", Toast.LENGTH_SHORT).show();

        } else {

            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading File...");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            String fileNameWithPdf = fileName + ".pdf";
            final StorageReference storageReference = storage.getReference().child("Uploaded Files").child(subjects[subjectPosition]).child(fileNameWithPdf);

            final UploadTask uploadTask = storageReference.putFile(pdfUri);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Toast.makeText(MainActivity.this, "File uploaded successfully...", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    databaseReference.child("Uploaded Files").child(subjects[subjectPosition]).child("fileExist").setValue("true");

                    Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }

                            downloadUrl = storageReference.getDownloadUrl().toString();
                            return storageReference.getDownloadUrl();

                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {

                            if (task.isSuccessful()) {

                                downloadUrl = task.getResult().toString();

                                databaseReference.child("Uploaded Files").child(subjects[subjectPosition]).child("All Units").child(fileName).setValue(downloadUrl)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if (task.isSuccessful()) {
                                            Toast.makeText(MainActivity.this, "File download url saved successfully.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Error: " + task.getException(), Toast.LENGTH_SHORT).show();

                                        }
                                    }
                                });
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "File not Uploaded...", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                    int progress = (int) (100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    progressDialog.setMessage(progress + "% Uploaded...");
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == READ_WRITE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectPdf();
        } else {
            Toast.makeText(MainActivity.this, "Please provide permission first", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectPdf() {
        // to offer a user to select a file using file manager
        // we will be using an Intent
        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT); // to fetch files...
        startActivityForResult(intent, FILE_SELECTION_CONFIRMED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // check whether a user has selected a file or not... Ex: pdf
        if (requestCode == 86 && resultCode == RESULT_OK && data != null) {
            pdfUri = data.getData(); // return the Uri for selected file...
            notification.setText(data.getData().getLastPathSegment());
        } else {
            Toast.makeText(MainActivity.this, "Please select a file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (currentUser == null) {
            senUserToLoginActivity();
        }
    }

    private void senUserToLoginActivity() {

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}

