# -*- coding: utf-8 -*-
"""让 Level 1 的方块接入 BiomeTint（灰度贴图 + 代码染色）。

步骤：
  1. 为 8 个基础方块生成灰度贴图 <id>_tint.png
  2. 把这 8 个方块的模型贴图指向 _tint
  3. 在 BiomeTint 里注册这 8 个方块，并按「区段」取色（区段不同颜色不同）
"""
import io
import json
import os
import re
import zipfile
from PIL import Image

ROOT = r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.1"
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "the_backrooms_complex")
A = os.path.join(ROOT, "src", "main", "resources", "assets", "backrooms_complex")
TEX = os.path.join(A, "textures", "block")
JAR = r"C:\Users\lin13\.mcreator\gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar"
NS = "backrooms_complex"
z = zipfile.ZipFile(JAR)

# id -> (源, 是否灰度化)；源为 ("vanilla", 路径) 或 ("mod", 路径)
BASES = {
    "concrete_pillar":         ("vanilla", "block/light_gray_concrete"),
    "concrete_warning_pillar": ("vanilla_side_top", "block/reinforced_deepslate_side", "block/reinforced_deepslate_top"),
    "concrete_floor":          ("vanilla", "block/stone"),
    "concrete_ceiling":        ("vanilla", "block/smooth_stone"),
    "white_plastered_wall":    ("vanilla_side_top", "block/stripped_birch_log", "block/stripped_birch_log_top"),
    "yellowed_concrete_floor": ("mod", "yellowed_concrete_floor"),
    "yellowed_concrete_wall":  ("mod", "yellowed_concrete_wall"),
    "yellowed_corrugated_metal": ("mod_side_top", "yellowed_corrugated_metal", "yellowed_corrugated_metal_top"),
}


def to_gray(im):
    out = Image.new("RGBA", im.size, (0, 0, 0, 0))
    px = []
    for r, g, b, a in im.getdata():
        if a == 0:
            px.append((0, 0, 0, 0))
            continue
        v = int(0.299 * r + 0.587 * g + 0.114 * b)
        px.append((v, v, v, 255))
    out.putdata(px)
    return out


def gray_from_vanilla(path, dst):
    im = Image.open(io.BytesIO(z.read("assets/minecraft/textures/" + path + ".png"))).convert("RGBA")
    to_gray(im).save(dst, format="PNG")


def gray_from_mod(name, dst):
    im = Image.open(os.path.join(TEX, name + ".png")).convert("RGBA")
    to_gray(im).save(dst, format="PNG")


# ---------- 1. 灰度贴图 ----------
made = []
for bid, spec in BASES.items():
    kind = spec[0]
    if kind == "vanilla":
        gray_from_vanilla(spec[1], os.path.join(TEX, bid + "_tint.png"))
        made.append(bid + "_tint.png")
    elif kind == "mod":
        gray_from_mod(spec[1], os.path.join(TEX, bid + "_tint.png"))
        made.append(bid + "_tint.png")
    else:
        if kind.startswith("mod"):
            gray_from_mod(spec[1], os.path.join(TEX, bid + "_tint.png"))
            gray_from_mod(spec[2], os.path.join(TEX, bid + "_tint_top.png"))
        else:
            gray_from_vanilla(spec[1], os.path.join(TEX, bid + "_tint.png"))
            gray_from_vanilla(spec[2], os.path.join(TEX, bid + "_tint_top.png"))
        made += [bid + "_tint.png", bid + "_tint_top.png"]
print("灰度贴图:", len(made), "个")

