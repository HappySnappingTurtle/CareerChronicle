#!/bin/bash
# Career Chronicle SFX processing pipeline
# Sources: Kenney CC0 audio packs
# All outputs: mono 44100Hz OGG Vorbis (except UI/event: stereo)
set -e

SRC="/Users/hongyuwu/Documents/MC_MOD/tools/sfx_sources"
IMPACT="$SRC/kenney_impact/Audio"
UI_SRC="$SRC/kenney_ui/Audio"
RPG="$SRC/kenney_rpg/Audio"
OUT="/Users/hongyuwu/Documents/MC_MOD/src/main/resources/assets/careerchronicle/sounds"
TMP="/tmp/cc_sfx_tmp"
mkdir -p "$TMP" "$OUT/skill/cast" "$OUT/skill/hit" "$OUT/ui" "$OUT/event"

echo "=== Skill Cast Layer (10, mono) ==="

# 1. skill.cast.fire — impactSoft_heavy pitched up + impactPlate tail
ffmpeg -y -i "$IMPACT/impactSoft_heavy_000.ogg" -i "$IMPACT/impactPlate_light_000.ogg" \
  -filter_complex "[0:a]asetrate=44100*1.35,aresample=44100[a];[1:a]adelay=80|80,volume=0.5[b];[a][b]amix=inputs=2:duration=first[out]" \
  -map "[out]" -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/cast/fire.ogg" 2>/dev/null
echo "  fire OK"

# 2. skill.cast.frost — impactGlass_light pitched down + reverb
ffmpeg -y -i "$IMPACT/impactGlass_light_000.ogg" \
  -af "asetrate=44100*0.75,aresample=44100,aecho=0.8:0.7:40:0.35,afade=t=out:st=0.6:d=0.3" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/cast/frost.ogg" 2>/dev/null
echo "  frost OK"

# 3. skill.cast.holy — impactBell_heavy + reverb
ffmpeg -y -i "$IMPACT/impactBell_heavy_000.ogg" \
  -af "aecho=0.8:0.85:80:0.5,volume=0.8,afade=t=out:st=0.8:d=0.4" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/cast/holy.ogg" 2>/dev/null
echo "  holy OK"

# 4. skill.cast.dark — impactSoft_medium pitched way down + reverse overlay
ffmpeg -y -i "$IMPACT/impactSoft_medium_000.ogg" \
  -af "asetrate=44100*0.55,aresample=44100,volume=1.3" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$TMP/dark_low.ogg" 2>/dev/null
ffmpeg -y -i "$IMPACT/impactSoft_medium_001.ogg" \
  -af "areverse,asetrate=44100*0.6,aresample=44100,volume=0.6" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$TMP/dark_rev.ogg" 2>/dev/null
ffmpeg -y -i "$TMP/dark_low.ogg" -i "$TMP/dark_rev.ogg" \
  -filter_complex "[0:a][1:a]amix=inputs=2:duration=longest[out]" \
  -map "[out]" -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/cast/dark.ogg" 2>/dev/null
echo "  dark OK"

# 5. skill.cast.blade — impactMetal_light pitched slightly
ffmpeg -y -i "$IMPACT/impactMetal_light_000.ogg" \
  -af "asetrate=44100*1.15,aresample=44100,afade=t=out:st=0.4:d=0.2" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/cast/blade.ogg" 2>/dev/null
echo "  blade OK"

# 6. skill.cast.shield — impactPlate_heavy + impactMetal_medium
ffmpeg -y -i "$IMPACT/impactPlate_heavy_000.ogg" -i "$IMPACT/impactMetal_medium_000.ogg" \
  -filter_complex "[0:a]volume=0.8[a];[1:a]adelay=50|50,volume=0.6[b];[a][b]amix=inputs=2:duration=first[out]" \
  -map "[out]" -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/cast/shield.ogg" 2>/dev/null
echo "  shield OK"

# 7. skill.cast.arrow — impactWood_light pitched up for twang
ffmpeg -y -i "$IMPACT/impactWood_light_000.ogg" \
  -af "asetrate=44100*1.4,aresample=44100,afade=t=out:st=0.3:d=0.15" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/cast/arrow.ogg" 2>/dev/null
echo "  arrow OK"

