#!/usr/bin/env python3
"""
Icon Pipeline v2 — Career Chronicle
Generates 32x32 pixel art icons using Pillow with the 10-family color system.
Outputs: skill/class/race PNGs + contact sheet for review.
"""

import json
import math
import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data/careerchronicle/careerchronicle"
ASSETS = ROOT / "src/main/resources/assets/careerchronicle"
SIZE = 32

# ── Color Families ──────────────────────────────────────────────────────────

FAMILIES = {
    "fire":    {"primary": (226, 88, 34),  "secondary": (245, 166, 35)},
    "ice":     {"primary": (127, 212, 232), "secondary": (232, 246, 250)},
    "holy":    {"primary": (245, 215, 110), "secondary": (255, 253, 242)},
    "dark":    {"primary": (107, 74, 158),  "secondary": (29, 21, 38)},
    "melee":   {"primary": (154, 160, 166), "secondary": (192, 57, 43)},
    "defense": {"primary": (176, 141, 87),  "secondary": (110, 91, 62)},
    "bow":     {"primary": (111, 163, 92),  "secondary": (239, 230, 208)},
    "stealth": {"primary": (62, 92, 97),    "secondary": (140, 158, 163)},
    "magic":   {"primary": (74, 105, 189),  "secondary": (155, 126, 222)},
    "summon":  {"primary": (46, 139, 110),  "secondary": (163, 217, 197)},
}

TAG_TO_FAMILY = {
    "fire": "fire", "ice": "ice", "holy": "holy", "heal": "holy",
    "support": "holy", "dark": "dark", "melee": "melee", "crit": "melee",
    "physical": "melee", "defense": "defense", "shield": "defense",
    "bow": "bow", "ranged": "bow", "precision": "bow",
    "stealth": "stealth", "mobility": "stealth",
    "spell": "magic", "control": "magic", "magic": "magic",
    "summon": "summon", "alchemy": "summon", "hidden": "dark",
}

RACE_FAMILIES = {
    "human": "holy", "elf": "bow", "dwarf": "defense",
    "orc": "melee", "undead": "dark", "demon": "fire",
}

CLASS_FAMILIES = {
    "fire_mage": "fire", "archer": "bow", "warrior": "melee",
    "priest": "holy", "guardian": "defense", "rogue": "stealth",
    "ice_mage": "ice", "necromancer": "dark",
    "ashen_warden": "fire", "death_knight": "dark", "lich": "dark",
}


# ── Data Loading ────────────────────────────────────────────────────────────

def load_json_dir(folder):
    result = {}
    d = DATA / folder
    if not d.exists():
        return result
    for p in sorted(d.glob("*.json")):
        with open(p) as f:
            result[p.stem] = json.load(f)
    return result


def build_skill_class_map(classes):
    """skill_id (path only) -> class_id (path only)"""
    m = {}
    for cls_id, cls_data in classes.items():
        for skill_rl in cls_data.get("grants_skills", []):
            sid = skill_rl.split(":")[-1]
            m[sid] = cls_id
        for rr in cls_data.get("repeat_rewards", []):
            sid = rr.get("unlock_skill", "").split(":")[-1]
            if sid:
                m[sid] = cls_id
    return m


def build_fusion_class_map(fusions):
    """fusion_skill_id -> [class1, class2]"""
    m = {}
    for fus_data in fusions.values():
        skill = fus_data.get("unlock_skill", "").split(":")[-1]
        cls_list = [k.split(":")[-1] for k in fus_data.get("required_class_counts", {}).keys()]
        if skill and cls_list:
            m[skill] = cls_list
    return m


def family_for_skill(skill_id, skill_class_map, fusion_class_map, classes):
    if skill_id in fusion_class_map:
        return None  # handled separately as dual-gradient
    cls_id = skill_class_map.get(skill_id)
    if cls_id and cls_id in CLASS_FAMILIES:
        return CLASS_FAMILIES[cls_id]
    if cls_id:
        cls_data = classes.get(cls_id, {})
        tags = cls_data.get("tags", [])
        for t in tags:
            tag_path = t.split(":")[-1]
            if tag_path in TAG_TO_FAMILY:
                return TAG_TO_FAMILY[tag_path]
    return "magic"


