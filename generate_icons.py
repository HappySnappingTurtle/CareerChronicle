#!/usr/bin/env python3
"""
Generate 32x32 pixel art PNG icons for Career Chronicle Minecraft Forge MOD.
Uses only Python standard library (struct, zlib) for PNG generation.
All icons are 32x32 RGBA with transparent backgrounds.
"""

import struct
import zlib
import os

# ─── PNG Writer ──────────────────────────────────────────────────────────────

def write_png(filepath, pixels, width=32, height=32):
    """Write a 32x32 RGBA image as PNG. pixels is a list of height rows,
    each row is a list of width (R,G,B,A) tuples."""
    os.makedirs(os.path.dirname(filepath), exist_ok=True)

    def chunk(chunk_type, data):
        c = chunk_type + data
        crc = struct.pack('>I', zlib.crc32(c) & 0xFFFFFFFF)
        return struct.pack('>I', len(data)) + c + crc

    signature = b'\x89PNG\r\n\x1a\n'
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    ihdr = chunk(b'IHDR', ihdr_data)

    raw = b''
    for row in pixels:
        raw += b'\x00'  # filter none
        for r, g, b, a in row:
            raw += struct.pack('BBBB', r, g, b, a)

    compressed = zlib.compress(raw)
    idat = chunk(b'IDAT', compressed)
    iend = chunk(b'IEND', b'')

    with open(filepath, 'wb') as f:
        f.write(signature + ihdr + idat + iend)


# ─── Canvas Helpers ──────────────────────────────────────────────────────────

def blank():
    """Return a 32x32 transparent canvas."""
    return [[(0, 0, 0, 0) for _ in range(32)] for _ in range(32)]

def set_pixel(canvas, x, y, color):
    """Set pixel if in bounds."""
    if 0 <= x < 32 and 0 <= y < 32:
        canvas[y][x] = color

def fill_rect(canvas, x0, y0, w, h, color):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            set_pixel(canvas, x, y, color)

def draw_rect(canvas, x0, y0, w, h, color):
    for x in range(x0, x0 + w):
        set_pixel(canvas, x, y0, color)
        set_pixel(canvas, x, y0 + h - 1, color)
    for y in range(y0, y0 + h):
        set_pixel(canvas, x0, y, color)
        set_pixel(canvas, x0 + w - 1, y, color)

def draw_circle(canvas, cx, cy, r, color):
    for y in range(32):
        for x in range(32):
            dx, dy = x - cx, y - cy
            dist = (dx*dx + dy*dy) ** 0.5
            if abs(dist - r) < 0.8:
                set_pixel(canvas, x, y, color)

def fill_circle(canvas, cx, cy, r, color):
    for y in range(32):
        for x in range(32):
            dx, dy = x - cx, y - cy
            if dx*dx + dy*dy <= r*r:
                set_pixel(canvas, x, y, color)

def draw_line(canvas, x0, y0, x1, y1, color):
    """Bresenham line."""
    dx = abs(x1 - x0)
    dy = abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx - dy
    while True:
        set_pixel(canvas, x0, y0, color)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 > -dy:
            err -= dy
            x0 += sx
        if e2 < dx:
            err += dx
            y0 += sy

def draw_triangle(canvas, x0, y0, x1, y1, x2, y2, color):
    draw_line(canvas, x0, y0, x1, y1, color)
    draw_line(canvas, x1, y1, x2, y2, color)
    draw_line(canvas, x2, y2, x0, y0, color)

def fill_triangle(canvas, x0, y0, x1, y1, x2, y2, color):
    """Scanline fill triangle."""
    min_y = max(0, min(y0, y1, y2))
    max_y = min(31, max(y0, y1, y2))
    for y in range(min_y, max_y + 1):
        xs = []
        edges = [(x0, y0, x1, y1), (x1, y1, x2, y2), (x2, y2, x0, y0)]
        for ex0, ey0, ex1, ey1 in edges:
            if ey0 == ey1:
                if y == ey0:
                    xs.extend([ex0, ex1])
                continue
            if min(ey0, ey1) <= y <= max(ey0, ey1):
                x = ex0 + (y - ey0) * (ex1 - ex0) / (ey1 - ey0)
                xs.append(int(x))
        if len(xs) >= 2:
            xs.sort()
            for x in range(max(0, xs[0]), min(31, xs[-1]) + 1):
                set_pixel(canvas, x, y, color)

def draw_diamond(canvas, cx, cy, r, color):
    draw_line(canvas, cx, cy - r, cx + r, cy, color)
    draw_line(canvas, cx + r, cy, cx, cy + r, color)
    draw_line(canvas, cx, cy + r, cx - r, cy, color)
    draw_line(canvas, cx - r, cy, cx, cy - r, color)

def fill_diamond(canvas, cx, cy, r, color):
    for y in range(cy - r, cy + r + 1):
        half = r - abs(y - cy)
        for x in range(cx - half, cx + half + 1):
            set_pixel(canvas, x, y, color)

def draw_cross(canvas, cx, cy, size, thickness, color):
    half_t = thickness // 2
    fill_rect(canvas, cx - size, cy - half_t, size * 2 + 1, thickness, color)
    fill_rect(canvas, cx - half_t, cy - size, thickness, size * 2 + 1, color)

def draw_star(canvas, cx, cy, r_out, r_in, points, color):
    """Draw a star outline."""
    import math
    angles = []
    for i in range(points * 2):
        a = math.pi * 2 * i / (points * 2) - math.pi / 2
        rad = r_out if i % 2 == 0 else r_in
        angles.append((int(cx + rad * math.cos(a)), int(cy + rad * math.sin(a))))
    for i in range(len(angles)):
        x0, y0 = angles[i]
        x1, y1 = angles[(i + 1) % len(angles)]
        draw_line(canvas, x0, y0, x1, y1, color)


# ─── Color Palettes ──────────────────────────────────────────────────────────

# Fire Mage (warm orange/red)
C_FIRE_BRIGHT = (255, 160, 30, 255)
C_FIRE_CORE = (255, 100, 20, 255)
C_FIRE_DARK = (200, 60, 10, 255)
C_FIRE_GLOW = (255, 200, 80, 255)
C_FIRE_HOT = (255, 230, 120, 255)

# Archer (green/teal)
C_ARCHER_LIGHT = (100, 200, 130, 255)
C_ARCHER = (50, 160, 100, 255)
C_ARCHER_DARK = (30, 120, 70, 255)
C_ARCHER_TEAL = (60, 180, 160, 255)

# Warrior (steel/brown)
C_STEEL_LIGHT = (200, 200, 210, 255)
C_STEEL = (160, 160, 175, 255)
C_STEEL_DARK = (110, 110, 130, 255)
C_BROWN = (140, 100, 60, 255)
C_BROWN_DARK = (100, 70, 40, 255)

# Priest (gold/white)
C_GOLD_BRIGHT = (255, 220, 80, 255)
C_GOLD = (220, 180, 50, 255)
C_GOLD_DARK = (180, 140, 30, 255)
C_WHITE = (240, 240, 240, 255)
C_HOLY_GLOW = (255, 255, 200, 255)

# Fusion (golden accent)
C_FUSION_GOLD = (255, 200, 60, 255)
C_FUSION_ORANGE = (255, 150, 40, 255)

# Guardian (silver/steel blue)
C_GUARD_LIGHT = (180, 200, 220, 255)
C_GUARD = (130, 155, 185, 255)
C_GUARD_DARK = (90, 110, 145, 255)
C_GUARD_ACCENT = (160, 180, 210, 255)

# Rogue (dark purple)
C_ROGUE_LIGHT = (160, 100, 200, 255)
C_ROGUE = (120, 60, 170, 255)
C_ROGUE_DARK = (80, 30, 120, 255)
C_ROGUE_SHADOW = (50, 20, 80, 255)

# Ice Mage (ice blue/cyan)
C_ICE_BRIGHT = (180, 230, 255, 255)
C_ICE = (120, 200, 240, 255)
C_ICE_DARK = (70, 150, 200, 255)
C_ICE_WHITE = (220, 245, 255, 255)

# Necromancer (dark green/black)
C_NECRO_GREEN = (80, 160, 60, 255)
C_NECRO = (50, 120, 40, 255)
C_NECRO_DARK = (30, 80, 20, 255)
C_NECRO_BLACK = (20, 30, 15, 255)
C_BONE = (220, 210, 180, 255)

# Ashen
C_ASHEN_ORANGE = (220, 130, 50, 255)
C_ASHEN_GREY = (140, 130, 120, 255)
C_ASHEN_DARK = (80, 70, 60, 255)

# Race colors
C_HUMAN = (220, 190, 160, 255)
C_ELF_GREEN = (100, 180, 100, 255)
C_DWARF_BROWN = (160, 120, 70, 255)
C_ORC_GREEN = (120, 160, 80, 255)
C_UNDEAD_GREY = (140, 160, 130, 255)
C_DEMON_RED = (180, 50, 40, 255)

C_BLACK = (30, 30, 30, 255)
C_DARK_GREY = (60, 60, 60, 255)
C_GREY = (120, 120, 120, 255)
C_LIGHT_GREY = (180, 180, 180, 255)
C_OUTLINE = (40, 40, 40, 255)


