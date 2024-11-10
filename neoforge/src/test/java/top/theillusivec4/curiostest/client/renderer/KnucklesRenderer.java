//package top.theillusivec4.curiostest.client.renderer;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.model.EntityModel;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.client.renderer.entity.EntityRenderer;
//import net.minecraft.client.renderer.entity.EntityRendererProvider;
//import net.minecraft.client.renderer.entity.RenderLayerParent;
//import net.minecraft.client.renderer.entity.state.EntityRenderState;
//import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.item.ItemStack;
//import top.theillusivec4.curios.api.SlotContext;
//import top.theillusivec4.curios.api.client.ICurioRenderer;
//import top.theillusivec4.curiostest.CuriosTest;
//import top.theillusivec4.curiostest.client.CuriosLayerDefinitions;
//import top.theillusivec4.curiostest.client.model.KnucklesModel;
//
//public class KnucklesRenderer<T extends LivingEntity, S extends LivingEntityRenderState> extends EntityRenderer<T, S> implements ICurioRenderer {
//    private static final ResourceLocation KNUCKLES_TEXTURE = ResourceLocation.fromNamespaceAndPath(CuriosTest.MODID,
//            "textures/entity/knuckles.png");
//
//    private final KnucklesModel model;
//
//    public KnucklesRenderer(EntityRendererProvider.Context context) {
//        super(context);
//
//        this.model = new KnucklesModel(Minecraft.getInstance().getEntityModels().bakeLayer(CuriosLayerDefinitions.KNUCKLES));
//    }
//
//    @Override
//    public <H extends EntityRenderState, M extends EntityModel<H>> void render(ItemStack stack, SlotContext slotContext, PoseStack matrixStack, RenderLayerParent<H, M> renderLayerParent, MultiBufferSource renderTypeBuffer, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
////        LivingEntity entity = slotContext.entity();
////        this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
////        ICurioRenderer.followBodyRotations(entity, this.model);
////        VertexConsumer vertexconsumer = ItemRenderer
////                .getArmorFoilBuffer(renderTypeBuffer, RenderType.armorCutoutNoCull(KNUCKLES_TEXTURE), stack.hasFoil());
////        this.model
////                .renderToBuffer(matrixStack, vertexconsumer, light, OverlayTexture.NO_OVERLAY);
//    }
//
//    @Override
//    public S createRenderState() {
//        return null;
//    }
//
//    @Override
//    public void extractRenderState(T entity, S state, float partialTicks) {
//        super.extractRenderState(entity, state, partialTicks);
//    }
//}
