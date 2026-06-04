package sv.edu.agroconecta.network;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import sv.edu.agroconecta.model.LoginResponse;

public interface AuthApi {
    @FormUrlEncoded
    @POST("login")
    Call<LoginResponse> login(
            @Field("correo") String correo,
            @Field("password") String password
    );
}