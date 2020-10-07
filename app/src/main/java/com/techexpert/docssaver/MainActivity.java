package com.techexpert.docssaver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {


    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference rootRef;

    private String currentUserID;

    private CircleImageView circleImageView;

    private String checker = "", myUrl = "";

    private Uri fileUri;

    private ProgressDialog loadingBar;

    private StorageTask uploadTask;

    RecyclerView recyclerView;

    ProgressDialog progressDialog;

    final List<ImageUploadInfo> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();

        if(mAuth.getCurrentUser()!=null)
        {
            currentUser = mAuth.getCurrentUser();
            currentUserID = mAuth.getCurrentUser().getUid();
        }
        else
        {
            Intent loginIntent=new Intent(MainActivity.this,Login_Activity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(loginIntent);
            finish();
        }

        Initialization();
    }


    private void Initialization()
    {
        circleImageView = findViewById(R.id.buttons);

        loadingBar = new ProgressDialog(this);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        circleImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {

                AddFiles();

            }
        });
    }

    private void AddFiles()
    {

        CharSequence[] options = new CharSequence[]
                {
                        "Images",
                        "PDF File",
                        "MS Word File"
                };

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Select File");
        builder.setItems(options, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                if (i == 0)
                {
                    checker = "image";

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent, "Select Image"), 5);
                }
                if (i == 1)
                {
                    checker = "pdf";

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/pdf");
                    startActivityForResult(Intent.createChooser(intent, "Select PDF File"), 5);

                }
                if (i == 2)
                {
                    checker = "docx";

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/msword");
                    startActivityForResult(Intent.createChooser(intent, "Select Ms Word File"), 5);
                }
            }
        });
        builder.show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 5 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            loadingBar.setTitle("Sending File");
            loadingBar.setMessage("Please wait, We are sending your file...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();


            //store the file that is selected by user
            fileUri = data.getData();

            //if user has not selected the image type
            if (!checker.equals("image"))
            {

                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Document Files");

                DatabaseReference userKeyRef = rootRef.child("Document").child(currentUserID).push();

                final String PushId = userKeyRef.getKey();

                final String Ref = "Document/"+currentUserID;

                final StorageReference Filepath = storageReference.child(PushId + "." + checker);

                Filepath.putFile(fileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {

                            FirebaseStorage storageReference = FirebaseStorage.getInstance();

                            StorageReference storageRef = storageReference.getReference().child("Document Files");

                            storageRef.child(PushId + "." + checker).getDownloadUrl()
                                    .addOnCompleteListener(new OnCompleteListener<Uri>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Uri> task) {
                                            String PdfUrl = task.getResult().toString();

                                            Map messageFile = new HashMap();
                                            messageFile.put("location", PdfUrl); //link to file present in Storage
                                            messageFile.put("type", checker);

                                            Map BodyDetails = new HashMap();
                                            BodyDetails.put(Ref + "/" + currentUserID +"/" + PushId, messageFile);

                                            rootRef.updateChildren(BodyDetails);

                                            loadingBar.dismiss();

                                        }
                                    });

                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingBar.dismiss();
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                        double p = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        loadingBar.setMessage((int) p + "%  Uploading....");
                    }
                });

            }


            else if(checker.equals("image")) {
                //it will create a folder in storage of firebase
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");

                DatabaseReference userKeyRef = rootRef.child("Document").child(currentUserID).push();

                final String PushId = userKeyRef.getKey();

                final String Ref = "Document/"+currentUserID;

                //it pointing to folder of images in database and we are giving unique key for each image
                final StorageReference Filepath = storageReference.child(PushId + ".jpg");

                //putting file inside the storage
                //fileUri contains image
                uploadTask = Filepath.putFile(fileUri);

                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return Filepath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUrl = task.getResult();

                            //myUrl will contain link of the image
                            myUrl = downloadUrl.toString();


                            Map Body = new HashMap();
                            Body.put("location", myUrl);
                            Body.put("type", checker);

                            Map BodyDetails = new HashMap();
                            BodyDetails.put(Ref + "/" + PushId, Body);


                            rootRef.updateChildren(BodyDetails);

                            loadingBar.dismiss();

                        }
                        else
                        {
                            loadingBar.dismiss();
                            Toast.makeText(MainActivity.this, "Please select any file...", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        }
    }


    @Override
    protected void onStart()
    {
        super.onStart();

        if(currentUser==null)
        {
            SendUserToLoginActivity();
        }
        else
        {
            VerifyUserExistence();
        }

//        rootRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);
//
//
//        FirebaseRecyclerOptions<ImageUploadInfo> options =
//                new FirebaseRecyclerOptions.Builder<ImageUploadInfo>()
//                        .setQuery(rootRef, ImageUploadInfo.class)
//                        .build();
//
//        FirebaseRecyclerAdapter<ImageUploadInfo, ViewHolder> adapter=
//                new FirebaseRecyclerAdapter<ImageUploadInfo, ViewHolder>(options)
//                {
//                    @Override
//                    protected void onBindViewHolder(@NonNull final ViewHolder holder, final int position, @NonNull final ImageUploadInfo model)
//                    {
//                            //setting the data to the recyclerview list
//
//                            String keyId = this.getRef(position).getKey();
//
//                            Picasso.get().load(model.getImageUrl()).into(holder.img);
//
//
////                            //if you click on any list item then you will be go on other activity
////                            holder.itemView.setOnClickListener(new View.OnClickListener()
////                            {
////                                @Override
////                                public void onClick(View v)
////                                {
////                                    String visitUserId = getRef(position).getKey();
////
////                                    //passing userId to other activity of the person which is clicked by us
////                                    Intent profileIntent = new Intent(FindFriendActivity.this,ProfileActivity.class);
////                                    profileIntent.putExtra("visitUserId",visitUserId);
////                                    startActivity(profileIntent);
////
////                                }
////                            });
//
//
//                    }
//
//                    @NonNull
//                    @Override
//                    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
//                    {
//                        //setting connection with the xml file
//                        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview, parent,false);
//                        ViewHolder viewHolder = new ViewHolder(view);
//                        return  viewHolder;
//                    }
//                };
//
//        recyclerView.setAdapter(adapter);
//        adapter.startListening();

    }

//    public static class ViewHolder extends RecyclerView.ViewHolder
//    {
//
//        ImageView img;
//
//        //constructor
//        public ViewHolder(@NonNull View itemView)
//        {
//            super(itemView);
//
//           img = itemView.findViewById(R.id.img1);
//
//        }
//    }


    private void VerifyUserExistence()
    {

        String currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if((dataSnapshot.child("name").exists()))
                {
                }
                else
                {
                    SendUserToSettingsActivity();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void SendUserToLoginActivity()
    {
        Intent loginIntent=new Intent(MainActivity.this,Login_Activity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }
    private void SendUserToSettingsActivity()
    {
        Intent loginIntent=new Intent(MainActivity.this,Settings_Activity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }


}