def fusion_families(skill_id, fusion_class_map):
    cls_list = fusion_class_map.get(skill_id, [])
    if len(cls_list) >= 2:
        f1 = CLASS_FAMILIES.get(cls_list[0], "magic")
        f2 = CLASS_FAMILIES.get(cls_list[1], "magic")
        return f1, f2
    return "magic", "magic"


# ── Motif Detection ─────────────────────────────────────────────────────────

def detect_motif(name):
    n = name.lower()
    if any(k in n for k in ("fire", "flame", "ember", "inferno", "meteor", "hellfire", "blaz")):
        return "flame"
    if any(k in n for k in ("frost", "ice", "frozen", "blizzard", "crystal", "permafrost", "glacial", "absolute_zero")):
        return "snowflake"
    if any(k in n for k in ("arrow", "shot", "volley", "marksman")):
        return "arrow"
    if any(k in n for k in ("heal", "mend", "blessing", "grace", "prayer", "twilight")):
        return "cross"
    if any(k in n for k in ("shield", "guard", "ward", "bulwark", "fortress", "aegis", "sanctuary", "wall")):
        return "shield_motif"
    if any(k in n for k in ("strike", "slam", "charge", "blade", "smash", "lunge", "assault", "breaker", "reaper")):
        return "sword"
    if any(k in n for k in ("shadow", "dark", "death", "soul", "lich", "spectral", "wither", "legion", "domain")):
        return "dark_circle"
    if any(k in n for k in ("holy", "sacred", "consecrat", "sunfire", "nova", "guiding")):
        return "star"
    if any(k in n for k in ("smoke", "phantom")):
        return "smoke"
    if any(k in n for k in ("drain", "coil")):
        return "spiral"
    if any(k in n for k in ("provoke",)):
        return "taunt"
    if any(k in n for k in ("runic",)):
        return "rune"
    return "diamond_dot"


# ── Drawing Primitives ──────────────────────────────────────────────────────

def lerp_color(c1, c2, t):
    return tuple(int(a + (b - a) * t) for a, b in zip(c1, c2))


def gradient_fill(img, c1, c2, diagonal=False):
    draw = ImageDraw.Draw(img)
    for y in range(SIZE):
        for x in range(SIZE):
            if diagonal:
                t = (x + y) / (2 * SIZE - 2) if SIZE > 1 else 0
            else:
                t = y / (SIZE - 1) if SIZE > 1 else 0
            c = lerp_color(c1, c2, t)
            draw.point((x, y), fill=c + (255,))


def add_highlight(img):
    draw = ImageDraw.Draw(img)
    hl = (255, 255, 255, 60)
    for y in range(4, 10):
        for x in range(4, 10):
            if x + y < 16:
                px = img.getpixel((x, y))
                if px[3] > 0:
                    blended = tuple(min(255, c + 20) for c in px[:3]) + (px[3],)
                    draw.point((x, y), fill=blended)
    draw.point((5, 5), fill=(255, 255, 255, 90))
    draw.point((6, 5), fill=(255, 255, 255, 70))
    draw.point((5, 6), fill=(255, 255, 255, 70))


# ── Shape Frames ────────────────────────────────────────────────────────────

def draw_rounded_rect(draw, color):
    c = color + (255,)
    for x in range(4, SIZE - 4):
        draw.point((x, 0), fill=c)
        draw.point((x, SIZE - 1), fill=c)
        draw.point((x, 1), fill=c)
        draw.point((x, SIZE - 2), fill=c)
    for y in range(4, SIZE - 4):
        draw.point((0, y), fill=c)
        draw.point((SIZE - 1, y), fill=c)
        draw.point((1, y), fill=c)
        draw.point((SIZE - 2, y), fill=c)
    corners = [(2, 1), (3, 1), (1, 2), (1, 3)]
    for dx, dy in corners:
        for ox, oy in [(0, 0), (SIZE - 1, 0), (0, SIZE - 1), (SIZE - 1, SIZE - 1)]:
            sx = ox + dx if ox == 0 else ox - dx
            sy = oy + dy if oy == 0 else oy - dy
            draw.point((sx, sy), fill=c)


