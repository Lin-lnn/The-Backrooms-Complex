"""把墙壁/踢脚线/地板/天花板贴图转成灰度，并输出群系色表（供 BlockColor 染色）。

规则：
  1. 每个群系的"色相"取其墙壁贴图的平均色；
  2. 每张贴图转灰度时，按 "该贴图平均亮度 / 该群系墙壁平均亮度 × 255" 归一化，
     从而保留不同材质之间的相对明暗（地板比墙亮、裙边比墙暗等）；
  3. 染色器在运行时做邻域平均，因此边界处会平滑过渡。
"""
import json
import os

from PIL import Image

ROOT = r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.0\src\main\resources"
TEX = os.path.join(ROOT, "assets", "backrooms_complex", "textures", "block")

# (贴图名, 所属群系)
ITEMS = [
    ("wall_yellow", "yellow"),
    ("wall_concrete", "concrete"),
    ("wall_damp", "damp"),
    ("wall_yellow_plinth", "yellow"),
    ("wall_concrete_plinth", "concrete"),
    ("wall_damp_plinth", "damp"),
    ("floor_tile", "yellow"),
    ("stripped_carpet_floor", "concrete"),
    ("damp_floor", "damp"),
    ("ceiling_tile", "yellow"),
    ("ruined_ceiling", "concrete"),
    ("damp_ceiling", "damp"),
    ("wall_oaked", "yellow"),
    ("chiseled_concrete_wall", "concrete"),
]

WALL_OF = {"yellow": "wall_yellow", "concrete": "wall_concrete", "damp": "wall_damp"}


def load(name):
    return Image.open(os.path.join(TEX, name + ".png")).convert("RGBA")


def mean_rgb(img):
    px = [p for p in img.getdata() if p[3] > 128]
    if not px:
        return (128, 128, 128)
    return tuple(sum(p[i] for p in px) // len(px) for i in range(3))


def mean_lum(img):
    px = [p for p in img.getdata() if p[3] > 128]
    if not px:
        return 128.0
    return sum(0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2] for p in px) / len(px)


print("1) 提取每个群系的色相（墙壁平均色）")
hue = {}
wall_lum = {}
for b, wall in WALL_OF.items():
    img = load(wall)
    hue[b] = mean_rgb(img)
    wall_lum[b] = mean_lum(img)
    print("   %-9s 色相 RGB=%s  平均亮度=%.1f" % (b, hue[b], wall_lum[b]))

print("2) 生成灰度贴图")
for name, biome in ITEMS:
    img = load(name)
    ref = wall_lum[biome]
    px = []
    for r, g, b, a in img.getdata():
        if a == 0:
            px.append((0, 0, 0, 0))
            continue
        lum = 0.299 * r + 0.587 * g + 0.114 * b
        v = int(round(lum * 255.0 / max(1.0, ref)))
        v = max(0, min(255, v))
        px.append((v, v, v, a))
    out = Image.new("RGBA", img.size)
    out.putdata(px)
    out.save(os.path.join(TEX, name + ".png"))
    print("   %-26s -> 灰度（相对亮度基准 %.1f）" % (name, ref))

print("3) 输出群系色表（Java 常量用）")
for b in ("yellow", "concrete", "damp"):
    c = hue[b]
    print("   %-9s 0x%02X%02X%02X" % (b, c[0], c[1], c[2]))

# 记录到文件，便于之后核对
with open(os.path.join(ROOT, "..", "tools", "biome_hues.json"), "w", encoding="utf-8", newline="\n") as f:
    json.dump({b: list(hue[b]) for b in hue}, f, ensure_ascii=False, indent=2)
print("完成")