# 8. skill.cast.shadow — footstep_carpet pitched + quiet
ffmpeg -y -i "$IMPACT/footstep_carpet_000.ogg" \
  -af "asetrate=44100*0.7,aresample=44100,volume=0.4,aecho=0.6:0.5:30:0.3,afade=t=out:st=0.4:d=0.3" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/cast/shadow.ogg" 2>/dev/null
echo "  shadow OK"

# 9. skill.cast.arcane — impactGlass_medium pitched up + stretched
ffmpeg -y -i "$IMPACT/impactGlass_medium_000.ogg" \
  -af "asetrate=44100*1.5,aresample=44100,aecho=0.7:0.8:50:0.45,afade=t=out:st=0.5:d=0.3" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/cast/arcane.ogg" 2>/dev/null
echo "  arcane OK"

# 10. skill.cast.nature — impactWood_medium + reverb
ffmpeg -y -i "$IMPACT/impactWood_medium_000.ogg" \
  -af "aecho=0.8:0.7:70:0.4,volume=0.9,afade=t=out:st=0.6:d=0.3" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/cast/nature.ogg" 2>/dev/null
echo "  nature OK"

echo "=== Skill Hit Layer (5, mono) ==="

# 11. skill.hit.fire
ffmpeg -y -i "$IMPACT/impactSoft_heavy_001.ogg" \
  -af "atrim=0:0.35,afade=t=out:st=0.2:d=0.15,asetrate=44100*1.2,aresample=44100" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/hit/fire.ogg" 2>/dev/null
echo "  fire OK"

# 12. skill.hit.frost
ffmpeg -y -i "$IMPACT/impactGlass_heavy_000.ogg" \
  -af "atrim=0:0.4,afade=t=out:st=0.25:d=0.15" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/hit/frost.ogg" 2>/dev/null
echo "  frost OK"

# 13. skill.hit.holy
ffmpeg -y -i "$IMPACT/impactBell_heavy_001.ogg" \
  -af "volume=0.5,atrim=0:0.5,afade=t=out:st=0.3:d=0.2" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/hit/holy.ogg" 2>/dev/null
echo "  holy OK"

# 14. skill.hit.dark
ffmpeg -y -i "$IMPACT/impactSoft_medium_002.ogg" \
  -af "asetrate=44100*0.7,aresample=44100,atrim=0:0.4,afade=t=out:st=0.25:d=0.15" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/hit/dark.ogg" 2>/dev/null
echo "  dark OK"

# 15. skill.hit.physical
ffmpeg -y -i "$IMPACT/impactPunch_medium_000.ogg" \
  -af "atrim=0:0.35,afade=t=out:st=0.2:d=0.15" \
  -ac 1 -ar 44100 -c:a vorbis -q:a 4 "$OUT/skill/hit/physical.ogg" 2>/dev/null
echo "  physical OK"

echo "=== UI Layer (4, stereo) ==="

# 16. ui.chronicle_open — book page flip
ffmpeg -y -i "$RPG/bookFlip1.ogg" \
  -af "volume=0.8,afade=t=out:st=0.4:d=0.2" \
  -ac 2 -ar 44100 -c:a vorbis -q:a 4 "$OUT/ui/chronicle_open.ogg" 2>/dev/null
echo "  chronicle_open OK"

# 17. ui.skill_equip — light metallic click
ffmpeg -y -i "$IMPACT/impactTin_medium_000.ogg" \
  -af "volume=0.5,asetrate=44100*1.2,aresample=44100,atrim=0:0.3,afade=t=out:st=0.15:d=0.15" \
  -ac 2 -ar 44100 -c:a vorbis -q:a 4 "$OUT/ui/skill_equip.ogg" 2>/dev/null
echo "  skill_equip OK"

# 18. ui.tab_flip — very short soft sound
ffmpeg -y -i "$RPG/bookFlip2.ogg" \
  -af "volume=0.6,atrim=0:0.2,afade=t=out:st=0.1:d=0.1,asetrate=44100*1.3,aresample=44100" \
  -ac 2 -ar 44100 -c:a vorbis -q:a 4 "$OUT/ui/tab_flip.ogg" 2>/dev/null
echo "  tab_flip OK"