# ─── Skill Icon Generators ──────────────────────────────────────────────────

# === FIRE MAGE ===

def icon_fireball():
    c = blank()
    fill_circle(c, 16, 16, 9, C_FIRE_CORE)
    fill_circle(c, 16, 16, 6, C_FIRE_BRIGHT)
    fill_circle(c, 16, 16, 3, C_FIRE_HOT)
    # glow wisps
    for dx, dy in [(-10, -4), (10, -2), (-8, 6), (9, 5), (0, -11), (0, 11)]:
        set_pixel(c, 16+dx, 16+dy, C_FIRE_GLOW)
    draw_circle(c, 16, 16, 10, C_FIRE_DARK)
    return c

def icon_ember_burst():
    c = blank()
    # center core
    fill_circle(c, 16, 16, 4, C_FIRE_HOT)
    fill_circle(c, 16, 16, 3, C_FIRE_BRIGHT)
    # expanding flame ring
    draw_circle(c, 16, 16, 9, C_FIRE_CORE)
    draw_circle(c, 16, 16, 10, C_FIRE_BRIGHT)
    draw_circle(c, 16, 16, 12, C_FIRE_DARK)
    # flame tips radiating
    for angle_idx in range(8):
        import math
        a = math.pi * 2 * angle_idx / 8
        for r in range(10, 14):
            x = int(16 + r * math.cos(a))
            y = int(16 + r * math.sin(a))
            set_pixel(c, x, y, C_FIRE_BRIGHT)
    return c

def icon_flame_step():
    c = blank()
    # foot shape (boot)
    fill_rect(c, 8, 10, 10, 5, C_BROWN)
    fill_rect(c, 7, 15, 12, 4, C_BROWN)
    fill_rect(c, 6, 19, 14, 3, C_BROWN_DARK)
    # sole
    fill_rect(c, 5, 22, 16, 2, C_BROWN_DARK)
    # flame trail behind/below
    for x in range(6, 22):
        set_pixel(c, x, 24, C_FIRE_BRIGHT)
        set_pixel(c, x, 25, C_FIRE_CORE)
    for x in range(8, 20):
        set_pixel(c, x, 26, C_FIRE_DARK)
    # small flames rising
    for bx in [8, 12, 16, 19]:
        set_pixel(c, bx, 23, C_FIRE_HOT)
        set_pixel(c, bx, 22, C_FIRE_GLOW)
    # outline
    draw_rect(c, 7, 9, 12, 15, C_OUTLINE)
    return c

