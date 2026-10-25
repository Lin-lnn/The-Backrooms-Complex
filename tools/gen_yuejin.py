# -*- coding: utf-8 -*-
"""生成「跃金段」的方块与其 4 种变体。

贴图：从现有 Level 1 方块贴图派生为米黄色调（先灰度化再加米黄乘色，保留明暗）。
方块：
    yellowed_concrete_floor    泛黄的混凝土地板     ← stone
    yellowed_concrete_wall     泛黄的混凝土墙       ← light_gray_concrete
    yellowed_corrugated_metal  泛黄的金属瓦楞板     ← stripped_birch_log
变体：每个方块各有 stairs / slab / wall / fence 四版。
"""
import io
import json
import os
import zipfile
from PIL import Image

ROOT = r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.1"
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "the_backrooms_complex")
A = os.path.join(ROOT, "src", "main", "resources", "assets", "backrooms_complex")
JAR = r"C:\Users\lin13\.mcreator\gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar"
NS = "backrooms_complex"
z = zipfile.ZipFile(JAR)

# 米黄色（跃金段主色）
BEIGE = (232, 214, 160)

# id, 中文名, 英文名, 源贴图（原版路径，不含 .png）, 是否用顶面变体
BASES = [
    ("yellowed_concrete_floor", "泛黄的混凝土地板", "Yellowed Concrete Floor",
     "block/stone", None),
    ("yellowed_concrete_wall", "泛黄的混凝土墙", "Yellowed Concrete Wall",
     "block/light_gray_concrete", None),
    ("yellowed_corrugated_metal", "泛黄的金属瓦楞板", "Yellowed Corrugated Metal",
     "block/stripped_birch_log", "block/stripped_birch_log_top"),
]

VARIANTS = ("stairs", "slab", "wall", "fence")
SUFFIX_ZH = {"stairs": "楼梯", "slab": "台阶", "wall": "墙", "fence": "栅栏"}
SUFFIX_EN = {"stairs": "Stairs", "slab": "Slab", "wall": "Wall", "fence": "Fence"}


def beige_png(vanilla_path, dst):
    """取原版贴图 -> 灰度 -> 乘米黄，保留明暗层次。"""
    im = Image.open(io.BytesIO(z.read("assets/minecraft/textures/" + vanilla_path + ".png"))).convert("RGBA")
    out = Image.new("RGBA", im.size, (0, 0, 0, 0))
    px = []
    for r, g, b, a in im.getdata():
        if a == 0:
            px.append((0, 0, 0, 0))
            continue
        lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        # 以 0.55 为基准把亮度映射到米黄的明暗区间
        k = 0.45 + lum * 0.75
        px.append((min(255, int(BEIGE[0] * k)),
                   min(255, int(BEIGE[1] * k)),
                   min(255, int(BEIGE[2] * k)), 255))
    out.putdata(px)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    out.save(dst, format="PNG")


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


# ---------- 1. 贴图 ----------
TEX = os.path.join(A, "textures", "block")
for bid, _, _, side_src, top_src in BASES:
    beige_png(side_src, os.path.join(TEX, bid + ".png"))
    if top_src:
        beige_png(top_src, os.path.join(TEX, bid + "_top.png"))
    print("  贴图:", bid + ".png" + (" + _top" if top_src else ""))

