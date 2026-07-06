#!/usr/bin/env python3
import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MOD_ID = "careerchronicle"
DATA_ROOT = ROOT / "src/main/resources/data/careerchronicle/careerchronicle"
ASSET_ROOT = ROOT / "src/main/resources/assets/careerchronicle"
JAVA_ROOT = ROOT / "src/main/java"


def main():
    errors = []
    json_files = validate_json(errors)
    data = load_data(errors)
    lang = load_lang(errors)
    executor_ids = load_executor_ids()

    validate_data_references(data, executor_ids, errors)
    validate_lang_keys(data, lang, errors)
    validate_textures(data, errors)
    validate_model_texture_refs(errors)
    validate_java_texture_refs(errors)

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        print(f"Career Chronicle resource QA failed with {len(errors)} error(s).", file=sys.stderr)
        return 1

    print(
        "Career Chronicle resource QA OK: "
        f"{json_files} json, "
        f"{len(data['races'])} races, "
        f"{len(data['classes'])} classes, "
        f"{len(data['skills'])} skills, "
        f"{len(data['fusions'])} fusions, "
        f"{len(data['hidden_unlocks'])} hidden unlocks, "
        f"{len(data['xp_sources'])} XP sources"
    )
    return 0


def validate_json(errors):
    json_files = list((ROOT / "src/main/resources").rglob("*.json"))
    for path in json_files:
        read_json(path, errors)
    return len(json_files)


def load_data(errors):
    return {
        "races": load_folder("races", errors),
        "classes": load_folder("classes", errors),
        "skills": load_folder("skills", errors),
        "fusions": load_folder("fusions", errors),
        "hidden_unlocks": load_folder("hidden_unlocks", errors),
        "xp_sources": load_folder("xp_sources", errors),
    }


def load_folder(folder, errors):
    root = DATA_ROOT / folder
    values = {}
    if not root.exists():
        return values
    for path in sorted(root.glob("*.json")):
        values[f"{MOD_ID}:{path.stem}"] = read_json(path, errors)
    return values


def load_lang(errors):
    langs = {}
    for locale in ("en_us", "zh_cn"):
        path = ASSET_ROOT / f"lang/{locale}.json"
        parsed = read_json(path, errors)
        if not isinstance(parsed, dict):
            errors.append(f"{path} must be a JSON object")
            parsed = {}
        langs[locale] = parsed
    return langs


def load_executor_ids():
    path = JAVA_ROOT / "com/hongyuwu/careerchronicle/skill/SkillExecutorRegistry.java"
    if not path.exists():
        return set()
    text = path.read_text(encoding="utf-8")
    executor_paths = re.findall(
        r"ResourceLocation\.fromNamespaceAndPath\(\s*CareerChronicleMod\.MOD_ID\s*,\s*\"([a-z0-9_/.-]+)\"\s*\)",
        text,
    )
    return {f"{MOD_ID}:{executor_path}" for executor_path in executor_paths}


def validate_data_references(data, executor_ids, errors):
    races = data["races"]
    classes = data["classes"]
    skills = data["skills"]
    fusions = data["fusions"]
    hidden_unlocks = data["hidden_unlocks"]
    xp_sources = data["xp_sources"]
    hidden_flags = {as_id(entry.get("unlock_flag")) for entry in hidden_unlocks.values()}

    for race_id, race in races.items():
        for class_id in id_list(race, "allowed_classes"):
            require(class_id in classes, errors, f"{race_id} allowed_classes references missing class {class_id}")

    class_tags = set()
    for class_id, career_class in classes.items():
        class_tags.update(id_list(career_class, "tags"))
        for skill_id in id_list(career_class, "grants_skills"):
            require(skill_id in skills, errors, f"{class_id} grants_skills references missing skill {skill_id}")
        for reward in object_list(career_class, "repeat_rewards"):
            skill_id = as_id(reward.get("unlock_skill"))
            require(skill_id in skills, errors, f"{class_id} repeat reward references missing skill {skill_id}")
        if career_class.get("hidden", False):
            unlock_flag = as_id(career_class.get("unlock_flag"))
            require(bool(unlock_flag), errors, f"{class_id} hidden class must declare unlock_flag")
            require(unlock_flag in hidden_flags, errors, f"{class_id} unlock_flag {unlock_flag} is not produced by hidden_unlocks")

    known_ops = {"damage", "apply_effect", "heal", "knockback", "projectile", "aoe", "dash", "shield", "resource", "ignite", "arrow", "summon"}
    for skill_id, skill in skills.items():
        effects = skill.get("effects", [])
        if isinstance(effects, list) and effects:
            for effect in effects:
                op_name = effect.get("op", "")
                require(op_name in known_ops, errors,
                        f"{skill_id} effect references unknown op '{op_name}'")
        else:
            executor_id = as_id(skill.get("executor"))
            require(executor_id in executor_ids, errors,
                    f"{skill_id} references missing Java executor {executor_id}")
        requirements = skill.get("requirements", {})
        require(isinstance(requirements, dict), errors, f"{skill_id} requirements must be an object")
        if isinstance(requirements, dict):
            equipment_tags = requirements.get("equipment_tags", [])
            require(isinstance(equipment_tags, list), errors, f"{skill_id} requirements.equipment_tags must be an array")
            if isinstance(equipment_tags, list):
                for tag_id in equipment_tags:
                    require(isinstance(tag_id, str) and bool(tag_id),
                            errors, f"{skill_id} requirements.equipment_tags must contain resource id strings")

    for fusion_id, fusion in fusions.items():
        requirements = id_int_map(fusion, "required_class_counts")
        require(requirements, errors, f"{fusion_id} must declare required_class_counts")
        for class_id in requirements:
            require(class_id in classes, errors, f"{fusion_id} references missing class {class_id}")
        skill_id = as_id(fusion.get("unlock_skill"))
        require(skill_id in skills, errors, f"{fusion_id} unlock_skill references missing skill {skill_id}")

    for hidden_id, hidden in hidden_unlocks.items():
        class_requirements = id_int_map(hidden, "required_class_counts")
        tag_requirements = id_int_map(hidden, "required_tag_scores")
        require(class_requirements or tag_requirements, errors, f"{hidden_id} must declare at least one condition")
        for class_id in class_requirements:
            require(class_id in classes, errors, f"{hidden_id} references missing class {class_id}")
        for tag_id in tag_requirements:
            require(tag_id in class_tags, errors, f"{hidden_id} references unused class tag {tag_id}")

    for source_id, source in xp_sources.items():
        display_key = source.get("display_key")
        base_amount = number_value(source.get("base_amount", 0))
        health_multiplier = number_value(source.get("health_multiplier", 0))
        min_amount = number_value(source.get("min_amount", 0))
        max_amount = number_value(source.get("max_amount", 0))
        require(isinstance(display_key, str) and bool(display_key), errors, f"{source_id} must declare display_key")
        require(base_amount >= 0, errors, f"{source_id} base_amount must be >= 0")
        require(health_multiplier >= 0, errors, f"{source_id} health_multiplier must be >= 0")
        require(min_amount >= 0, errors, f"{source_id} min_amount must be >= 0")
        require(max_amount >= 0, errors, f"{source_id} max_amount must be >= 0")
        require(base_amount > 0 or health_multiplier > 0 or min_amount > 0,
                errors, f"{source_id} must award positive XP")
        require(max_amount == 0 or max_amount >= min_amount,
                errors, f"{source_id} max_amount must be >= min_amount or 0")


