"""生成混凝土大厅群系的 5 个新方块资源，并把混凝土踢脚线改为暗灰色。"""
import json
import os

from PIL import Image

SRC = r"C:\Users\lin13\Desktop\新建文件夹 (2)"
ROOT = r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.0\src\main\resources"
ASSETS = os.path.join(ROOT, "assets", "backrooms_complex")
TEXB = os.path.join(ASSETS, "textures", "block")
TEXI = os.path.join(ASSETS, "textures", "item")

# (贴图文件, 方块ID, 中文名, 英文名)
BLOCKS = [
    ("剥离地毯的地板.png",   "stripped_carpet_floor", "剥离地毯的地板",   "Stripped Carpet Floor"),
    ("破败的天花板.png",     "ruined_ceiling",        "破败的天花板",     "Ruined Ceiling"),
    ("破败的通风口.png",     "ruined_vent",           "破败的通风口",     "Ruined Vent"),
    ("破碎的混凝土墙壁.png", "cracked_concrete_wall", "破碎的混凝土墙壁", "Cracked Concrete Wall"),
    ("警示的混凝土墙壁.png", "warning_concrete_wall", "警示的混凝土墙壁", "Warning Concrete Wall"),
]


def save_json(obj, rel):
    p = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


print("1) 复制贴图并生成模型")
for src, bid, zh, en in BLOCKS:
    img = Image.open(os.path.join(SRC, src)).convert("RGBA")
    if img.size != (16, 16):
        img = img.resize((16, 16), Image.NEAREST)
    img.save(os.path.join(TEXB, bid + ".png"))
    img.save(os.path.join(TEXI, bid + ".png"))
    # 方块状态 + 模型
    save_json({"variants": {"": {"model": "backrooms_complex:block/" + bid}}},
              "blockstates/%s.json" % bid)
    save_json({"parent": "minecraft:block/cube_all",
               "textures": {"all": "backrooms_complex:block/" + bid}},
              "models/block/%s.json" % bid)
    save_json({"parent": "backrooms_complex:block/" + bid}, "models/item/%s.json" % bid)
    print("   %-22s -> %s" % (src, bid))

print("2) 混凝土踢脚线 -> 暗灰色")
pl = os.path.join(TEXB, "wall_concrete_plinth.png")
im = Image.open(pl).convert("RGBA")
px = []
for r, g, b, a in im.getdata():
    if a == 0:
        px.append((0, 0, 0, 0))
    else:
        # 先去饱和再压暗，得到暗灰调
        lum = int(0.299 * r + 0.587 * g + 0.114 * b)
        v = int(lum * 0.52)
        px.append((min(255, v + 6), min(255, v + 6), min(255, v + 8), a))
im.putdata(px)
im.save(pl)
avg = [sum(q[i] for q in px if q[3] > 128) // max(1, len([q for q in px if q[3] > 128])) for i in range(3)]
print("   wall_concrete_plinth.png 平均 RGB = %s" % avg)

print("3) 语言文件")
for loc, idx in (("zh_cn", 2), ("en_us", 3)):
    p = os.path.join(ASSETS, "lang", loc + ".json")
    with open(p, encoding="utf-8") as f:
        d = json.load(f)
    for src, bid, zh, en in BLOCKS:
        d["block.backrooms_complex." + bid] = zh if loc == "zh_cn" else en
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("   %s: %d keys" % (loc, len(d)))
print("完成")
