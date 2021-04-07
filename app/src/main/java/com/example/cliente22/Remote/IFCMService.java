package com.example.cliente22.Remote;

import com.example.cliente22.Model.FCMResponse;
import com.example.cliente22.Model.FCMSendData;
import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAOJMjXJU:APA91bHa9nbjJzNb0GtaMT6ia3huP094jkbpNT8xoJgNBfNlWwNMJ-H2vp4E4v1yM_0ALXfJ3_T4Oq1AlUA-g0Wlcf9yQPXc_sSwYGPE-JaM-N0F67OW_ygz4CUEoaZYMmA9Eqn67x5j"
    })
    @POST("fcm/send")
    Observable<FCMResponse> sendNotification(@Body FCMSendData body);
}
