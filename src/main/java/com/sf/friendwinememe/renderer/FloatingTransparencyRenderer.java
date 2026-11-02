package com.sf.friendwinememe.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sf.friendwinememe.entity.FloatingTransparencyEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class FloatingTransparencyRenderer extends EntityRenderer<FloatingTransparencyEntity> {

    private final ItemRenderer itemRenderer;

    public FloatingTransparencyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.itemRenderer = ctx.getItemRenderer();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(FloatingTransparencyEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int light) {
        ItemStack stack = entity.getRenderStack();
        if (stack.isEmpty()) return;

        pose.pushPose();
        float rotation = (System.currentTimeMillis() % 6000L) / 6000.0F * 360.0F;
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, light,
                OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(), entity.getId());
        pose.popPose();

        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public boolean shouldShowName(FloatingTransparencyEntity entity) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(FloatingTransparencyEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    }
}