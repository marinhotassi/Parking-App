package com.example.cliente22.Utils;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.cliente22.Common.Common;
import com.example.cliente22.Model.FCMSendData;
import com.example.cliente22.Model.FCMResponse;
import com.example.cliente22.Model.LocatorGeoModel;
import com.example.cliente22.Model.TokenModel;
import com.example.cliente22.R;
import com.example.cliente22.Remote.IFCMService;
import com.example.cliente22.Remote.RetrofitFCMClient;
import com.example.cliente22.RequestLocatorActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class UserUtils {

    public static void updateUser(View view, Map<String,Object> updateData){
        //FirebaseDatabase.getInstance()
        FirebaseDatabase.getInstance("https://estacionamento-64541-default-rtdb.firebaseio.com/")
                .getReference(Common.CLIENT_INFO_REFERENCE)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .updateChildren(updateData)
                .addOnFailureListener(e -> Snackbar.make(view,e.getMessage(),Snackbar.LENGTH_SHORT).show())
                .addOnSuccessListener(aVoid -> Snackbar.make(view,"Update information successfully!",Snackbar.LENGTH_SHORT).show());

    }

    public static void updateToken(Context context, String token) {
        TokenModel tokenModel = new TokenModel(token);

        FirebaseDatabase.getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(tokenModel)
                .addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show()).addOnSuccessListener(aVoid -> {

        });
    }

    public static void sendRequestToLocator(Context context, RelativeLayout main_layout, LocatorGeoModel foundLocator, LatLng target) {

        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        //Get token Firebase
        FirebaseDatabase
                .getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(foundLocator.getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            TokenModel tokenModel = dataSnapshot.getValue(TokenModel.class);

                            Map<String,String> notificationData = new HashMap<>();
                            notificationData.put(Common.NOTI_TITLE,Common.REQUEST_LOCATOR_TITLE);
                            notificationData.put(Common.NOTI_CONTENT,"This message represent for request locator action");
                            notificationData.put(Common.CLIENT_PICKUP_LOCATION,new StringBuilder("")
                            .append(target.latitude)
                            .append(",")
                            .append(target.longitude)
                                    .toString());

                            FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(),notificationData);

                            compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            //.subscribe(fcmResponse -> {
                                    .subscribe((Consumer<FCMResponse>) fcmResponse -> {
                                        //if(fcmResponse.getSuccess() == 0){
                                        if (fcmResponse.getSuccess() == 0) {
                                            compositeDisposable.clear();
                                            Snackbar.make(main_layout, context.getString(R.string.request_locator_failed), Snackbar.LENGTH_LONG).show();
                                        }
                                    }, throwable -> {
                                    compositeDisposable.clear();
                                Snackbar.make(main_layout,throwable.getMessage(),Snackbar.LENGTH_LONG).show();
                            }));

                        }
                        else {
                            Snackbar.make(main_layout,context.getString(R.string.token_not_found),Snackbar.LENGTH_LONG).show();
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Snackbar.make(main_layout,databaseError.getMessage(),Snackbar.LENGTH_LONG).show();
                    }
                });
    }
}
