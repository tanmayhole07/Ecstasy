package com.example.socialm;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialm.adapters.AdapterComments;
import com.example.socialm.models.ModelComment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PostDetailsActivity extends AppCompatActivity {

    String hisUid, myUid, myEmail, myName, myDp, pImage,
    postId, pLikes, hisDp, hisName;

    boolean mProcessComment = false;
    boolean mProcessLike = false;

    ProgressDialog pd;

    //Views
    ImageView uPictureIv, pImageIv;
    TextView uNameTv, pTimeTv, pTitleTv, pDescriptionTv, pLikesTv, pCommentTv;
    ImageButton moreBtn;
    Button likeBtn, shareBtn;
    LinearLayout profileLayout;

    //add comment views
    EditText commentEt;
    ImageButton sendBtn;
    ImageView cAvatarIv;
    RecyclerView recyclerViewcomments;

    List<ModelComment> commentList;
    AdapterComments adapterComments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Post Detail");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        uPictureIv = findViewById(R.id.uPictureIv);
        pImageIv = findViewById(R.id.pImageIv);
        uNameTv = findViewById(R.id.uNameTv);
        pTimeTv = findViewById(R.id.pTimeTv);
        pTitleTv = findViewById(R.id.pTitleTv);
        pDescriptionTv = findViewById(R.id.pDescriptionTv);
        pLikesTv = findViewById(R.id.pLikesTv);
        pCommentTv = findViewById(R.id.pCommentsTv);
        moreBtn = findViewById(R.id.moreBtn);
        likeBtn = findViewById(R.id.likeBtn);
        shareBtn = findViewById(R.id.shareBtn);
        profileLayout = findViewById(R.id.profileLayout);
        recyclerViewcomments = findViewById(R.id.recyclerViewComments);

        commentEt = findViewById(R.id.commentEt);
        sendBtn = findViewById(R.id.sendBtn);
        cAvatarIv = findViewById(R.id.cAvatarIv);

        loadPostInfo();

        checkUserStatus();
        
        loadUserInfo();

        setLikes();

        actionBar.setSubtitle("SignedIn as: "+myEmail);
        
        loadComments();

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postComment();
            }
        });

        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                likePost();
            }
        });

        moreBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                showMoreOptions();
            }
        });

        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pTitle = pTitleTv.getText().toString().trim();
                String pDescription = pDescriptionTv.getText().toString().trim();

                BitmapDrawable bitmapDrawable = (BitmapDrawable) pImageIv.getDrawable();
                if (bitmapDrawable == null){
                    //post without image
                    shareTextOnly(pTitle, pDescription);
                }else {
                    // Post with image
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    shareImageAndIext(pTitle, pDescription, bitmap);
                }
            }
        });
    }

    private void addToHisNotifications(String hisUid, String pId, String notification){
        String timestamp = ""+System.currentTimeMillis();

        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("pId", pId);
        hashMap.put("timestamp", timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notification", notification);
        hashMap.put("sUid", myUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUid).child("Notification").child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private void shareImageAndIext(String pTitle, String pDescription, Bitmap bitmap) {
        String shareBody = pTitle + "\n" + pDescription;
        Uri uri = saveImageToShare(bitmap);

        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sIntent.putExtra(Intent.EXTRA_TEXT,shareBody);
        sIntent.putExtra(Intent.EXTRA_SUBJECT,"Subject Here");
        sIntent.setType("image/png");
        startActivity(Intent.createChooser(sIntent,"Share Via"));
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(getCacheDir(), "images");
        Uri uri = null;
        try{
            imageFolder.mkdirs();
            File file = new File(imageFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this, "com.example.socialm.fileprovider", file);

        } catch (Exception e) {
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }

    private void shareTextOnly(String pTitle, String pDescription) {
        String shareBody = pTitle +"\n"+ pDescription;

        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.setType("text/plain");
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sIntent,"Share Via"));
    }

    private void loadComments() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerViewcomments.setLayoutManager(layoutManager);
        commentList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                commentList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    ModelComment modelComment = ds.getValue(ModelComment.class);
                    commentList.add(modelComment);

                    adapterComments = new AdapterComments(getApplicationContext(), commentList, myUid, postId);
                    recyclerViewcomments.setAdapter(adapterComments);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void showMoreOptions() {
        PopupMenu popupMenu = new PopupMenu(this, moreBtn, Gravity.END);

        if (hisUid.equals(myUid)){
            popupMenu.getMenu().add(Menu.NONE,0,0,"Delete");
            popupMenu.getMenu().add(Menu.NONE,1,0,"Edit Post");
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == 0){
                    beginDelete();
                }
                else if (id == 1){
                    Intent intent = new Intent(PostDetailsActivity.this, AddPostActivity.class);
                    intent.putExtra("key","editPost");
                    intent.putExtra("editPostId",postId);
                    startActivity(intent);
                }

                return false;
            }
        });
        popupMenu.show();
    }

    private void beginDelete() {
        if (pImage.equals("noImage")){
            deleteWithoutImage();
        }else{
            deleteWithImage();
        }
    }

    private void deleteWithImage() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting");

        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
                fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()){
                            ds.getRef().removeValue();
                            Toast.makeText(PostDetailsActivity.this, "Deleted Message Successfully", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(PostDetailsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteWithoutImage() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting");

        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    ds.getRef().removeValue();
                    Toast.makeText(PostDetailsActivity.this, "Deleted Message Successfully", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setLikes() {
        DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");

        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(postId).hasChild(myUid)){
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked,0,0,0);
                    likeBtn.setText("Liked");
                }else {
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black,0,0,0);
                    likeBtn.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void likePost() {

        mProcessLike = true;

        DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mProcessLike){
                    if (dataSnapshot.child(postId).hasChild(myUid)){
                        postsRef.child(postId).child("pLikes").setValue(""+(Integer.parseInt(pLikes)-1));
                        likesRef.child(postId).child(myUid).removeValue();
                        mProcessLike = false;

                    }else {
                        postsRef.child(postId).child("pLikes").setValue(""+(Integer.parseInt(pLikes)+1));
                        likesRef.child(postId).child(myUid).setValue("Liked");
                        mProcessLike = false;

                        addToHisNotifications(""+hisUid, ""+postId, " Liked Your post");

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void postComment() {
        pd = new ProgressDialog(this);
        pd.setMessage("Adding comment...");

        String comment = commentEt.getText().toString().trim();

        if (TextUtils.isEmpty(comment)){
            Toast.makeText(this, "Comment is Empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = String.valueOf(System.currentTimeMillis());

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");

        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("cId", timeStamp);
        hashMap.put("comment", comment);
        hashMap.put("timestamp", timeStamp);
        hashMap.put("uid", myUid);
        hashMap.put("uEmail", myEmail);
        hashMap.put("uDp", myDp);
        hashMap.put("uName", myName);

        ref.child(timeStamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        pd.dismiss();
                        Toast.makeText(PostDetailsActivity.this, "Comment Added", Toast.LENGTH_SHORT).show();
                        commentEt.setText("");
                        updateCommentCount();

                        addToHisNotifications(""+hisUid, ""+postId, " Commented on your post");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        Toast.makeText(PostDetailsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void updateCommentCount() {
        mProcessComment = true;
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mProcessComment){
                    String comments = ""+dataSnapshot.child("pComments").getValue();
                    int newCommentsVal = Integer.parseInt(comments)+1;
                    ref.child("pComments").setValue(""+newCommentsVal);
                    mProcessComment = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadUserInfo() {

        Query myRef = FirebaseDatabase.getInstance().getReference("Users");
        myRef.orderByChild("uid").equalTo(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    myName = ""+ds.child("name").getValue();
                    myDp = ""+ds.child("image").getValue();

                    try {
                        Picasso.get().load(myDp).placeholder(R.drawable.ic_default_img).into(cAvatarIv);
                    }catch (Exception e){
                        Picasso.get().load(R.drawable.ic_default_img).into(cAvatarIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void loadPostInfo() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = ref.orderByChild("pId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    String pTitle = ""+ds.child("pTitle").getValue();
                    String pDescr = ""+ds.child("pDescr").getValue();
                    pLikes = ""+ds.child("pLikes").getValue();
                    String pTimestamp = ""+ds.child("pTime").getValue();
                    pImage = ""+ds.child("pImage").getValue();
                    hisDp = ""+ds.child("uDp").getValue();
                    hisUid = ""+ds.child("uid").getValue();
                    String uEmail = ""+ds.child("uEmail").getValue();
                    hisName = ""+ds.child("uName").getValue();
                    String commentCount = ""+ds.child("pComments").getValue();


                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.setTimeInMillis(Long.parseLong(pTimestamp));
                    String pTime = DateFormat.format("dd/MM/yyyy hh:mm:aa", calendar).toString();

                    pTitleTv.setText(pTitle);
                    pDescriptionTv.setText(pDescr);
                    pLikesTv.setText(pLikes + "Likes");
                    pTimeTv.setText(pTime);
                    pCommentTv.setText(commentCount + "Comments");

                    uNameTv.setText(hisName);

                    if (pImage.equals("noImage")){
                        pImageIv.setVisibility(View.GONE);
                    }else {
                        pImageIv.setVisibility(View.VISIBLE);
                        try{
                            Picasso.get().load(pImage).into(pImageIv);
                        }catch (Exception e){
                        }
                    }

                    try {
                        Picasso.get().load(hisDp).placeholder(R.drawable.ic_default_img).into(uPictureIv);
                    }catch (Exception e){
                        Picasso.get().load(R.drawable.ic_default_img).into(uPictureIv);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void checkUserStatus(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            myEmail = user.getEmail();
            myUid = user.getUid();
        }else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}