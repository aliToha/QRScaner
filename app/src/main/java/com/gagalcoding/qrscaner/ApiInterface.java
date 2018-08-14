package com.gagalcoding.qrscaner;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Created by Ali on 3/11/2018.
 */

public interface ApiInterface {

    @FormUrlEncoded
    @POST("addBarcode.php")
    Call<Value> addBarcode (@Field("nilai_barcode") String nilai_barcode);
}
