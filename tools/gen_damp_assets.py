"""潮湿的地板 / 潮湿的天花板 资源 + 潮湿群系的渗水粒子。"""
import json
import os

from PIL import Image

SRC = r"C:\Users\lin13\Desktop\新建文件夹 (2)"
ROOT = r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.0\src\main\resources"
ASSETS = os.path.join(ROOT, "assets", "backrooms_complex")
TEXB = os.path.join(ASSETS, "textures", "block")
TEXI = os.path.join(ASSETS, "textures", "item")

BLOCKS = [
    ("潮湿的地板.png",   "damp_floor",   "潮湿的地板",   "Damp Floor"),
    ("潮湿的天花板.png", "damp_ceiling", "潮湿的天花板", "Damp Ceiling"),
]


def save_json(obj, rel):
    p = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


print("1) 贴图与模型")
for src, bid, zh, en in BLOCKS:
    img = Image.open(os.path.join(SRC, src)).convert("RGBA")
    if img.size != (16, 16):
        img = img.resize((16, 16), Image.NEAREST)
    img.save(os.path.join(TEXB, bid + ".png"))
    img.save(os.path.join(TEXI, bid + ".png"))
    save_json({"variants": {"": {"model": "backrooms_complex:block/" + bid}}}, "blockstates/%s.json" % bid)
    save_json({"parent": "minecraft:block/cube_all", "textures": {"all": "backrooms_complex:block/" + bid}},
              "models/block/%s.json" % bid)
    save_json({"parent": "backrooms_complex:block/" + bid}, "models/item/%s.json" % bid)
    print("   %-18s -> %s" % (src, bid))

print("2) 语言")
add = {
    "zh_cn": {"block.backrooms_complex.damp_floor": "潮湿的地板",
              "block.backrooms_complex.damp_ceiling": "潮湿的天花板"},
    "en_us": {"block.backrooms_complex.damp_floor": "Damp Floor",
              "block.backrooms_complex.damp_ceiling": "Damp Ceiling"},
}
for loc, kv in add.items():
    p = os.path.join(ASSETS, "lang", loc + ".json")
    with open(p, encoding="utf-8") as f:
        d = json.load(f)
    d.update(kv)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("   %s: %d keys" % (loc, len(d)))

print("3) 潮湿群系加入渗水粒子（原版 dripping_water）")
bp = os.path.join(ROOT, "data", "backrooms_complex", "worldgen", "biome", "damp_corridors.json")
with open(bp, encoding="utf-8") as f:
    b = json.load(f)
b.setdefault("effects", {})["particle"] = {
    "options": {"type": "minecraft:dripping_water"},
    "probability": 0.02,
}
with open(bp, "w", encoding="utf-8", newline="\n") as f:
    json.dump(b, f, ensure_ascii=False, indent=2)
    f.write("\n")
print("   damp_corridors.particle = dripping_water, p=0.02")
print("完成")
