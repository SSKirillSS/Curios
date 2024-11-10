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
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.conditions.ICondition;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.api.type.ISlotType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CuriosEntityManager extends SimpleJsonResourceReloadListener<CuriosEntityData> {
    public static CuriosEntityManager SERVER = new CuriosEntityManager();
    public static CuriosEntityManager CLIENT = new CuriosEntityManager();

    private Map<EntityType<?>, Map<String, ISlotType>> entitySlots = ImmutableMap.of();
    private Map<String, Set<String>> idToMods = ImmutableMap.of();

    private ICondition.IContext ctx = ICondition.IContext.EMPTY;

    public CuriosEntityManager() {
        super(CuriosEntityData.CODEC, "curios/entities");
    }

    public CuriosEntityManager(ICondition.IContext ctx) {
        super(CuriosEntityData.CODEC, "curios/entities");

        this.ctx = ctx;
    }

    @Override
    protected void apply(Map<ResourceLocation, CuriosEntityData> resourceLocationCurioEntityDataMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<EntityType<?>, ImmutableMap.Builder<String, ISlotType>> map = new HashMap<>();
        Map<String, ImmutableSet.Builder<String>> modMap = new HashMap<>();

        for (Map.Entry<ResourceLocation, CuriosEntityData> entry : resourceLocationCurioEntityDataMap.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            CuriosEntityData data = entry.getValue();

            if (resourcelocation.getPath().startsWith("_"))
                continue;

            try {
                Map<EntityType<?>, Map<String, ISlotType>> slotsMap = getSlotsForEntities(data, resourcelocation, this.ctx);

                for (Map.Entry<EntityType<?>, Map<String, ISlotType>> entry1 : slotsMap.entrySet()) {
                    if (data.replace()) {
                        ImmutableMap.Builder<String, ISlotType> builder = ImmutableMap.builder();

                        builder.putAll(entry1.getValue());

                        map.put(entry1.getKey(), builder);
                    } else
                        map.computeIfAbsent(entry1.getKey(), k -> ImmutableMap.builder()).putAll(entry1.getValue());

                    modMap.computeIfAbsent(resourcelocation.getPath(), k -> ImmutableSet.builder()).add(resourcelocation.getNamespace());
                }
            } catch (Exception e) {
                CuriosConstants.LOG.error("Error processing curio entity {}: {}", resourcelocation, e.getMessage());
            }
        }

        Map<String, ISlotType> configSlots = new HashMap<>();

        for (String configSlot : CuriosSlotManager.SERVER.getConfigSlots())
            CuriosSlotManager.SERVER.getSlot(configSlot).ifPresentOrElse(slot -> configSlots.put(configSlot, slot), () -> CuriosConstants.LOG.error("{} slot type is not registered!", configSlot));

        map.computeIfAbsent(EntityType.PLAYER, k -> ImmutableMap.builder()).putAll(configSlots);

        this.entitySlots = map.entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, entry -> entry.getValue().buildKeepingLast()));
        this.idToMods = modMap.entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, entry -> entry.getValue().build()));

        CuriosConstants.LOG.info("Loaded {} curio entities", map.size());
    }

    public static ListTag getSyncPacket() {
        ListTag tag = new ListTag();

        for (Map.Entry<EntityType<?>, Map<String, ISlotType>> entry : SERVER.entitySlots.entrySet()) {
            ResourceLocation rl = BuiltInRegistries.ENTITY_TYPE.getKey(entry.getKey());

            CompoundTag entity = new CompoundTag();

            entity.putString("Entity", rl.toString());

            ListTag tag1 = new ListTag();

            for (Map.Entry<String, ISlotType> val : entry.getValue().entrySet())
                tag1.add(StringTag.valueOf(val.getKey()));

            entity.put("Slots", tag1);

            tag.add(entity);
        }
        return tag;
    }

    public static void applySyncPacket(ListTag tag) {
        Map<EntityType<?>, ImmutableMap.Builder<String, ISlotType>> map = new HashMap<>();

        for (Tag tag1 : tag) {
            if (tag1 instanceof CompoundTag entity) {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(entity.getString("Entity"))).orElse(null);

                if (type != null) {
                    ListTag slots = entity.getList("Slots", Tag.TAG_STRING);

                    for (Tag slot : slots) {
                        if (slot instanceof StringTag stringTag) {
                            String id = stringTag.getAsString();

                            CuriosSlotManager.CLIENT.getSlot(id).ifPresent(slotType -> map.computeIfAbsent(type, k -> ImmutableMap.builder()).put(id, slotType));
                        }
                    }
                }
            }
        }

        CLIENT.entitySlots = map.entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, entry -> entry.getValue().build()));
    }

    private static Map<EntityType<?>, Map<String, ISlotType>> getSlotsForEntities(CuriosEntityData data, ResourceLocation resourceLocation, ICondition.IContext ctx) {
        Map<EntityType<?>, Map<String, ISlotType>> map = new HashMap<>();

        if (!ICondition.conditionsMatched(JsonOps.INSTANCE, data.toJson())) {
            CuriosConstants.LOG.debug("Skipping loading entity file {} due to unmet conditions", resourceLocation);

            return map;
        }

        Set<EntityType<?>> toAdd = new HashSet<>();

        for (String entity : data.entities()) {
            if (entity.startsWith("#")) {
                TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(entity.substring(1)));

                BuiltInRegistries.ENTITY_TYPE.get(tagKey).ifPresent(named -> {
                    for (Holder<EntityType<?>> entityTypeHolder : named)
                        toAdd.add(entityTypeHolder.value());
                });
            } else {
                ResourceLocation entityRL = ResourceLocation.tryParse(entity);

                if (entityRL == null) {
                    CuriosConstants.LOG.error("Invalid entity ResourceLocation: {}", entity);

                    continue;
                }

                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityRL).orElse(null);

                if (type != null)
                    toAdd.add(type);
                else
                    CuriosConstants.LOG.error("{} entity type is not registered!", entity);
            }
        }

        Map<String, ISlotType> slots = new HashMap<>();

        for (String slotId : data.slots())
            CuriosSlotManager.SERVER.getSlot(slotId).ifPresentOrElse(slot -> slots.put(slotId, slot), () -> CuriosConstants.LOG.error("{} slot type is not registered!", slotId));

        for (EntityType<?> entityType : toAdd)
            map.computeIfAbsent(entityType, k -> new HashMap<>()).putAll(slots);

        return map;
    }

    public Map<String, ISlotType> getEntitySlots(EntityType<?> type) {
        return this.entitySlots.getOrDefault(type, ImmutableMap.of());
    }

    public Map<String, Set<String>> getModsFromSlots() {
        return ImmutableMap.copyOf(idToMods);
    }
}