package com.sf.friendwinememe.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRangeMode {
    public static final ClientRangeMode INSTANCE = new ClientRangeMode();

    public static class State {
        public final int centerId;
        public final long startMs;
        public final long durationMs;
        public final int radius;
        public final boolean orbit;
        public final double orbitSpeed;
        public final boolean affectBlocks;
        public final boolean blockOrbit;
        public final int layerY;
        public final Vec3 centerPos;

        State(int centerId, long startMs, long durationMs, int radius,
              boolean orbit, double orbitSpeed, boolean affectBlocks,
              boolean blockOrbit, int layerY, Vec3 centerPos) {
            this.centerId = centerId;
            this.startMs = startMs;
            this.durationMs = durationMs;
            this.radius = radius;
            this.orbit = orbit;
            this.orbitSpeed = orbitSpeed;
            this.affectBlocks = affectBlocks;
            this.blockOrbit = blockOrbit;
            this.layerY = layerY;
            this.centerPos = centerPos;
        }
    }

    private final Map<Integer, State> STATES = new ConcurrentHashMap<>();

    public void start(int centerId, long startMs, long durationMs, int radius,
                      boolean orbit, double orbitSpeed, boolean affectBlocks,
                      boolean blockOrbit, int layerY, Vec3 centerPos) {
        STATES.put(centerId, new State(centerId, startMs, durationMs, radius,
                orbit, orbitSpeed, affectBlocks, blockOrbit, layerY, centerPos));
    }

    public void stop(int centerId) { STATES.remove(centerId); }
    public void stopAll() { STATES.clear(); }

    public double getOrbitAngle(int centerId) {
        State s = STATES.get(centerId);
        if (s == null) return 0;
        long elapsed = System.currentTimeMillis() - s.startMs;
        return (elapsed / 1000.0) * s.orbitSpeed;
    }

    public State findForBlock(double x, double z) {
        for (State s : STATES.values()) {
            double dx = x - s.centerPos.x;
            double dz = z - s.centerPos.z;
            if (dx * dx + dz * dz <= (double) s.radius * s.radius) {
                return s;
            }
        }
        return null;
    }

    public State findFor(Entity entity) {
        return findForBlock(entity.getX(), entity.getZ());
    }

    public boolean isEmpty() { return STATES.isEmpty(); }
}