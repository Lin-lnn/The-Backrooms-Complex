# -*- coding: utf-8 -*-
"""资源与常量自检（不启动游戏，随时可跑）。

用法:
    python tools/selfcheck.py

检查:
  1) src/main/resources 下所有 JSON 能否解析
  2) 维度 JSON 的 layer_height 与生成器常量一致（L0 / L1 各一组）
  3) L1 的子层常量自洽（SUB_H * SUB_COUNT == LAYER_H）
  4) blockstate / model 引用的模型与贴图存在（只校验本模组命名空间）
  5) en_us 与 zh_cn 的语言键一致

设计原则:
  - 只读，不生成、不修改任何文件；
  - 只校验本模组命名空间，原版引用（minecraft:）交给游戏；
  - 不依赖 Gradle / Minecraft，纯标准库，CI 与本地都能跑。
"""
import io
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "the_backrooms_complex")
NS = "backrooms_complex"
ASSETS = os.path.join(RES, "assets", NS)
DATA = os.path.join(RES, "data", NS)

problems = []
checks = 0


def fail(msg):
    problems.append(msg)


def read_text(path):
    with io.open(path, encoding="utf-8") as fh:
        return fh.read()


def load_json(path):
    try:
        return json.loads(read_text(path))
    except Exception as exc:                      # noqa: BLE001 - 报告原始错误更重要
        fail("JSON 解析失败 %s: %s" % (os.path.relpath(path, ROOT), exc))
        return None


def java_const(path, name):
    text = read_text(path)
    m = re.search(r"\b%s\s*=\s*(\d+)\s*;" % re.escape(name), text)
    if not m:
        fail("找不到常量 %s（%s）" % (name, os.path.relpath(path, ROOT)))
        return None
    return int(m.group(1))


def check_json_files():
    global checks
    for base, _, files in os.walk(RES):
        for name in files:
            if name.endswith(".json"):
                checks += 1
                load_json(os.path.join(base, name))


def check_layer_heights():
    global checks
    l1 = os.path.join(JAVA, "worldgen", "level1", "Level1ChunkGenerator.java")
    l0 = os.path.join(JAVA, "worldgen", "level0", "BackroomsChunkGenerator.java")
    biome_source = os.path.join(JAVA, "worldgen", "BackroomsBiomeSource.java")
    l1_json = os.path.join(DATA, "dimension", "level_1.json")
    l0_json = os.path.join(DATA, "dimension", "level_0.json")

    default_layer_h = java_const(biome_source, "DEFAULT_LAYER_HEIGHT")
    layer_h = java_const(l1, "LAYER_H")
    sub_h = java_const(l1, "SUB_H")
    sub_count = java_const(l1, "SUB_COUNT")
    if None not in (layer_h, sub_h, sub_count):
        checks += 1
        if sub_h * sub_count != layer_h:
            fail("L1 常量不自洽：SUB_H(%d) * SUB_COUNT(%d) != LAYER_H(%d)"
                 % (sub_h, sub_count, layer_h))
        checks += 1
        if layer_h < 2 * sub_h:
            fail("L1 只有 %d 格高，放不下两个 %d 格子层" % (layer_h, sub_h))

    for zjson, const, label in (
            (l1_json, layer_h, "L1"),
            (l0_json, java_const(l0, "LAYER_HEIGHT"), "L0")):
        if const is None:
            continue
        data = load_json(zjson)
        if data is None:
            continue
        checks += 1
        actual = data.get("generator", {}).get("biome_source", {}).get("layer_height")
        # 字段省略时 codec 使用 DEFAULT_LAYER_HEIGHT（Level 0 就是这样）
        effective = default_layer_h if actual is None else actual
        if effective != const:
            fail("%s 的维度 JSON layer_height=%r，生成器常量=%d（雾色会与楼层错开）"
                 % (label, actual, const))


