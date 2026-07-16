package com.hongyuwu.careerchronicle.network;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.config.ModConfig;
import com.hongyuwu.careerchronicle.data.CareerRegistry;
import com.hongyuwu.careerchronicle.data.FxComponent;
import com.hongyuwu.careerchronicle.data.SkillDef;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * Single business-logic owner of "a skill just did something, tell nearby
 * clients how it should look/sound" (0.4-05a, component-array Schema since
 * 0.4-06). {@link #toOps} is a pure function (fx component list + fxType in,
 * FxOpSpec list out) so it is JUnit-testable without any Minecraft bootstrap;
 * everything network/registry-shaped lives in {@link #send}, which
 * {@link com.hongyuwu.careerchronicle.network.NetworkHandler#playSkillFx}
 * forwards to unchanged — every existing call site (legacy executors,
 * projectile/arrow hit handlers) gains casterId/seed/ops for free.
 */
public final class FxDispatcher {
    private static final double NEAR_RADIUS = 32.0D;
    private static final ResourceLocation BASIC_ATTACK_ID =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "basic_attack");
    private static final String BASIC_ATTACK_FX_TYPE = "attack";

    private FxDispatcher() {
    }

    /** New cast-time hook, called from CareerSkillService's component-effects success branch. */
    public static void dispatchCast(ServerPlayer caster, SkillDef skill) {
        Vec3 origin = caster.position().add(0.0D, caster.getEyeHeight() * 0.5D, 0.0D);
        Vec3 target = origin.add(caster.getLookAngle().scale(2.5D));
        sendInternal(caster, skill.id(), "cast", origin, target, toOps(skill.fx(), "cast"));
    }

    /** 阶段3-任务6-设计文档-普通攻击动作系统.md §三: called by {@link BasicAttackAnimationEvents} once
     * per basic (non-skill) melee/ranged attack that resolved to a known weapon category. Not a
     * skill -- there is no {@link SkillDef} to read an fx component array from, so this builds a
     * single {@code anim} {@link FxOpSpec} directly, reusing the exact same wire format/dispatch
     * path (v2 packet, casterId, 32-block broadcast) that skill fx already goes through. */
    public static void dispatchBasicAttack(ServerPlayer attacker, ResourceLocation animId) {
        Vec3 origin = attacker.position().add(0.0D, attacker.getEyeHeight() * 0.5D, 0.0D);
        Vec3 target = origin.add(attacker.getLookAngle().scale(2.5D));
        CompoundTag params = new CompoundTag();
        params.putString("id", animId.toString());
        // upper_body=false: every basic-attack animation (阶段3-任务6-设计文档 §五) drives leg/knee
        // tracks too ("重心转移先于上肢发力"), unlike the upper-body-only cast animations.
        params.putBoolean("upper_body", false);
        params.putFloat("speed", 1.0F);
        // 引擎审计修复 任务B / A7 (表现引擎全面审计报告_2026-07-15.md A7): lets the client-side
        // AnimFxOp/CustomAnimationPlayer arbitration tell this apart from a skill's own anim
        // component (which never sets 'source') -- a basic attack landing mid-cast must not hard-cut
        // a skill's animation back to frame 0.
        params.putString("source", "basic_attack");
        List<FxOpSpec> ops = List.of(new FxOpSpec("anim", params));
        sendInternal(attacker, BASIC_ATTACK_ID, BASIC_ATTACK_FX_TYPE, origin, target, ops);
    }

    /** Unified send used by every existing NetworkHandler.playSkillFx caller. */
    public static void send(ServerPlayer caster, ResourceLocation skillId, String fxType, Vec3 origin, Vec3 target) {
        List<FxOpSpec> ops = CareerRegistry.snapshot().skill(skillId)
                .map(skill -> toOps(skill.fx(), fxType))
                .orElse(List.of());
        sendInternal(caster, skillId, fxType, origin, target, ops);
    }

    private static void sendInternal(ServerPlayer caster, ResourceLocation skillId, String fxType,
                                      Vec3 origin, Vec3 target, List<FxOpSpec> ops) {
        long seed = caster.level().getRandom().nextLong();
        float multiplier = ModConfig.SKILL_PARTICLE_MULTIPLIER.get().floatValue();
        TestHooks.increment(fxType, ops.size());
        if (!NetworkHandler.hasLiveConnection(caster)) {
            // A caster with no real network connection (GameTest's
            // makeMockServerPlayerInLevel()) has nothing to send to anyway;
            // TestHooks has already recorded the dispatch above.
            return;
        }
        try {
            // 0.4-07: belt-and-suspenders alongside the hasLiveConnection()
            // check above -- see NetworkHandler.sendToPlayer's Javadoc for the
            // real GameTest evidence of why a pre-check alone was once
            // assumed sufficient and later proven not to be for every caller.
            // A purely cosmetic fx packet failing to reach one recipient must
            // never propagate up through skill-cast logic.
            NetworkHandler.CHANNEL.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(
                    origin.x, origin.y, origin.z, NEAR_RADIUS, caster.level().dimension()
            )), new S2CPlaySkillFxPacket(skillId, fxType, caster.getId(), seed, origin, target, multiplier, ops));
        } catch (RuntimeException exception) {
            com.hongyuwu.careerchronicle.CareerChronicleMod.LOGGER.debug(
                    "Skipped fx dispatch for {} ({}): {}", skillId, fxType, exception.toString());
        }
    }

    /**
     * Pure fx-components-to-wire-ops translation (0.4-06). Each component's
     * {@code op}/{@code params} are forwarded verbatim to an {@link FxOpSpec}
     * when its {@code when} matches the requested fxType — the generic
     * component array replaces the old hardcoded sound/particles/shake
     * three-branch translation (that logic now lives once, in
     * {@code CareerDataParsers.expandLegacyFx}, as the legacy-format
     * conversion that produces these same components for older JSON).
     */
    public static List<FxOpSpec> toOps(List<FxComponent> components, String fxType) {
        List<FxOpSpec> ops = new ArrayList<>();
        if (components == null || fxType == null) {
            return ops;
        }
        for (FxComponent component : components) {
            if (fxType.equals(component.when())) {
                ops.add(new FxOpSpec(component.op(), component.params()));
            }
        }
        // Unknown fxType: no matches, no throw (forward-compatible with future fx types).
        return List.copyOf(ops);
    }

    /** Headless-observable test hooks (0.4-05a M0 fix for the "did it actually send a packet" gap). */
    public static final class TestHooks {
        private static final Map<String, Integer> SENT_COUNTS = new ConcurrentHashMap<>();
        // 0.4-07: separate from SENT_COUNTS (dispatch attempts) -- this counts
        // the total number of FxOpSpec entries actually carried across all
        // dispatches for a fxType, so GameTest can assert "casting a castable
        // skill carries >=1 real fx op", not just "a dispatch happened" (which
        // is true even for a skill with a completely empty fx list, since
        // dispatchCast() always calls sendInternal unconditionally).
        private static final Map<String, Integer> OP_COUNTS = new ConcurrentHashMap<>();

        private TestHooks() {
        }

        public static void clearForTesting() {
            SENT_COUNTS.clear();
            OP_COUNTS.clear();
        }

        static void increment(String fxType, int opCount) {
            SENT_COUNTS.merge(fxType, 1, Integer::sum);
            OP_COUNTS.merge(fxType, opCount, Integer::sum);
        }

        public static int sentCount(String fxType) {
            return SENT_COUNTS.getOrDefault(fxType, 0);
        }

        public static int opCount(String fxType) {
            return OP_COUNTS.getOrDefault(fxType, 0);
        }
    }
}
