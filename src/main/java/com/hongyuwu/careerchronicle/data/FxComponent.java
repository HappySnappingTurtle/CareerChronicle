package com.hongyuwu.careerchronicle.data;

import net.minecraft.nbt.CompoundTag;

/**
 * One entry in a skill's {@code fx} component array (0.4-06). {@code op} names
 * a handler registered in {@code FxOpRegistry} (client) / consumed generically
 * by {@code FxDispatcher} (server); {@code when} is the fx phase this
 * component fires on ({@code "cast"}, {@code "hit"}, {@code "entity_hit"});
 * {@code params} carries the op's own fields verbatim, read defensively via
 * {@code FxParams} (missing fields fall back to per-op defaults, unknown
 * fields are ignored) so new fields can be added without breaking older data.
 *
 * <p>{@code key} is an optional disambiguator (引擎审计修复 任务B / A6, 表现引擎全面审计报告_
 * 2026-07-15.md A6): {@code CareerDataParsers.resolveFx}'s {@code fx_template} merge is keyed by
 * {@code op+"|"+when} when {@code key} is absent, so two components sharing both {@code op} and
 * {@code when} (e.g. ground_slam's two {@code particles}@{@code cast} entries, distinguished only
 * by their own {@code delay_ticks} param) would silently collapse to just the last one the moment
 * that skill also references a {@code fx_template} -- {@code null} normally, set this to a unique
 * string per such component and the merge uses it as the key instead.
 */
public record FxComponent(String op, String when, String key, CompoundTag params) {
    public FxComponent {
        if (op == null || op.isBlank()) {
            throw new RegistryValidationException("fx component missing required field 'op'");
        }
        if (when == null || when.isBlank()) {
            throw new RegistryValidationException("fx component missing required field 'when'");
        }
        key = (key == null || key.isBlank()) ? null : key;
        params = params == null ? new CompoundTag() : params;
    }

    /** Convenience constructor for the overwhelming majority of components that don't declare an
     * explicit {@code key} -- keeps every pre-A6 call site (every legacy-fx-expansion branch in
     * {@code CareerDataParsers}, and the existing test suite) source-compatible unchanged. */
    public FxComponent(String op, String when, CompoundTag params) {
        this(op, when, null, params);
    }
}
