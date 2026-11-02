package com.sf.friendwinememe.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sf.friendwinememe.Config;
import com.sf.friendwinememe.FriendWineMemeMod;
import com.sf.friendwinememe.client.DisguiseShadowCache;
import com.sf.friendwinememe.entity.BlockDisguiseEntity;
import com.sf.friendwinememe.init.FriendWineMemeModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class BlockDisguiseRenderer extends EntityRenderer<BlockDisguiseEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public BlockDisguiseRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(BlockDisguiseEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int light) {
        BlockState state = entity.getBlockState();
        if (state == null || state.isAir()) return;

        pose.pushPose();
        pose.translate(-0.5D, 0.0D, -0.5D);

        com.sf.friendwinememe.client.OrbitHelper.applyBlockOrbit(pose, entity.getId(),
                entity.getX(), entity.getZ());

        float[] anim = FriendWineMemeModNetwork.getAnimProgress(entity.getId());
        if (anim != null) {
            long elapsed = (long) anim[0];
            float maxH = (float) Config.getMaxHeight();
            float minH = (float) Config.getMinHeight();
            long cycle = FriendWineMemeModNetwork.cycleMs();
            float t = (elapsed % cycle) / (float) cycle;

            float yScale = FriendWineMemeModNetwork.scaleY(t, maxH, minH);
            float xzScale = 1.0F + (maxH - yScale) * 0.5F;
            float yOffset = (maxH - yScale) * 0.15F;

            pose.translate(0.5D, 0.0D, 0.5D);
            pose.translate(0.0D, yOffset, 0.0D);
            pose.scale(xzScale, yScale, xzScale);
            pose.translate(-0.5D, 0.0D, -0.5D);

            if (Config.isRotate()) {
                float deg = (float) (elapsed / 1000.0 * Config.getRotateSpeed());
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(deg));
                pose.translate(-0.5D, -0.5D, -0.5D);
            }
        }

        BlockEntity shadow = DisguiseShadowCache.get(entity);
        if (shadow != null) {
            var dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
            var renderer = dispatcher.getRenderer(shadow);
            if (renderer != null) {
                try {
                    renderer.render(shadow, partialTick, pose, buffer, light, OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                    return;
                } catch (Exception ex) {
                    FriendWineMemeMod.LOGGER.error("null (entityId={}): {}",
                            entity.getId(), ex.toString());
                    ex.printStackTrace();
                    DisguiseShadowCache.clear(entity.getId());
                }
            }
        }

        if (state.getRenderShape() == RenderShape.MODEL) {
            blockRenderer.renderSingleBlock(state, pose, buffer, light,
                    OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(BlockDisguiseEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    }
}