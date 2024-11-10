package top.theillusivec4.curios.common.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record CuriosEntityData(boolean replace, List<String> entities, List<String> slots) {
    public static final Codec<CuriosEntityData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(CuriosEntityData::replace),
            Codec.STRING.listOf().fieldOf("entities").forGetter(CuriosEntityData::entities),
            Codec.STRING.listOf().fieldOf("slots").forGetter(CuriosEntityData::slots)
    ).apply(instance, CuriosEntityData::new));

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();

        obj.addProperty("replace", replace);

        JsonArray entitiesArray = new JsonArray();

        for (String entity : entities)
            entitiesArray.add(new JsonPrimitive(entity));

        obj.add("entities", entitiesArray);

        JsonArray slotsArray = new JsonArray();

        for (String slot : slots)
            slotsArray.add(new JsonPrimitive(slot));

        obj.add("slots", slotsArray);

        return obj;
    }
}