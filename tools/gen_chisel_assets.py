"""凿子 / 被凿破的混凝土墙壁 / 墙灰 的贴图与资源。"""
import json
import os
import zipfile

from PIL import Image

SRC = r"C:\Users\lin13\Desktop\新建文件夹 (2)"
ROOT = r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.0\src\main\resources"
ASSETS = os.path.join(ROOT, "assets", "backrooms_complex")
DATA = os.path.join(ROOT, "data", "backrooms_complex")
TEXB = os.path.join(ASSETS, "textures", "block")
TEXI = os.path.join(ASSETS, "textures", "item")
CLIENT = r"C:\Users\lin13\.mcreator\gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar"


def save_json(obj, rel, base=ASSETS):
    p = os.path.join(base, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


print("1) 复制贴图")
chisel = Image.open(os.path.join(SRC, "凿子.png")).convert("RGBA")
if chisel.size != (16, 16):
    chisel = chisel.resize((16, 16), Image.NEAREST)
chisel.save(os.path.join(TEXI, "chisel.png"))
print("   凿子.png -> item/chisel.png")

wall = Image.open(os.path.join(SRC, "被凿破的混凝土墙壁.png")).convert("RGBA")
if wall.size != (16, 16):
    wall = wall.resize((16, 16), Image.NEAREST)
wall.save(os.path.join(TEXB, "chiseled_concrete_wall.png"))
wall.save(os.path.join(TEXI, "chiseled_concrete_wall.png"))
print("   被凿破的混凝土墙壁.png -> block/item chiseled_concrete_wall.png")

print("2) 墙灰 = 原版火药染淡灰")
zj = zipfile.ZipFile(CLIENT)
with zj.open("assets/minecraft/textures/item/gunpowder.png") as f:
    gp = Image.open(f).convert("RGBA")
zj.close()
px = []
for r, g, b, a in gp.getdata():
    if a == 0:
        px.append((0, 0, 0, 0))
    else:
        lum = 0.299 * r + 0.587 * g + 0.114 * b
        # 提亮 + 去饱和 => 淡灰
        v = int(min(255, lum * 1.45 + 55))
        px.append((min(255, v + 2), min(255, v + 2), min(255, v + 3), a))
dust = Image.new("RGBA", gp.size)
dust.putdata(px)
dust.save(os.path.join(TEXI, "wall_dust.png"))
op = [q for q in px if q[3] > 128]
avg = tuple(sum(q[i] for q in op) // len(op) for i in range(3))
print("   item/wall_dust.png 平均 RGB = %s" % (avg,))

print("3) 方块状态与模型")
bid = "chiseled_concrete_wall"
save_json({"variants": {"": {"model": "backrooms_complex:block/" + bid}}}, "blockstates/%s.json" % bid)
save_json({"parent": "minecraft:block/cube_all", "textures": {"all": "backrooms_complex:block/" + bid}},
          "models/block/%s.json" % bid)
save_json({"parent": "backrooms_complex:block/" + bid}, "models/item/%s.json" % bid)
for it in ("chisel", "wall_dust"):
    save_json({"parent": "minecraft:item/generated", "textures": {"layer0": "backrooms_complex:item/" + it}},
              "models/item/%s.json" % it)
print("   blockstates/models 完成")

print("4) 合成配方：2 铁粒 + 1 木棍 -> 凿子")
save_json({"type": "minecraft:crafting_shaped",
           "pattern": ["N", "N", "S"],
           "key": {"N": {"item": "minecraft:iron_nugget"}, "S": {"item": "minecraft:stick"}},
           "result": {"id": "backrooms_complex:chisel", "count": 1}},
          "recipe/chisel.json", base=DATA)
print("   data/backrooms_complex/recipe/chisel.json")

print("5) 语言文件")
add = {
    "zh_cn": {
        "item.backrooms_complex.chisel": "凿子",
        "item.backrooms_complex.wall_dust": "墙灰",
        "block.backrooms_complex.chiseled_concrete_wall": "被凿破的混凝土墙壁",
    },
    "en_us": {
        "item.backrooms_complex.chisel": "Chisel",
        "item.backrooms_complex.wall_dust": "Wall Dust",
        "block.backrooms_complex.chiseled_concrete_wall": "Chiseled Concrete Wall",
    },
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
print("完成")
