package com.bluberry.adclient;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import cs.ipc.AdItemType;

public class AdItemTypeSerializer implements JsonSerializer<AdItemType>,
        JsonDeserializer<AdItemType> {

    // 对象转为Json时调用,实现JsonSerializer<PackageState>接口  
    @Override
    public JsonElement serialize(AdItemType state, Type arg1,
                                 JsonSerializationContext arg2) {
        return new JsonPrimitive(state.ordinal());
    }

    // json转为对象时调用,实现JsonDeserializer<PackageState>接口  
    @Override
    public AdItemType deserialize(JsonElement json, Type typeOfT,
                                  JsonDeserializationContext context) throws JsonParseException {
        if (json.getAsInt() < AdItemType.values().length)
            return AdItemType.values()[json.getAsInt()];
        return null;
    }

}  