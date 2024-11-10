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

package top.theillusivec4.curios.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class CuriosLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private final RenderLayerParent<S, M> renderLayerParent;

    public CuriosLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);

        this.renderLayerParent = renderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, S renderState, float v, float v1) {
//        poseStack.pushPose();
//        CuriosApi.getCuriosInventory(renderState.entity)
//                .ifPresent(handler -> handler.getCurios().forEach((id, stacksHandler) -> {
//                    IDynamicStackHandler stackHandler = stacksHandler.getStacks();
//                    IDynamicStackHandler cosmeticStacksHandler = stacksHandler.getCosmeticStacks();
//
//                    for (int i = 0; i < stackHandler.getSlots(); i++) {
//                        ItemStack stack = cosmeticStacksHandler.getStackInSlot(i);
//                        boolean cosmetic = true;
//                        NonNullList<Boolean> renderStates = stacksHandler.getRenders();
//                        boolean renderable = renderStates.size() > i && renderStates.get(i);
//
//                        if (stack.isEmpty() && renderable) {
//                            stack = stackHandler.getStackInSlot(i);
//                            cosmetic = false;
//                        }
//
//                        if (!stack.isEmpty()) {
//                            SlotContext slotContext = new SlotContext(id, renderState.entity, i, cosmetic, renderable);
//                            ItemStack finalStack = stack;
//                            CuriosRendererRegistry.getRenderer(stack.getItem()).ifPresent(
//                                    renderer -> renderer
//                                            .render(finalStack, slotContext, poseStack, renderLayerParent,
//                                                    multiBufferSource, light, limbSwing, limbSwingAmount, renderState.partialTick,
//                                                    renderState.ageInTicks, renderState.netHeadYaw, renderState.headPitch));
//                        }
//                    }
//                }));
//        poseStack.popPose();
    }
}
