"""生成「刮纸刀 / 墙纸 / 墙纸大衣 / 墙纸帽 / 去墙纸墙面」的全部贴图与资源 JSON。"""
import json
import os

from PIL import Image

ROOT = r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.0\src\main\resources"
ASSETS = os.path.join(ROOT, "assets", "backrooms_complex")
DATA = os.path.join(ROOT, "data", "backrooms_complex")
CLIENT = r"C:\Users\lin13\.mcreator\gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar"
OAKED_SRC = r"C:\Users\lin13\.dsh\attachments\v1\objects\70\70146fc247952f534b307452a3f1be455b881338c4aa094afa4dba6d88b8438c"


def save_img(img, rel):
    p = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.convert("RGBA").save(p)
    print("  贴图 " + rel)


def save_json(obj, rel, base=ASSETS):
    p = os.path.join(base, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("  JSON " + rel)


# ---------- 从原版 jar 取素材 ----------
import zipfile

zj = zipfile.ZipFile(CLIENT)
WALL = Image.open(os.path.join(ASSETS, "textures", "block", "wall_yellow.png")).convert("RGBA")


def vanilla(name):
    with zj.open(name) as f:
        return Image.open(f).convert("RGBA")


def armor_icon(src_name, wall, button=False):
    """用原版皮革护甲图标的 alpha 作形状，填入黄色墙壁纹理，并保留明暗。"""
    src = vanilla(src_name)
    w, h = src.size
    wall_small = wall.resize((w, h), Image.NEAREST)
    lum = [0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2] for p in src.getdata()]
    vals = [v for v, p in zip(lum, src.getdata()) if p[3] > 0]
    mean = sum(vals) / len(vals) if vals else 1.0
    out = Image.new("RGBA", (w, h))
    px = []
    for (sr, sg, sb, sa), (wr, wg, wb, _), l in zip(src.getdata(), wall_small.getdata(), lum):
        if sa == 0:
            px.append((0, 0, 0, 0))
            continue
        k = l / mean
        px.append((min(255, int(wr * k)), min(255, int(wg * k)), min(255, int(wb * k)), sa))
    out.putdata(px)
    if button:
        # 在帽子正面画一颗深色纽扣
        for (bx, by) in [(7, 9), (8, 9), (7, 10), (8, 10)]:
            px[by * w + bx] = (90, 78, 40, 255)
    out.putdata(px)
    return out


print("生成贴图:")
# 1) 刮纸刀：木柄 + 顶部刀片
sc = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
sp = sc.load()
for i in range(9):                      # 木柄：左下 -> 右上
    x, y = 3 + i, 13 - i
    sp[x, y] = (124, 88, 46, 255)
    sp[x + 1, y] = (96, 66, 32, 255)
for i in range(4):                      # 刀片
    x, y = 12 + i, 4 - i
    sp[x, y] = (198, 202, 208, 255)
    sp[x, y + 1] = (150, 154, 160, 255)
sp[12, 5] = (232, 236, 240, 255)        # 刃口高光
sp[13, 3] = (110, 114, 120, 255)
save_img(sc, "textures/item/scraper.png")

# 2) 墙纸：黄色底 + 黄褐色条纹
wp = Image.new("RGBA", (16, 16))
wpp = wp.load()
for y in range(16):
    for x in range(16):
        stripe = (x % 4 == 0) or (x % 4 == 1 and (x // 4) % 2 == 1)
        base = (214, 196, 106) if not stripe else (176, 148, 66)
        if (x + y) % 7 == 0:
            base = tuple(min(255, c + 12) for c in base)
        wpp[x, y] = base + (255,)
save_img(wp, "textures/item/wallpaper.png")

# 3/4) 墙纸大衣 / 墙纸帽
save_img(armor_icon("assets/minecraft/textures/item/leather_chestplate.png", WALL),
         "textures/item/wallpaper_coat.png")
save_img(armor_icon("assets/minecraft/textures/item/leather_helmet.png", WALL, button=True),
         "textures/item/wallpaper_hat.png")

# 5) 去墙纸墙面（直接使用用户提供的图）
oak = Image.open(OAKED_SRC).convert("RGBA")
save_img(oak, "textures/block/wall_oaked.png")

# 6/7) 穿戴时的护甲层贴图（沿用原版皮革层 + 黄色墙壁上色）
for layer in (1, 2):
    src = vanilla("assets/minecraft/textures/models/armor/leather_layer_%d.png" % layer)
    w, h = src.size
    wall_small = WALL.resize((w, h), Image.NEAREST)
    lum = [0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2] for p in src.getdata()]
    vals = [v for v, p in zip(lum, src.getdata()) if p[3] > 0]
    mean = sum(vals) / len(vals) if vals else 1.0
    out = Image.new("RGBA", (w, h))
    px = []
    for (sr, sg, sb, sa), (wr, wg, wb, _), l in zip(src.getdata(), wall_small.getdata(), lum):
        if sa == 0:
            px.append((0, 0, 0, 0))
            continue
        k = min(1.35, l / mean)
        px.append((min(255, int(wr * k)), min(255, int(wg * k)), min(255, int(wb * k)), sa))
    out.putdata(px)
    save_img(out, "textures/models/armor/wallpaper_layer_%d.png" % layer)

print("生成 JSON:")
# 去墙纸方块：三种变体共用一个贴图
save_json({
    "variants": {
        "variant=yellow":   {"model": "backrooms_complex:block/wall_oaked"},
        "variant=concrete": {"model": "backrooms_complex:block/wall_oaked"},
        "variant=damp":     {"model": "backrooms_complex:block/wall_oaked"}
    }
}, "blockstates/wall_oaked.json")
save_json({
    "parent": "minecraft:block/cube_all",
    "textures": {"all": "backrooms_complex:block/wall_oaked"}
}, "models/block/wall_oaked.json")
save_json({"parent": "backrooms_complex:block/wall_oaked"}, "models/item/wall_oaked.json")

for it in ("scraper", "wallpaper", "wallpaper_coat", "wallpaper_hat"):
    save_json({"parent": "minecraft:item/generated",
               "textures": {"layer0": "backrooms_complex:item/" + it}},
              "models/item/%s.json" % it)

# 合成配方
save_json({"type": "minecraft:crafting_shaped",
           "pattern": ["N", "S"],
           "key": {"N": {"item": "minecraft:iron_nugget"}, "S": {"item": "minecraft:stick"}},
           "result": {"id": "backrooms_complex:scraper", "count": 1}},
          "recipe/scraper.json", base=DATA)
save_json({"type": "minecraft:crafting_shaped",
           "pattern": ["W W", "WWW", "WWW"],
           "key": {"W": {"item": "backrooms_complex:wallpaper"}},
           "result": {"id": "backrooms_complex:wallpaper_coat", "count": 1}},
          "recipe/wallpaper_coat.json", base=DATA)
save_json({"type": "minecraft:crafting_shaped",
           "pattern": ["WWW", "W W"],
           "key": {"W": {"item": "backrooms_complex:wallpaper"}},
           "result": {"id": "backrooms_complex:wallpaper_hat", "count": 1}},
          "recipe/wallpaper_hat.json", base=DATA)

# 可替代纸：加入 NeoForge 通用标签 c:paper（其它模组用 c:paper 的配方即可接受墙纸）
save_json({"replace": False, "values": ["backrooms_complex:wallpaper"]},
          "tags/item/paper.json", base=os.path.join(ROOT, "data", "c"))

zj.close()
print("完成")
