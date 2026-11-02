package com.sf.friendwinememe.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.entity.Entity;

public class OrbitHelper {

    public static void applyOrbit(PoseStack pose, Entity entity) {
    }

    public static void applyBlockOrbit(PoseStack pose, int entityId,
                                       double entityX, double entityZ) {
        ClientRangeMode.State state = ClientRangeMode.INSTANCE.findForBlock(entityX, entityZ);
        if (state == null || !state.orbit || !state.blockOrbit) return;

        double angle = ClientRangeMode.INSTANCE.getOrbitAngle(state.centerId);
        double dx = state.centerPos.x - entityX;
        double dz = state.centerPos.z - entityZ;

        pose.translate(dx, 0, dz);
        pose.mulPose(Axis.YP.rotationDegrees((float) -angle));
        pose.translate(-dx, 0, -dz);
    }
}