"""生成枢纽之门（DoorBlock）与层级密钥所需的模型 / blockstate / 物品图标。

门模型完全复用原版模板（minecraft:block/door_*），只替换贴图为模组的黄色门贴图，
因此正反面贴图与开关/合页朝向都严格符合原版门模型规范。
"""
import io
import json
import os
import zipfile

from PIL import Image

CLIENT_JAR = r"C:\Users\lin13\.mcreator\gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar"
NS = "backrooms_complex"
ASSETS = (r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.0"
          r"\src\main\resources\assets") + "\\" + NS

jar = zipfile.ZipFile(CLIENT_JAR)


def write_json(rel_path, obj):
    path = os.path.join(ASSETS, rel_path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print("生成 " + rel_path)


# ---------------- 1. 门方块模型（8 个，复用原版模板） ----------------
DOOR_PARTS = [
    "bottom_left", "bottom_left_open", "bottom_right", "bottom_right_open",
    "top_left", "top_left_open", "top_right", "top_right_open",
]
for part in DOOR_PARTS:
    write_json("models/block/hub_door_%s.json" % part, {
        "parent": "minecraft:block/door_%s" % part,
        "textures": {
            "bottom": "%s:block/hub_door_bottom" % NS,
            "top": "%s:block/hub_door_top" % NS,
        },
    })

# ---------------- 2. 门 blockstate（照抄原版 32 变体，仅换模型名） ----------------
raw = jar.read("assets/minecraft/blockstates/spruce_door.json").decode("utf-8")
raw = raw.replace("minecraft:block/spruce_door_", "%s:block/hub_door_" % NS)
data = json.loads(raw)
write_json("blockstates/hub_door.json", data)
print("   门 blockstate 变体数 = %d" % len(data["variants"]))

# ---------------- 3. 物品模型 ----------------
write_json("models/item/hub_door.json", {
    "parent": "minecraft:item/generated",
    "textures": {"layer0": "%s:item/hub_door" % NS},
})
write_json("models/item/threshold_key.json", {
    "parent": "minecraft:item/generated",
    "textures": {"layer0": "%s:item/threshold_key" % NS},
})

# ---------------- 4. 门的物品图标（原版云杉门图标染成黄色调） ----------------
img = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/item/spruce_door.png"))).convert("RGBA")
r, g, b, a = img.split()
h, s, v = Image.merge("RGB", (r, g, b)).convert("HSV").split()
h = h.point(lambda _: 42)
s = s.point(lambda p: min(255, int(p * 1.15) + 45))
out = Image.merge("HSV", (h, s, v)).convert("RGB").convert("RGBA")
out.putalpha(a)
item_dir = os.path.join(ASSETS, "textures", "item")
os.makedirs(item_dir, exist_ok=True)
out.save(os.path.join(item_dir, "hub_door.png"))
print("生成 textures/item/hub_door.png  %s" % (out.size,))
print("完成")
