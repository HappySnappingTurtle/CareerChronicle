#!/usr/bin/env python3
"""0.4-08: generate docs/技能视听规格表.md from tools/skill_fx_map.json + each
skill JSON's fx component array.

Idempotent (same inputs -> byte-identical output, P2) except for the 备注
(remarks) column, which is human-authored: if docs/技能视听规格表.md already
exists, its per-skill 备注 content is read back and merged into the freshly
regenerated table (keyed by 技能ID) instead of being overwritten -- this is
the "rerun protection" 0.4-08 §2.2 requires, mirroring fill_skill_fx.py's
own idempotent-rewrite convention. Skills with no prior remark default to
"待人工核对" (pending manual review).

Fails loudly (non-zero exit, no file written) if tools/skill_fx_map.json and
the skills/ directory disagree on which skills exist (P4) -- a silently
wrong-row-count table is worse than no table.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA_ROOT = ROOT / "src/main/resources/data/careerchronicle/careerchronicle"
SKILLS_DIR = DATA_ROOT / "skills"
FX_TEMPLATES_DIR = DATA_ROOT / "fx_templates"
MAP_PATH = ROOT / "tools/skill_fx_map.json"
OUT_PATH = ROOT / "docs/技能视听规格表.md"

DEFAULT_REMARK = "待人工核对"

HEADERS = ["技能ID", "家族", "类型", "cast音效", "cast粒子", "hit音效", "hit粒子", "震屏", "其他op", "备注"]

# Ops that get their own dedicated column; anything else falls into "其他op".
SOUND_OPS = {"sound", "hit_layered"}
PARTICLE_OPS = {"particles"}
SHAKE_OPS = {"shake", "camera_punch"}
DEDICATED_OPS = SOUND_OPS | PARTICLE_OPS | SHAKE_OPS


def read_json(path):
    with path.open(encoding="utf-8") as f:
        return json.load(f)


def load_mapping():
    if not MAP_PATH.exists():
        raise SystemExit(f"{MAP_PATH} not found -- run tools/fill_skill_fx.py first")
    mapping = read_json(MAP_PATH)
    rows = mapping.get("skills", [])
    if not rows:
        raise SystemExit(f"{MAP_PATH} has no 'skills' entries")
    return rows


def check_completeness(mapping_rows):
    """P4: the mapping and the on-disk skills/ directory must agree on the exact skill set."""
    mapped_ids = {row["skill_id"] for row in mapping_rows}
    on_disk_ids = {f"careerchronicle:{p.stem}" for p in SKILLS_DIR.glob("*.json")}
    if mapped_ids != on_disk_ids:
        missing_from_disk = mapped_ids - on_disk_ids
        missing_from_mapping = on_disk_ids - mapped_ids
        raise SystemExit(
            "skill_fx_map.json and skills/ directory disagree -- refusing to generate a "
            f"table with the wrong row count. In map but not on disk: {sorted(missing_from_disk)}. "
            f"On disk but not in map: {sorted(missing_from_mapping)}."
        )
    return len(mapped_ids)


def skill_kind(skill_json):
    """castable (component-effects) vs executor (legacy hardcoded), mirrors SkillDef.hasComponentEffects()."""
    effects = skill_json.get("effects", [])
    return "castable" if effects else "executor"


def load_fx_templates():
    templates = {}
    if not FX_TEMPLATES_DIR.exists():
        return templates
    for path in sorted(FX_TEMPLATES_DIR.glob("*.json")):
        templates[path.stem] = read_json(path)
    return templates


def merge_key(component):
    return component.get("op"), component.get("when")


def fx_components(skill_json, fx_templates):
    """The skill's *effective* fx, mirroring CareerDataParsers.resolveFx: if the skill
    declares fx_template, the template's components seed the result (in the template's
    own order) and the skill's own fx either replaces a matching op+when slot in place or
    is appended as a new entry -- reading the skill JSON's own "fx" field alone (as this
    script did before this fix) silently misses any component a template supplies, which
    is exactly frost_bolt's case (its cast sound/particles come from fx_templates/frost_active.json,
    not its own fx array)."""
    own = skill_json.get("fx", [])
    own = own if isinstance(own, list) else []
    template_name = skill_json.get("fx_template")
    if not template_name:
        return own
    template = fx_templates.get(template_name, [])
    merged = {}
    for component in template:
        merged[merge_key(component)] = component
    for component in own:
        merged[merge_key(component)] = component
    return list(merged.values())


def find_component(components, ops, when=None):
    for component in components:
        if component.get("op") not in ops:
            continue
        if when is not None and component.get("when") != when:
            continue
        return component
    return None


def sound_cell(components, when):
    component = find_component(components, SOUND_OPS, when)
    if component is None:
        return "—"
    op = component.get("op")
    suffix = "" if op == "sound" else f" ({op})"
    return f"{component.get('id', '?')}{suffix}"


def particle_cell(components, when):
    component = find_component(components, PARTICLE_OPS, when)
    if component is None:
        return "—"
    count = component.get("count")
    count_part = f", count={count}" if count is not None else ""
    return f"{component.get('id', '?')}{count_part}"


def shake_cell(components):
    parts = []
    for component in components:
        op = component.get("op")
        if op == "shake":
            strength = component.get("strength")
            ticks = component.get("ticks")
            parts.append(f"shake(strength={strength}, ticks={ticks})")
        elif op == "camera_punch":
            strength = component.get("strength")
            direction = component.get("direction")
            parts.append(f"camera_punch(strength={strength}, direction={direction})")
    return "; ".join(parts) if parts else "—"


def other_ops_cell(components):
    parts = []
    for component in components:
        op = component.get("op")
        if op in DEDICATED_OPS:
            continue
        when = component.get("when", "?")
        parts.append(f"{op}({when})")
    return "; ".join(parts) if parts else "—"


def build_row(mapping_row, remarks, fx_templates):
    skill_id = mapping_row["skill_id"]
    path = SKILLS_DIR / f"{skill_id.split(':', 1)[1]}.json"
    skill_json = read_json(path)
    components = fx_components(skill_json, fx_templates)
    return [
        skill_id,
        mapping_row.get("family", "—"),
        skill_kind(skill_json),
        sound_cell(components, "cast"),
        particle_cell(components, "cast"),
        sound_cell(components, "hit"),
        particle_cell(components, "hit"),
        shake_cell(components),
        other_ops_cell(components),
        remarks.get(skill_id, DEFAULT_REMARK),
    ]


def escape_cell(value):
    return str(value).replace("|", "\\|").replace("\n", " ")


def render_table(rows):
    lines = []
    lines.append("# 技能视听规格表\n")
    lines.append(
        "自动生成（`tools/gen_av_spec_sheet.py`），来源：`tools/skill_fx_map.json` + 各技能 JSON 的 "
        "`fx` 组件数组。**除“备注”列外**其余列由脚本重新生成时覆盖刷新；“备注”列的人工填写内容会在重跑时"
        "按技能ID保留，不会被覆盖（重跑保护，见0.4-08设计文档§2.2）。\n"
    )
    lines.append("| " + " | ".join(HEADERS) + " |")
    lines.append("|" + "|".join(["---"] * len(HEADERS)) + "|")
    for row in rows:
        lines.append("| " + " | ".join(escape_cell(cell) for cell in row) + " |")
    lines.append("")
    return "\n".join(lines)


def read_existing_remarks(path):
    """Parses an existing generated table (if any) back into {skill_id: remark}, for rerun protection (P3)."""
    if not path.exists():
        return {}
    remarks = {}
    lines = path.read_text(encoding="utf-8").splitlines()
    data_lines = [line for line in lines if line.startswith("| careerchronicle:")]
    for line in data_lines:
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if len(cells) != len(HEADERS):
            continue
        skill_id = cells[0]
        remark = cells[-1]
        remarks[skill_id] = remark
    return remarks


def main():
    mapping_rows = load_mapping()
    total = check_completeness(mapping_rows)
    remarks = read_existing_remarks(OUT_PATH)
    fx_templates = load_fx_templates()

    rows = [build_row(mapping_row, remarks, fx_templates) for mapping_row in mapping_rows]

    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUT_PATH.write_text(render_table(rows), encoding="utf-8")

    print(f"wrote {OUT_PATH.relative_to(ROOT)} ({total} skill rows)")
    reused = sum(1 for row in rows if row[-1] != DEFAULT_REMARK)
    print(f"preserved {reused} existing 备注 entries from a prior run" if remarks else "no prior file found, all 备注 default to 待人工核对")
    return 0


if __name__ == "__main__":
    sys.exit(main())
