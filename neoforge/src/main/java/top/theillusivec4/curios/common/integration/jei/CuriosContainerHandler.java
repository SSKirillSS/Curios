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

package top.theillusivec4.curios.common.integration.jei;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.Rect2i;
import top.theillusivec4.curios.client.gui.CuriosScreen;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CuriosContainerHandler implements IGuiContainerHandler<CuriosScreen> {

    @Override
    @Nonnull
    public List<Rect2i> getGuiExtraAreas(CuriosScreen containerScreen) {
        LocalPlayer player = containerScreen.getMinecraft().player;

        if (player != null) {
            List<Rect2i> areas = new ArrayList<>();
            int left = containerScreen.getGuiLeft() - containerScreen.panelWidth;
            int top = containerScreen.getGuiTop();

            List<Integer> list = containerScreen.getMenu().grid;
            int height = 0;

            if (!list.isEmpty()) {
                height = list.getFirst() * 18 + 14;

                if (containerScreen.getMenu().hasCosmetics) {
                    areas.add(new Rect2i(containerScreen.getGuiLeft() - 30, top - 34, 28, 34));
                }
            }
            areas.add(new Rect2i(left, top, containerScreen.panelWidth, height));
            return areas;
        } else {
            return Collections.emptyList();
        }
    }
}

