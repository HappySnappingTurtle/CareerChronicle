#!/usr/bin/env python3
"""Career Chronicle SFX processing pipeline.
Sources: Kenney CC0 audio packs. Output: 24 OGG Vorbis files."""

import numpy as np
import soundfile as sf
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "tools/sfx_sources"
IMPACT = SRC / "kenney_impact/Audio"
RPG = SRC / "kenney_rpg/Audio"
OUT = ROOT / "src/main/resources/assets/careerchronicle/sounds"

def load(path, mono=True):
    data, sr = sf.read(str(path), dtype='float32')
    if mono and data.ndim > 1:
        data = data.mean(axis=1)
    return data, sr

def save_ogg(path, data, sr=44100, mono=True):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if mono and data.ndim > 1:
        data = data.mean(axis=1)
    elif not mono and data.ndim == 1:
        data = np.column_stack([data, data])
    sf.write(str(path), data, sr, format='OGG', subtype='VORBIS')

def pitch_shift(data, sr, factor):
    """Pitch shift by resampling. factor>1 = higher pitch."""
    indices = np.arange(0, len(data), factor)
    indices = indices[indices < len(data)].astype(int)
    return data[indices]

def fade_out(data, sr, start_sec, duration_sec):
    start = int(start_sec * sr)
    dur = int(duration_sec * sr)
    end = min(start + dur, len(data))
    if start < len(data):
        fade = np.linspace(1.0, 0.0, end - start)
        data[start:end] *= fade
        data[end:] = 0
    return data

def fade_in(data, sr, duration_sec):
    dur = min(int(duration_sec * sr), len(data))
    data[:dur] *= np.linspace(0.0, 1.0, dur)
    return data

def echo(data, sr, delay_ms=60, decay=0.4):
    delay_samples = int(sr * delay_ms / 1000)
    out = data.copy()
    if delay_samples < len(out):
        out[delay_samples:] += data[:len(data) - delay_samples] * decay
    return np.clip(out, -1.0, 1.0)

def trim(data, sr, end_sec):
    end = min(int(end_sec * sr), len(data))
    return data[:end]

def mix(a, b, b_delay_ms=0, b_vol=1.0):
    b = b * b_vol
    delay = int(44100 * b_delay_ms / 1000)
    length = max(len(a), len(b) + delay)
    out = np.zeros(length, dtype='float32')
    out[:len(a)] += a
    out[delay:delay + len(b)] += b
    return np.clip(out, -1.0, 1.0)

def reverse(data):
    return data[::-1].copy()

def pad_to(data, sr, seconds):
    target = int(sr * seconds)
    if len(data) < target:
        data = np.concatenate([data, np.zeros(target - len(data), dtype='float32')])
    return data

print("=== Skill Cast Layer (10, mono) ===")

# 1. fire
d, sr = load(IMPACT / "impactSoft_heavy_000.ogg")
d = pitch_shift(d, sr, 1.35)
d2, _ = load(IMPACT / "impactPlate_light_000.ogg")
d = mix(d, d2, b_delay_ms=80, b_vol=0.5)
save_ogg(OUT / "skill/cast/fire.ogg", d)
print("  fire OK")

# 2. frost
d, sr = load(IMPACT / "impactGlass_light_000.ogg")
d = pitch_shift(d, sr, 0.75)
d = echo(d, sr, 40, 0.35)
d = fade_out(d, sr, 0.6, 0.3)
save_ogg(OUT / "skill/cast/frost.ogg", d)
print("  frost OK")

# 3. holy
d, sr = load(IMPACT / "impactBell_heavy_000.ogg")
d = echo(d, sr, 80, 0.5)
d *= 0.8
d = fade_out(d, sr, 0.8, 0.4)
save_ogg(OUT / "skill/cast/holy.ogg", d)
print("  holy OK")

# 4. dark
d1, sr = load(IMPACT / "impactSoft_medium_000.ogg")
d1 = pitch_shift(d1, sr, 0.55)
d1 *= 1.3
d2, _ = load(IMPACT / "impactSoft_medium_001.ogg")
d2 = reverse(d2)
d2 = pitch_shift(d2, sr, 0.6)
d2 *= 0.6
d = mix(d1, d2)
save_ogg(OUT / "skill/cast/dark.ogg", d)
print("  dark OK")

