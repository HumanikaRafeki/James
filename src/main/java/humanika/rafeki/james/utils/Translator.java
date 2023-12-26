// Originally written by me.mcofficer (AKA M*C*O) in EndlessSky-Discord-Bot

package humanika.rafeki.james.utils;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;

public class Translator {

    /**
     * @param sourceLang The source language, defaults to "auto"
     * @param targetLang The target language, defaults to "en"
     * @param query The text to be translated
     * @return The translated text.
     */
    public static String translate(@Nullable String sourceLang, @Nullable String targetLang, String query, OkHttpClient okHttpClient) throws IOException{
        if (sourceLang == null)
            sourceLang = "auto";
        if (targetLang == null)
            targetLang = "en";

        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("translate.googleapis.com")
                .addPathSegments("translate_a/single")
                .addQueryParameter("client", "gtx")
                .addQueryParameter("sl", sourceLang)
                .addQueryParameter("tl", targetLang)
                .addQueryParameter("dt", "t")
                .addQueryParameter("q", query)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0")
                .get()
                .build();

        Response response = okHttpClient.newCall(request).execute();
        JSONArray json = new JSONArray(response.body().string());

        StringBuilder stringBuilder = new StringBuilder();
        for (Object snippet : json.getJSONArray(0)) // Why use a raw iterator here, org.json? WHY?
            if (snippet instanceof JSONArray)
                stringBuilder.append(((JSONArray) snippet).getString(0));

        return stringBuilder.toString();
    }
}
