package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.network.FxOpSpec;
import com.hongyuwu.careerchronicle.network.FxParams;
import com.hongyuwu.careerchronicle.network.S2CPlaySkillFxPacket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/** Client-side entry point for the v2 fx packet (0.4-05a). */
public final class FxClientDispatch {
    private static final Set<String> WARNED_UNKNOWN_OPS = ConcurrentHashMap.newKeySet();

    private FxClientDispatch() {
    }

    public static void play(S2CPlaySkillFxPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            List<FxOpSpec> ops = packet.ops();
            if (ops.isEmpty()) {
                // Legacy skills with no fx block yet (10 executor skills, 0.4-07 exemption
                // list) and any skill this packet couldn't resolve fall back to the
                // pre-0.4-05a hardcoded/heuristic renderer.
                SkillFxRenderer.play(packet.skillId(), packet.fxType(), packet.origin(), packet.target(), packet.particleMultiplier());
                return;
            }
            ClientLevel level = minecraft.level;
            if (level == null) {
                return;
            }
            FxPlayContext ctx = new FxPlayContext(
                    level, packet.skillId(), packet.fxType(), packet.casterId(), packet.seed(),
                    packet.origin(), packet.target(), packet.particleMultiplier());
            for (FxOpSpec spec : ops) {
                FxOp op = FxOpRegistry.get(spec.opId());
                if (op == null) {
                    if (WARNED_UNKNOWN_OPS.add(spec.opId())) {
                        CareerChronicleMod.LOGGER.warn("Unknown FxOp id '{}', skipping.", spec.opId());
                    }
                    continue;
                }
                int delayTicks = FxParams.getInt(spec.params(), "delay_ticks", 0);
                DelayedFxScheduler.schedule(op, ctx, spec.params(), delayTicks);
            }
        });
    }
}
