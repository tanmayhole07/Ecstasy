package com.example.socialm.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialm.R;
import com.example.socialm.models.ModelNotification;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AdapterNotification extends RecyclerView.Adapter<AdapterNotification.HolderNotification> {
    private Context context;
    private ArrayList<ModelNotification> notificationsList;

    @NonNull
    @Override
    public HolderNotification onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_notification, parent, false);

        return new HolderNotification(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderNotification holder, int position) {

        ModelNotification model = notificationsList.get(position);
        String name = model.getsName();
        String notification  = model.getNotification();
        String image = model.getsImage();
        String timestamp = model.getTimeStamp();
        String senderUid = model.getsUid();

        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(Long.parseLong(timestamp));
        String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.orderByChild("uid").equalTo(senderUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            String name = ""+ds.child("name").getValue();
                            String image = ""+ds.child("image").getValue();
                            String email = ""+ds.child("email").getValue();

                            holder.nameTv.setText(name);
                            try {
                                Picasso.get().load(image).placeholder(R.drawable.ic_default_img).into(holder.avatarIv);
                            }catch (Exception e){
                                holder.avatarIv.setImageResource(R.drawable.ic_default_img);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        holder.notificationTv.setText(notification);
        holder.timeTv.setText(pTime);

    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    class HolderNotification extends RecyclerView.ViewHolder{

        ImageView avatarIv;
        TextView nameTv, timeTv, notificationTv;
        public HolderNotification(@NonNull View itemView) {
            super(itemView);

            avatarIv = itemView.findViewById(R.id.avatarIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            notificationTv = itemView.findViewById(R.id.notificationTv);

        }
    }
}
