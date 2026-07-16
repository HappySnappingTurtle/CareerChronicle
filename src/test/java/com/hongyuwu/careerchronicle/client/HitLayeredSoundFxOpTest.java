package com.hongyuwu.careerchronicle.client;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 0.4-07 C group: HitLayeredSoundFxOp.layeredPitch. */
class HitLayeredSoundFxOpTest {

    // C1/C2/C3: 1000 samples all land in [0.85, 1.3], including near the rand=0 edge that would
    // otherwise produce ~1.818 unclamped.
    @Test
    void layeredPitch_alwaysWithinClampedRange() {
        RandomSource random = RandomSource.create(42L);
        for (int i = 0; i < 1000; i++) {
            float pitch = HitLayeredSoundFxOp.layeredPitch(random);
            assertTrue(pitch >= 0.85F && pitch <= 1.3F, "pitch out of range: " + pitch);
        }
    }

    // C4: two independent calls (base layer, accent layer) usually differ -- proves the two
    // layers are not accidentally sharing a single sampled value. Note: pitch=k/(rand*a+b) clamped
    // to 1.3 means every rand below ~0.365 collapses to the *same* clamped value (1.3), so some
    // coincidental collisions are expected by construction (~13% of pairs, both landing in that
    // range) -- the threshold here (50%) is set well above that natural rate and is meant to catch
    // a real bug (e.g. both layers accidentally reusing one sampled value, which would push the
    // collision rate towards 100%), not to demand near-zero collisions.
    @Test
    void layeredPitch_independentCallsUsuallyDiffer() {
        RandomSource random = RandomSource.create(7L);
        int sameCount = 0;
        int total = 1000;
        for (int i = 0; i < total; i++) {
            float a = HitLayeredSoundFxOp.layeredPitch(random);
            float b = HitLayeredSoundFxOp.layeredPitch(random);
            if (Float.compare(a, b) == 0) {
                sameCount++;
            }
        }
        assertTrue(sameCount < total / 2, "too many identical base/accent pitches: " + sameCount + "/" + total);
    }
}