# ---------- 2. 模型指向 _tint ----------
# 已有独立顶面的：cube_column 结构，改 end/side
HAS_TOP = {"concrete_warning_pillar", "white_plastered_wall", "yellowed_corrugated_metal"}
for bid in BASES:
    p = os.path.join(A, "models", "block", bid + ".json")
    if not os.path.exists(p):
        print("  跳过（模型不存在）:", bid)
        continue
    data = json.loads(io.open(p, encoding="utf-8").read())
    tex = data.get("textures") or {}
    if bid in HAS_TOP:
        tex = {"end": "%s:block/%s_tint_top" % (NS, bid),
               "side": "%s:block/%s_tint" % (NS, bid),
               "particle": "%s:block/%s_tint" % (NS, bid)}
    else:
        tex = {"all": "%s:block/%s_tint" % (NS, bid)}
    data["textures"] = tex
    # cube_all 自带 tintindex；cube_column 的自建模型需要显式加
    if "elements" in data:
        for el in data["elements"]:
            for face in el.get("faces", {}).values():
                face["tintindex"] = 0
    with io.open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
print("模型已指向灰度贴图:", len(BASES), "个")

# ---------- 3. BiomeTint 注册 + 区段取色 ----------
p = os.path.join(JAVA, "client", "BiomeTint.java")
src = io.open(p, encoding="utf-8").read()

if "yellowed_concrete_floor" not in src:
    # 3a 方块列表：加到 return out; 之前
    anchor = "        Block[] out = new Block[holders.length];"
    add = '''                // Level 1 的 8 个基础方块（灰度贴图 + 区段染色）
                ModBlocks.CONCRETE_PILLAR, ModBlocks.CONCRETE_WARNING_PILLAR,
                ModBlocks.CONCRETE_FLOOR, ModBlocks.CONCRETE_CEILING,
                ModBlocks.WHITE_PLASTERED_WALL,
                ModBlocks.YELLOWED_CONCRETE_FLOOR, ModBlocks.YELLOWED_CONCRETE_WALL,
                ModBlocks.YELLOWED_CORRUGATED_METAL,
'''
    src = src.replace("        };\n" + anchor, "\n".join([add]) + "        };\n" + anchor, 1)

    # 3b 区段取色：插到 getBlockColor 开头（马尼拉判断之后）
    marker = "        int layerBaseY = "
    region_block = '''        // Level 1 的方块：按「区段」取色（维度判定 + 区段色）
        if (isLevel1Block(state.getBlock()) && level != null && pos != null) {
            int sectorHue = sectorHue(pos);
            if (sectorHue != 0) {
                return sectorHue;
            }
        }
'''
    src = src.replace(marker, region_block + marker, 1)

    # 3c 两个辅助方法
    helper = '''
    /** 是否是参与染色的 Level 1 方块。 */
    private static boolean isLevel1Block(Block b) {
        return b == ModBlocks.CONCRETE_PILLAR.get()
                || b == ModBlocks.CONCRETE_WARNING_PILLAR.get()
                || b == ModBlocks.CONCRETE_FLOOR.get()
                || b == ModBlocks.CONCRETE_CEILING.get()
                || b == ModBlocks.WHITE_PLASTERED_WALL.get()
                || b == ModBlocks.YELLOWED_CONCRETE_FLOOR.get()
                || b == ModBlocks.YELLOWED_CONCRETE_WALL.get()
                || b == ModBlocks.YELLOWED_CORRUGATED_METAL.get();
    }

    /** 按 Level 1 的区段返回代表色；0 表示「不加区段色，交给群系着色」。 */
    private static int sectorHue(BlockPos pos) {
        var region = com.the_backrooms_complex.worldgen.level1.Level1ChunkGenerator
                .regionAt(pos.getX(), pos.getZ());
        return switch (region) {
            case YUEJIN -> 0xC9B36A;      // 泛黄米金
            case GOTHIC -> 0x8A8A85;      // 冷灰
            case GARDEN -> 0x6E9A4E;      // 苔绿
            case LEGEND -> 0x7A5B3E;      // 旧木棕
            case OUROBOROS -> 0x9A8F80;   // 毛坯灰褐
            default -> 0;                 // 天鹰 / 走廊：交给群系
        };
    }
'''
    anchor2 = "    private static Block[] tintedBlocks()"
    src = src.replace(anchor2, helper + "\n" + anchor2, 1)

    io.open(p, "w", encoding="utf-8", newline="\n").write(src)
    print("BiomeTint: 已注册 8 个 L1 方块 + 区段取色")
else:
    print("BiomeTint: 已存在，跳过")
