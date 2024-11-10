/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package top.theillusivec4.curios.common.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.apache.commons.lang3.EnumUtils;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.CuriosConfig;
import top.theillusivec4.curios.common.slottype.SlotType;

import javax.annotation.Nonnull;
import java.util.*;

public class CuriosSlotManager extends SimpleJsonResourceReloadListener<CuriosSlotData> {
    public static CuriosSlotManager SERVER = new CuriosSlotManager();
    public static CuriosSlotManager CLIENT = new CuriosSlotManager();

    private Map<String, ISlotType> slots = ImmutableMap.of();
    private Set<String> configSlots = ImmutableSet.of();
    private Map<String, ResourceLocation> icons = ImmutableMap.of();
    private Map<String, Set<String>> idToMods = ImmutableMap.of();

    private ICondition.IContext ctx = ICondition.IContext.EMPTY;

    public CuriosSlotManager() {
        super(CuriosSlotData.CODEC, "curios/slots");
    }

    public CuriosSlotManager(ICondition.IContext ctx) {
        super(CuriosSlotData.CODEC, "curios/slots");
        this.ctx = ctx;
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, CuriosSlotData> resourceLocationSlotTypeDataMap,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        Map<String, SlotType.Builder> map = new HashMap<>();
        Map<String, ImmutableSet.Builder<String>> modMap = new HashMap<>();
        Map<ResourceLocation, CuriosSlotData> sorted = new LinkedHashMap<>();

        resourceManager.listPacks().forEach(packResources -> {
            Set<String> namespaces = packResources.getNamespaces(PackType.SERVER_DATA);
            namespaces.forEach(namespace -> packResources.listResources(PackType.SERVER_DATA, namespace, "curios/slots",
                    (resourceLocation, inputStreamIoSupplier) -> {
                        String path = resourceLocation.getPath();
                        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(namespace, path.substring("curios/slots/".length(), path.length() - ".json".length()));

                        CuriosSlotData data = resourceLocationSlotTypeDataMap.get(rl);
                        if (data != null) {
                            sorted.put(rl, data);
                        }
                    }));
        });

        for (Map.Entry<ResourceLocation, CuriosSlotData> entry : sorted.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();

            if (resourcelocation.getNamespace().equals("curios")) {
                try {
                    String id = resourcelocation.getPath();
                    CuriosSlotData data = entry.getValue();

                    if (!ICondition.conditionsMatched(JsonOps.INSTANCE, data.toJson())) {
                        CuriosConstants.LOG.debug("Skipping loading slot {} as its conditions were not met",
                                resourcelocation);
                        continue;
                    }

                    SlotType.Builder builder = map.computeIfAbsent(id, SlotType.Builder::new);
                    populateBuilderFromData(builder, data);
                    modMap.computeIfAbsent(id, k -> ImmutableSet.builder())
                            .add(resourcelocation.getNamespace());
                } catch (IllegalArgumentException | JsonParseException e) {
                    CuriosConstants.LOG.error("Parsing error loading curio slot {}", resourcelocation, e);
                }
            }
        }

        for (Map.Entry<ResourceLocation, CuriosSlotData> entry : sorted.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();

            if (resourcelocation.getPath().startsWith("_") ||
                    resourcelocation.getNamespace().equals("curios")) {
                continue;
            }

            try {
                String id = resourcelocation.getPath();
                CuriosSlotData data = entry.getValue();

                if (!ICondition.conditionsMatched(JsonOps.INSTANCE, data.toJson())) {
                    CuriosConstants.LOG.debug("Skipping loading slot {} as its conditions were not met",
                            resourcelocation);
                    continue;
                }

                SlotType.Builder builder = map.computeIfAbsent(id, SlotType.Builder::new);
                populateBuilderFromData(builder, data);
                modMap.computeIfAbsent(id, k -> ImmutableSet.builder())
                        .add(resourcelocation.getNamespace());
            } catch (IllegalArgumentException | JsonParseException e) {
                CuriosConstants.LOG.error("Parsing error loading curio slot {}", resourcelocation, e);
            }
        }

        try {
            Set<String> configs = fromConfig(map);
            this.configSlots = ImmutableSet.copyOf(configs);

            for (String id : configs) {
                modMap.computeIfAbsent(id, k -> ImmutableSet.builder()).add("config");
            }
        } catch (IllegalArgumentException e) {
            CuriosConstants.LOG.error("Config parsing error", e);
        }

        // Build immutable maps
        this.slots = map.entrySet().stream()
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, entry -> entry.getValue().build()));
        this.idToMods = modMap.entrySet().stream()
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, entry -> entry.getValue().build()));

        CuriosConstants.LOG.info("Loaded {} curio slots", map.size());
    }

    private static void populateBuilderFromData(SlotType.Builder builder, CuriosSlotData data) {
        if (data.order() != null) {
            builder.order(data.order(), true);
        }

        if (!data.icon().isEmpty()) {
            builder.icon(ResourceLocation.parse(data.icon()));
        }

        if (!data.dropRule().isEmpty()) {
            builder.dropRule(data.dropRule());
        }

        if (data.size() != null) {
            builder.size(data.size(), data.operation(), true);
        }

        if (data.addCosmetic() != null) {
            builder.hasCosmetic(data.addCosmetic(), true);
        }

        if (data.useNativeGui() != null) {
            builder.useNativeGui(data.useNativeGui(), true);
        }

        if (data.renderToggle() != null) {
            builder.renderToggle(data.renderToggle(), true);
        }

        if (data.validators() != null) {
            for (String validator : data.validators()) {
                builder.validator(ResourceLocation.parse(validator));
            }
        }
    }

    public Map<String, ISlotType> getSlots() {
        return this.slots;
    }

    public Optional<ISlotType> getSlot(String id) {
        return Optional.ofNullable(this.slots.get(id));
    }

    public static ListTag getSyncPacket() {
        ListTag tag = new ListTag();

        for (Map.Entry<String, ISlotType> entry : SERVER.slots.entrySet()) {
            tag.add(entry.getValue().writeNbt());
        }
        return tag;
    }

    public static void applySyncPacket(ListTag tag) {
        ImmutableMap.Builder<String, ISlotType> map = ImmutableMap.builder();

        for (Tag tag1 : tag) {
            if (tag1 instanceof CompoundTag slotType) {
                ISlotType type = SlotType.from(slotType);
                map.put(type.getIdentifier(), type);
            }
        }
        CLIENT.slots = map.build();
    }

    public void setIcons(Map<String, ResourceLocation> icons) {
        this.icons = ImmutableMap.copyOf(icons);
    }

    public Set<String> getConfigSlots() {
        return this.configSlots;
    }

    public Map<String, ResourceLocation> getIcons() {
        return this.icons;
    }

    public ResourceLocation getIcon(String identifier) {
        return this.icons.getOrDefault(identifier, ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "slot/empty_curio_slot"));
    }

    public Map<String, Set<String>> getModsFromSlots() {
        return this.idToMods;
    }

    /**
     * Parses configuration entries and populates SlotType.Builder instances accordingly.
     *
     * @param map The map of slot IDs to their corresponding SlotType.Builder.
     * @return A set of slot IDs that were configured.
     * @throws IllegalArgumentException If any configuration entry is invalid.
     */
    public static Set<String> fromConfig(Map<String, SlotType.Builder> map)
            throws IllegalArgumentException {
        List<Map<String, String>> parsed = new ArrayList<>();
        List<? extends String> list = CuriosConfig.COMMON.slots.get();
        Set<String> results = new HashSet<>();

        for (String s : list) {
            StringTokenizer tokenizer = new StringTokenizer(s, ";");
            Map<String, String> subMap = new HashMap<>();

            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                String[] keyValue = token.split("=");
                if (keyValue.length == 2) {
                    subMap.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }

            if (subMap.containsKey("id")) {
                parsed.add(subMap);
            } else {
                throw new IllegalArgumentException(
                        "Cannot load config entry " + s + " due to missing id field");
            }
        }

        for (Map<String, String> entry : parsed) {
            String id = entry.get("id");
            SlotType.Builder builder = map.computeIfAbsent(id, SlotType.Builder::new);
            Integer size = entry.containsKey("size") ? Integer.parseInt(entry.get("size")) : null;

            if (size != null && size < 0) {
                throw new IllegalArgumentException("Size cannot be less than 0!");
            }
            String operation = entry.getOrDefault("operation", "SET");

            if (!operation.equals("SET") && !operation.equals("ADD") && !operation.equals("REMOVE")) {
                throw new IllegalArgumentException(operation + " is not a valid operation!");
            }
            String dropRule = entry.getOrDefault("drop_rule", "");

            if (!dropRule.isEmpty() && !EnumUtils.isValidEnum(ICurio.DropRule.class, dropRule)) {
                throw new IllegalArgumentException(dropRule + " is not a valid drop rule!");
            }
            results.add(id);
            boolean replace = true;
            Integer order = entry.containsKey("order") ? Integer.parseInt(entry.get("order")) : null;
            String icon = entry.getOrDefault("icon", "");
            Boolean toggle =
                    entry.containsKey("render_toggle") ? Boolean.parseBoolean(entry.get("render_toggle")) :
                            null;
            Boolean cosmetic =
                    entry.containsKey("add_cosmetic") ? Boolean.parseBoolean(entry.get("add_cosmetic")) :
                            null;
            Boolean nativeGui =
                    entry.containsKey("use_native_gui") ? Boolean.parseBoolean(entry.get("use_native_gui")) :
                            null;

            if (order != null) {
                builder.order(order, replace);
            }

            if (!icon.isEmpty()) {
                builder.icon(ResourceLocation.parse(icon));
            }

            if (!dropRule.isEmpty()) {
                builder.dropRule(dropRule);
            }

            if (size != null) {
                builder.size(size, operation, replace);
            }

            if (cosmetic != null) {
                builder.hasCosmetic(cosmetic, replace);
            }

            if (nativeGui != null) {
                builder.useNativeGui(nativeGui, replace);
            }

            if (toggle != null) {
                builder.renderToggle(toggle, replace);
            }
        }
        return results;
    }

    public static void fromJson(SlotType.Builder builder, CuriosSlotData slotTypeData)
            throws IllegalArgumentException, JsonParseException {

        if (slotTypeData.order() != null) {
            builder.order(slotTypeData.order(), slotTypeData.replace());
        }

        if (!slotTypeData.icon().isEmpty()) {
            builder.icon(ResourceLocation.parse(slotTypeData.icon()));
        }

        if (!slotTypeData.dropRule().isEmpty()) {
            builder.dropRule(slotTypeData.dropRule());
        }

        if (slotTypeData.size() != null) {
            builder.size(slotTypeData.size(), slotTypeData.operation(), slotTypeData.replace());
        }

        if (slotTypeData.addCosmetic() != null) {
            builder.hasCosmetic(slotTypeData.addCosmetic(), slotTypeData.replace());
        }

        if (slotTypeData.useNativeGui() != null) {
            builder.useNativeGui(slotTypeData.useNativeGui(), slotTypeData.replace());
        }

        if (slotTypeData.renderToggle() != null) {
            builder.renderToggle(slotTypeData.renderToggle(), slotTypeData.replace());
        }

        if (slotTypeData.validators() != null) {
            for (String validator : slotTypeData.validators()) {
                builder.validator(ResourceLocation.parse(validator));
            }
        }
    }
}