def icon_inferno_focus():
    c = blank()
    # outer glow ring
    draw_circle(c, 16, 16, 12, C_FIRE_DARK)
    draw_circle(c, 16, 16, 11, C_FIRE_CORE)
    # inner orb
    fill_circle(c, 16, 16, 7, C_FIRE_CORE)
    fill_circle(c, 16, 16, 5, C_FIRE_BRIGHT)
    fill_circle(c, 16, 16, 3, C_FIRE_HOT)
    fill_circle(c, 16, 16, 1, C_FIRE_GLOW)
    # concentration lines
    for dx in [-9, 9]:
        draw_line(c, 16+dx, 16-2, 16+dx//2, 16, C_FIRE_GLOW)
        draw_line(c, 16+dx, 16+2, 16+dx//2, 16, C_FIRE_GLOW)
    return c

def icon_meteor_rite():
    c = blank()
    # meteor body (top-right falling to bottom-left)
    fill_circle(c, 20, 10, 5, C_FIRE_CORE)
    fill_circle(c, 20, 10, 3, C_FIRE_BRIGHT)
    fill_circle(c, 20, 10, 1, C_FIRE_HOT)
    # trail going upper-right
    for i in range(1, 10):
        x = 20 + i
        y = 10 - i
        if 0 <= x < 32 and 0 <= y < 32:
            set_pixel(c, x, y, C_FIRE_DARK)
            if 0 <= x-1 < 32:
                set_pixel(c, x-1, y, C_FIRE_CORE)
            if 0 <= y+1 < 32:
                set_pixel(c, x, y+1, C_FIRE_CORE)
    # impact area at bottom
    draw_line(c, 6, 26, 26, 26, C_FIRE_DARK)
    for x in range(10, 23):
        set_pixel(c, x, 25, C_FIRE_BRIGHT)
        set_pixel(c, x, 27, C_FIRE_GLOW)
    # motion lines
    draw_line(c, 18, 14, 12, 22, C_FIRE_CORE)
    draw_line(c, 22, 12, 16, 20, C_FIRE_CORE)
    return c

# === ARCHER ===

def draw_arrow(canvas, x0, y0, x1, y1, color, head_color=None):
    """Draw arrow from tail to head."""
    if head_color is None:
        head_color = color
    draw_line(canvas, x0, y0, x1, y1, color)
    # arrowhead
    dx = x1 - x0
    dy = y1 - y0
    length = max(1, (dx*dx + dy*dy) ** 0.5)
    ndx, ndy = dx / length, dy / length
    px, py = -ndy, ndx  # perpendicular
    for s in range(1, 4):
        hx = int(x1 - ndx * s + px * s * 0.6)
        hy = int(y1 - ndy * s + py * s * 0.6)
        set_pixel(canvas, hx, hy, head_color)
        hx = int(x1 - ndx * s - px * s * 0.6)
        hy = int(y1 - ndy * s - py * s * 0.6)
        set_pixel(canvas, hx, hy, head_color)

def icon_charged_shot():
    c = blank()
    # arrow shaft
    draw_line(c, 6, 26, 26, 6, C_ARCHER)
    # arrowhead
    fill_triangle(c, 26, 6, 22, 6, 26, 10, C_ARCHER_DARK)
    # fletching
    draw_line(c, 6, 26, 8, 28, C_ARCHER_LIGHT)
    draw_line(c, 6, 26, 4, 24, C_ARCHER_LIGHT)
    # energy glow around tip
    draw_circle(c, 25, 7, 4, C_ARCHER_TEAL)
    set_pixel(c, 25, 5, C_ARCHER_LIGHT)
    set_pixel(c, 27, 7, C_ARCHER_LIGHT)
    set_pixel(c, 25, 9, C_ARCHER_LIGHT)
    return c

def icon_scatter_shot():
    c = blank()
    # three arrows diverging from left
    # center arrow
    draw_line(c, 4, 16, 28, 16, C_ARCHER)
    fill_triangle(c, 28, 16, 24, 14, 24, 18, C_ARCHER_DARK)
    # upper arrow
    draw_line(c, 4, 16, 26, 8, C_ARCHER)
    fill_triangle(c, 26, 8, 22, 8, 24, 12, C_ARCHER_DARK)
    # lower arrow
    draw_line(c, 4, 16, 26, 24, C_ARCHER)
    fill_triangle(c, 26, 24, 22, 24, 24, 20, C_ARCHER_DARK)
    # origin point
    fill_circle(c, 4, 16, 2, C_ARCHER_DARK)
    return c

def icon_snare_shot():
    c = blank()
    # arrow shaft
    draw_line(c, 4, 24, 22, 8, C_ARCHER)
    fill_triangle(c, 22, 8, 18, 8, 20, 12, C_ARCHER_DARK)
    # net/vine at arrowhead
    draw_rect(c, 19, 5, 10, 10, C_ARCHER_LIGHT)
    # cross pattern inside net
    draw_line(c, 19, 10, 28, 10, C_ARCHER_LIGHT)
    draw_line(c, 24, 5, 24, 14, C_ARCHER_LIGHT)
    # vine tendrils
    set_pixel(c, 20, 7, C_ARCHER)
    set_pixel(c, 27, 7, C_ARCHER)
    set_pixel(c, 20, 13, C_ARCHER)
    set_pixel(c, 27, 13, C_ARCHER)
    return c

def icon_eagle_eye():
    c = blank()
    # eye shape
    # upper lid
    for i in range(12):
        x = 10 + i
        y = 16 - int(4 * (1 - ((i - 6) / 6.0) ** 2) ** 0.5) if abs(i - 6) <= 6 else 16
        set_pixel(c, x, y, C_ARCHER_DARK)
    # lower lid
    for i in range(12):
        x = 10 + i
        y = 16 + int(4 * (1 - ((i - 6) / 6.0) ** 2) ** 0.5) if abs(i - 6) <= 6 else 16
        set_pixel(c, x, y, C_ARCHER_DARK)
    # iris
    fill_circle(c, 16, 16, 3, C_ARCHER)
    fill_circle(c, 16, 16, 1, C_ARCHER_DARK)
    # crosshair lines
    draw_line(c, 16, 8, 16, 12, C_ARCHER_TEAL)
    draw_line(c, 16, 20, 16, 24, C_ARCHER_TEAL)
    draw_line(c, 8, 16, 12, 16, C_ARCHER_TEAL)
    draw_line(c, 20, 16, 24, 16, C_ARCHER_TEAL)
    return c

def icon_storm_marksman():
    c = blank()
    # arrow going up
    draw_line(c, 16, 28, 16, 6, C_ARCHER)
    fill_triangle(c, 16, 4, 13, 9, 19, 9, C_ARCHER_DARK)
    # lightning bolt around arrow
    # left bolt
    draw_line(c, 8, 8, 12, 14, C_GOLD_BRIGHT)
    draw_line(c, 12, 14, 9, 14, C_GOLD_BRIGHT)
    draw_line(c, 9, 14, 13, 22, C_GOLD_BRIGHT)
    # right bolt
    draw_line(c, 24, 10, 20, 16, C_GOLD_BRIGHT)
    draw_line(c, 20, 16, 23, 16, C_GOLD_BRIGHT)
    draw_line(c, 23, 16, 19, 24, C_GOLD_BRIGHT)
    return c

# === WARRIOR ===

def icon_lunge_strike():
    c = blank()
    # sword blade (diagonal thrust)
    draw_line(c, 6, 26, 26, 6, C_STEEL_LIGHT)
    draw_line(c, 7, 26, 27, 6, C_STEEL)
    # sword tip
    set_pixel(c, 27, 5, C_STEEL_LIGHT)
    set_pixel(c, 28, 4, C_WHITE)
    # crossguard
    draw_line(c, 8, 22, 14, 22, C_BROWN)
    draw_line(c, 10, 20, 10, 24, C_BROWN)
    # handle
    draw_line(c, 5, 27, 8, 24, C_BROWN_DARK)
    # motion lines
    draw_line(c, 24, 10, 28, 10, C_STEEL_DARK)
    draw_line(c, 22, 8, 28, 8, C_STEEL_DARK)
    return c

def icon_guard_stance():
    c = blank()
    # shield shape
    fill_rect(c, 10, 6, 12, 18, C_STEEL)
    fill_rect(c, 11, 7, 10, 16, C_STEEL_LIGHT)
    # shield boss (center circle)
    fill_circle(c, 16, 14, 3, C_GOLD)
    fill_circle(c, 16, 14, 1, C_GOLD_BRIGHT)
    # shield edges
    draw_rect(c, 9, 5, 14, 20, C_STEEL_DARK)
    # bottom V shape
    draw_line(c, 9, 24, 16, 28, C_STEEL_DARK)
    draw_line(c, 23, 24, 16, 28, C_STEEL_DARK)
    fill_triangle(c, 10, 24, 22, 24, 16, 28, C_STEEL)
    # glow
    for dx in [-5, 5]:
        for dy in [-3, 3]:
            set_pixel(c, 16+dx, 14+dy, C_GOLD_BRIGHT)
    return c

def icon_ground_slam():
    c = blank()
    # fist
    fill_rect(c, 12, 6, 8, 8, C_BROWN)
    fill_rect(c, 13, 7, 6, 6, C_HUMAN)
    # knuckles
    for x in [13, 15, 17]:
        set_pixel(c, x, 6, C_HUMAN)
    # arm
    fill_rect(c, 14, 2, 4, 5, C_BROWN)
    # impact ground
    fill_rect(c, 4, 18, 24, 2, C_STEEL_DARK)
    # crack lines
    draw_line(c, 16, 20, 10, 26, C_BROWN_DARK)
    draw_line(c, 16, 20, 22, 26, C_BROWN_DARK)
    draw_line(c, 16, 20, 16, 28, C_BROWN_DARK)
    draw_line(c, 16, 20, 6, 24, C_BROWN_DARK)
    draw_line(c, 16, 20, 26, 24, C_BROWN_DARK)
    # impact sparks
    for dx, dy in [(-6, -2), (6, -2), (-4, 0), (4, 0)]:
        set_pixel(c, 16+dx, 18+dy, C_GOLD_BRIGHT)
    return c

def icon_iron_vanguard():
    c = blank()
    # shield (left half)
    fill_rect(c, 4, 6, 12, 18, C_STEEL)
    draw_rect(c, 3, 5, 14, 20, C_STEEL_DARK)
    fill_triangle(c, 4, 24, 16, 24, 10, 28, C_STEEL)
    # shield cross
    draw_line(c, 10, 8, 10, 22, C_GOLD)
    draw_line(c, 5, 14, 15, 14, C_GOLD)
    # sword (right, behind shield)
    draw_line(c, 18, 24, 28, 4, C_STEEL_LIGHT)
    draw_line(c, 19, 24, 29, 4, C_STEEL)
    # sword crossguard
    draw_line(c, 20, 18, 26, 18, C_BROWN)
    # handle
    draw_line(c, 17, 26, 19, 24, C_BROWN_DARK)
    return c

def icon_unyielding_colossus():
    c = blank()
    # large tower shield
    fill_rect(c, 6, 3, 20, 22, C_STEEL)
    fill_rect(c, 7, 4, 18, 20, C_STEEL_LIGHT)
    draw_rect(c, 5, 2, 22, 24, C_STEEL_DARK)
    # bottom point
    fill_triangle(c, 6, 25, 26, 25, 16, 30, C_STEEL)
    draw_line(c, 5, 25, 16, 30, C_STEEL_DARK)
    draw_line(c, 27, 25, 16, 30, C_STEEL_DARK)
    # central emblem
    fill_diamond(c, 16, 14, 5, C_GOLD)
    fill_diamond(c, 16, 14, 3, C_GOLD_BRIGHT)
    # rivets
    for pos in [(8, 5), (24, 5), (8, 22), (24, 22)]:
        set_pixel(c, pos[0], pos[1], C_STEEL_DARK)
    return c

# === PRIEST ===

def icon_mend():
    c = blank()
    # heart shape
    fill_circle(c, 12, 12, 5, C_GOLD)
    fill_circle(c, 20, 12, 5, C_GOLD)
    fill_triangle(c, 7, 14, 25, 14, 16, 26, C_GOLD)
    # inner bright
    fill_circle(c, 12, 12, 3, C_GOLD_BRIGHT)
    fill_circle(c, 20, 12, 3, C_GOLD_BRIGHT)
    fill_triangle(c, 9, 14, 23, 14, 16, 23, C_GOLD_BRIGHT)
    # cross on top
    draw_cross(c, 16, 14, 3, 2, C_WHITE)
    return c

def icon_holy_nova():
    c = blank()
    # center bright point
    fill_circle(c, 16, 16, 3, C_WHITE)
    fill_circle(c, 16, 16, 2, C_HOLY_GLOW)
    # radiating lines (8 directions)
    import math
    for i in range(8):
        a = math.pi * 2 * i / 8
        for r in range(5, 13):
            x = int(16 + r * math.cos(a))
            y = int(16 + r * math.sin(a))
            col = C_GOLD_BRIGHT if r < 9 else C_GOLD
            set_pixel(c, x, y, col)
    # outer glow dots
    for i in range(8):
        a = math.pi * 2 * i / 8 + math.pi / 8
        x = int(16 + 11 * math.cos(a))
        y = int(16 + 11 * math.sin(a))
        set_pixel(c, x, y, C_GOLD)
    return c

def icon_blessing():
    c = blank()
    # two raised hands
    # left hand
    fill_rect(c, 8, 14, 5, 8, C_HUMAN)
    for i in range(3):
        fill_rect(c, 8 + i * 2, 11, 1, 4, C_HUMAN)
    # right hand
    fill_rect(c, 19, 14, 5, 8, C_HUMAN)
    for i in range(3):
        fill_rect(c, 19 + i * 2, 11, 1, 4, C_HUMAN)
    # light from above
    fill_triangle(c, 16, 2, 8, 12, 24, 12, C_GOLD_BRIGHT)
    fill_triangle(c, 16, 4, 10, 12, 22, 12, C_HOLY_GLOW)
    # rays
    for x in [10, 16, 22]:
        draw_line(c, x, 2, x, 10, C_WHITE)
    return c

def icon_seraphic_grace():
    c = blank()
    # angel wing (single wing, stylized)
    # main feather curves (left wing)
    for i in range(8):
        y = 8 + i * 2
        w = max(1, 10 - i)
        fill_rect(c, 16 - w, y, w, 2, C_WHITE)
    # right wing mirror
    for i in range(8):
        y = 8 + i * 2
        w = max(1, 10 - i)
        fill_rect(c, 16, y, w, 2, C_WHITE)
    # golden highlights
    for i in range(6):
        y = 10 + i * 2
        set_pixel(c, 16 - 8 + i, y, C_GOLD_BRIGHT)
        set_pixel(c, 16 + 8 - i, y, C_GOLD_BRIGHT)
    # top curve
    draw_line(c, 6, 10, 10, 6, C_GOLD)
    draw_line(c, 26, 10, 22, 6, C_GOLD)
    # center halo
    draw_circle(c, 16, 6, 3, C_GOLD_BRIGHT)
    return c

def icon_sanctuary_descent():
    c = blank()
    # pillar of light from top
    fill_rect(c, 12, 0, 8, 28, C_GOLD_BRIGHT)
    fill_rect(c, 13, 0, 6, 28, C_HOLY_GLOW)
    fill_rect(c, 14, 0, 4, 28, C_WHITE)
    # widening at bottom
    fill_triangle(c, 8, 24, 24, 24, 16, 20, C_GOLD_BRIGHT)
    fill_rect(c, 8, 24, 16, 4, C_GOLD)
    # sparkles in pillar
    for pos in [(15, 4), (17, 8), (14, 12), (18, 16), (15, 20)]:
        set_pixel(c, pos[0], pos[1], C_WHITE)
    # ground glow
    draw_line(c, 4, 28, 28, 28, C_GOLD)
    return c

# === FUSION ===

def icon_flame_arrow():
    c = blank()
    # arrow shaft
    draw_line(c, 4, 26, 26, 6, C_ARCHER_DARK)
    draw_line(c, 5, 26, 27, 6, C_ARCHER)
    # arrowhead
    fill_triangle(c, 27, 5, 23, 5, 26, 10, C_ARCHER_DARK)
    # flame on arrow
    fill_circle(c, 24, 8, 3, C_FIRE_CORE)
    fill_circle(c, 24, 8, 2, C_FIRE_BRIGHT)
    set_pixel(c, 24, 5, C_FIRE_HOT)
    set_pixel(c, 22, 7, C_FIRE_GLOW)
    set_pixel(c, 26, 6, C_FIRE_GLOW)
    # trailing fire
    for i in range(3):
        set_pixel(c, 20 - i*3, 12 + i*3, C_FIRE_CORE)
        set_pixel(c, 19 - i*3, 13 + i*3, C_FIRE_BRIGHT)
    return c

def icon_blazing_charge():
    c = blank()
    # sword pointing right
    draw_line(c, 4, 16, 26, 16, C_STEEL_LIGHT)
    draw_line(c, 4, 17, 26, 17, C_STEEL)
    # blade tip
    set_pixel(c, 27, 16, C_STEEL_LIGHT)
    set_pixel(c, 28, 16, C_WHITE)
    # crossguard
    fill_rect(c, 8, 13, 2, 8, C_BROWN)
    # handle
    fill_rect(c, 3, 15, 5, 4, C_BROWN_DARK)
    # fire trail
    for y in range(12, 21):
        for x in range(12, 28):
            if abs(y - 16) > 1 and abs(y - 16) < 5:
                if (x + y) % 3 == 0:
                    col = C_FIRE_BRIGHT if abs(y-16) < 3 else C_FIRE_CORE
                    set_pixel(c, x, y, col)
    # fire envelope around blade
    for x in range(12, 27):
        set_pixel(c, x, 14, C_FIRE_CORE)
        set_pixel(c, x, 19, C_FIRE_CORE)
    return c

def icon_sunfire_aegis():
    c = blank()
    # shield shape
    fill_rect(c, 8, 6, 16, 16, C_FUSION_GOLD)
    fill_rect(c, 9, 7, 14, 14, C_FUSION_ORANGE)
    draw_rect(c, 7, 5, 18, 18, C_FIRE_DARK)
    # bottom V
    fill_triangle(c, 8, 22, 24, 22, 16, 28, C_FUSION_GOLD)
    draw_line(c, 7, 22, 16, 28, C_FIRE_DARK)
    draw_line(c, 25, 22, 16, 28, C_FIRE_DARK)
    # sun emblem in center
    fill_circle(c, 16, 14, 4, C_FIRE_HOT)
    fill_circle(c, 16, 14, 2, C_FIRE_GLOW)
    # sun rays
    import math
    for i in range(8):
        a = math.pi * 2 * i / 8
        x = int(16 + 6 * math.cos(a))
        y = int(14 + 6 * math.sin(a))
        set_pixel(c, x, y, C_FIRE_HOT)
    return c

def icon_piercing_volley():
    c = blank()
    # five arrows pointing right, parallel
    for i in range(5):
        y = 6 + i * 5
        draw_line(c, 4, y, 24, y, C_ARCHER)
        # arrowhead
        fill_triangle(c, 26, y, 23, y-2, 23, y+2, C_ARCHER_DARK)
    # speed lines
    for i in range(5):
        y = 6 + i * 5
        set_pixel(c, 2, y, C_ARCHER_LIGHT)
        set_pixel(c, 1, y, C_ARCHER_TEAL)
    return c

def icon_guiding_light_arrow():
    c = blank()
    # arrow shaft diagonal
    draw_line(c, 6, 26, 26, 6, C_ARCHER)
    # arrowhead
    fill_triangle(c, 26, 5, 22, 5, 25, 10, C_ARCHER_DARK)
    # glowing aura around entire arrow
    for i in range(0, 20, 2):
        import math
        t = i / 20.0
        x = int(6 + (26-6) * t)
        y = int(26 + (6-26) * t)
        set_pixel(c, x-1, y, C_GOLD_BRIGHT)
        set_pixel(c, x+1, y, C_GOLD_BRIGHT)
        set_pixel(c, x, y-1, C_GOLD_BRIGHT)
        set_pixel(c, x, y+1, C_GOLD_BRIGHT)
    # bright tip glow
    fill_circle(c, 26, 6, 2, C_HOLY_GLOW)
    return c

def icon_consecrated_slam():
    c = blank()
    # hammer head
    fill_rect(c, 8, 4, 16, 8, C_STEEL)
    fill_rect(c, 9, 5, 14, 6, C_STEEL_LIGHT)
    draw_rect(c, 7, 3, 18, 10, C_STEEL_DARK)
    # handle
    fill_rect(c, 15, 12, 3, 14, C_BROWN)
    fill_rect(c, 14, 26, 5, 2, C_BROWN_DARK)
    # holy cross on hammer face
    draw_cross(c, 16, 8, 2, 1, C_GOLD_BRIGHT)
    # impact glow at bottom
    draw_line(c, 8, 28, 24, 28, C_GOLD)
    for x in [10, 14, 18, 22]:
        set_pixel(c, x, 27, C_HOLY_GLOW)
    return c

# === GUARDIAN ===

def icon_shield_wall():
    c = blank()
    # tall narrow shield
    fill_rect(c, 8, 2, 16, 24, C_GUARD)
    fill_rect(c, 9, 3, 14, 22, C_GUARD_LIGHT)
    draw_rect(c, 7, 1, 18, 26, C_GUARD_DARK)
    # bottom point
    fill_triangle(c, 8, 26, 24, 26, 16, 30, C_GUARD)
    draw_line(c, 7, 26, 16, 30, C_GUARD_DARK)
    draw_line(c, 25, 26, 16, 30, C_GUARD_DARK)
    # vertical line
    draw_line(c, 16, 3, 16, 28, C_GUARD_DARK)
    # horizontal bands
    draw_line(c, 8, 10, 24, 10, C_GUARD_DARK)
    draw_line(c, 8, 18, 24, 18, C_GUARD_DARK)
    return c

def icon_provoke():
    c = blank()
    # face circle
    fill_circle(c, 16, 14, 9, C_GUARD)
    fill_circle(c, 16, 14, 7, C_GUARD_LIGHT)
    draw_circle(c, 16, 14, 9, C_GUARD_DARK)
    # angry eyes (V shapes)
    draw_line(c, 10, 11, 13, 13, C_GUARD_DARK)
    draw_line(c, 13, 13, 10, 13, C_GUARD_DARK)
    draw_line(c, 22, 11, 19, 13, C_GUARD_DARK)
    draw_line(c, 19, 13, 22, 13, C_GUARD_DARK)
    # open shouting mouth
    fill_rect(c, 13, 17, 6, 3, C_GUARD_DARK)
    # shout lines
    draw_line(c, 7, 10, 4, 8, C_GOLD_BRIGHT)
    draw_line(c, 7, 14, 3, 14, C_GOLD_BRIGHT)
    draw_line(c, 7, 18, 4, 20, C_GOLD_BRIGHT)
    draw_line(c, 25, 10, 28, 8, C_GOLD_BRIGHT)
    draw_line(c, 25, 14, 29, 14, C_GOLD_BRIGHT)
    draw_line(c, 25, 18, 28, 20, C_GOLD_BRIGHT)
    return c

def icon_aegis_smash():
    c = blank()
    # shield tilted (smashing forward)
    fill_rect(c, 6, 6, 14, 14, C_GUARD)
    fill_rect(c, 7, 7, 12, 12, C_GUARD_LIGHT)
    draw_rect(c, 5, 5, 16, 16, C_GUARD_DARK)
    # shield boss
    fill_circle(c, 13, 13, 3, C_GUARD_ACCENT)
    fill_circle(c, 13, 13, 1, C_WHITE)
    # impact lines from right edge
    draw_line(c, 22, 8, 28, 6, C_GOLD_BRIGHT)
    draw_line(c, 22, 13, 28, 13, C_GOLD_BRIGHT)
    draw_line(c, 22, 18, 28, 20, C_GOLD_BRIGHT)
    # impact sparks
    for pos in [(24, 10), (26, 14), (24, 16)]:
        set_pixel(c, pos[0], pos[1], C_WHITE)
    # motion lines
    for y in [8, 13, 18]:
        draw_line(c, 2, y, 5, y, C_GUARD_DARK)
    return c

def icon_fortified_bulwark():
    c = blank()
    # large reinforced shield
    fill_rect(c, 6, 4, 20, 20, C_GUARD)
    fill_rect(c, 7, 5, 18, 18, C_GUARD_LIGHT)
    draw_rect(c, 5, 3, 22, 22, C_GUARD_DARK)
    # bottom point
    fill_triangle(c, 6, 24, 26, 24, 16, 29, C_GUARD)
    draw_line(c, 5, 24, 16, 29, C_GUARD_DARK)
    draw_line(c, 27, 24, 16, 29, C_GUARD_DARK)
    # reinforcement cross
    draw_line(c, 16, 5, 16, 27, C_GUARD_DARK)
    draw_line(c, 7, 14, 25, 14, C_GUARD_DARK)
    # corner rivets
    for pos in [(9, 7), (23, 7), (9, 21), (23, 21)]:
        fill_circle(c, pos[0], pos[1], 1, C_GUARD_DARK)
    # center emblem
    fill_diamond(c, 16, 14, 3, C_GUARD_ACCENT)
    return c

def icon_impregnable_fortress():
    c = blank()
    # castle tower shape
    # battlements
    for x in range(6, 28, 4):
        fill_rect(c, x, 2, 3, 4, C_GUARD)
    # main tower body
    fill_rect(c, 8, 6, 16, 20, C_GUARD)
    fill_rect(c, 9, 7, 14, 18, C_GUARD_LIGHT)
    draw_rect(c, 7, 5, 18, 22, C_GUARD_DARK)
    # gate/door
    fill_rect(c, 13, 18, 6, 8, C_GUARD_DARK)
    fill_rect(c, 14, 19, 4, 7, C_GUARD)
    # window
    fill_rect(c, 14, 10, 4, 4, C_GUARD_DARK)
    set_pixel(c, 16, 11, C_GUARD_ACCENT)
    # flag on top
    fill_rect(c, 15, 0, 2, 3, C_GUARD_DARK)
    fill_rect(c, 17, 0, 4, 2, C_GUARD_ACCENT)
    return c

# === ROGUE ===

def icon_shadow_strike():
    c = blank()
    # dagger blade
    draw_line(c, 12, 24, 24, 6, C_STEEL_LIGHT)
    draw_line(c, 13, 24, 25, 6, C_STEEL)
    # dagger point
    set_pixel(c, 25, 5, C_WHITE)
    # crossguard
    draw_line(c, 10, 20, 16, 20, C_ROGUE)
    # handle
    draw_line(c, 10, 25, 12, 23, C_ROGUE_DARK)
    fill_rect(c, 9, 25, 3, 2, C_ROGUE_DARK)
    # shadow silhouette behind
    fill_circle(c, 10, 14, 6, C_ROGUE_SHADOW)
    fill_circle(c, 10, 14, 4, C_ROGUE_DARK)
    # shadow wisps
    for pos in [(4, 10), (6, 8), (3, 16), (5, 18)]:
        set_pixel(c, pos[0], pos[1], C_ROGUE_SHADOW)
    return c

def icon_smoke_bomb():
    c = blank()
    # smoke cloud (overlapping circles)
    fill_circle(c, 14, 14, 6, C_GREY)
    fill_circle(c, 20, 12, 5, C_LIGHT_GREY)
    fill_circle(c, 12, 18, 5, C_GREY)
    fill_circle(c, 18, 18, 6, C_LIGHT_GREY)
    fill_circle(c, 16, 10, 4, C_LIGHT_GREY)
    # bomb body at bottom
    fill_circle(c, 16, 24, 3, C_ROGUE_DARK)
    fill_circle(c, 16, 24, 2, C_ROGUE)
    # fuse
    draw_line(c, 16, 21, 18, 19, C_BROWN_DARK)
    set_pixel(c, 19, 18, C_FIRE_BRIGHT)
    # smoke wisps at edges
    for pos in [(6, 10), (24, 8), (8, 22), (24, 16)]:
        set_pixel(c, pos[0], pos[1], C_GREY)
    return c

def icon_blade_flurry():
    c = blank()
    # three spinning blades in a circle
    import math
    for i in range(3):
        a = math.pi * 2 * i / 3 - math.pi / 2
        cx = int(16 + 7 * math.cos(a))
        cy = int(16 + 7 * math.sin(a))
        # each blade is a small line
        bx = int(cx + 4 * math.cos(a + 0.5))
        by = int(cy + 4 * math.sin(a + 0.5))
        draw_line(c, cx, cy, bx, by, C_STEEL_LIGHT)
        bx2 = int(cx - 4 * math.cos(a + 0.5))
        by2 = int(cy - 4 * math.sin(a + 0.5))
        draw_line(c, cx, cy, bx2, by2, C_STEEL_LIGHT)
    # center spin point
    fill_circle(c, 16, 16, 2, C_ROGUE)
    # motion arcs
    draw_circle(c, 16, 16, 10, C_ROGUE_LIGHT)
    # purple energy
    for i in range(3):
        a = math.pi * 2 * i / 3
        x = int(16 + 10 * math.cos(a))
        y = int(16 + 10 * math.sin(a))
        set_pixel(c, x, y, C_ROGUE_LIGHT)
    return c

def icon_assassin_mark():
    c = blank()
    # target/crosshair
    draw_circle(c, 16, 16, 10, C_ROGUE)
    draw_circle(c, 16, 16, 6, C_ROGUE_LIGHT)
    fill_circle(c, 16, 16, 2, C_ROGUE)
    # crosshair lines
    draw_line(c, 16, 2, 16, 10, C_ROGUE_DARK)
    draw_line(c, 16, 22, 16, 30, C_ROGUE_DARK)
    draw_line(c, 2, 16, 10, 16, C_ROGUE_DARK)
    draw_line(c, 22, 16, 30, 16, C_ROGUE_DARK)
    # skull in center
    set_pixel(c, 15, 15, C_ROGUE_LIGHT)
    set_pixel(c, 17, 15, C_ROGUE_LIGHT)
    set_pixel(c, 16, 17, C_ROGUE_LIGHT)
    return c

def icon_death_blossom():
    c = blank()
    # flower shape with blade petals
    import math
    for i in range(6):
        a = math.pi * 2 * i / 6 - math.pi / 2
        # petal (elongated)
        for r in range(3, 10):
            x = int(16 + r * math.cos(a))
            y = int(16 + r * math.sin(a))
            col = C_ROGUE if r < 7 else C_ROGUE_DARK
            set_pixel(c, x, y, col)
            # make petals wider
            px, py = -math.sin(a), math.cos(a)
            if r < 8:
                set_pixel(c, int(x + px), int(y + py), col)
                set_pixel(c, int(x - px), int(y - py), col)
    # dark center
    fill_circle(c, 16, 16, 3, C_ROGUE_DARK)
    fill_circle(c, 16, 16, 1, C_ROGUE_SHADOW)
    # blade edges
    for i in range(6):
        a = math.pi * 2 * i / 6 - math.pi / 2
        x = int(16 + 10 * math.cos(a))
        y = int(16 + 10 * math.sin(a))
        set_pixel(c, x, y, C_STEEL_LIGHT)
    return c

# === ICE MAGE ===

def icon_frost_bolt():
    c = blank()
    # ice shard (crystalline shape) pointing right
    # main shard body
    fill_triangle(c, 28, 16, 10, 10, 10, 22, C_ICE)
    fill_triangle(c, 26, 16, 12, 12, 12, 20, C_ICE_BRIGHT)
    # inner highlight
    draw_line(c, 14, 16, 24, 16, C_ICE_WHITE)
    # trailing particles
    for pos in [(6, 14), (4, 16), (6, 18), (8, 12), (8, 20)]:
        set_pixel(c, pos[0], pos[1], C_ICE_BRIGHT)
    # outline
    draw_line(c, 28, 16, 10, 10, C_ICE_DARK)
    draw_line(c, 28, 16, 10, 22, C_ICE_DARK)
    draw_line(c, 10, 10, 10, 22, C_ICE_DARK)
    return c

def icon_frozen_ground():
    c = blank()
    # cracked ice surface
    fill_rect(c, 2, 16, 28, 12, C_ICE)
    fill_rect(c, 3, 17, 26, 10, C_ICE_BRIGHT)
    # cracks
    draw_line(c, 16, 16, 8, 24, C_ICE_DARK)
    draw_line(c, 16, 16, 24, 26, C_ICE_DARK)
    draw_line(c, 16, 16, 4, 20, C_ICE_DARK)
    draw_line(c, 16, 16, 28, 20, C_ICE_DARK)
    draw_line(c, 16, 16, 16, 28, C_ICE_DARK)
    # ice crystals poking up
    fill_triangle(c, 8, 16, 6, 16, 7, 8, C_ICE_WHITE)
    fill_triangle(c, 16, 16, 14, 16, 15, 6, C_ICE_WHITE)
    fill_triangle(c, 24, 16, 22, 16, 23, 10, C_ICE_WHITE)
    # edges
    draw_line(c, 2, 16, 30, 16, C_ICE_DARK)
    draw_line(c, 2, 28, 30, 28, C_ICE_DARK)
    return c

def icon_blizzard():
    c = blank()
    # swirling snow
    import math
    # wind lines
    for y in [6, 12, 18, 24]:
        for x in range(4, 28, 3):
            offset = int(2 * math.sin(x * 0.5 + y * 0.3))
            set_pixel(c, x, y + offset, C_ICE_BRIGHT)
    # snowflakes (small crosses)
    snowflake_positions = [(8, 6), (20, 10), (12, 16), (24, 20), (6, 24), (18, 26)]
    for sx, sy in snowflake_positions:
        set_pixel(c, sx, sy, C_ICE_WHITE)
        set_pixel(c, sx-1, sy, C_ICE)
        set_pixel(c, sx+1, sy, C_ICE)
        set_pixel(c, sx, sy-1, C_ICE)
        set_pixel(c, sx, sy+1, C_ICE)
    # denser snow particles
    for pos in [(10, 4), (14, 8), (22, 6), (6, 14), (26, 14), (16, 22), (8, 28), (22, 28)]:
        set_pixel(c, pos[0], pos[1], C_ICE_WHITE)
    return c

def icon_glacial_prison():
    c = blank()
    # ice cage (vertical bars)
    for x in [8, 12, 16, 20, 24]:
        fill_rect(c, x, 4, 2, 24, C_ICE)
        fill_rect(c, x, 4, 1, 24, C_ICE_BRIGHT)
    # top bar
    fill_rect(c, 6, 4, 22, 3, C_ICE_DARK)
    fill_rect(c, 7, 5, 20, 1, C_ICE)
    # bottom bar
    fill_rect(c, 6, 26, 22, 3, C_ICE_DARK)
    fill_rect(c, 7, 27, 20, 1, C_ICE)
    # ice crystals on top
    set_pixel(c, 10, 3, C_ICE_WHITE)
    set_pixel(c, 16, 2, C_ICE_WHITE)
    set_pixel(c, 22, 3, C_ICE_WHITE)
    # frost inside
    set_pixel(c, 14, 14, C_ICE_WHITE)
    set_pixel(c, 18, 18, C_ICE_WHITE)
    return c

def icon_absolute_zero():
    c = blank()
    # large frozen crystal (hexagonal)
    # main crystal body
    fill_diamond(c, 16, 16, 10, C_ICE)
    fill_diamond(c, 16, 16, 8, C_ICE_BRIGHT)
    fill_diamond(c, 16, 16, 5, C_ICE_WHITE)
    # crystal facets
    draw_line(c, 16, 6, 16, 26, C_ICE_DARK)
    draw_line(c, 6, 16, 26, 16, C_ICE_DARK)
    # inner glow
    fill_circle(c, 16, 16, 2, C_ICE_WHITE)
    # frost rays
    for dx, dy in [(-8, -8), (8, -8), (-8, 8), (8, 8)]:
        set_pixel(c, 16+dx, 16+dy, C_ICE_BRIGHT)
    # outline
    draw_diamond(c, 16, 16, 11, C_ICE_DARK)
    return c

# === NECROMANCER ===

def icon_soul_drain():
    c = blank()
    # dark spiral
    import math
    for t in range(100):
        angle = t * 0.15
        r = 2 + t * 0.1
        x = int(16 + r * math.cos(angle))
        y = int(16 + r * math.sin(angle))
        intensity = max(0, 255 - t * 2)
        col = (30 + t, max(20, 120 - t), 20, 255)
        set_pixel(c, x, y, C_NECRO_GREEN if t < 50 else C_NECRO)
    # center dark point
    fill_circle(c, 16, 16, 3, C_NECRO_DARK)
    fill_circle(c, 16, 16, 1, C_NECRO_BLACK)
    # green wisps
    for pos in [(8, 8), (24, 8), (8, 24), (24, 24)]:
        set_pixel(c, pos[0], pos[1], C_NECRO_GREEN)
    return c

def icon_bone_spike():
    c = blank()
    # bone shard pointing up
    # main bone
    fill_triangle(c, 16, 2, 12, 28, 20, 28, C_BONE)
    fill_triangle(c, 16, 4, 13, 26, 19, 26, (235, 225, 200, 255))
    # bone texture lines
    draw_line(c, 15, 8, 15, 20, (200, 190, 160, 255))
    draw_line(c, 17, 10, 17, 22, (200, 190, 160, 255))
    # crack
    draw_line(c, 14, 14, 16, 16, C_NECRO_DARK)
    # outline
    draw_line(c, 16, 2, 12, 28, C_NECRO_DARK)
    draw_line(c, 16, 2, 20, 28, C_NECRO_DARK)
    draw_line(c, 12, 28, 20, 28, C_NECRO_DARK)
    # green glow at base
    for x in range(11, 21):
        set_pixel(c, x, 29, C_NECRO_GREEN)
    return c

def icon_undead_shield():
    c = blank()
    # shield shape
    fill_rect(c, 8, 4, 16, 18, C_NECRO_DARK)
    fill_rect(c, 9, 5, 14, 16, C_NECRO)
    draw_rect(c, 7, 3, 18, 20, C_NECRO_BLACK)
    # bottom V
    fill_triangle(c, 8, 22, 24, 22, 16, 28, C_NECRO_DARK)
    draw_line(c, 7, 22, 16, 28, C_NECRO_BLACK)
    draw_line(c, 25, 22, 16, 28, C_NECRO_BLACK)
    # skull emblem
    fill_circle(c, 16, 12, 4, C_BONE)
    # eyes
    fill_rect(c, 13, 11, 2, 2, C_NECRO_BLACK)
    fill_rect(c, 17, 11, 2, 2, C_NECRO_BLACK)
    # jaw
    fill_rect(c, 14, 16, 4, 3, C_BONE)
    draw_line(c, 14, 17, 18, 17, C_NECRO_DARK)
    return c

def icon_death_coil():
    c = blank()
    # dark coiling energy
    import math
    # two intertwined spirals
    for t in range(80):
        angle = t * 0.2
        x = int(16 + (4 + t * 0.08) * math.cos(angle))
        y = int(4 + t * 0.35)
        set_pixel(c, x, y, C_NECRO)
        # second spiral offset
        x2 = int(16 + (4 + t * 0.08) * math.cos(angle + math.pi))
        set_pixel(c, x2, y, C_NECRO_GREEN)
    # dark core at top
    fill_circle(c, 16, 6, 3, C_NECRO_DARK)
    fill_circle(c, 16, 6, 1, C_NECRO_GREEN)
    # trailing particles
    for pos in [(10, 26), (22, 26), (8, 22), (24, 22)]:
        set_pixel(c, pos[0], pos[1], C_NECRO_GREEN)
    return c

def icon_lich_form():
    c = blank()
    # skull
    fill_circle(c, 16, 14, 8, C_BONE)
    fill_circle(c, 16, 14, 6, (235, 225, 200, 255))
    # eye sockets
    fill_circle(c, 13, 12, 2, C_NECRO_BLACK)
    fill_circle(c, 19, 12, 2, C_NECRO_BLACK)
    # glowing eyes
    set_pixel(c, 13, 12, C_NECRO_GREEN)
    set_pixel(c, 19, 12, C_NECRO_GREEN)
    # nose
    set_pixel(c, 15, 15, C_NECRO_DARK)
    set_pixel(c, 16, 15, C_NECRO_DARK)
    set_pixel(c, 17, 15, C_NECRO_DARK)
    # jaw/teeth
    fill_rect(c, 12, 18, 8, 3, C_BONE)
    for x in range(12, 20, 2):
        set_pixel(c, x, 19, C_NECRO_DARK)
    # crown
    for x in range(10, 23, 3):
        fill_triangle(c, x, 6, x-1, 10, x+2, 10, C_NECRO_GREEN)
    draw_line(c, 9, 10, 23, 10, C_NECRO_GREEN)
    # crown jewels
    for x in [11, 16, 21]:
        set_pixel(c, x, 8, C_NECRO_BLACK)
    return c

# === HIDDEN CLASS ===

def icon_ashen_bulwark():
    c = blank()
    # shield shape (grey)
    fill_rect(c, 8, 4, 16, 18, C_ASHEN_GREY)
    fill_rect(c, 9, 5, 14, 16, (160, 150, 140, 255))
    draw_rect(c, 7, 3, 18, 20, C_ASHEN_DARK)
    # bottom V
    fill_triangle(c, 8, 22, 24, 22, 16, 28, C_ASHEN_GREY)
    draw_line(c, 7, 22, 16, 28, C_ASHEN_DARK)
    draw_line(c, 25, 22, 16, 28, C_ASHEN_DARK)
    # fire emblem in center
    fill_circle(c, 16, 13, 4, C_ASHEN_ORANGE)
    fill_circle(c, 16, 13, 2, C_FIRE_BRIGHT)
    # ash particles
    for pos in [(10, 8), (22, 8), (12, 18), (20, 18), (16, 6)]:
        set_pixel(c, pos[0], pos[1], C_GREY)
    # flame wisps at edges
    for pos in [(8, 6), (24, 6), (10, 20), (22, 20)]:
        set_pixel(c, pos[0], pos[1], C_FIRE_CORE)
    return c


# ─── Class Icon Generators ──────────────────────────────────────────────────

def class_fire_mage():
    c = blank()
    # flame emblem
    # outer flame
    fill_triangle(c, 16, 3, 8, 24, 24, 24, C_FIRE_CORE)
    fill_triangle(c, 16, 6, 10, 22, 22, 22, C_FIRE_BRIGHT)
    fill_triangle(c, 16, 10, 12, 20, 20, 20, C_FIRE_HOT)
    # inner flicker
    fill_triangle(c, 14, 14, 12, 22, 16, 22, C_FIRE_GLOW)
    fill_triangle(c, 18, 14, 16, 22, 20, 22, C_FIRE_GLOW)
    # base
    fill_rect(c, 7, 24, 18, 3, C_FIRE_DARK)
    # outline
    draw_line(c, 16, 3, 8, 24, C_OUTLINE)
    draw_line(c, 16, 3, 24, 24, C_OUTLINE)
    draw_line(c, 8, 24, 24, 24, C_OUTLINE)
    return c

def class_archer():
    c = blank()
    # bow shape
    # bow limbs (curved)
    import math
    for t in range(20):
        angle = -math.pi/3 + t * math.pi * 2/3 / 20
        x = int(12 + 12 * math.cos(angle))
        y = int(16 + 12 * math.sin(angle))
        set_pixel(c, x, y, C_ARCHER_DARK)
        if 0 <= x+1 < 32:
            set_pixel(c, x+1, y, C_ARCHER)
    # bowstring
    draw_line(c, 20, 5, 20, 27, C_ARCHER_LIGHT)
    # arrow
    draw_line(c, 8, 16, 26, 16, C_ARCHER)
    # arrowhead
    fill_triangle(c, 27, 16, 24, 14, 24, 18, C_ARCHER_DARK)
    # grip
    fill_rect(c, 18, 14, 3, 5, C_BROWN)
    return c

def class_warrior():
    c = blank()
    # sword emblem
    # blade
    fill_rect(c, 15, 2, 3, 20, C_STEEL_LIGHT)
    fill_rect(c, 14, 2, 1, 20, C_STEEL)
    fill_rect(c, 18, 2, 1, 20, C_STEEL_DARK)
    # blade tip
    fill_triangle(c, 14, 2, 18, 2, 16, 0, C_STEEL_LIGHT)
    # crossguard
    fill_rect(c, 8, 22, 16, 3, C_GOLD)
    fill_rect(c, 9, 23, 14, 1, C_GOLD_BRIGHT)
    # handle
    fill_rect(c, 14, 25, 4, 4, C_BROWN)
    # pommel
    fill_circle(c, 16, 30, 2, C_GOLD)
    return c

def class_priest():
    c = blank()
    # cross/sun emblem
    # main cross
    draw_cross(c, 16, 14, 8, 4, C_GOLD)
    fill_rect(c, 14, 6, 5, 17, C_GOLD_BRIGHT)
    fill_rect(c, 8, 12, 17, 5, C_GOLD_BRIGHT)
    # inner cross bright
    fill_rect(c, 15, 8, 3, 13, C_HOLY_GLOW)
    fill_rect(c, 10, 13, 13, 3, C_HOLY_GLOW)
    # glow dots
    import math
    for i in range(8):
        a = math.pi * 2 * i / 8
        x = int(16 + 12 * math.cos(a))
        y = int(14 + 12 * math.sin(a))
        set_pixel(c, x, y, C_GOLD)
    return c

def class_guardian():
    c = blank()
    # shield emblem
    fill_rect(c, 6, 4, 20, 18, C_GUARD)
    fill_rect(c, 7, 5, 18, 16, C_GUARD_LIGHT)
    draw_rect(c, 5, 3, 22, 20, C_GUARD_DARK)
    # bottom V
    fill_triangle(c, 6, 22, 26, 22, 16, 29, C_GUARD)
    draw_line(c, 5, 22, 16, 29, C_GUARD_DARK)
    draw_line(c, 27, 22, 16, 29, C_GUARD_DARK)
    # emblem cross
    draw_cross(c, 16, 14, 5, 3, C_GUARD_ACCENT)
    fill_rect(c, 15, 9, 3, 11, C_WHITE)
    fill_rect(c, 11, 13, 11, 3, C_WHITE)
    return c

def class_rogue():
    c = blank()
    # crossed daggers
    # dagger 1 (top-left to bottom-right)
    draw_line(c, 6, 6, 26, 26, C_STEEL_LIGHT)
    draw_line(c, 7, 6, 27, 26, C_STEEL)
    # dagger 2 (top-right to bottom-left)
    draw_line(c, 26, 6, 6, 26, C_STEEL_LIGHT)
    draw_line(c, 25, 6, 5, 26, C_STEEL)
    # handles
    fill_rect(c, 4, 24, 4, 4, C_ROGUE)
    fill_rect(c, 24, 24, 4, 4, C_ROGUE)
    # blade tips
    set_pixel(c, 6, 5, C_ROGUE_LIGHT)
    set_pixel(c, 26, 5, C_ROGUE_LIGHT)
    # center gem
    fill_circle(c, 16, 16, 2, C_ROGUE)
    set_pixel(c, 16, 16, C_ROGUE_LIGHT)
    return c

def class_ice_mage():
    c = blank()
    # snowflake emblem
    # 6 lines from center
    import math
    for i in range(6):
        a = math.pi * 2 * i / 6
        x1 = int(16 + 12 * math.cos(a))
        y1 = int(16 + 12 * math.sin(a))
        draw_line(c, 16, 16, x1, y1, C_ICE)
        # branches
        for r in [5, 9]:
            bx = int(16 + r * math.cos(a))
            by = int(16 + r * math.sin(a))
            for side in [1, -1]:
                ba = a + side * math.pi / 3
                ex = int(bx + 3 * math.cos(ba))
                ey = int(by + 3 * math.sin(ba))
                draw_line(c, bx, by, ex, ey, C_ICE_BRIGHT)
    # center
    fill_circle(c, 16, 16, 2, C_ICE_WHITE)
    return c

def class_necromancer():
    c = blank()
    # skull emblem
    fill_circle(c, 16, 13, 9, C_NECRO_DARK)
    fill_circle(c, 16, 13, 7, C_NECRO)
    # eye sockets
    fill_circle(c, 12, 11, 3, C_NECRO_BLACK)
    fill_circle(c, 20, 11, 3, C_NECRO_BLACK)
    # glowing eyes
    fill_circle(c, 12, 11, 1, C_NECRO_GREEN)
    fill_circle(c, 20, 11, 1, C_NECRO_GREEN)
    # nose
    fill_triangle(c, 15, 14, 17, 14, 16, 16, C_NECRO_BLACK)
    # jaw
    fill_rect(c, 10, 19, 12, 5, C_NECRO_DARK)
    fill_rect(c, 11, 20, 10, 3, C_NECRO)
    # teeth
    for x in range(11, 21, 2):
        set_pixel(c, x, 20, C_BONE)
        set_pixel(c, x, 22, C_BONE)
    draw_line(c, 11, 21, 21, 21, C_NECRO_BLACK)
    # outline
    draw_circle(c, 16, 13, 9, C_NECRO_BLACK)
    return c

def class_ashen_warden():
    c = blank()
    # shield base
    fill_rect(c, 8, 6, 16, 16, C_ASHEN_GREY)
    fill_rect(c, 9, 7, 14, 14, (160, 150, 140, 255))
    draw_rect(c, 7, 5, 18, 18, C_ASHEN_DARK)
    # bottom V
    fill_triangle(c, 8, 22, 24, 22, 16, 28, C_ASHEN_GREY)
    draw_line(c, 7, 22, 16, 28, C_ASHEN_DARK)
    draw_line(c, 25, 22, 16, 28, C_ASHEN_DARK)
    # flame emblem inside
    fill_triangle(c, 16, 8, 11, 20, 21, 20, C_ASHEN_ORANGE)
    fill_triangle(c, 16, 10, 13, 18, 19, 18, C_FIRE_BRIGHT)
    fill_triangle(c, 16, 12, 14, 17, 18, 17, C_FIRE_HOT)
    return c


# ─── Race Icon Generators ───────────────────────────────────────────────────

def draw_basic_face(canvas, skin_color, outline_color):
    """Draw a basic face shape."""
    fill_circle(canvas, 16, 14, 10, skin_color)
    draw_circle(canvas, 16, 14, 10, outline_color)
    # eyes
    fill_rect(canvas, 12, 12, 2, 2, outline_color)
    fill_rect(canvas, 18, 12, 2, 2, outline_color)
    # mouth
    draw_line(canvas, 13, 19, 19, 19, outline_color)

def race_human():
    c = blank()
    draw_basic_face(c, C_HUMAN, C_BROWN_DARK)
    # hair
    fill_rect(c, 8, 4, 16, 5, C_BROWN)
    fill_rect(c, 6, 6, 3, 6, C_BROWN)
    fill_rect(c, 23, 6, 3, 6, C_BROWN)
    # nose
    set_pixel(c, 16, 16, C_BROWN_DARK)
    # eyebrows
    draw_line(c, 11, 10, 14, 10, C_BROWN_DARK)
    draw_line(c, 18, 10, 21, 10, C_BROWN_DARK)
    return c

def race_elf():
    c = blank()
    skin = (210, 230, 200, 255)
    draw_basic_face(c, skin, C_DARK_GREY)
    # pointed ears
    fill_triangle(c, 5, 12, 2, 6, 8, 14, C_ELF_GREEN)
    fill_triangle(c, 27, 12, 30, 6, 24, 14, C_ELF_GREEN)
    # inner ear
    draw_line(c, 4, 8, 7, 13, skin)
    draw_line(c, 28, 8, 25, 13, skin)
    # elegant hair
    fill_rect(c, 8, 3, 16, 4, C_ELF_GREEN)
    fill_rect(c, 6, 5, 3, 8, C_ELF_GREEN)
    fill_rect(c, 23, 5, 3, 8, C_ELF_GREEN)
    # thinner eyebrows
    draw_line(c, 11, 10, 15, 10, (60, 120, 60, 255))
    draw_line(c, 17, 10, 21, 10, (60, 120, 60, 255))
    return c

def race_dwarf():
    c = blank()
    draw_basic_face(c, (200, 170, 140, 255), C_BROWN_DARK)
    # thick beard
    fill_rect(c, 8, 18, 16, 8, C_DWARF_BROWN)
    fill_rect(c, 10, 20, 12, 6, (140, 100, 50, 255))
    # mustache
    fill_rect(c, 10, 17, 5, 2, C_DWARF_BROWN)
    fill_rect(c, 17, 17, 5, 2, C_DWARF_BROWN)
    # thick eyebrows
    fill_rect(c, 10, 9, 5, 2, C_DWARF_BROWN)
    fill_rect(c, 17, 9, 5, 2, C_DWARF_BROWN)
    # helmet
    fill_rect(c, 6, 2, 20, 6, C_STEEL)
    fill_rect(c, 7, 3, 18, 4, C_STEEL_LIGHT)
    draw_rect(c, 5, 1, 22, 8, C_STEEL_DARK)
    # helmet horn/ridge
    fill_rect(c, 14, 0, 4, 3, C_STEEL_DARK)
    return c

def race_orc():
    c = blank()
    orc_skin = (130, 150, 80, 255)
    draw_basic_face(c, orc_skin, C_DARK_GREY)
    # tusks from lower jaw
    fill_rect(c, 11, 18, 2, 4, C_BONE)
    fill_rect(c, 19, 18, 2, 4, C_BONE)
    # broader jaw
    fill_rect(c, 8, 16, 4, 6, orc_skin)
    fill_rect(c, 20, 16, 4, 6, orc_skin)
    # angry brow
    draw_line(c, 10, 10, 14, 12, C_DARK_GREY)
    draw_line(c, 22, 10, 18, 12, C_DARK_GREY)
    # war paint
    draw_line(c, 8, 14, 12, 14, C_DEMON_RED)
    draw_line(c, 20, 14, 24, 14, C_DEMON_RED)
    # short hair/bald with scar
    fill_rect(c, 8, 3, 16, 4, (100, 120, 60, 255))
    return c

def race_undead():
    c = blank()
    # skull-like face
    fill_circle(c, 16, 14, 10, C_UNDEAD_GREY)
    fill_circle(c, 16, 14, 8, (160, 175, 150, 255))
    draw_circle(c, 16, 14, 10, C_DARK_GREY)
    # hollow eyes
    fill_circle(c, 12, 12, 3, C_DARK_GREY)
    fill_circle(c, 20, 12, 3, C_DARK_GREY)
    # green glow in eyes
    set_pixel(c, 12, 12, C_NECRO_GREEN)
    set_pixel(c, 20, 12, C_NECRO_GREEN)
    # exposed teeth/jaw
    fill_rect(c, 12, 18, 8, 3, C_BONE)
    for x in range(12, 20, 2):
        set_pixel(c, x, 19, C_DARK_GREY)
    # cracks
    draw_line(c, 10, 6, 12, 10, C_DARK_GREY)
    draw_line(c, 20, 8, 22, 12, C_DARK_GREY)
    # tattered hood
    fill_rect(c, 6, 2, 20, 5, (80, 90, 70, 255))
    draw_line(c, 6, 7, 6, 16, (80, 90, 70, 255))
    draw_line(c, 25, 7, 25, 16, (80, 90, 70, 255))
    return c

def race_demon():
    c = blank()
    demon_skin = (160, 60, 50, 255)
    draw_basic_face(c, demon_skin, C_BLACK)
    # horns
    fill_triangle(c, 8, 8, 4, 0, 10, 6, C_DEMON_RED)
    fill_triangle(c, 24, 8, 28, 0, 22, 6, C_DEMON_RED)
    # horn tips
    set_pixel(c, 3, 0, (100, 30, 20, 255))
    set_pixel(c, 4, 1, (100, 30, 20, 255))
    set_pixel(c, 28, 0, (100, 30, 20, 255))
    set_pixel(c, 27, 1, (100, 30, 20, 255))
    # glowing eyes (red-orange)
    fill_rect(c, 12, 12, 2, 2, C_FIRE_BRIGHT)
    fill_rect(c, 18, 12, 2, 2, C_FIRE_BRIGHT)
    # fangs
    set_pixel(c, 13, 20, C_BONE)
    set_pixel(c, 14, 21, C_BONE)
    set_pixel(c, 18, 20, C_BONE)
    set_pixel(c, 19, 21, C_BONE)
    # dark hair/forehead
    fill_rect(c, 8, 4, 16, 4, (80, 20, 15, 255))
    # evil eyebrows
    draw_line(c, 10, 10, 14, 11, C_BLACK)
    draw_line(c, 22, 10, 18, 11, C_BLACK)
    return c


# ─── Main Generation ────────────────────────────────────────────────────────

def main():
    base = '/Users/hongyuwu/Documents/MC_MOD/src/main/resources/assets/careerchronicle/textures/gui'

    # Skill icons
    skill_icons = {
        # Fire Mage
        'fireball': icon_fireball,
        'ember_burst': icon_ember_burst,
        'flame_step': icon_flame_step,
        'inferno_focus': icon_inferno_focus,
        'meteor_rite': icon_meteor_rite,
        # Archer
        'charged_shot': icon_charged_shot,
        'scatter_shot': icon_scatter_shot,
        'snare_shot': icon_snare_shot,
        'eagle_eye': icon_eagle_eye,
        'storm_marksman': icon_storm_marksman,
        # Warrior
        'lunge_strike': icon_lunge_strike,
        'guard_stance': icon_guard_stance,
        'ground_slam': icon_ground_slam,
        'iron_vanguard': icon_iron_vanguard,
        'unyielding_colossus': icon_unyielding_colossus,
        # Priest
        'mend': icon_mend,
        'holy_nova': icon_holy_nova,
        'blessing': icon_blessing,
        'seraphic_grace': icon_seraphic_grace,
        'sanctuary_descent': icon_sanctuary_descent,
        # Fusion
        'flame_arrow': icon_flame_arrow,
        'blazing_charge': icon_blazing_charge,
        'sunfire_aegis': icon_sunfire_aegis,
        'piercing_volley': icon_piercing_volley,
        'guiding_light_arrow': icon_guiding_light_arrow,
        'consecrated_slam': icon_consecrated_slam,
        # Guardian
        'shield_wall': icon_shield_wall,
        'provoke': icon_provoke,
        'aegis_smash': icon_aegis_smash,
        'fortified_bulwark': icon_fortified_bulwark,
        'impregnable_fortress': icon_impregnable_fortress,
        # Rogue
        'shadow_strike': icon_shadow_strike,
        'smoke_bomb': icon_smoke_bomb,
        'blade_flurry': icon_blade_flurry,
        'assassin_mark': icon_assassin_mark,
        'death_blossom': icon_death_blossom,
        # Ice Mage
        'frost_bolt': icon_frost_bolt,
        'frozen_ground': icon_frozen_ground,
        'blizzard': icon_blizzard,
        'glacial_prison': icon_glacial_prison,
        'absolute_zero': icon_absolute_zero,
        # Necromancer
        'soul_drain': icon_soul_drain,
        'bone_spike': icon_bone_spike,
        'undead_shield': icon_undead_shield,
        'death_coil': icon_death_coil,
        'lich_form': icon_lich_form,
        # Hidden
        'ashen_bulwark': icon_ashen_bulwark,
    }

    # Class icons
    class_icons = {
        'fire_mage': class_fire_mage,
        'archer': class_archer,
        'warrior': class_warrior,
        'priest': class_priest,
        'guardian': class_guardian,
        'rogue': class_rogue,
        'ice_mage': class_ice_mage,
        'necromancer': class_necromancer,
        'ashen_warden': class_ashen_warden,
    }

    # Race icons
    race_icons = {
        'human': race_human,
        'elf': race_elf,
        'dwarf': race_dwarf,
        'orc': race_orc,
        'undead': race_undead,
        'demon': race_demon,
    }

    total = 0

    print("Generating skill icons...")
    for name, gen_func in skill_icons.items():
        filepath = os.path.join(base, 'skill', f'{name}.png')
        canvas = gen_func()
        write_png(filepath, canvas)
        total += 1
        print(f"  [OK] skill/{name}.png")

    print("\nGenerating class icons...")
    for name, gen_func in class_icons.items():
        filepath = os.path.join(base, 'class', f'{name}.png')
        canvas = gen_func()
        write_png(filepath, canvas)
        total += 1
        print(f"  [OK] class/{name}.png")

    print("\nGenerating race icons...")
    for name, gen_func in race_icons.items():
        filepath = os.path.join(base, 'race', f'{name}.png')
        canvas = gen_func()
        write_png(filepath, canvas)
        total += 1
        print(f"  [OK] race/{name}.png")

    print(f"\nDone! Generated {total} icons total.")
    print(f"  - {len(skill_icons)} skill icons")
    print(f"  - {len(class_icons)} class icons")
    print(f"  - {len(race_icons)} race icons")


if __name__ == '__main__':
    main()