def draw_diamond(draw, color):
    c = color + (255,)
    mid = SIZE // 2
    for i in range(mid + 1):
        draw.point((mid - i, i), fill=c)
        draw.point((mid + i, i), fill=c)
        draw.point((mid - i, SIZE - 1 - i), fill=c)
        draw.point((mid + i, SIZE - 1 - i), fill=c)
        if i > 0:
            draw.point((mid - i + 1, i), fill=c)
            draw.point((mid + i - 1, i), fill=c)
            draw.point((mid - i + 1, SIZE - 1 - i), fill=c)
            draw.point((mid + i - 1, SIZE - 1 - i), fill=c)


def draw_hex(draw, color):
    c = color + (255,)
    inset = 8
    for x in range(inset, SIZE - inset):
        draw.point((x, 0), fill=c)
        draw.point((x, 1), fill=c)
        draw.point((x, SIZE - 1), fill=c)
        draw.point((x, SIZE - 2), fill=c)
    for y in range(inset, SIZE - inset):
        draw.point((0, y), fill=c)
        draw.point((1, y), fill=c)
        draw.point((SIZE - 1, y), fill=c)
        draw.point((SIZE - 2, y), fill=c)
    for i in range(inset):
        for thickness in range(2):
            draw.point((inset - i - 1 + thickness, i + 1), fill=c)
            draw.point((SIZE - inset + i - thickness, i + 1), fill=c)
            draw.point((inset - i - 1 + thickness, SIZE - i - 2), fill=c)
            draw.point((SIZE - inset + i - thickness, SIZE - i - 2), fill=c)


def draw_square(draw, color):
    c = color + (255,)
    for x in range(SIZE):
        draw.point((x, 0), fill=c)
        draw.point((x, 1), fill=c)
        draw.point((x, SIZE - 1), fill=c)
        draw.point((x, SIZE - 2), fill=c)
    for y in range(SIZE):
        draw.point((0, y), fill=c)
        draw.point((1, y), fill=c)
        draw.point((SIZE - 1, y), fill=c)
        draw.point((SIZE - 2, y), fill=c)


def draw_double_rect(draw, color):
    c = color + (255,)
    draw_square(draw, color)
    for x in range(4, SIZE - 4):
        draw.point((x, 4), fill=c)
        draw.point((x, SIZE - 5), fill=c)
    for y in range(4, SIZE - 4):
        draw.point((4, y), fill=c)
        draw.point((SIZE - 5, y), fill=c)


def draw_corner_frame(draw, color):
    c = color + (255,)
    L = 10
    for t in range(2):
        for i in range(L):
            draw.point((i, t), fill=c)
            draw.point((t, i), fill=c)
            draw.point((SIZE - 1 - i, t), fill=c)
            draw.point((SIZE - 1 - t, i), fill=c)
            draw.point((i, SIZE - 1 - t), fill=c)
            draw.point((t, SIZE - 1 - i), fill=c)
            draw.point((SIZE - 1 - i, SIZE - 1 - t), fill=c)
            draw.point((SIZE - 1 - t, SIZE - 1 - i), fill=c)


SHAPE_DRAWERS = {
    "active": draw_rounded_rect,
    "fusion": draw_diamond,
    "hidden": draw_hex,
    "passive": draw_square,
    "ultimate": draw_double_rect,
    "race": draw_corner_frame,
}


# ── Motif Drawing ───────────────────────────────────────────────────────────

