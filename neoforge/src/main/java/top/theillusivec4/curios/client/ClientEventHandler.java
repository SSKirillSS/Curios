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

package top.theillusivec4.curios.client;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.ICuriosMenu;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.network.client.CPacketOpenCurios;

import java.util.*;

import static net.minecraft.world.item.component.ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT;

public class ClientEventHandler {

    private static final ResourceLocation ATTACK_DAMAGE_MODIFIER = ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "attack_damage");
    private static final ResourceLocation ATTACK_SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "attack_speed");

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post evt) {
        Minecraft mc = Minecraft.getInstance();

        if (KeyRegistry.openCurios.consumeClick() && mc.isWindowActive()) {
            PacketDistributor.sendToServer(new CPacketOpenCurios(ItemStack.EMPTY));
        }
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent evt) {
        ItemStack stack = evt.getItemStack();
        Player player = evt.getEntity();

        if (!stack.isEmpty()) {
            List<Component> tooltip = evt.getToolTip();

            // Process non-Curio equipment items with slot modifier tooltips
            // todo: There's probably a better way to accomplish this
            for (int i = 0; i < tooltip.size(); i++) {
                Component component = tooltip.get(i);

                if (component.getContents() instanceof TranslatableContents contents) {
                    boolean replace = false;
                    Object[] args = contents.getArgs();

                    // https://github.com/TheIllusiveC4/Curios/issues/388
                    // noinspection ConstantConditions
                    if (args != null) {

                        for (int i1 = 0; i1 < args.length; i1++) {
                            Object arg = args[i1];

                            if (arg instanceof MutableComponent mutableComponent &&
                                    mutableComponent.getContents() instanceof TranslatableContents contents1) {

                                if (contents1.getKey().startsWith("curios.slot.")) {
                                    String actualKey = contents1.getKey().replace(".slot.", ".identifier.");
                                    contents.getArgs()[i1] = Component.translatable(actualKey, contents1.getArgs());
                                    replace = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (replace) {
                        tooltip.set(i, Component.translatable(
                                contents.getKey().replace("attribute.modifier.", "curios.modifiers.slots."),
                                contents.getArgs()).withStyle(component.getStyle()));
                    }
                }
            }
            Map<String, ISlotType> map = player != null ? CuriosApi.getItemStackSlots(stack, player) :
                    CuriosApi.getItemStackSlots(stack, FMLLoader.getDist() == Dist.CLIENT);
            Set<String> curioTags = Set.copyOf(map.keySet());

            if (curioTags.contains("curio")) {
                curioTags = Set.of("curio");
            }
            List<String> slots = new ArrayList<>(curioTags);

            if (!slots.isEmpty()) {
                List<Component> tagTooltips = new ArrayList<>();
                MutableComponent slotsTooltip =
                        Component.translatable("curios.tooltip.slot").append(" ")
                                .withStyle(ChatFormatting.GOLD);

                for (int j = 0; j < slots.size(); j++) {
                    String key = "curios.identifier." + slots.get(j);
                    MutableComponent type = Component.translatable(key);

                    if (j < slots.size() - 1) {
                        type = type.append(", ");
                    }

                    type = type.withStyle(ChatFormatting.YELLOW);
                    slotsTooltip.append(type);
                }
                tagTooltips.add(slotsTooltip);

                Optional<ICurio> optionalCurio = CuriosApi.getCurio(stack);
                optionalCurio.ifPresent(curio -> {
                    List<Component> actualSlotsTooltip = curio.getSlotsTooltip(tagTooltips);

                    if (!actualSlotsTooltip.isEmpty()) {
                        tooltip.addAll(1, actualSlotsTooltip);
                    }
                });

                if (optionalCurio.isEmpty()) {
                    tooltip.addAll(1, tagTooltips);
                }
                List<Component> attributeTooltip = new ArrayList<>();

                for (String identifier : slots) {
                    Multimap<Holder<Attribute>, AttributeModifier> multimap =
                            CuriosApi.getAttributeModifiers(new SlotContext(identifier, player, 0, false, true),
                                    ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, identifier), stack);

                    if (!multimap.isEmpty()) {
                        boolean init = false;

                        for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : multimap.entries()) {

                            // Some mods are adding null attributes so add a guard here to not crash the client
                            if (entry.getKey() == null) {
                                continue;
                            }

                            if (!init) {
                                attributeTooltip.add(Component.empty());
                                attributeTooltip.add(Component.translatable("curios.modifiers." + identifier)
                                        .withStyle(ChatFormatting.GOLD));
                                init = true;
                            }
                            AttributeModifier attributemodifier = entry.getValue();
                            double amount = attributemodifier.amount();
                            boolean flag = false;

                            if (player != null) {

                                if (attributemodifier.id() == ATTACK_DAMAGE_MODIFIER) {
                                    AttributeInstance att = player.getAttribute(Attributes.ATTACK_DAMAGE);

                                    if (att != null) {
                                        amount = amount + att.getBaseValue();
                                    }
                                    //amount = amount + EnchantmentHelper.getDamageBonus(stack, null); TODO: ???
                                    flag = true;
                                } else if (attributemodifier.id() == ATTACK_SPEED_MODIFIER) {
                                    AttributeInstance att = player.getAttribute(Attributes.ATTACK_SPEED);

                                    if (att != null) {
                                        amount += att.getBaseValue();
                                    }
                                    flag = true;
                                }

                                double d1;

                                if (attributemodifier.operation() !=
                                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE &&
                                        attributemodifier.operation() !=
                                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {

                                    if (entry.getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
                                        d1 = amount * 10.0D;
                                    } else {
                                        d1 = amount;
                                    }
                                } else {
                                    d1 = amount * 100.0D;
                                }

                                if (entry.getKey().value() instanceof SlotAttribute slotAttribute) {

                                    if (amount > 0.0D) {
                                        attributeTooltip.add((Component.translatable(
                                                "curios.modifiers.slots.plus." +
                                                        attributemodifier.operation().id(),
                                                ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                                                Component.translatable(
                                                        "curios.identifier." + slotAttribute.getIdentifier())))
                                                .withStyle(ChatFormatting.BLUE));
                                    } else {
                                        d1 = d1 * -1.0D;
                                        attributeTooltip.add((Component.translatable(
                                                "curios.modifiers.slots.take." +
                                                        attributemodifier.operation().id(),
                                                ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                                                Component.translatable(
                                                        "curios.identifier." + slotAttribute.getIdentifier())))
                                                .withStyle(ChatFormatting.RED));
                                    }
                                } else if (flag) {
                                    attributeTooltip.add(
                                            (Component.literal(" ")).append(Component.translatable(
                                                            "attribute.modifier.equals." + attributemodifier.operation().id(),
                                                            ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                                                            Component.translatable(entry.getKey().value().getDescriptionId())))
                                                    .withStyle(ChatFormatting.DARK_GREEN));
                                } else if (amount > 0.0D) {
                                    attributeTooltip.add((Component.translatable(
                                            "attribute.modifier.plus." + attributemodifier.operation().id(),
                                            ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                                            Component.translatable(entry.getKey().value().getDescriptionId())))
                                            .withStyle(ChatFormatting.BLUE));
                                } else if (amount < 0.0D) {
                                    d1 = d1 * -1.0D;
                                    attributeTooltip.add((Component.translatable(
                                            "attribute.modifier.take." + attributemodifier.operation().id(),
                                            ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                                            Component.translatable(entry.getKey().value().getDescriptionId())))
                                            .withStyle(ChatFormatting.RED));
                                }
                            }
                        }
                    }
                }
                optionalCurio.ifPresent(curio -> {
                    List<Component> actualAttributeTooltips =
                            curio.getAttributesTooltip(attributeTooltip);

                    if (!actualAttributeTooltips.isEmpty()) {
                        tooltip.addAll(actualAttributeTooltips);
                    }
                });

                if (optionalCurio.isEmpty()) {
                    tooltip.addAll(attributeTooltip);
                }
            }
        }
    }
}
