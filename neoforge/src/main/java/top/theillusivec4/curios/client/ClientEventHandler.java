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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.common.util.AttributeUtil;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.common.network.client.CPacketOpenCurios;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ClientEventHandler {
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (KeyRegistry.openCurios.consumeClick() && Minecraft.getInstance().isWindowActive())
            PacketDistributor.sendToServer(new CPacketOpenCurios(ItemStack.EMPTY));
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        var player = event.getEntity();

        if (stack.isEmpty())
            return;

        var optional = CuriosApi.getCurio(stack);

        if (optional.isEmpty())
            return;

        var curio = optional.get();

        var tooltip = event.getToolTip();

        var tags = Set.copyOf((player != null ? CuriosApi.getItemStackSlots(stack, player) : CuriosApi.getItemStackSlots(stack, FMLLoader.getDist() == Dist.CLIENT)).keySet());

        if (tags.contains("curio"))
            tags = Set.of("curio");

        var slots = new ArrayList<>(tags);

        if (slots.isEmpty())
            return;

        var slotsTooltip = Component.translatable("curios.tooltip.slot").append(" ").withStyle(ChatFormatting.GOLD);

        for (int j = 0; j < slots.size(); j++) {
            var key = "curios.identifier." + slots.get(j);
            var type = Component.translatable(key);

            if (j < slots.size() - 1)
                type = type.append(", ");

            type = type.withStyle(ChatFormatting.YELLOW);

            slotsTooltip.append(type);
        }

        tooltip.addAll(1, curio.getSlotsTooltip(Arrays.asList(slotsTooltip)));

        List<Component> attributesTooltip = new ArrayList<>();

        for (String identifier : slots) {
            Multimap<Holder<Attribute>, AttributeModifier> attributes = CuriosApi.getAttributeModifiers(new SlotContext(identifier, player, 0, false, true), ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, identifier), stack);

            if (attributes.isEmpty())
                continue;

            attributesTooltip.add(Component.empty());
            attributesTooltip.add(Component.translatable("curios.modifiers." + identifier).withStyle(ChatFormatting.GOLD));

            if (player != null)
                AttributeUtil.applyTextFor(stack, attributesTooltip::add, attributes, AttributeTooltipContext.of(player, event.getContext(), event.getFlags()));
        }

        tooltip.addAll(2, curio.getAttributesTooltip(attributesTooltip, event.getContext()));
    }
}