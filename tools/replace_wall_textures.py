"""用用户提供的新贴图替换三种墙壁 + 去墙纸墙面，并同步更新踢脚线的"墙壁部分"。"""
import os

from PIL import Image

ROOT = r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.0\src\main\resources"
TEX = os.path.join(ROOT, "assets", "backrooms_complex", "textures")
BLOCK = os.path.join(TEX, "block")
AT = r"C:\Users\lin13\.dsh\attachments\v1\objects"

SRC = {
    "wall_yellow":   os.path.join(AT, "2f", "2f743969c97c68f5f18958e6f68d96c01f968efee41a265e1f404993408988b5"),
    "wall_concrete": os.path.join(AT, "ef", "ef84a16cc6f9fea58d2d8f6b601c5d6f3139e327878e68a7f4858b87cbbcc39b"),
    "wall_oaked":    os.path.join(AT, "7b", "7bd642bb8fb912c8e98fe6c5b17758342f48353c260c2c94558422fdb55b8fd9"),
    "wall_damp":     os.path.join(AT, "6d", "6db25b30d344d323b0cc32ff3eb7a81847a26e3cd4802007eeb3c1234149da3a"),
}

# 踢脚线图集中"墙壁部分"对应的 16x16 图块（模型 UV × 4 = 像素）
WALL_TILES = [(0, 0), (0, 16), (16, 0), (16, 16), (0, 32), (32, 0)]

print("1) 替换墙壁主体贴图")
for name, path in SRC.items():
    im = Image.open(path).convert("RGBA")
    if im.size != (16, 16):
        im = im.resize((16, 16), Image.NEAREST)
    im.save(os.path.join(BLOCK, name + ".png"))
    print("   %-16s <- %s" % (name + ".png", os.path.basename(path)[:12] + "..."))

print("2) 更新踢脚线图集的墙壁部分（保留裙边区域）")
for name in ("wall_yellow", "wall_concrete", "wall_damp"):
    wall = Image.open(os.path.join(BLOCK, name + ".png")).convert("RGBA")
    atlas_path = os.path.join(BLOCK, name + "_plinth.png")
    atlas = Image.open(atlas_path).convert("RGBA")
    for (px, py) in WALL_TILES:
        atlas.paste(wall, (px, py))
    atlas.save(atlas_path)
    print("   %-28s 已替换 %d 个图块" % (name + "_plinth.png", len(WALL_TILES)))

print("3) 用新黄色墙壁重绘墙纸护甲贴图（保持『黄色墙壁纹理』设定）")
wall = Image.open(os.path.join(BLOCK, "wall_yellow.png")).convert("RGBA")
import zipfile
CLIENT = r"C:\Users\lin13\.mcreator\gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar"
zj = zipfile.ZipFile(CLIENT)


def recolor(src, button=False):
    w, h = src.size
    small = wall.resize((w, h), Image.NEAREST)
    px_src = list(src.getdata())
    lum = [0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2] for p in px_src]
    vals = [v for v, p in zip(lum, px_src) if p[3] > 0]
    mean = sum(vals) / len(vals) if vals else 1.0
    out = []
    for p, q, l in zip(px_src, small.getdata(), lum):
        if p[3] == 0:
            out.append((0, 0, 0, 0))
        else:
            k = min(1.35, l / mean)
            out.append((min(255, int(q[0] * k)), min(255, int(q[1] * k)), min(255, int(q[2] * k)), p[3]))
    if button:
        for (bx, by) in [(7, 9), (8, 9), (7, 10), (8, 10)]:
            out[by * w + bx] = (90, 78, 40, 255)
    img = Image.new("RGBA", (w, h))
    img.putdata(out)
    return img


for icon, dest, btn in (
        ("assets/minecraft/textures/item/leather_chestplate.png", "item/wallpaper_coat.png", False),
        ("assets/minecraft/textures/item/leather_helmet.png", "item/wallpaper_hat.png", True)):
    with zj.open(icon) as f:
        src = Image.open(f).convert("RGBA")
    recolor(src, btn).save(os.path.join(TEX, dest))
    print("   " + dest)

for layer in (1, 2):
    with zj.open("assets/minecraft/textures/models/armor/leather_layer_%d.png" % layer) as f:
        src = Image.open(f).convert("RGBA")
    recolor(src).save(os.path.join(TEX, "models", "armor", "wallpaper_layer_%d.png" % layer))
    print("   models/armor/wallpaper_layer_%d.png" % layer)

zj.close()
print("完成")
