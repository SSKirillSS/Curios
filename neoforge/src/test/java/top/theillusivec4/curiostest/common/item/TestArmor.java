package top.theillusivec4.curiostest.common.item;

import java.util.UUID;
import javax.annotation.Nonnull;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import top.theillusivec4.curios.api.CuriosApi;

public class TestArmor extends ArmorItem {

  private static final ResourceLocation ARMOR_ID = ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "ARMOR");

  public TestArmor(Holder<ArmorMaterial> pMaterial, Type pType, Properties pProperties) {
    super(pMaterial, pType, pProperties);
  }

  @Nonnull
  @Override
  public ItemAttributeModifiers getAttributeModifiers(@Nonnull ItemStack stack) {
    ItemAttributeModifiers modifiers = super.getAttributeModifiers(stack);
    modifiers = CuriosApi.withSlotModifier(modifiers, "ring", ARMOR_ID, 1,
        AttributeModifier.Operation.ADD_VALUE, EquipmentSlotGroup.bySlot(this.type.getSlot()));
    modifiers = CuriosApi.withSlotModifier(modifiers, "necklace", ARMOR_ID, -3,
        AttributeModifier.Operation.ADD_VALUE, EquipmentSlotGroup.bySlot(this.type.getSlot()));
    return modifiers;
  }
}
