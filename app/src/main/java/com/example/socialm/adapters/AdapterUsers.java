package com.example.socialm.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialm.ChatActivity;
import com.example.socialm.R;
import com.example.socialm.ThereProfileActivity;
import com.example.socialm.models.ModelUsers;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

public class AdapterUsers extends RecyclerView.Adapter<AdapterUsers.MyHolder>{

    Context context;
    List<ModelUsers> usersList;

    FirebaseAuth firebaseAuth;
    String myUid;

    public AdapterUsers(Context context, List<ModelUsers> usersList) {
        this.context = context;
        this.usersList = usersList;

        firebaseAuth = FirebaseAuth.getInstance();
        myUid = firebaseAuth.getUid();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_users, viewGroup, false);

        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder myholder, int i) {

        String hisUID = usersList.get(i).getUid();
        String userImage = usersList.get(i).getImage();
        String userName= usersList.get(i).getName();
        final String userEmail = usersList.get(i).getEmail();

        myholder.mNameTv.setText(userName);
        myholder.mEmailTv.setText(userEmail);
        try{
            Picasso.get().load(userImage).placeholder(R.drawable.ic_default_img).into(myholder.mAvtarIv);
        }catch (Exception e){

        }

        myholder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setItems(new String[]{"Profile", "Chat"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        if (which == 0){
                            Intent intent = new Intent(context, ThereProfileActivity.class);
                            intent.putExtra("uid",hisUID);
                            context.startActivity(intent);
                        }if (which==1){

                            imBlockedORNot(hisUID);
//                            Intent intent = new Intent(context, ChatActivity.class);
//                            intent.putExtra("hisUid",hisUID);
//                            context.startActivity(intent);
                        }
                    }
                });

                builder.create().show();

            }
        });
        
        myholder.blockIv.setImageResource(R.drawable.ic_unblocked_green);
        checkIsBlocked(hisUID, myholder, i);

        myholder.blockIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (usersList.get(i).isBlocked()){
                    unBlockUser(hisUID);
                }else {
                    blockUser(hisUID);
                }
            }
        });

    }

    private void imBlockedORNot(String hisUID){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUID).child("BlockedUsers").orderByChild("uid").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()){
                            if (ds.exists()){
                                Toast.makeText(context, "You're blocked by that user, can't send message", Toast.LENGTH_SHORT).show();

                                return;
                            }
                        }

                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra("hisUid",hisUID);
                        context.startActivity(intent);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void checkIsBlocked(String hisUID, MyHolder myholder, int i) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()){
                            myholder.blockIv.setImageResource(R.drawable.ic_block_red);
                            usersList.get(i).setBlocked(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void blockUser(String hisUID) {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid",hisUID);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").child(hisUID).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(context, "Blocked Successfully ", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Failed :"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void unBlockUser(String hisUID) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            if (ds.exists()){
                                ds.getRef().removeValue()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(context, "UnBlocked Successfully", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(context, "Failed :"+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{
        ImageView mAvtarIv, blockIv;
        TextView mNameTv, mEmailTv;

        public MyHolder(@NonNull View itemView){
            super(itemView);

            mAvtarIv = itemView.findViewById(R.id.avatarIv);
            mEmailTv = itemView.findViewById(R.id.emailTv);
            mNameTv = itemView.findViewById(R.id.nameTv);
            blockIv = itemView.findViewById(R.id.blockIv);
        }
    }

}