# 5. blade
d, sr = load(IMPACT / "impactMetal_light_000.ogg")
d = pitch_shift(d, sr, 1.15)
d = fade_out(d, sr, 0.4, 0.2)
save_ogg(OUT / "skill/cast/blade.ogg", d)
print("  blade OK")

# 6. shield
d1, sr = load(IMPACT / "impactPlate_heavy_000.ogg")
d1 *= 0.8
d2, _ = load(IMPACT / "impactMetal_medium_000.ogg")
d = mix(d1, d2, b_delay_ms=50, b_vol=0.6)
save_ogg(OUT / "skill/cast/shield.ogg", d)
print("  shield OK")

# 7. arrow
d, sr = load(IMPACT / "impactWood_light_000.ogg")
d = pitch_shift(d, sr, 1.4)
d = fade_out(d, sr, 0.3, 0.15)
save_ogg(OUT / "skill/cast/arrow.ogg", d)
print("  arrow OK")

# 8. shadow
d, sr = load(IMPACT / "footstep_carpet_000.ogg")
d = pitch_shift(d, sr, 0.7)
d *= 0.4
d = echo(d, sr, 30, 0.3)
d = fade_out(d, sr, 0.4, 0.3)
save_ogg(OUT / "skill/cast/shadow.ogg", d)
print("  shadow OK")

# 9. arcane
d, sr = load(IMPACT / "impactGlass_medium_000.ogg")
d = pitch_shift(d, sr, 1.5)
d = echo(d, sr, 50, 0.45)
d = fade_out(d, sr, 0.5, 0.3)
save_ogg(OUT / "skill/cast/arcane.ogg", d)
print("  arcane OK")

# 10. nature
d, sr = load(IMPACT / "impactWood_medium_000.ogg")
d = echo(d, sr, 70, 0.4)
d *= 0.9
d = fade_out(d, sr, 0.6, 0.3)
save_ogg(OUT / "skill/cast/nature.ogg", d)
print("  nature OK")

print("=== Skill Hit Layer (5, mono) ===")

# 11. fire hit
d, sr = load(IMPACT / "impactSoft_heavy_001.ogg")
d = trim(d, sr, 0.35)
d = pitch_shift(d, sr, 1.2)
d = fade_out(d, sr, 0.2, 0.15)
save_ogg(OUT / "skill/hit/fire.ogg", d)
print("  fire OK")

# 12. frost hit
d, sr = load(IMPACT / "impactGlass_heavy_000.ogg")
d = trim(d, sr, 0.4)
d = fade_out(d, sr, 0.25, 0.15)
save_ogg(OUT / "skill/hit/frost.ogg", d)
print("  frost OK")

# 13. holy hit
d, sr = load(IMPACT / "impactBell_heavy_001.ogg")
d *= 0.5
d = trim(d, sr, 0.5)
d = fade_out(d, sr, 0.3, 0.2)
save_ogg(OUT / "skill/hit/holy.ogg", d)
print("  holy OK")

# 14. dark hit
d, sr = load(IMPACT / "impactSoft_medium_002.ogg")
d = pitch_shift(d, sr, 0.7)
d = trim(d, sr, 0.4)
d = fade_out(d, sr, 0.25, 0.15)
save_ogg(OUT / "skill/hit/dark.ogg", d)
print("  dark OK")

# 15. physical hit
d, sr = load(IMPACT / "impactPunch_medium_000.ogg")
d = trim(d, sr, 0.35)
d = fade_out(d, sr, 0.2, 0.15)
save_ogg(OUT / "skill/hit/physical.ogg", d)
print("  physical OK")

print("=== UI Layer (4, stereo) ===")

# 16. chronicle_open
d, sr = load(RPG / "bookFlip1.ogg", mono=False)
if d.ndim == 1:
    d = np.column_stack([d, d])
d *= 0.8
save_ogg(OUT / "ui/chronicle_open.ogg", d, mono=False)
print("  chronicle_open OK")

