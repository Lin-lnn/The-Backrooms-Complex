"""生成枢纽之门与层级密钥的贴图。

1. 从原版客户端 jar 提取云杉木门贴图，做"黄色调"染色 -> hub_door_top.png / hub_door_bottom.png
2. 绘制 16x16 黄色钥匙贴图 -> threshold_key.png
"""
import io
import os
import zipfile

from PIL import Image

CLIENT_JAR = r"C:\Users\lin13\.mcreator\gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar"
RES = r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.0\src\main\resources\assets\backrooms_complex\textures"
OUT_BLOCK = os.path.join(RES, "block")
OUT_ITEM = os.path.join(RES, "item")
os.makedirs(OUT_ITEM, exist_ok=True)

assert os.path.isfile(CLIENT_JAR), "找不到原版客户端 jar: " + CLIENT_JAR
jar = zipfile.ZipFile(CLIENT_JAR)


def load_png(name):
    return Image.open(io.BytesIO(jar.read(name))).convert("RGBA")


def tint_yellow(img, hue=42, sat_boost=1.15, sat_add=45):
    """把图片整体染成黄色调，保留明度与透明度（云杉门 -> 黄色门）。"""
    r, g, b, a = img.split()
    hsv = Image.merge("RGB", (r, g, b)).convert("HSV")
    h, s, v = hsv.split()
    h = h.point(lambda _: hue)
    s = s.point(lambda p: min(255, int(p * sat_boost) + sat_add))
    rgb = Image.merge("HSV", (h, s, v)).convert("RGB").convert("RGBA")
    rgb.putalpha(a)
    return rgb


for src, dst in (("spruce_door_top.png", "hub_door_top.png"),
                 ("spruce_door_bottom.png", "hub_door_bottom.png")):
    img = load_png("assets/minecraft/textures/block/" + src)
    out = tint_yellow(img)
    out.save(os.path.join(OUT_BLOCK, dst))
    print("生成 %s  %s  不透明像素=%d" % (dst, out.size, sum(1 for p in out.getdata() if p[3] > 0)))

# ---------------- 钥匙贴图（16x16 像素画） ----------------
PATTERN = [
    "................",
    ".....####.......",
    "....#*ooo#......",
    "....#o##o#......",
    "....#o##o#......",
    "....#ooo*#......",
    ".....####.......",
    "......#o........",
    "......#o........",
    "......#o........",
    "......#o#.......",
    "......#o#.......",
    "......#o##......",
    "......#o........",
    "......#o........",
    "................",
]
COLORS = {
    "#": (74, 56, 10, 255),     # 深棕描边
    "o": (242, 201, 64, 255),   # 金色主体
    "*": (255, 244, 170, 255),  # 高光
    ".": (0, 0, 0, 0),          # 透明
}

key = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
px = key.load()
for y, row in enumerate(PATTERN):
    assert len(row) == 16, "第 %d 行长度不是 16" % y
    for x, ch in enumerate(row):
        px[x, y] = COLORS[ch]
key.save(os.path.join(OUT_ITEM, "threshold_key.png"))
print("生成 threshold_key.png  %s  不透明像素=%d"
      % (key.size, sum(1 for p in key.getdata() if p[3] > 0)))
print("完成")