def draw_motif(draw, motif, color):
    c = color + (220,)
    cx, cy = SIZE // 2, SIZE // 2

    if motif == "flame":
        pts = [(cx, cy - 7), (cx - 5, cy + 5), (cx, cy + 1), (cx + 5, cy + 5)]
        draw.polygon(pts, fill=c)
        draw.polygon([(cx, cy - 4), (cx - 2, cy + 3), (cx + 2, cy + 3)],
                      fill=lerp_color(color, (255, 255, 200), 0.5) + (200,))
    elif motif == "snowflake":
        for angle_deg in range(0, 360, 60):
            a = math.radians(angle_deg)
            for r in range(2, 8):
                px = int(cx + math.cos(a) * r)
                py = int(cy + math.sin(a) * r)
                if 0 <= px < SIZE and 0 <= py < SIZE:
                    draw.point((px, py), fill=c)
        draw.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=c)
    elif motif == "arrow":
        pts = [(cx + 6, cy), (cx - 2, cy - 5), (cx - 2, cy - 2),
               (cx - 6, cy - 2), (cx - 6, cy + 2), (cx - 2, cy + 2),
               (cx - 2, cy + 5)]
        draw.polygon(pts, fill=c)
    elif motif == "cross":
        draw.rectangle((cx - 2, cy - 6, cx + 2, cy + 6), fill=c)
        draw.rectangle((cx - 6, cy - 2, cx + 6, cy + 2), fill=c)
    elif motif == "shield_motif":
        pts = [(cx, cy - 7), (cx + 7, cy - 3), (cx + 5, cy + 4),
               (cx, cy + 7), (cx - 5, cy + 4), (cx - 7, cy - 3)]
        draw.polygon(pts, fill=c)
        inner = lerp_color(color, (255, 255, 255), 0.3) + (150,)
        draw.polygon([(cx, cy - 4), (cx + 4, cy - 1), (cx + 3, cy + 2),
                       (cx, cy + 4), (cx - 3, cy + 2), (cx - 4, cy - 1)], fill=inner)
    elif motif == "sword":
        draw.rectangle((cx - 1, cy - 8, cx + 1, cy + 4), fill=c)
        draw.rectangle((cx - 5, cy + 1, cx + 5, cy + 3), fill=c)
        draw.rectangle((cx - 2, cy + 4, cx + 2, cy + 6), fill=c)
    elif motif == "dark_circle":
        draw.ellipse((cx - 6, cy - 6, cx + 6, cy + 6), fill=c)
        inner = lerp_color(color, (0, 0, 0), 0.6) + (200,)
        draw.ellipse((cx - 3, cy - 3, cx + 3, cy + 3), fill=inner)
    elif motif == "star":
        for i in range(5):
            a1 = math.radians(i * 72 - 90)
            a2 = math.radians(i * 72 - 90 + 36)
            pts_star = [
                (cx + int(7 * math.cos(a1)), cy + int(7 * math.sin(a1))),
                (cx + int(3 * math.cos(a2)), cy + int(3 * math.sin(a2))),
            ]
            for px, py in pts_star:
                if 0 <= px < SIZE and 0 <= py < SIZE:
                    draw.ellipse((px - 1, py - 1, px + 1, py + 1), fill=c)
        draw.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=c)
    elif motif == "smoke":
        for i in range(5):
            ox = cx + (i * 7 % 5) - 2
            oy = cy + (i * 3 % 5) - 2
            r = 2 + i % 2
            a = 180 - i * 25
            draw.ellipse((ox - r, oy - r, ox + r, oy + r),
                          fill=color + (a,))
    elif motif == "spiral":
        for t in range(60):
            angle = t * 0.15
            r = 1.5 + t * 0.1
            px = int(cx + math.cos(angle) * r)
            py = int(cy + math.sin(angle) * r)
            if 0 <= px < SIZE and 0 <= py < SIZE:
                draw.point((px, py), fill=c)
    elif motif == "taunt":
        draw.ellipse((cx - 5, cy - 6, cx + 5, cy + 4), fill=c)
        draw.rectangle((cx - 1, cy + 4, cx + 1, cy + 7), fill=c)
    elif motif == "rune":
        draw.rectangle((cx - 5, cy - 5, cx + 5, cy + 5), outline=c, width=1)
        draw.line((cx - 3, cy, cx + 3, cy), fill=c, width=1)
        draw.line((cx, cy - 3, cx, cy + 3), fill=c, width=1)
        draw.line((cx - 3, cy - 3, cx + 3, cy + 3), fill=c, width=1)
    else:  # diamond_dot
        pts = [(cx, cy - 4), (cx + 4, cy), (cx, cy + 4), (cx - 4, cy)]
        draw.polygon(pts, fill=c)


# ── Icon Generation ─────────────────────────────────────────────────────────

def make_icon(family_name, shape_type, motif_name, family2=None):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    fam = FAMILIES.get(family_name, FAMILIES["magic"])

    if family2 and family2 != family_name:
        fam2 = FAMILIES.get(family2, FAMILIES["magic"])
        gradient_fill(img, fam["primary"], fam2["primary"], diagonal=True)
    else:
        gradient_fill(img, fam["primary"], fam["secondary"])

    draw = ImageDraw.Draw(img)
    motif_color = lerp_color(fam["secondary"], (255, 255, 255), 0.4)
    if family2 and family2 != family_name:
        fam2 = FAMILIES.get(family2, FAMILIES["magic"])
        motif_color = lerp_color(
            lerp_color(fam["secondary"], fam2["secondary"], 0.5),
            (255, 255, 255), 0.4
        )

    draw_motif(draw, motif_name, motif_color)

    frame_fn = SHAPE_DRAWERS.get(shape_type, draw_rounded_rect)
    frame_fn(draw, fam["primary"])

    add_highlight(img)
    return img


