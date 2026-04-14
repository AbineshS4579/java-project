package com.complaints.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Thread-safe Gson helpers used across controllers.
 */
public final class JsonUtil {

    public static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .serializeNulls()
            .create();

    private JsonUtil() {}

    public static String ok(Object data) {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.add("data", GSON.toJsonTree(data));
        return GSON.toJson(obj);
    }

    public static String ok(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("message", message);
        return GSON.toJson(obj);
    }

    public static String error(int code, String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", false);
        obj.addProperty("code", code);
        obj.addProperty("message", message);
        return GSON.toJson(obj);
    }
}