def resolve(namespace, path, kind):
    if namespace != NS:
        return None
    if kind == "model":
        return os.path.join(ASSETS, "models", path + ".json")
    return os.path.join(ASSETS, "textures", path + ".png")


def split_ref(ref):
    if ":" in ref:
        ns, path = ref.split(":", 1)
    else:
        # 原版 JSON 的裸路径默认属于 minecraft，这里不校验
        return None, ref
    return ns, path


def check_reference(ref, kind, where):
    global checks
    ns, path = split_ref(ref)
    if ns != NS:
        return
    checks += 1
    target = resolve(ns, path, kind)
    if target is None or not os.path.isfile(target):
        fail("%s 引用了不存在的 %s: %s" % (where, kind, ref))


def check_model_file(path):
    data = load_json(path)
    if data is None:
        return
    where = os.path.relpath(path, ROOT).replace("\\", "/")
    parent = data.get("parent")
    if isinstance(parent, str):
        check_reference(parent, "model", where + " 的 parent")
    textures = data.get("textures", {})
    if isinstance(textures, dict):
        for key, value in textures.items():
            if isinstance(value, str) and not value.startswith("#"):
                check_reference(value, "texture", "%s 的 textures.%s" % (where, key))


def check_assets():
    global checks
    blockstates = os.path.join(ASSETS, "blockstates")
    for base, _, files in os.walk(blockstates):
        for name in files:
            if not name.endswith(".json"):
                continue
            path = os.path.join(base, name)
            data = load_json(path)
            if data is None:
                continue
            where = os.path.relpath(path, ROOT).replace("\\", "/")
            variants = data.get("variants", {})
            if isinstance(variants, dict):
                for key, value in variants.items():
                    entries = value if isinstance(value, list) else [value]
                    for entry in entries:
                        if isinstance(entry, dict) and isinstance(entry.get("model"), str):
                            check_reference(entry["model"], "model",
                                            "%s 的 variants[%s]" % (where, key))
            multipart = data.get("multipart", [])
            for entry in multipart:
                apply = entry.get("apply", {}) if isinstance(entry, dict) else {}
                entries = apply if isinstance(apply, list) else [apply]
                for item in entries:
                    if isinstance(item, dict) and isinstance(item.get("model"), str):
                        check_reference(item["model"], "model", where + " 的 multipart")

    for folder in ("block", "item", "armor"):
        base = os.path.join(ASSETS, "models", folder)
        if not os.path.isdir(base):
            continue
        for name in os.listdir(base):
            if name.endswith(".json"):
                check_model_file(os.path.join(base, name))


def check_lang():
    global checks
    paths = {
        "en_us": os.path.join(ASSETS, "lang", "en_us.json"),
        "zh_cn": os.path.join(ASSETS, "lang", "zh_cn.json"),
    }
    tables = {}
    for key, path in paths.items():
        if not os.path.isfile(path):
            fail("缺少语言文件 %s" % os.path.relpath(path, ROOT))
            return
        data = load_json(path)
        if isinstance(data, dict):
            tables[key] = data
    if len(tables) != 2:
        return
    checks += 1
    only_en = sorted(set(tables["en_us"]) - set(tables["zh_cn"]))
    only_zh = sorted(set(tables["zh_cn"]) - set(tables["en_us"]))
    if only_en:
        fail("zh_cn 缺少 %d 个键，例如 %s" % (len(only_en), ", ".join(only_en[:5])))
    if only_zh:
        fail("en_us 缺少 %d 个键，例如 %s" % (len(only_zh), ", ".join(only_zh[:5])))


def main():
    check_json_files()
    check_layer_heights()
    check_assets()
    check_lang()

    print("=" * 62)
    print("自检完成：%d 项检查，%d 个问题" % (checks, len(problems)))
    for msg in problems:
        print("  [问题] " + msg)
    print("=" * 62)
    print("结论: %s" % ("全部通过" if not problems else "存在问题，见上"))
    return 0 if not problems else 1


if __name__ == "__main__":
    sys.exit(main())