# 19. ui.deny — metallic dull thud
ffmpeg -y -i "$IMPACT/impactMetal_light_001.ogg" \
  -af "asetrate=44100*0.7,aresample=44100,volume=0.6,atrim=0:0.35,afade=t=out:st=0.2:d=0.15" \
  -ac 2 -ar 44100 -c:a vorbis -q:a 4 "$OUT/ui/deny.ogg" 2>/dev/null
echo "  deny OK"

echo "=== Event Layer (5, stereo) ==="

# 20. event.level_up — two bells ascending
ffmpeg -y -i "$IMPACT/impactBell_heavy_000.ogg" -i "$IMPACT/impactBell_heavy_001.ogg" \
  -filter_complex "[0:a]volume=0.7[a];[1:a]adelay=200|200,asetrate=44100*1.15,aresample=44100,volume=0.8[b];[a][b]amix=inputs=2:duration=longest,aecho=0.6:0.7:60:0.3,afade=t=out:st=1.0:d=0.5[out]" \
  -map "[out]" -ac 2 -ar 44100 -c:a vorbis -q:a 4 "$OUT/event/level_up.ogg" 2>/dev/null
echo "  level_up OK"

# 21. event.segment_choice — plate with heavy reverb
ffmpeg -y -i "$IMPACT/impactPlate_heavy_001.ogg" \
  -af "aecho=0.8:0.9:100:0.5,aecho=0.6:0.7:60:0.3,volume=0.8,afade=t=out:st=1.2:d=0.6" \
  -ac 2 -ar 44100 -c:a vorbis -q:a 4 "$OUT/event/segment_choice.ogg" 2>/dev/null
echo "  segment_choice OK"

# 22. event.fusion_unlock — BRAND SOUND: bell + glass offset layered 2.5s
ffmpeg -y -i "$IMPACT/impactBell_heavy_002.ogg" -i "$IMPACT/impactGlass_medium_000.ogg" \
  -filter_complex "[0:a]aecho=0.8:0.85:90:0.5,volume=0.7[a];[1:a]adelay=400|400,asetrate=44100*1.25,aresample=44100,aecho=0.7:0.8:70:0.4,volume=0.6[b];[a][b]amix=inputs=2:duration=longest,afade=t=in:st=0:d=0.1,afade=t=out:st=2.0:d=0.5[out]" \
  -map "[out]" -ac 2 -ar 44100 -c:a vorbis -q:a 4 "$OUT/event/fusion_unlock.ogg" 2>/dev/null
echo "  fusion_unlock OK"

# 23. event.hidden_unlock — reverse soft + glass overlay
ffmpeg -y -i "$IMPACT/impactSoft_heavy_002.ogg" \
  -af "areverse,volume=0.8" -ac 1 -ar 44100 -c:a vorbis "$TMP/hidden_rev.ogg" 2>/dev/null
ffmpeg -y -i "$TMP/hidden_rev.ogg" -i "$IMPACT/impactGlass_light_001.ogg" \
  -filter_complex "[0:a][1:a]amix=inputs=2:duration=longest,aecho=0.7:0.8:50:0.35,afade=t=out:st=0.8:d=0.4[out]" \
  -map "[out]" -ac 2 -ar 44100 -c:a vorbis -q:a 4 "$OUT/event/hidden_unlock.ogg" 2>/dev/null
echo "  hidden_unlock OK"

# 24. event.skill_upgrade — tin ascending short
ffmpeg -y -i "$IMPACT/impactTin_medium_001.ogg" \
  -af "asetrate=44100*1.35,aresample=44100,volume=0.7,afade=t=out:st=0.3:d=0.2" \
  -ac 2 -ar 44100 -c:a vorbis -q:a 4 "$OUT/event/skill_upgrade.ogg" 2>/dev/null
echo "  skill_upgrade OK"

echo ""
echo "=== Verifying all outputs ==="
for f in "$OUT"/skill/cast/*.ogg "$OUT"/skill/hit/*.ogg "$OUT"/ui/*.ogg "$OUT"/event/*.ogg; do
  info=$(ffprobe -v quiet -show_entries stream=channels,sample_rate,duration -of csv=p=0 "$f" 2>/dev/null)
  name=$(basename "$f")
  echo "  $name: $info"
done

echo ""
echo "=== Done: $(find "$OUT" -name '*.ogg' | wc -l | tr -d ' ') OGG files produced ==="

# Cleanup
rm -rf "$TMP"
