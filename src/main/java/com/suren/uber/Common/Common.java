package com.suren.uber.Common;

import com.suren.uber.Remote.IGoogleApi;
import com.suren.uber.Remote.RetrofitClient;

public class Common {
    public static final String baseUrl = "https://maps.googleapis.com";
    public static IGoogleApi getGoogleApi()
    {
       return RetrofitClient.getClient(baseUrl).create(IGoogleApi.class);
    }
}
