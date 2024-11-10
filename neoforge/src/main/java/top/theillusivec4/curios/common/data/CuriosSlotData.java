package top.theillusivec4.curios.common.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

public record CuriosSlotData(Integer size, String operation, String dropRule, boolean replace, Integer order, String icon, Boolean renderToggle, Boolean addCosmetic, Boolean useNativeGui, List<String> validators) {
    public static final Codec<CuriosSlotData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("size").forGetter(data -> Optional.ofNullable(data.size())),

            Codec.STRING.optionalFieldOf("operation", "SET").forGetter(CuriosSlotData::operation),
            Codec.STRING.optionalFieldOf("drop_rule", "").forGetter(CuriosSlotData::dropRule),
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(CuriosSlotData::replace),

            Codec.INT.optionalFieldOf("order").forGetter(data -> Optional.ofNullable(data.order())),

            Codec.STRING.optionalFieldOf("icon", "").forGetter(CuriosSlotData::icon),

            Codec.BOOL.optionalFieldOf("render_toggle").forGetter(data -> Optional.ofNullable(data.renderToggle())),
            Codec.BOOL.optionalFieldOf("add_cosmetic").forGetter(data -> Optional.ofNullable(data.addCosmetic())),
            Codec.BOOL.optionalFieldOf("use_native_gui").forGetter(data -> Optional.ofNullable(data.useNativeGui())),

            Codec.STRING.listOf().optionalFieldOf("validators").forGetter(data -> Optional.ofNullable(data.validators()))
    ).apply(instance, (sizeOpt, operation, dropRule, replace, orderOpt, icon, renderToggleOpt, addCosmeticOpt, useNativeGuiOpt, validatorsOpt) ->
            new CuriosSlotData(sizeOpt.orElse(null), operation, dropRule, replace, orderOpt.orElse(null), icon, renderToggleOpt.orElse(null), addCosmeticOpt.orElse(null), useNativeGuiOpt.orElse(null), validatorsOpt.orElse(null))
    ));

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();

        if (size != null)
            obj.addProperty("size", size);

        obj.addProperty("operation", operation);
        obj.addProperty("drop_rule", dropRule);
        obj.addProperty("replace", replace);

        if (order != null)
            obj.addProperty("order", order);

        obj.addProperty("icon", icon);

        if (renderToggle != null)
            obj.addProperty("render_toggle", renderToggle);

        if (addCosmetic != null)
            obj.addProperty("add_cosmetic", addCosmetic);

        if (useNativeGui != null)
            obj.addProperty("use_native_gui", useNativeGui);

        if (validators != null && !validators.isEmpty()) {
            JsonElement validatorsElement = validators.stream()
                    .map(JsonPrimitive::new)
                    .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

            obj.add("validators", validatorsElement);
        }

        return obj;
    }
}