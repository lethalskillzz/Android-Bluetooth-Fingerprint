package com.fgtit.fingerprintattendance.rest;

import android.database.Observable;

import com.fgtit.fingerprintattendance.model.CaptureItem;
import com.fgtit.fingerprintattendance.model.EnrollItem;

import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;


import static com.fgtit.fingerprintattendance.app.AppConfig.*;

/**
 * Created by letha on 9/14/2016.
 */
public interface ApiInterface {

    @Headers("Authorization :" + AUTHORIZATION)
    @POST(ENROLL)
    Call<EnrollItem> enroll(@Body JSONObject payload);

    @Headers("Authorization :" + AUTHORIZATION)
    @POST(CAPTURE)
    Observable<CaptureItem> capture(@Body JSONObject payload);

    @Headers("Authorization :" + AUTHORIZATION)
    @GET(GET_ENROLL)
    Observable<List<EnrollItem>> getEnroll(@Query("item_pos") String item_pos);

    @Headers("Authorization :" + AUTHORIZATION)
    @GET(GET_CAPTURE)
    Observable<List<CaptureItem>> getCapture(@Query("item_pos") String item_pos);
}
