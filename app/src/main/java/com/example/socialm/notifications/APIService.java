package com.example.socialm.notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAA1CIFI1Y:APA91bEqeN0WC0f8xIFLSacHx95ULIUycYBpIFSkNtjiWMzVYv8rcS4eOerZ_T6Skx4zAikxQqrz665277uX4oSTCIHyuBlgMdWn2rOsO8uVbz9sa_egV_wTSh1qbSMUkNN2jjvy-Gd2"
    })

    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);
}
