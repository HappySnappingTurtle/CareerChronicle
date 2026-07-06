#!/usr/bin/env python3
import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TEXTURE_ROOT = ROOT / "src/main/resources/assets/careerchronicle/textures"


def png(path, pixels):
    path.parent.mkdir(parents=True, exist_ok=True)
    height = len(pixels)
    width = len(pixels[0])
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for r, g, b, a in row:
            raw.extend((r, g, b, a))
    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    data += chunk(b"IEND", b"")
    path.write_bytes(data)


def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def canvas(bg=(0, 0, 0, 0)):
    return [[bg for _ in range(16)] for _ in range(16)]


def set_px(img, x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        img[y][x] = color


def rect(img, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            set_px(img, x, y, color)


def line(img, x0, y0, x1, y1, color):
    dx = abs(x1 - x0)
    sx = 1 if x0 < x1 else -1
    dy = -abs(y1 - y0)
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    while True:
        set_px(img, x0, y0, color)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def circle(img, cx, cy, radius, color):
    r2 = radius * radius
    for y in range(cy - radius, cy + radius + 1):
        for x in range(cx - radius, cx + radius + 1):
            if (x - cx) * (x - cx) + (y - cy) * (y - cy) <= r2:
                set_px(img, x, y, color)


def frame(img, color):
    for i in range(16):
        set_px(img, i, 0, color)
        set_px(img, i, 15, color)
        set_px(img, 0, i, color)
        set_px(img, 15, i, color)


def skill_bg(color_a, color_b):
    img = canvas((18, 22, 30, 255))
    frame(img, color_b)
    rect(img, 2, 2, 13, 13, color_a)
    rect(img, 3, 3, 12, 12, (max(color_a[0] - 25, 0), max(color_a[1] - 25, 0), max(color_a[2] - 25, 0), 255))
    return img


def fireball():
    img = skill_bg((88, 28, 18, 255), (255, 150, 46, 255))
    circle(img, 8, 8, 4, (252, 73, 24, 255))
    circle(img, 8, 8, 2, (255, 224, 86, 255))
    return img


def ember_burst():
    img = skill_bg((78, 33, 22, 255), (255, 105, 40, 255))
    for x, y in [(8, 4), (11, 6), (12, 10), (8, 12), (4, 10), (5, 6)]:
        circle(img, x, y, 1, (255, 206, 64, 255))
    circle(img, 8, 8, 2, (235, 60, 28, 255))
    return img


def flame_step():
    img = skill_bg((36, 33, 40, 255), (255, 122, 30, 255))
    line(img, 4, 11, 10, 5, (255, 211, 88, 255))
    line(img, 5, 12, 12, 7, (244, 73, 28, 255))
    rect(img, 3, 12, 7, 13, (248, 95, 32, 255))
    return img


def charged_shot():
    img = skill_bg((19, 42, 64, 255), (92, 200, 255, 255))
    line(img, 3, 8, 12, 8, (206, 246, 255, 255))
    line(img, 10, 5, 13, 8, (206, 246, 255, 255))
    line(img, 10, 11, 13, 8, (206, 246, 255, 255))
    circle(img, 4, 8, 2, (80, 170, 255, 255))
    return img


def scatter_shot():
    img = skill_bg((22, 36, 48, 255), (95, 218, 255, 255))
    for y in [5, 8, 11]:
        line(img, 3, y, 12, y, (214, 246, 255, 255))
        set_px(img, 13, y, (95, 218, 255, 255))
    return img


def snare_shot():
    img = skill_bg((31, 38, 36, 255), (121, 224, 156, 255))
    line(img, 3, 8, 12, 8, (210, 255, 218, 255))
    circle(img, 10, 8, 3, (69, 156, 104, 255))
    line(img, 8, 6, 12, 10, (210, 255, 218, 255))
    line(img, 12, 6, 8, 10, (210, 255, 218, 255))
    return img


def flame_arrow():
    img = charged_shot()
    circle(img, 5, 8, 2, (255, 106, 35, 255))
    circle(img, 5, 8, 1, (255, 227, 85, 255))
    return img


def lunge_strike():
    img = skill_bg((42, 42, 48, 255), (206, 214, 220, 255))
    line(img, 4, 12, 12, 4, (226, 234, 240, 255))
    line(img, 5, 13, 13, 5, (122, 132, 144, 255))
    rect(img, 3, 11, 6, 13, (118, 76, 42, 255))
    line(img, 9, 4, 12, 4, (255, 255, 255, 255))
    return img


def guard_stance():
    img = skill_bg((30, 38, 48, 255), (164, 184, 202, 255))
    rect(img, 5, 4, 10, 11, (92, 112, 130, 255))
    rect(img, 6, 5, 9, 10, (152, 174, 190, 255))
    line(img, 5, 11, 8, 13, (72, 86, 100, 255))
    line(img, 10, 11, 8, 13, (72, 86, 100, 255))
    return img


def ground_slam():
    img = skill_bg((45, 36, 30, 255), (216, 168, 88, 255))
    rect(img, 6, 3, 10, 7, (114, 82, 56, 255))
    rect(img, 4, 7, 12, 9, (172, 120, 68, 255))
    line(img, 3, 12, 13, 12, (234, 196, 110, 255))
    line(img, 5, 10, 3, 13, (116, 82, 54, 255))
    line(img, 11, 10, 13, 13, (116, 82, 54, 255))
    return img


def mend():
    img = skill_bg((24, 48, 42, 255), (106, 238, 178, 255))
    rect(img, 7, 4, 9, 12, (210, 255, 230, 255))
    rect(img, 4, 7, 12, 9, (210, 255, 230, 255))
    circle(img, 8, 8, 2, (94, 210, 146, 255))
    return img


def holy_nova():
    img = skill_bg((48, 43, 28, 255), (255, 226, 116, 255))
    circle(img, 8, 8, 4, (244, 218, 104, 255))
    circle(img, 8, 8, 2, (255, 255, 226, 255))
    for x, y in [(8, 3), (8, 13), (3, 8), (13, 8)]:
        set_px(img, x, y, (255, 255, 210, 255))
    return img


def blessing():
    img = skill_bg((35, 34, 56, 255), (204, 174, 255, 255))
    line(img, 8, 3, 8, 12, (238, 222, 255, 255))
    line(img, 5, 6, 11, 6, (238, 222, 255, 255))
    circle(img, 8, 10, 2, (168, 124, 230, 255))
    set_px(img, 6, 4, (255, 244, 180, 255))
    set_px(img, 10, 4, (255, 244, 180, 255))
    return img


def blazing_charge():
    img = skill_bg((54, 32, 28, 255), (255, 130, 42, 255))
    line(img, 4, 12, 12, 4, (238, 226, 198, 255))
    line(img, 5, 13, 13, 5, (255, 82, 28, 255))
    circle(img, 5, 11, 2, (255, 204, 74, 255))
    line(img, 3, 13, 7, 9, (255, 96, 30, 255))
    return img


def sunfire_aegis():
    img = skill_bg((48, 36, 28, 255), (255, 202, 82, 255))
    rect(img, 5, 4, 10, 11, (160, 104, 48, 255))
    rect(img, 6, 5, 9, 10, (255, 226, 128, 255))
    circle(img, 8, 8, 2, (255, 90, 32, 255))
    line(img, 5, 11, 8, 13, (255, 226, 128, 255))
    line(img, 10, 11, 8, 13, (255, 226, 128, 255))
    return img


def piercing_volley():
    img = skill_bg((28, 34, 48, 255), (166, 210, 238, 255))
    for y in [6, 10]:
        line(img, 3, y, 12, y - 1, (226, 244, 255, 255))
        set_px(img, 13, y - 1, (96, 210, 250, 255))
    line(img, 6, 3, 11, 12, (160, 172, 190, 255))
    line(img, 8, 3, 13, 12, (210, 226, 238, 255))
    return img


def guiding_light_arrow():
    img = skill_bg((42, 40, 30, 255), (255, 226, 116, 255))
    line(img, 3, 8, 12, 8, (255, 255, 226, 255))
    line(img, 10, 5, 13, 8, (255, 255, 226, 255))
    line(img, 10, 11, 13, 8, (255, 255, 226, 255))
    circle(img, 5, 8, 3, (238, 204, 84, 255))
    circle(img, 5, 8, 1, (255, 255, 232, 255))
    return img


def consecrated_slam():
    img = skill_bg((42, 38, 34, 255), (255, 224, 124, 255))
    rect(img, 6, 3, 10, 7, (116, 98, 78, 255))
    rect(img, 4, 7, 12, 9, (210, 180, 100, 255))
    rect(img, 7, 4, 9, 12, (255, 255, 224, 255))
    rect(img, 5, 7, 11, 9, (255, 255, 224, 255))
    line(img, 3, 13, 13, 13, (255, 226, 116, 255))
    return img


def inferno_focus():
    img = skill_bg((72, 26, 18, 255), (255, 112, 32, 255))
    circle(img, 8, 8, 4, (220, 48, 22, 255))
    circle(img, 8, 8, 2, (255, 222, 80, 255))
    line(img, 8, 3, 6, 8, (255, 170, 48, 255))
    line(img, 8, 3, 10, 8, (255, 90, 30, 255))
    set_px(img, 8, 14, (255, 224, 80, 255))
    return img


def eagle_eye():
    img = skill_bg((20, 42, 58, 255), (102, 218, 255, 255))
    circle(img, 8, 8, 4, (54, 108, 148, 255))
    circle(img, 8, 8, 2, (222, 248, 255, 255))
    set_px(img, 8, 8, (32, 76, 108, 255))
    line(img, 3, 8, 13, 8, (172, 234, 255, 255))
    line(img, 8, 3, 8, 13, (172, 234, 255, 255))
    return img


def iron_vanguard():
    img = skill_bg((34, 38, 44, 255), (188, 198, 210, 255))
    rect(img, 4, 4, 11, 11, (74, 88, 106, 255))
    rect(img, 6, 5, 9, 10, (166, 180, 194, 255))
    line(img, 4, 11, 8, 14, (84, 98, 116, 255))
    line(img, 11, 11, 8, 14, (84, 98, 116, 255))
    set_px(img, 8, 3, (236, 244, 250, 255))
    return img


def seraphic_grace():
    img = skill_bg((42, 38, 56, 255), (238, 214, 255, 255))
    rect(img, 7, 3, 9, 12, (255, 255, 232, 255))
    rect(img, 4, 7, 12, 9, (255, 255, 232, 255))
    circle(img, 8, 8, 2, (210, 166, 244, 255))
    line(img, 3, 5, 6, 8, (255, 226, 142, 255))
    line(img, 13, 5, 10, 8, (255, 226, 142, 255))
    return img


def meteor_rite():
    img = skill_bg((80, 26, 18, 255), (255, 138, 36, 255))
    circle(img, 8, 6, 3, (238, 64, 24, 255))
    circle(img, 8, 6, 1, (255, 232, 88, 255))
    line(img, 8, 8, 5, 13, (255, 190, 58, 255))
    line(img, 8, 8, 10, 13, (255, 90, 28, 255))
    line(img, 3, 13, 13, 13, (255, 210, 76, 255))
    set_px(img, 4, 11, (255, 104, 30, 255))
    set_px(img, 12, 10, (255, 104, 30, 255))
    return img


def storm_marksman():
    img = skill_bg((18, 38, 58, 255), (96, 224, 255, 255))
    line(img, 3, 8, 12, 8, (226, 250, 255, 255))
    line(img, 10, 5, 13, 8, (226, 250, 255, 255))
    line(img, 10, 11, 13, 8, (226, 250, 255, 255))
    line(img, 5, 4, 8, 8, (88, 198, 255, 255))
    line(img, 8, 8, 6, 13, (88, 198, 255, 255))
    set_px(img, 9, 6, (255, 255, 180, 255))
    set_px(img, 7, 10, (255, 255, 180, 255))
    return img


def unyielding_colossus():
    img = skill_bg((36, 38, 42, 255), (198, 204, 214, 255))
    rect(img, 5, 3, 10, 11, (78, 88, 104, 255))
    rect(img, 6, 4, 9, 10, (158, 172, 188, 255))
    rect(img, 4, 11, 11, 13, (96, 108, 126, 255))
    line(img, 5, 6, 3, 9, (220, 230, 238, 255))
    line(img, 10, 6, 13, 9, (220, 230, 238, 255))
    set_px(img, 8, 2, (244, 248, 252, 255))
    return img


def sanctuary_descent():
    img = skill_bg((42, 40, 48, 255), (255, 228, 132, 255))
    rect(img, 7, 3, 9, 12, (255, 255, 224, 255))
    rect(img, 4, 7, 12, 9, (255, 255, 224, 255))
    circle(img, 8, 8, 4, (228, 192, 86, 255))
    circle(img, 8, 8, 2, (255, 246, 196, 255))
    line(img, 4, 12, 8, 14, (210, 166, 244, 255))
    line(img, 12, 12, 8, 14, (210, 166, 244, 255))
    return img


def ashen_bulwark():
    img = skill_bg((54, 36, 34, 255), (220, 156, 82, 255))
    rect(img, 5, 4, 10, 11, (76, 70, 68, 255))
    rect(img, 6, 5, 9, 10, (138, 116, 96, 255))
    circle(img, 8, 8, 2, (238, 80, 30, 255))
    line(img, 4, 12, 8, 14, (255, 168, 62, 255))
    line(img, 12, 12, 8, 14, (255, 92, 30, 255))
    set_px(img, 8, 3, (255, 224, 92, 255))
    return img


def class_fire_mage():
    img = skill_bg((74, 26, 18, 255), (255, 126, 34, 255))
    circle(img, 8, 9, 4, (228, 58, 24, 255))
    line(img, 8, 3, 6, 8, (255, 218, 78, 255))
    line(img, 8, 3, 10, 8, (255, 116, 35, 255))
    circle(img, 8, 9, 2, (255, 228, 86, 255))
    return img


def class_archer():
    img = skill_bg((22, 39, 50, 255), (96, 210, 250, 255))
    line(img, 5, 3, 10, 8, (112, 74, 44, 255))
    line(img, 10, 8, 5, 13, (112, 74, 44, 255))
    line(img, 5, 3, 5, 13, (225, 245, 255, 255))
    line(img, 4, 8, 12, 8, (210, 245, 255, 255))
    set_px(img, 13, 8, (96, 210, 250, 255))
    return img


def class_warrior():
    img = skill_bg((36, 38, 44, 255), (190, 198, 208, 255))
    rect(img, 5, 4, 10, 10, (104, 122, 144, 255))
    line(img, 5, 10, 8, 13, (78, 88, 104, 255))
    line(img, 10, 10, 8, 13, (78, 88, 104, 255))
    line(img, 10, 4, 13, 1, (220, 230, 238, 255))
    line(img, 11, 5, 14, 2, (92, 116, 150, 255))
    return img


def class_priest():
    img = skill_bg((44, 40, 30, 255), (255, 226, 124, 255))
    circle(img, 8, 8, 4, (238, 204, 84, 255))
    rect(img, 7, 4, 9, 12, (255, 255, 224, 255))
    rect(img, 4, 7, 12, 9, (255, 255, 224, 255))
    set_px(img, 8, 2, (255, 246, 150, 255))
    set_px(img, 8, 14, (255, 246, 150, 255))
    return img


def class_ashen_warden():
    img = skill_bg((52, 38, 36, 255), (226, 142, 72, 255))
    rect(img, 5, 4, 10, 11, (76, 74, 72, 255))
    rect(img, 6, 5, 9, 10, (150, 134, 118, 255))
    line(img, 4, 11, 8, 14, (96, 84, 78, 255))
    line(img, 11, 11, 8, 14, (96, 84, 78, 255))
    circle(img, 8, 8, 2, (236, 70, 28, 255))
    line(img, 11, 5, 14, 2, (255, 166, 58, 255))
    set_px(img, 8, 3, (255, 224, 88, 255))
    return img


def race_human():
    img = skill_bg((40, 42, 50, 255), (178, 198, 220, 255))
    circle(img, 8, 6, 3, (222, 184, 136, 255))
    rect(img, 5, 9, 11, 13, (88, 124, 174, 255))
    set_px(img, 8, 2, (255, 230, 122, 255))
    set_px(img, 5, 4, (255, 230, 122, 255))
    set_px(img, 11, 4, (255, 230, 122, 255))
    return img


def race_elf():
    img = skill_bg((24, 44, 34, 255), (116, 228, 156, 255))
    line(img, 4, 12, 11, 5, (122, 228, 156, 255))
    line(img, 5, 11, 12, 4, (210, 255, 218, 255))
    circle(img, 8, 8, 2, (84, 162, 116, 255))
    set_px(img, 4, 5, (210, 255, 218, 255))
    set_px(img, 12, 11, (210, 255, 218, 255))
    return img


def race_dwarf():
    img = skill_bg((46, 38, 32, 255), (210, 172, 92, 255))
    rect(img, 5, 4, 10, 9, (132, 96, 58, 255))
    rect(img, 4, 8, 11, 12, (188, 128, 72, 255))
    line(img, 11, 4, 14, 7, (190, 198, 206, 255))
    line(img, 14, 7, 11, 10, (116, 124, 132, 255))
    rect(img, 6, 2, 9, 3, (210, 172, 92, 255))
    return img


def race_orc():
    img = skill_bg((38, 50, 34, 255), (142, 212, 108, 255))
    circle(img, 8, 7, 4, (88, 154, 74, 255))
    rect(img, 5, 10, 11, 13, (72, 114, 62, 255))
    set_px(img, 5, 8, (238, 236, 198, 255))
    set_px(img, 11, 8, (238, 236, 198, 255))
    line(img, 4, 4, 2, 2, (142, 212, 108, 255))
    line(img, 12, 4, 14, 2, (142, 212, 108, 255))
    return img


def ember_staff():
    img = canvas()
    line(img, 5, 14, 10, 4, (120, 70, 42, 255))
    line(img, 6, 14, 11, 4, (81, 47, 31, 255))
    circle(img, 10, 4, 3, (195, 52, 25, 255))
    circle(img, 10, 4, 1, (255, 210, 76, 255))
    return img


def chronicle_recurve():
    img = canvas()
    line(img, 5, 2, 9, 8, (108, 73, 46, 255))
    line(img, 9, 8, 5, 14, (108, 73, 46, 255))
    line(img, 6, 2, 11, 8, (70, 150, 205, 255))
    line(img, 11, 8, 6, 14, (70, 150, 205, 255))
    line(img, 5, 2, 5, 14, (220, 235, 245, 255))
    line(img, 4, 8, 12, 8, (225, 246, 255, 255))
    return img


def snare_launcher():
    img = canvas()
    rect(img, 4, 6, 12, 9, (52, 72, 68, 255))
    rect(img, 3, 8, 6, 11, (44, 50, 55, 255))
    rect(img, 11, 5, 13, 7, (88, 170, 116, 255))
    line(img, 5, 12, 9, 9, (105, 80, 55, 255))
    line(img, 13, 6, 15, 6, (180, 255, 196, 255))
    return img


def runic_blade():
    img = canvas()
    line(img, 4, 14, 12, 4, (184, 204, 214, 255))
    line(img, 5, 14, 13, 4, (84, 118, 152, 255))
    line(img, 7, 11, 11, 15, (94, 60, 42, 255))
    rect(img, 5, 11, 8, 13, (142, 96, 58, 255))
    set_px(img, 9, 7, (108, 218, 255, 255))
    set_px(img, 10, 6, (108, 218, 255, 255))
    set_px(img, 11, 5, (108, 218, 255, 255))
    return img


def sunlit_sigil():
    img = canvas()
    circle(img, 8, 8, 5, (184, 134, 38, 255))
    circle(img, 8, 8, 4, (246, 214, 108, 255))
    rect(img, 7, 3, 9, 13, (255, 252, 214, 255))
    rect(img, 3, 7, 13, 9, (255, 252, 214, 255))
    circle(img, 8, 8, 2, (255, 232, 92, 255))
    for x, y in [(8, 1), (8, 15), (1, 8), (15, 8)]:
        set_px(img, x, y, (255, 226, 116, 220))
    return img


def ember_core():
    img = canvas()
    circle(img, 8, 8, 5, (90, 24, 18, 210))
    circle(img, 8, 8, 4, (210, 56, 22, 255))
    circle(img, 8, 8, 2, (255, 185, 54, 255))
    set_px(img, 7, 6, (255, 238, 116, 255))
    set_px(img, 9, 7, (255, 238, 116, 255))
    set_px(img, 5, 10, (255, 104, 28, 255))
    set_px(img, 11, 10, (255, 104, 28, 255))
    for x, y in [(4, 5), (12, 6), (5, 13), (13, 11)]:
        set_px(img, x, y, (255, 155, 44, 220))
    return img


def fireball_projectile():
    img = canvas()
    circle(img, 8, 8, 6, (255, 78, 26, 170))
    circle(img, 8, 8, 4, (255, 126, 34, 230))
    circle(img, 8, 8, 2, (255, 238, 98, 255))
    line(img, 3, 8, 0, 6, (255, 94, 28, 145))
    line(img, 3, 10, 0, 13, (255, 138, 38, 155))
    line(img, 5, 4, 2, 1, (255, 196, 70, 160))
    line(img, 12, 5, 15, 3, (255, 110, 30, 130))
    for x, y in [(6, 5), (10, 6), (5, 10), (11, 11), (8, 3)]:
        set_px(img, x, y, (255, 248, 172, 255))
    return img


def main():
    skill_icons = {
        "fireball": fireball(),
        "ember_burst": ember_burst(),
        "flame_step": flame_step(),
        "charged_shot": charged_shot(),
        "scatter_shot": scatter_shot(),
        "snare_shot": snare_shot(),
        "flame_arrow": flame_arrow(),
        "lunge_strike": lunge_strike(),
        "guard_stance": guard_stance(),
        "ground_slam": ground_slam(),
        "mend": mend(),
        "holy_nova": holy_nova(),
        "blessing": blessing(),
        "blazing_charge": blazing_charge(),
        "sunfire_aegis": sunfire_aegis(),
        "piercing_volley": piercing_volley(),
        "guiding_light_arrow": guiding_light_arrow(),
        "consecrated_slam": consecrated_slam(),
        "inferno_focus": inferno_focus(),
        "eagle_eye": eagle_eye(),
        "iron_vanguard": iron_vanguard(),
        "seraphic_grace": seraphic_grace(),
        "meteor_rite": meteor_rite(),
        "storm_marksman": storm_marksman(),
        "unyielding_colossus": unyielding_colossus(),
        "sanctuary_descent": sanctuary_descent(),
        "ashen_bulwark": ashen_bulwark(),
    }
    item_icons = {
        "ember_staff": ember_staff(),
        "chronicle_recurve": chronicle_recurve(),
        "snare_launcher": snare_launcher(),
        "runic_blade": runic_blade(),
        "sunlit_sigil": sunlit_sigil(),
        "ember_core": ember_core(),
    }
    for name, pixels in skill_icons.items():
        png(TEXTURE_ROOT / f"gui/skill/{name}.png", pixels)
    class_icons = {
        "fire_mage": class_fire_mage(),
        "archer": class_archer(),
        "warrior": class_warrior(),
        "priest": class_priest(),
        "ashen_warden": class_ashen_warden(),
    }
    for name, pixels in class_icons.items():
        png(TEXTURE_ROOT / f"gui/class/{name}.png", pixels)
    race_icons = {
        "human": race_human(),
        "elf": race_elf(),
        "dwarf": race_dwarf(),
        "orc": race_orc(),
    }
    for name, pixels in race_icons.items():
        png(TEXTURE_ROOT / f"gui/race/{name}.png", pixels)
    for name, pixels in item_icons.items():
        png(TEXTURE_ROOT / f"item/{name}.png", pixels)
    projectile_textures = {
        "fireball": fireball_projectile(),
    }
    for name, pixels in projectile_textures.items():
        png(TEXTURE_ROOT / f"entity/career_projectile/{name}.png", pixels)
    print(f"generated {len(skill_icons)} skill icons, {len(class_icons)} class icons, "
          f"{len(race_icons)} race icons, {len(item_icons)} item textures and "
          f"{len(projectile_textures)} projectile textures")


if __name__ == "__main__":
    main()