def validate_lang_keys(data, lang, errors):
    en_keys = set(lang["en_us"].keys())
    zh_keys = set(lang["zh_cn"].keys())
    for key in sorted(en_keys - zh_keys):
        errors.append(f"zh_cn missing lang key {key}")
    for key in sorted(zh_keys - en_keys):
        errors.append(f"en_us missing lang key {key}")

    used_keys = set()
    for folder in ("races", "classes", "skills"):
        for entry in data[folder].values():
            add_if_present(used_keys, entry, "display_key")
    for hidden in data["hidden_unlocks"].values():
        add_if_present(used_keys, hidden, "display_key")
        add_if_present(used_keys, hidden, "clue_key")
        add_if_present(used_keys, hidden, "revealed_key")
    for source in data["xp_sources"].values():
        add_if_present(used_keys, source, "display_key")

    for key in sorted(used_keys):
        require(key in lang["en_us"], errors, f"en_us missing data lang key {key}")
        require(key in lang["zh_cn"], errors, f"zh_cn missing data lang key {key}")


def validate_textures(data, errors):
    for race_id in data["races"]:
        require_texture(f"gui/race/{path_part(race_id)}.png", errors, f"{race_id} race icon")
    for class_id in data["classes"]:
        require_texture(f"gui/class/{path_part(class_id)}.png", errors, f"{class_id} class icon")
    for skill_id in data["skills"]:
        require_texture(f"gui/skill/{path_part(skill_id)}.png", errors, f"{skill_id} skill icon")


def validate_model_texture_refs(errors):
    for path in sorted((ASSET_ROOT / "models").rglob("*.json")):
        model = read_json(path, errors)
        textures = model.get("textures", {}) if isinstance(model, dict) else {}
        for texture_id in textures.values():
            if not isinstance(texture_id, str) or texture_id.startswith("#"):
                continue
            texture_path = resource_texture_path(texture_id)
            if texture_path is not None:
                require(texture_path.exists(), errors, f"{path.relative_to(ROOT)} references missing texture {texture_id}")


def validate_java_texture_refs(errors):
    for path in sorted(JAVA_ROOT.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        for texture in re.findall(r"\"(textures/[a-z0-9_/.-]+\.png)\"", text):
            texture_path = ASSET_ROOT / texture
            require(texture_path.exists(), errors, f"{path.relative_to(ROOT)} references missing texture {texture}")


def read_json(path, errors):
    try:
        with path.open(encoding="utf-8") as handle:
            return json.load(handle)
    except Exception as exception:
        errors.append(f"{path.relative_to(ROOT)} failed JSON parse: {exception}")
        return {}


def as_id(value):
    if not isinstance(value, str) or not value:
        return ""
    return value if ":" in value else f"{MOD_ID}:{value}"


def path_part(resource_id):
    return resource_id.split(":", 1)[1]


def id_list(entry, key):
    values = entry.get(key, [])
    return [as_id(value) for value in values if isinstance(value, str)]


def object_list(entry, key):
    values = entry.get(key, [])
    return [value for value in values if isinstance(value, dict)]


def id_int_map(entry, key):
    values = entry.get(key, {})
    if not isinstance(values, dict):
        return {}
    return {as_id(key): value for key, value in values.items()}


def add_if_present(values, entry, key):
    value = entry.get(key)
    if isinstance(value, str) and value:
        values.add(value)


def require(condition, errors, message):
    if not condition:
        errors.append(message)


def number_value(value):
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        return -1
    return value


def require_texture(texture, errors, label):
    path = ASSET_ROOT / f"textures/{texture}"
    require(path.exists(), errors, f"{label} missing texture assets/{MOD_ID}/textures/{texture}")


def resource_texture_path(texture_id):
    namespace, texture_path = texture_id.split(":", 1) if ":" in texture_id else ("minecraft", texture_id)
    if namespace != MOD_ID:
        return None
    return ASSET_ROOT / f"textures/{texture_path}.png"


if __name__ == "__main__":
    sys.exit(main())
