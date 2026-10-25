# -*- coding: utf-8 -*-
"""让 32 个变体（stairs/slab/wall/fence × 8 基底）也吃 BiomeTint 染色。

问题：变体模型继承原版父模型（slab/stairs/wall_post/...），这些父模型的面
      **没有 tintindex**，所以染色不生效，灰度贴图会呈现为灰白。

做法：
  1. 从客户端 jar 里读出这些父模型，沿 parent 链找到真正的 elements；
  2. 给每个面加上 `tintindex: 0`，连同 textures 的占位键一起写成
     `backrooms_complex:block/tinted_<name>`；
  3. 把所有变体模型改为继承这些「已着色的父模型」；
  4. 把 32 个变体方块加进 BiomeTint 的染色列表。
"""
import io
import json
import os
import re
import zipfile

ROOT = r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.1"
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "the_backrooms_complex")
A = os.path.join(ROOT, "src", "main", "resources", "assets", "backrooms_complex")
MB = os.path.join(A, "models", "block")
JAR = r"C:\Users\lin13\.mcreator\gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar"
NS = "backrooms_complex"
z = zipfile.ZipFile(JAR)

BASES = ["concrete_pillar", "concrete_warning_pillar", "concrete_floor",
         "concrete_ceiling", "white_plastered_wall",
         "yellowed_concrete_floor", "yellowed_concrete_wall", "yellowed_corrugated_metal"]

# 需要「已着色父模型」的原版模型名
PARENTS = ["slab", "slab_top", "stairs", "inner_stairs", "outer_stairs",
           "template_wall_post", "template_wall_side", "template_wall_side_tall",
           "wall_inventory", "fence_post", "fence_side", "fence_inventory"]


def load_vanilla(name):
    raw = z.read("assets/minecraft/models/block/" + name + ".json").decode("utf-8")
    return json.loads(raw)


def resolve(name, depth=0):
    """沿 parent 链找到含 elements 的模型，返回 (elements, textures)。"""
    if depth > 6:
        raise RuntimeError("parent 链过深: " + name)
    m = load_vanilla(name)
    if "elements" in m:
        return m["elements"], m.get("textures", {})
    parent = m.get("parent")
    if not parent:
        raise RuntimeError("模型无 elements 也无 parent: " + name)
    if parent.startswith("minecraft:"):
        parent = parent[len("minecraft:"):]
    if parent.startswith("block/"):
        parent = parent[len("block/"):]
    elem, tex = resolve(parent, depth + 1)
    # 子模型的 textures 覆盖父模型
    merged = dict(tex)
    merged.update(m.get("textures", {}))
    return elem, merged


made_parents = 0
for name in PARENTS:
    elem, tex = resolve(name)
    # 给每个面加 tintindex
    for el in elem:
        for face in el.get("faces", {}).values():
            face["tintindex"] = 0
    out = {"parent": "minecraft:block/block",
           "textures": tex,
           "elements": elem}
    with io.open(os.path.join(MB, "tinted_" + name + ".json"), "w",
                 encoding="utf-8", newline="\n") as f:
        json.dump(out, f, ensure_ascii=False, indent=2)
    made_parents += 1
print("已着色父模型:", made_parents, "个")

# ---------- 2. 变体模型改为继承已着色父模型 ----------
SUFFIX_MAP = {
    "_slab": "slab", "_slab_top": "slab_top",
    "_stairs": "stairs", "_stairs_inner": "inner_stairs", "_stairs_outer": "outer_stairs",
    "_wall_post": "template_wall_post", "_wall_side": "template_wall_side",
    "_wall_side_tall": "template_wall_side_tall", "_wall_inventory": "wall_inventory",
    "_fence_post": "fence_post", "_fence_side": "fence_side", "_fence_inventory": "fence_inventory",
}

changed = 0
for base in BASES:
    for suffix, target in SUFFIX_MAP.items():
        p = os.path.join(MB, "%s_%s.json" % (base, suffix.lstrip("_")))
        if not os.path.exists(p):
            continue
        data = json.loads(io.open(p, encoding="utf-8").read())
        data["parent"] = "%s:block/tinted_%s" % (NS, target)
        with io.open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        changed += 1
print("变体模型改为着色父模型:", changed, "个")

# ---------- 3. BiomeTint 列表补 32 个变体 ----------
p = os.path.join(JAVA, "client", "BiomeTint.java")
src = io.open(p, encoding="utf-8").read()
if "CONCRETE_PILLAR_STAIRS" not in src:
    entries = []
    for base in BASES:
        for v in ("stairs", "slab", "wall", "fence"):
            entries.append("                ModBlocks.%s_%s," % (base.upper(), v.upper()))
    anchor = "                ModBlocks.YELLOWED_CORRUGATED_METAL,\n"
    if anchor not in src:
        raise SystemExit("BiomeTint 锚点未找到")
    src = src.replace(anchor, anchor + "\n".join(entries) + "\n", 1)
    io.open(p, "w", encoding="utf-8", newline="\n").write(src)
    print("BiomeTint: 已追加 32 个变体")
else:
    print("BiomeTint: 变体已存在，跳过")
