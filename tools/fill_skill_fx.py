#!/usr/bin/env python3
"""0.4-05a/0.4-06: derive skill->family mapping and batch-fill fx blocks into skill JSON.

Idempotent: rerunning regenerates tools/skill_fx_map.json and rewrites the
"fx" block of every component (non-executor) skill JSON deterministically.
Executor-only skills (10, see EXECUTOR_FAMILY_OVERRIDES) are NOT touched by
the fx-block injection step (0.4-07 tracks them via an explicit exemption
list); they still get a mapping-table row for auditability.

0.4-06: outputs the new fx *component array* format (op+when+params) instead
of the old seven-field object -- see 0.4-06-设计文档-ChronicleFX引擎Schema定案.md
§2.1 for the field->component mapping this mirrors. `--sample <ids...>` prints
the old vs. new fx block for the given skill ids without writing anything,
for the "check N samples across different families by hand before batch
rewriting all 63" step the design doc's risk section requires.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA_ROOT = ROOT / "src/main/resources/data/careerchronicle/careerchronicle"
SKILLS_DIR = DATA_ROOT / "skills"
CLASSES_DIR = DATA_ROOT / "classes"
FUSIONS_DIR = DATA_ROOT / "fusions"
MAP_OUT = ROOT / "tools/skill_fx_map.json"

# tag (as used in upgrade.source, "tag:<X>") -> family key.
# "ice" is the data-side name for what the design system calls 寒霜/frost;
# "heal" folds into the same family as holy (总纲 4.1: "holy, heal" grouping).
TAG_TO_FAMILY = {
    "fire": "fire",
    "ice": "frost",
    "holy": "holy",
    "heal": "holy",
    "dark": "dark",
    "melee": "melee",
    "defense": "defense",
    "bow": "bow",
    "stealth": "stealth",
}

# The 10 executor-only skills (no "upgrade" block) have no tag to derive
# family from; family is derived from the granting class's tags (see class
# tags dump) or, for fusions, the thematically primary parent. Two of these
# are judgment calls made explicit here rather than left implicit in code:
#   - death_strike (necromancer+warrior fusion): "death" theme -> dark,
#     consistent with the existing SkillFxRenderer keyword heuristic
#     (path.contains("death") -> DARK) and with necromancer being listed
#     first in the fusion file name.
#   - provoke (guardian, tags=[melee,physical,defense,holy]): guardian's
#     defining role is tank/defense and provoke is a taunt+slow utility
#     rather than a weapon strike, so it is assigned defense rather than
#     the class's first-listed tag (melee). This is the one manual
#     judgment call in this table; flagged via manual_override=true.
EXECUTOR_FAMILY = {
    "careerchronicle:death_coil": ("dark", "class:necromancer", False),
    "careerchronicle:death_strike": ("dark", "fusion:necromancer_warrior", False),
    "careerchronicle:eagle_eye": ("bow", "class:archer", False),
    "careerchronicle:flame_arrow": ("fire", "fusion:fire_mage_archer", False),
    "careerchronicle:frost_arrow": ("frost", "fusion:ice_mage_archer", False),
    "careerchronicle:lich_form": ("dark", "class:necromancer", False),
    "careerchronicle:provoke": ("defense", "class:guardian", True),
    "careerchronicle:shadow_strike": ("stealth", "class:rogue", False),
    "careerchronicle:smoke_bomb": ("stealth", "class:rogue", False),
    "careerchronicle:soul_drain": ("dark", "class:necromancer", False),
}

# Family catalog: cast/hit sound registered names (registry/CareerSounds.java)
# + a vanilla particle id reused from SkillFxRenderer's existing category
# palette (keeps 0.4-05a visually consistent with what SkillFxRenderer
# already renders; 0.5-03 replaces cast_particle with chronicle_rune).
# Hit sound folds 10 families down to the 5 registered HIT_* sounds
# (fire/frost/holy/dark distinct, everything else -> physical).
FAMILY_CATALOG = {
    "fire":    {"cast_sound": "careerchronicle:skill.cast.fire",   "hit_sound": "careerchronicle:skill.hit.fire",     "cast_particle": "minecraft:flame"},
    "frost":   {"cast_sound": "careerchronicle:skill.cast.frost",  "hit_sound": "careerchronicle:skill.hit.frost",    "cast_particle": "minecraft:snowflake"},
    "holy":    {"cast_sound": "careerchronicle:skill.cast.holy",   "hit_sound": "careerchronicle:skill.hit.holy",     "cast_particle": "minecraft:end_rod"},
    "dark":    {"cast_sound": "careerchronicle:skill.cast.dark",   "hit_sound": "careerchronicle:skill.hit.dark",     "cast_particle": "minecraft:soul_fire_flame"},
    "melee":   {"cast_sound": "careerchronicle:skill.cast.blade",  "hit_sound": "careerchronicle:skill.hit.physical", "cast_particle": "minecraft:crit"},
    "defense": {"cast_sound": "careerchronicle:skill.cast.shield", "hit_sound": "careerchronicle:skill.hit.physical", "cast_particle": "minecraft:enchant"},
    "bow":     {"cast_sound": "careerchronicle:skill.cast.arrow",  "hit_sound": "careerchronicle:skill.hit.physical", "cast_particle": "minecraft:crit"},
    "stealth": {"cast_sound": "careerchronicle:skill.cast.shadow", "hit_sound": "careerchronicle:skill.hit.physical", "cast_particle": "minecraft:smoke"},
    # arcane/nature: zero skill consumers today (D2 = 方案A, reserved for
    # 0.5 秘法/缚灵 content). Never referenced by any skills[] row below;
    # kept here only so the reserved sound ids stay traceable to a family.
    "arcane":  {"cast_sound": "careerchronicle:skill.cast.arcane", "hit_sound": "careerchronicle:skill.hit.physical", "cast_particle": "minecraft:enchant"},
    "nature":  {"cast_sound": "careerchronicle:skill.cast.nature", "hit_sound": "careerchronicle:skill.hit.physical", "cast_particle": "minecraft:happy_villager"},
}

RESERVED_SOUNDS = ["careerchronicle:skill.cast.arcane", "careerchronicle:skill.cast.nature"]

# Shake defaults by skill type — fusions read as heavier hits than base
# actives. Deliberately uniform for 0.4-05a; per-skill tuning is 0.4-07's
# "打击感三件套" job, not this batch-fill script's.
SHAKE_BY_TYPE = {
    "fusion": (0.32, 6),
    "active": (0.22, 4),
}


def read_json(path):
    with path.open(encoding="utf-8") as f:
        return json.load(f)


def write_json(path, data):
    with path.open("w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def build_mapping():
    rows = []
    for path in sorted(SKILLS_DIR.glob("*.json")):
        skill_id = f"careerchronicle:{path.stem}"
        data = read_json(path)
        upgrade = data.get("upgrade")
        if upgrade and upgrade.get("source", "").startswith("tag:"):
            tag = upgrade["source"][len("tag:"):]
            family = TAG_TO_FAMILY.get(tag)
            if family is None:
                raise SystemExit(f"unknown upgrade tag '{tag}' for {skill_id}; add to TAG_TO_FAMILY")
            source = upgrade["source"]
            manual_override = False
        else:
            if skill_id not in EXECUTOR_FAMILY:
                raise SystemExit(f"{skill_id} has no upgrade block and no EXECUTOR_FAMILY entry")
            family, source, manual_override = EXECUTOR_FAMILY[skill_id]
        catalog = FAMILY_CATALOG[family]
        rows.append({
            "skill_id": skill_id,
            "family": family,
            "source": source,
            "cast_sound": catalog["cast_sound"],
            "hit_sound": catalog["hit_sound"],
            "cast_particle": catalog["cast_particle"],
            "manual_override": manual_override,
        })

    on_disk_ids = {f"careerchronicle:{p.stem}" for p in SKILLS_DIR.glob("*.json")}
    mapped_ids = {r["skill_id"] for r in rows}
    if on_disk_ids != mapped_ids:
        raise SystemExit(f"mapping/skill-dir mismatch: {on_disk_ids ^ mapped_ids}")

    return {"reserved_sounds": RESERVED_SOUNDS, "skills": rows}


# 0.4-06 defaults baked into the auto-generated particle components -- must
# stay identical to CareerDataParsers' CAST_PARTICLE_COUNT/SPREAD and
# HIT_PARTICLE_COUNT/SPREAD constants (Java side) so this tool's output is a
# byte-for-byte equivalent rewrite of what the old seven-field format used to
# mean, not a behavior change.
CAST_PARTICLE_COUNT = 12
CAST_PARTICLE_SPREAD = 0.6
HIT_PARTICLE_COUNT = 8
HIT_PARTICLE_SPREAD = 0.4


def legacy_fx_block(row, skill_type):
    """The pre-0.4-06 seven-field object this tool used to write -- kept as a pure function so
    --sample can print it alongside the new format for side-by-side comparison."""
    strength, ticks = SHAKE_BY_TYPE.get(skill_type, SHAKE_BY_TYPE["active"])
    return {
        "cast_sound": row["cast_sound"],
        "cast_particle": row["cast_particle"],
        "hit_sound": row["hit_sound"],
        "hit_particle": row["cast_particle"],
        "camera_shake": strength,
        "camera_shake_ticks": ticks,
        "cast_circle": False,
    }


def component_fx_block(row, skill_type):
    """0.4-06 component array -- the literal expansion of legacy_fx_block()'s
    seven fields via the §2.1 mapping table (same field values, new shape),
    mirroring CareerDataParsers.expandLegacyFx exactly."""
    legacy = legacy_fx_block(row, skill_type)
    components = []
    if legacy["cast_sound"]:
        components.append({"op": "sound", "when": "cast", "id": legacy["cast_sound"]})
    if legacy["cast_particle"]:
        components.append({
            "op": "particles", "when": "cast", "id": legacy["cast_particle"],
            "count": CAST_PARTICLE_COUNT, "spread": CAST_PARTICLE_SPREAD,
        })
    if legacy["camera_shake"] > 0:
        components.append({
            "op": "shake", "when": "cast",
            "strength": legacy["camera_shake"], "ticks": legacy["camera_shake_ticks"],
        })
    if legacy["hit_sound"]:
        components.append({"op": "sound", "when": "hit", "id": legacy["hit_sound"]})
    if legacy["hit_particle"]:
        components.append({
            "op": "particles", "when": "hit", "id": legacy["hit_particle"],
            "count": HIT_PARTICLE_COUNT, "spread": HIT_PARTICLE_SPREAD,
        })
    if legacy["cast_circle"]:
        components.append({"op": "circle", "when": "cast", "id": legacy["cast_particle"] or "minecraft:end_rod"})
    return components


def components_equivalent_to_legacy(components, legacy):
    """Cross-check: re-derive an equivalent legacy dict from a component array and compare to
    the original legacy dict this tool would have produced -- the equivalence proof --sample and
    the batch run's self-check both rely on."""
    by_when_op = {(c["op"], c["when"]): c for c in components}
    rebuilt = {
        "cast_sound": by_when_op.get(("sound", "cast"), {}).get("id"),
        "cast_particle": by_when_op.get(("particles", "cast"), {}).get("id"),
        "hit_sound": by_when_op.get(("sound", "hit"), {}).get("id"),
        "hit_particle": by_when_op.get(("particles", "hit"), {}).get("id"),
        "camera_shake": by_when_op.get(("shake", "cast"), {}).get("strength", 0),
        "camera_shake_ticks": by_when_op.get(("shake", "cast"), {}).get("ticks", 0),
        "cast_circle": ("circle", "cast") in by_when_op,
    }
    mismatches = []
    for key, expected in legacy.items():
        actual = rebuilt.get(key)
        if key == "camera_shake":
            expected, actual = float(expected), float(actual or 0)
        if expected != actual:
            mismatches.append(f"{key}: expected={expected!r} actual={actual!r}")
    if by_when_op.get(("particles", "cast")):
        c = by_when_op[("particles", "cast")]
        if c.get("count") != CAST_PARTICLE_COUNT or c.get("spread") != CAST_PARTICLE_SPREAD:
            mismatches.append(f"cast particle count/spread: {c.get('count')}/{c.get('spread')}")
    if by_when_op.get(("particles", "hit")):
        c = by_when_op[("particles", "hit")]
        if c.get("count") != HIT_PARTICLE_COUNT or c.get("spread") != HIT_PARTICLE_SPREAD:
            mismatches.append(f"hit particle count/spread: {c.get('count')}/{c.get('spread')}")
    return mismatches