# ---------- 2. 基础方块：方块模型 / blockstate / 物品模型 ----------
for bid, zh, en, _, top_src in BASES:
    tex = {"all": "%s:block/%s" % (NS, bid)} if not top_src else {
        "end": "%s:block/%s_top" % (NS, bid),
        "side": "%s:block/%s" % (NS, bid),
        "particle": "%s:block/%s" % (NS, bid),
    }
    if not top_src:
        write_json(os.path.join(A, "models", "block", bid + ".json"),
                   {"parent": "minecraft:block/cube_all", "textures": tex})
    else:
        # 有独立顶面：用 cube_column 结构（方块模型）
        write_json(os.path.join(A, "models", "block", bid + ".json"), {
            "parent": "minecraft:block/block",
            "textures": tex,
            "elements": [{
                "from": [0, 0, 0], "to": [16, 16, 16],
                "faces": {
                    "north": {"uv": [0, 0, 16, 16], "texture": "#side"},
                    "east": {"uv": [0, 0, 16, 16], "texture": "#side"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#side"},
                    "west": {"uv": [0, 0, 16, 16], "texture": "#side"},
                    "up": {"uv": [0, 0, 16, 16], "texture": "#end"},
                    "down": {"uv": [0, 0, 16, 16], "texture": "#end"},
                },
            }],
        })
    write_json(os.path.join(A, "blockstates", bid + ".json"),
               {"variants": {"": {"model": "%s:block/%s" % (NS, bid)}}})
    write_json(os.path.join(A, "models", "item", bid + ".json"),
               {"parent": "%s:block/%s" % (NS, bid)})

# ---------- 3. 变体资源（照抄原版 blockstate，换命名空间）----------
VANILLA_BS = {
    "slab": ("blockstates/oak_slab.json", "minecraft:block/oak_"),
    "stairs": ("blockstates/oak_stairs.json", "minecraft:block/oak_"),
    "wall": ("blockstates/cobblestone_wall.json", "minecraft:block/cobblestone_"),
    "fence": ("blockstates/oak_fence.json", "minecraft:block/oak_"),
}
MODELS = {
    "stairs": [("stairs", "minecraft:block/stairs"), ("stairs_inner", "minecraft:block/inner_stairs"),
               ("stairs_outer", "minecraft:block/outer_stairs")],
    "slab": [("slab", "minecraft:block/slab"), ("slab_top", "minecraft:block/slab_top")],
    "wall": [("wall_post", "minecraft:block/template_wall_post"),
             ("wall_side", "minecraft:block/template_wall_side"),
             ("wall_side_tall", "minecraft:block/template_wall_side_tall")],
    "fence": [("fence_post", "minecraft:block/fence_post"), ("fence_side", "minecraft:block/fence_side")],
}

for bid, _, _, _, top_src in BASES:
    side = "%s:block/%s" % (NS, bid)
    top = ("%s:block/%s_top" % (NS, bid)) if top_src else side
    for v in VARIANTS:
        vid = "%s_%s" % (bid, v)
        src, old_ns = VANILLA_BS[v]
        data = json.loads(z.read("assets/minecraft/" + src).decode("utf-8"))
        text = json.dumps(data, ensure_ascii=False, indent=2).replace(
            old_ns, "%s:block/%s_" % (NS, vid))
        with io.open(os.path.join(A, "blockstates", vid + ".json"), "w",
                     encoding="utf-8", newline="\n") as f:
            f.write(text)

        for suffix, parent in MODELS[v]:
            if v in ("wall",):
                tex = {"wall": side}
            elif v in ("fence",):
                tex = {"texture": side}
            else:
                tex = {"bottom": side, "top": top, "side": side}
            write_json(os.path.join(A, "models", "block", "%s_%s.json" % (vid, suffix)),
                       {"parent": parent, "textures": tex})

        if v == "wall":
            write_json(os.path.join(A, "models", "block", vid + "_inventory.json"),
                       {"parent": "minecraft:block/wall_inventory", "textures": {"wall": side}})
            item = {"parent": "%s:block/%s_inventory" % (NS, vid)}
        elif v == "fence":
            write_json(os.path.join(A, "models", "block", vid + "_inventory.json"),
                       {"parent": "minecraft:block/fence_inventory", "textures": {"texture": side}})
            item = {"parent": "%s:block/%s_inventory" % (NS, vid)}
        else:
            item = {"parent": "%s:block/%s_%s" % (NS, vid, v)}
        write_json(os.path.join(A, "models", "item", vid + ".json"), item)

print("  资源完成：基础 3 个 + 变体 12 个")

# ---------- 4. 语言 ----------
for lang in ("zh_cn", "en_us"):
    p = os.path.join(A, "lang", lang + ".json")
    data = json.loads(io.open(p, encoding="utf-8").read())
    for bid, zh, en, _, _ in BASES:
        data["block.%s.%s" % (NS, bid)] = zh if lang == "zh_cn" else en
        for v in VARIANTS:
            vid = "%s_%s" % (bid, v)
            data["block.%s.%s" % (NS, vid)] = (
                zh + SUFFIX_ZH[v]) if lang == "zh_cn" else (en + " " + SUFFIX_EN[v])
    with io.open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
print("  语言：3 基础 + 12 变体，中英各 15 条")

# ---------- 5. 注册方块与 BlockItem ----------
ALL = [(bid, None) for bid, _, _, _, _ in BASES] + \
      [("%s_%s" % (bid, v), v) for bid, _, _, _, _ in BASES for v in VARIANTS]

mb = os.path.join(JAVA, "ModBlocks.java")
src = io.open(mb, encoding="utf-8").read()
if "YELLOWED_CONCRETE_FLOOR" not in src:
    lines = ["    // ===== 跃金段（Level 1 第三区段）====="]
    for vid, v in ALL:
        up = vid.upper()
        if v is None:
            lines.append('    public static final DeferredBlock<Block> %s =' % up)
            lines.append('            BLOCKS.register("%s", () -> new Block(unbreakable()));' % vid)
        elif v == "stairs":
            base = vid[:-len("_stairs")].upper()
            lines.append('    public static final DeferredBlock<StairBlock> %s =' % up)
            lines.append('            BLOCKS.register("%s", () -> new StairBlock(%s.get().defaultBlockState(), unbreakable()));' % (vid, base))
        elif v == "slab":
            lines.append('    public static final DeferredBlock<SlabBlock> %s =' % up)
            lines.append('            BLOCKS.register("%s", () -> new SlabBlock(unbreakable()));' % vid)
        elif v == "wall":
            lines.append('    public static final DeferredBlock<WallBlock> %s =' % up)
            lines.append('            BLOCKS.register("%s", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));' % vid)
        else:
            lines.append('    public static final DeferredBlock<FenceBlock> %s =' % up)
            lines.append('            BLOCKS.register("%s", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));' % vid)
    anchor = "    private static BlockBehaviour.Properties unbreakable() {"
    src = src.replace(anchor, "\n".join(lines) + "\n\n" + anchor, 1)
    io.open(mb, "w", encoding="utf-8", newline="\n").write(src)
    print("  ModBlocks: 注册 15 个方块")

mi = os.path.join(JAVA, "ModItems.java")
src = io.open(mi, encoding="utf-8").read()
if "YELLOWED_CONCRETE_FLOOR" not in src:
    lines = ["    // ===== 跃金段 BlockItem ====="]
    for vid, _ in ALL:
        up = vid.upper()
        lines.append('    public static final DeferredItem<BlockItem> %s =' % up)
        lines.append('            ITEMS.register("%s", () -> new BlockItem(ModBlocks.%s.get(), new Item.Properties()));' % (vid, up))
    anchor = "    private ModItems() {"
    src = src.replace(anchor, "\n".join(lines) + "\n\n" + anchor, 1)
    io.open(mi, "w", encoding="utf-8", newline="\n").write(src)
    print("  ModItems: 注册 15 个 BlockItem")
