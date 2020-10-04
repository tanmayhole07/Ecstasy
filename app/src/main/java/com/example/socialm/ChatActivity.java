package com.example.socialm;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.socialm.adapters.AdapterChat;
import com.example.socialm.models.ModelChat;
import com.example.socialm.models.ModelUsers;
import com.example.socialm.notifications.Data;
import com.example.socialm.notifications.Sender;
import com.example.socialm.notifications.Token;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//import retrofit2.Call;
//import retrofit2.Callback;

public class ChatActivity extends AppCompatActivity {
    AdapterChat adapterChat;
    private RequestQueue requestQueue;

    List<ModelChat> chatList;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;

    String hisImage;
    String hisUid;
    String myUid;

    EditText messageEt;
    TextView nameTv;
    private boolean notify = false;
    ImageView profileIv;

    RecyclerView recyclerView;
    ValueEventListener seenListener;

    ImageButton sendBtn;
    Toolbar toolbar;

    DatabaseReference userDbref;
    DatabaseReference userRefForSeen;
    TextView userStatusTv;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");

        recyclerView = (RecyclerView) findViewById(R.id.chat_recyclerView);
        profileIv = (ImageView) findViewById(R.id.profileIv);
        nameTv = (TextView) findViewById(R.id.nameTv);
        userStatusTv = (TextView) findViewById(R.id.userStatusTv);
        messageEt = (EditText) findViewById(R.id.messageEt);
        sendBtn = (ImageButton) findViewById(R.id.sendBtn);

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        firebaseAuth = FirebaseAuth.getInstance();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);


        firebaseDatabase  = FirebaseDatabase.getInstance();
        userDbref = firebaseDatabase.getReference("Users");

        Query userQuery = userDbref.orderByChild("uid").equalTo(hisUid);
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    String name = "" + ds.child("name").getValue();
                    hisImage = "" + ds.child("image").getValue();

                    String typingStatus = "" + ds.child("typingTo").getValue();
                    if (typingStatus.equals(myUid)){
                        userStatusTv.setText("typing...");
                    }else {
                        String onlineStatus =  "" + ds.child("onlineStatus").getValue();
                        if (onlineStatus.equals("online")) {
                            ChatActivity.this.userStatusTv.setText(onlineStatus);
                        }
                        else {
                            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                            cal.setTimeInMillis(Long.parseLong(onlineStatus));
                            String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();
                            userStatusTv.setText("Last seen at: " + dateTime);
                        }

                    }


                    nameTv.setText(name);
                    try {
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_img).into(profileIv);
                    } catch (Exception e) {
                        Picasso.get().load(R.drawable.ic_default_img).into(profileIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                //notify = true;
                String message = messageEt.getText().toString().trim();
                if (TextUtils.isEmpty(message)) {
                    Toast.makeText(ChatActivity.this, "Cannot send the empty message", Toast.LENGTH_SHORT).show();
                } else {
                    sendMessage(message);
                }
                messageEt.setText("");
            }
        });

        messageEt.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.toString().trim().length() == 0) {
                    checkTypingStatus("noOne");
                    return;
                }
                checkTypingStatus(hisUid);
            }

            public void afterTextChanged(Editable editable) {
            }
        });

        readMessages();
        seenMessage();
    }

    private void seenMessage() {
        userRefForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userRefForSeen.addValueEventListener(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) {
                        HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put("isSeen", true);
                        ds.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void readMessages() {
        chatList = new ArrayList();
        DatabaseReference dbRef =  FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChat chat =ds.getValue(ModelChat.class);
                    if ((chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) ||
                            (chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid))) {
                        chatList.add(chat);
                    }
                    adapterChat = new AdapterChat(ChatActivity.this, chatList, hisImage);
                    adapterChat.notifyDataSetChanged();
                    recyclerView.setAdapter(ChatActivity.this.adapterChat);
                }
            }

            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public void sendMessage(final String message) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", message);
        hashMap.put("timestamp", timestamp);
        hashMap.put("isSeen", false);
        databaseReference.child("Chats").push().setValue(hashMap);
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                ModelUsers user = dataSnapshot.getValue(ModelUsers.class);
                if (notify) {
                    senNotification(hisUid, user.getName(), message);
                }
                notify = false;
            }
            public void onCancelled(DatabaseError databaseError) {
            }
        });
       //messageEt.setText("");

        DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("ChatList")
                .child(myUid)
                .child(hisUid);
        chatRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()){
                    chatRef1.child("id").setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("ChatList")
                .child(hisUid)
                .child(myUid);

        chatRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()){
                    chatRef2.child("id").setValue(myUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public void senNotification(final String hisUid, final String name, final String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(myUid, name+","+message,"New Message", hisUid, R.drawable.ic_default_img);
                    Sender sender = new Sender(data, token.getToken());

                    try {
                        JSONObject senderJsonObj = new JSONObject(new Gson().toJson(sender));
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", senderJsonObj,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        Log.d("JSON_RESPONSE","onResponse : " + response.toString());
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("JSON_RESPONSE","onResponse : " + error.toString());
                            }
                        }){
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError{
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Content-Type","application/json");
                                headers.put("Authorization", "key=AAAA1CIFI1Y:APA91bEqeN0WC0f8xIFLSacHx95ULIUycYBpIFSkNtjiWMzVYv8rcS4eOerZ_T6Skx4zAikxQqrz665277uX4oSTCIHyuBlgMdWn2rOsO8uVbz9sa_egV_wTSh1qbSMUkNN2jjvy-Gd2");
                                return super.getHeaders();
                            }
                        };
                        requestQueue.add(jsonObjectRequest);
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void checkUserStatus() {
        FirebaseUser user = this.firebaseAuth.getCurrentUser();
        if (user != null) {
            myUid = user.getUid();
            return;
        }
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void checkOnlineStatus(String status) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);
        dbRef.updateChildren(hashMap);
    }

    public void checkTypingStatus(String typing) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("typingTo", typing);
        dbRef.updateChildren(hashMap);
    }


    public void onPause() {
        super.onPause();
        String timestamp = String.valueOf(System.currentTimeMillis());
        checkOnlineStatus(timestamp);
        checkTypingStatus("noOne");
        userRefForSeen.removeEventListener(seenListener);
    }


    public void onResume() {
        checkOnlineStatus("online");
        super.onResume();
    }

    public void onStart() {
        checkUserStatus();
        checkOnlineStatus("online");
        super.onStart();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}
