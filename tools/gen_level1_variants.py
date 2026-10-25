# -*- coding: utf-8 -*-
"""为 Level 1 的 5 个方块批量生成「楼梯 / 台阶 / 墙 / 栅栏」变体。

做法与 Level 0 的 28 个变体完全一致：
  · blockstates 直接照抄原版 oak_slab / oak_stairs / cobblestone_wall / oak_fence，只换模型命名空间
  · 模型用原版父模型 + 覆盖贴图
  · 墙用 ConnectingWallBlock（能与栅栏双向连接），栅栏用 ConnectingFenceBlock
"""
import io
import json
import os
import zipfile

ROOT = r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.1"
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "the_backrooms_complex")
A = os.path.join(ROOT, "src", "main", "resources", "assets", "backrooms_complex")
JAR = r"C:\Users\lin13\.mcreator\gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar"
NS = "backrooms_complex"

# 基底方块: (id, 侧贴图, 顶贴图)
BASES = [
    ("concrete_pillar",         "minecraft:block/light_gray_concrete",        "minecraft:block/light_gray_concrete"),
    ("concrete_warning_pillar", "minecraft:block/reinforced_deepslate_side",  "minecraft:block/reinforced_deepslate_top"),
    ("concrete_floor",          "minecraft:block/stone",                      "minecraft:block/stone"),
    ("concrete_ceiling",        "minecraft:block/smooth_stone",               "minecraft:block/smooth_stone"),
    ("white_plastered_wall",    "minecraft:block/stripped_birch_log",         "minecraft:block/stripped_birch_log_top"),
]

VARIANTS = ("stairs", "slab", "wall", "fence")

# 原版 blockstate 模板
VANILLA_BS = {
    "slab": ("blockstates/oak_slab.json", "minecraft:block/oak_", NS + ":block/{id}_"),
    "stairs": ("blockstates/oak_stairs.json", "minecraft:block/oak_", NS + ":block/{id}_"),
    "wall": ("blockstates/cobblestone_wall.json", "minecraft:block/cobblestone_", NS + ":block/{id}_"),
    "fence": ("blockstates/oak_fence.json", "minecraft:block/oak_", NS + ":block/{id}_"),
}

# 模型清单: (后缀, 原版父模型, 贴图键 -> 取 side 还是 top)
MODELS = {
    "stairs": [
        ("stairs", "minecraft:block/stairs", ("bottom", "top", "side")),
        ("stairs_inner", "minecraft:block/inner_stairs", ("bottom", "top", "side")),
        ("stairs_outer", "minecraft:block/outer_stairs", ("bottom", "top", "side")),
    ],
    "slab": [
        ("slab", "minecraft:block/slab", ("bottom", "top", "side")),
        ("slab_top", "minecraft:block/slab_top", ("bottom", "top", "side")),
    ],
    "wall": [
        ("wall_post", "minecraft:block/template_wall_post", ("wall",)),
        ("wall_side", "minecraft:block/template_wall_side", ("wall",)),
        ("wall_side_tall", "minecraft:block/template_wall_side_tall", ("wall",)),
    ],
    "fence": [
        ("fence_post", "minecraft:block/fence_post", ("texture",)),
        ("fence_side", "minecraft:block/fence_side", ("texture",)),
    ],
}

z = zipfile.ZipFile(JAR)


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def tex_for(kind, side, top):
    return top if kind == "top" else side


count = {"bs": 0, "model": 0, "item": 0}

for base, side, top in BASES:
    for variant in VARIANTS:
        vid = "%s_%s" % (base, variant)

        # ---------- blockstate（照抄原版）----------
        src, old_ns, new_ns = VANILLA_BS[variant]
        data = json.loads(z.read("assets/minecraft/" + src).decode("utf-8"))
        text = json.dumps(data, ensure_ascii=False, indent=2)
        text = text.replace(old_ns, new_ns.format(id=vid))
        with io.open(os.path.join(A, "blockstates", vid + ".json"), "w",
                     encoding="utf-8", newline="\n") as f:
            f.write(text)
        count["bs"] += 1

        # ---------- 模型 ----------
        for suffix, parent, keys in MODELS[variant]:
            textures = {}
            for k in keys:
                # 原版模板里 side/bottom/top 分别对应 base 模型的不同槽位
                if k == "top":
                    textures[k] = top
                else:
                    textures[k] = side
            write(os.path.join(A, "models", "block", "%s_%s.json" % (vid, suffix)),
                  {"parent": parent, "textures": textures})
            count["model"] += 1

        # 物品模型：楼梯/台阶/栅栏直接用方块模型；墙用 inventory 模型
        if variant == "wall":
            write(os.path.join(A, "models", "block", vid + "_inventory.json"),
                  {"parent": "minecraft:block/wall_inventory",
                   "textures": {"wall": side}})
            count["model"] += 1
            item_model = {"parent": "%s:block/%s_inventory" % (NS, vid)}
        elif variant == "fence":
            write(os.path.join(A, "models", "block", vid + "_inventory.json"),
                  {"parent": "minecraft:block/fence_inventory",
                   "textures": {"texture": side}})
            count["model"] += 1
            item_model = {"parent": "%s:block/%s_inventory" % (NS, vid)}
        else:
            item_model = {"parent": "%s:block/%s_%s" % (NS, vid, variant)}
        write(os.path.join(A, "models", "item", vid + ".json"), item_model)
        count["item"] += 1

print("资源生成完成: blockstate %d / model %d / item %d" % (count["bs"], count["model"], count["item"]))

# ---------- 语言 ----------
NAMES = {
    "concrete_pillar": ("混凝土柱", "Concrete Pillar"),
    "concrete_warning_pillar": ("混凝土警示柱", "Concrete Warning Pillar"),
    "concrete_floor": ("混凝土地板", "Concrete Floor"),
    "concrete_ceiling": ("混凝土天花板", "Concrete Ceiling"),
    "white_plastered_wall": ("粉刷为白色的混凝土墙壁", "White Plastered Concrete Wall"),
}
SUFFIX_ZH = {"stairs": "楼梯", "slab": "台阶", "wall": "墙", "fence": "栅栏"}
SUFFIX_EN = {"stairs": "Stairs", "slab": "Slab", "wall": "Wall", "fence": "Fence"}

for lang, idx in (("zh_cn", 0), ("en_us", 1)):
    p = os.path.join(A, "lang", lang + ".json")
    data = json.loads(io.open(p, encoding="utf-8").read())
    for base, _, _ in BASES:
        for variant in VARIANTS:
            vid = "%s_%s" % (base, variant)
            if lang == "zh_cn":
                data["block.%s.%s" % (NS, vid)] = NAMES[base][0] + SUFFIX_ZH[variant]
            else:
                data["block.%s.%s" % (NS, vid)] = NAMES[base][1] + " " + SUFFIX_EN[variant]
    with io.open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
print("语言文件已更新（中/英各 20 条）")