def make_class_icon(class_id):
    fam = CLASS_FAMILIES.get(class_id, "magic")
    motif = detect_motif(class_id)
    return make_icon(fam, "active", motif)


def make_race_icon(race_id):
    fam = RACE_FAMILIES.get(race_id, "holy")
    motif = detect_motif(race_id)
    if race_id == "human":
        motif = "star"
    elif race_id == "elf":
        motif = "snowflake"
    elif race_id == "dwarf":
        motif = "shield_motif"
    elif race_id == "orc":
        motif = "sword"
    elif race_id == "undead":
        motif = "dark_circle"
    elif race_id == "demon":
        motif = "flame"
    return make_icon(fam, "race", motif)


# ── Main ────────────────────────────────────────────────────────────────────

def main():
    skills = load_json_dir("skills")
    classes = load_json_dir("classes")
    races = load_json_dir("races")
    fusions = load_json_dir("fusions")

    skill_class_map = build_skill_class_map(classes)
    fusion_class_map = build_fusion_class_map(fusions)

    all_icons = []  # (label, img, output_path)

    # Race icons
    for race_id in sorted(races.keys()):
        img = make_race_icon(race_id)
        out = ASSETS / f"textures/gui/race/{race_id}.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        img.save(str(out))
        all_icons.append((f"race:{race_id}", img))

    # Class icons
    for cls_id in sorted(classes.keys()):
        img = make_class_icon(cls_id)
        out = ASSETS / f"textures/gui/class/{cls_id}.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        img.save(str(out))
        all_icons.append((f"class:{cls_id}", img))

    # Skill icons
    for skill_id in sorted(skills.keys()):
        skill_data = skills[skill_id]
        skill_type = skill_data.get("type", "active")

        is_fusion = skill_id in fusion_class_map
        if is_fusion:
            f1, f2 = fusion_families(skill_id, fusion_class_map)
            fam_name = f1
            fam2 = f2
        else:
            fam_name = family_for_skill(skill_id, skill_class_map, fusion_class_map, classes)
            fam2 = None

        motif = detect_motif(skill_id)
        img = make_icon(fam_name, skill_type, motif, family2=fam2)
        out = ASSETS / f"textures/gui/skill/{skill_id}.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        img.save(str(out))
        all_icons.append((f"skill:{skill_id}", img))

    # Contact sheet
    cols = 10
    rows = math.ceil(len(all_icons) / cols)
    cell_w, cell_h = 48, 56
    sheet = Image.new("RGBA", (cols * cell_w, rows * cell_h), (24, 24, 32, 255))
    sheet_draw = ImageDraw.Draw(sheet)

    try:
        font = ImageFont.truetype("/System/Library/Fonts/Menlo.ttc", 7)
    except Exception:
        font = ImageFont.load_default()

    for idx, (label, icon) in enumerate(all_icons):
        col = idx % cols
        row = idx // cols
        x = col * cell_w + (cell_w - SIZE) // 2
        y = row * cell_h + 2
        sheet.paste(icon, (x, y), icon)
        short = label.split(":")[-1][:8]
        tw = sheet_draw.textlength(short, font=font) if hasattr(sheet_draw, 'textlength') else len(short) * 4
        tx = col * cell_w + (cell_w - tw) // 2
        sheet_draw.text((tx, y + SIZE + 2), short, fill=(200, 210, 220, 255), font=font)

    sheet_path = ROOT / "run" / "contact_sheet.png"
    sheet_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(str(sheet_path))

    print(f"Generated {len(all_icons)} icons ({sum(1 for l,_ in all_icons if l.startswith('race:'))} race, "
          f"{sum(1 for l,_ in all_icons if l.startswith('class:'))} class, "
          f"{sum(1 for l,_ in all_icons if l.startswith('skill:'))} skill)")
    print(f"Contact sheet: {sheet_path}")


if __name__ == "__main__":
    main()