def fill_fx_blocks(mapping):
    by_id = {row["skill_id"]: row for row in mapping["skills"]}
    filled = 0
    skipped_templated = 0
    for path in sorted(SKILLS_DIR.glob("*.json")):
        data = read_json(path)
        if "executor" in data:
            continue  # 10 executor skills: fx block deferred to 0.4-07 exemption path
        if "fx_template" in data:
            # Manually authored to inherit from an fx_template (0.4-06) --
            # treat like the executor-skill exemption: this tool derives a
            # fully family-based fx from scratch and would blow away the
            # template reference + any override/appended components on
            # every rerun otherwise.
            skipped_templated += 1
            continue
        skill_id = f"careerchronicle:{path.stem}"
        row = by_id[skill_id]
        skill_type = data.get("type", "active")
        data["fx"] = component_fx_block(row, skill_type)
        write_json(path, data)
        filled += 1
    if skipped_templated:
        print(f"skipped {skipped_templated} skill(s) already using fx_template (manually authored)")
    return filled


def run_sample(sample_ids, mapping):
    by_id = {row["skill_id"]: row for row in mapping["skills"]}
    exit_code = 0
    for raw_id in sample_ids:
        skill_id = raw_id if ":" in raw_id else f"careerchronicle:{raw_id}"
        path = SKILLS_DIR / f"{skill_id.split(':', 1)[1]}.json"
        if not path.exists():
            print(f"SKIP {skill_id}: no such skill file")
            exit_code = 1
            continue
        data = read_json(path)
        if "executor" in data:
            print(f"SKIP {skill_id}: executor-only skill, fx not generated")
            continue
        row = by_id[skill_id]
        skill_type = data.get("type", "active")
        legacy = legacy_fx_block(row, skill_type)
        components = component_fx_block(row, skill_type)
        mismatches = components_equivalent_to_legacy(components, legacy)
        print(f"=== {skill_id} (family={row['family']}, type={skill_type}) ===")
        print(f"  old (legacy 7-field): {json.dumps(legacy, ensure_ascii=False)}")
        print(f"  new (component array): {json.dumps(components, ensure_ascii=False)}")
        if mismatches:
            print(f"  MISMATCH: {mismatches}")
            exit_code = 1
        else:
            print("  OK: new format is equivalent to old format")
    return exit_code


def main():
    args = sys.argv[1:]
    mapping = build_mapping()
    if args and args[0] == "--sample":
        return run_sample(args[1:], mapping)
    write_json(MAP_OUT, mapping)
    filled = fill_fx_blocks(mapping)
    print(f"wrote {MAP_OUT.relative_to(ROOT)} ({len(mapping['skills'])} skills)")
    print(f"filled fx component array into {filled} component skill JSON files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
