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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialm.ChatActivity;
import com.example.socialm.R;
import com.example.socialm.ThereProfileActivity;
import com.example.socialm.models.ModelUsers;
import com.squareup.picasso.Picasso;

import java.util.List;

public class AdapterUsers extends RecyclerView.Adapter<AdapterUsers.MyHolder>{

    Context context;
    List<ModelUsers> usersList;

    public AdapterUsers(Context context, List<ModelUsers> usersList) {
        this.context = context;
        this.usersList = usersList;
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


//                Intent intent = new Intent(context, ChatActivity.class);
//                intent.putExtra("hisUid",hisUID);
//                context.startActivity(intent);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setItems(new String[]{"Profile", "Chat"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        if (which == 0){
                            Intent intent = new Intent(context, ThereProfileActivity.class);
                            intent.putExtra("uid",hisUID);
                            context.startActivity(intent);
                        }if (which==1){

                            Intent intent = new Intent(context, ChatActivity.class);
                            intent.putExtra("hisUid",hisUID);
                            context.startActivity(intent);
                        }
                    }
                });

                builder.create().show();

            }
        });

    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{
        ImageView mAvtarIv;
        TextView mNameTv, mEmailTv;

        public MyHolder(@NonNull View itemView){
            super(itemView);

            mAvtarIv = itemView.findViewById(R.id.avatarIv);
            mEmailTv = itemView.findViewById(R.id.emailTv);
            mNameTv = itemView.findViewById(R.id.nameTv);
        }
    }

}