# 17. skill_equip
d, sr = load(IMPACT / "impactTin_medium_000.ogg")
d *= 0.5
d = pitch_shift(d, sr, 1.2)
d = trim(d, sr, 0.3)
d = fade_out(d, sr, 0.15, 0.15)
save_ogg(OUT / "ui/skill_equip.ogg", d, mono=False)
print("  skill_equip OK")

# 18. tab_flip
d, sr = load(RPG / "bookFlip2.ogg")
d *= 0.6
d = trim(d, sr, 0.2)
d = pitch_shift(d, sr, 1.3)
d = fade_out(d, sr, 0.1, 0.1)
save_ogg(OUT / "ui/tab_flip.ogg", d, mono=False)
print("  tab_flip OK")

# 19. deny
d, sr = load(IMPACT / "impactMetal_light_001.ogg")
d = pitch_shift(d, sr, 0.7)
d *= 0.6
d = trim(d, sr, 0.35)
d = fade_out(d, sr, 0.2, 0.15)
save_ogg(OUT / "ui/deny.ogg", d, mono=False)
print("  deny OK")

print("=== Event Layer (5, stereo) ===")

# 20. level_up
d1, sr = load(IMPACT / "impactBell_heavy_000.ogg")
d1 *= 0.7
d2, _ = load(IMPACT / "impactBell_heavy_001.ogg")
d2 = pitch_shift(d2, sr, 1.15)
d2 *= 0.8
d = mix(d1, d2, b_delay_ms=200)
d = echo(d, sr, 60, 0.3)
d = fade_out(d, sr, 1.0, 0.5)
save_ogg(OUT / "event/level_up.ogg", d, mono=False)
print("  level_up OK")

# 21. segment_choice
d, sr = load(IMPACT / "impactPlate_heavy_001.ogg")
d = echo(d, sr, 100, 0.5)
d = echo(d, sr, 60, 0.3)
d *= 0.8
d = fade_out(d, sr, 1.2, 0.6)
save_ogg(OUT / "event/segment_choice.ogg", d, mono=False)
print("  segment_choice OK")

# 22. fusion_unlock (brand sound)
d1, sr = load(IMPACT / "impactBell_heavy_002.ogg")
d1 = echo(d1, sr, 90, 0.5)
d1 *= 0.7
d2, _ = load(IMPACT / "impactGlass_medium_000.ogg")
d2 = pitch_shift(d2, sr, 1.25)
d2 = echo(d2, sr, 70, 0.4)
d2 *= 0.6
d = mix(d1, d2, b_delay_ms=400)
d = pad_to(d, sr, 2.5)
d = fade_in(d, sr, 0.1)
d = fade_out(d, sr, 2.0, 0.5)
save_ogg(OUT / "event/fusion_unlock.ogg", d, mono=False)
print("  fusion_unlock OK")

# 23. hidden_unlock
d1, sr = load(IMPACT / "impactSoft_heavy_002.ogg")
d1 = reverse(d1)
d1 *= 0.8
d2, _ = load(IMPACT / "impactGlass_light_001.ogg")
d = mix(d1, d2)
d = echo(d, sr, 50, 0.35)
d = fade_out(d, sr, 0.8, 0.4)
save_ogg(OUT / "event/hidden_unlock.ogg", d, mono=False)
print("  hidden_unlock OK")

# 24. skill_upgrade
d, sr = load(IMPACT / "impactTin_medium_001.ogg")
d = pitch_shift(d, sr, 1.35)
d *= 0.7
d = fade_out(d, sr, 0.3, 0.2)
save_ogg(OUT / "event/skill_upgrade.ogg", d, mono=False)
print("  skill_upgrade OK")

print("\n=== Verification ===")
count = 0
for ogg in sorted(OUT.rglob("*.ogg")):
    info = sf.info(str(ogg))
    rel = ogg.relative_to(OUT)
    print(f"  {rel}: {info.channels}ch {info.samplerate}Hz {info.duration:.2f}s")
    count += 1
print(f"\n=== Done: {count} OGG files produced ===")
