package com.hongyuwu.careerchronicle.skill.effect;

import com.hongyuwu.careerchronicle.skill.effect.ops.AoeOp;
import com.hongyuwu.careerchronicle.skill.effect.ops.ApplyEffectOp;
import com.hongyuwu.careerchronicle.skill.effect.ops.ArrowOp;
import com.hongyuwu.careerchronicle.skill.effect.ops.DamageOp;
import com.hongyuwu.careerchronicle.skill.effect.ops.DashOp;
import com.hongyuwu.careerchronicle.skill.effect.ops.HealOp;
import com.hongyuwu.careerchronicle.skill.effect.ops.IgniteOp;
import com.hongyuwu.careerchronicle.skill.effect.ops.KnockbackOp;
import com.hongyuwu.careerchronicle.skill.effect.ops.ProjectileOp;
import com.hongyuwu.careerchronicle.skill.effect.ops.ResourceOp;
import com.hongyuwu.careerchronicle.skill.effect.ops.ShieldOp;
import com.hongyuwu.careerchronicle.skill.effect.ops.SummonOp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class EffectOpRegistry {
    private static final Map<String, EffectOp> OPS = new LinkedHashMap<>();

    private EffectOpRegistry() {
    }

    public static void register(String name, EffectOp op) {
        if (OPS.containsKey(name)) {
            throw new IllegalStateException("Duplicate EffectOp: " + name);
        }
        OPS.put(name, op);
    }

    public static EffectOp get(String name) {
        return OPS.get(name);
    }

    public static boolean exists(String name) {
        return OPS.containsKey(name);
    }

    public static Set<String> allNames() {
        return Set.copyOf(OPS.keySet());
    }

    public static void registerBuiltins() {
        register("damage", new DamageOp());
        register("apply_effect", new ApplyEffectOp());
        register("heal", new HealOp());
        register("knockback", new KnockbackOp());
        register("projectile", new ProjectileOp());
        register("aoe", new AoeOp());
        register("dash", new DashOp());
        register("shield", new ShieldOp());
        register("resource", new ResourceOp());
        register("ignite", new IgniteOp());
        register("arrow", new ArrowOp());
        register("summon", new SummonOp());
    }

    public static void clearForTesting() {
        OPS.clear();
    }
}
