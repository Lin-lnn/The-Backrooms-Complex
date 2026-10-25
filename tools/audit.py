# -*- coding: utf-8 -*-
"""一次性静态审计（放进仓库，随时可跑）。

用法:
    python tools/audit.py

输出:
  1) 未使用 import / 未引用私有方法 / 未引用私有字段
  2) ModBalance 常量是否全部接线
  3) ★ 跨层级干涉：level0 与 level1 是否互相引用
  4) 外部代码是否越界依赖某一层的内部常量
  5) 连续 Javadoc（删代码留下的孤儿注释）
  6) 规模总览

设计要点（为什么这样"读代码"更快）：
  只把每个文件读一次进内存（S 字典），之后所有检查都在内存里做，
  不再重复 I/O，也不依赖反复 grep。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, "src", "main", "java")
PKG = os.path.join(JAVA, "com", "the_backrooms_complex")

L0 = "worldgen.level0"
L1 = "worldgen.level1"


def load():
    """一次读完，返回 {路径: 内容}。"""
    files = []
    for root, _, fs in os.walk(JAVA):
        for f in fs:
            if f.endswith(".java"):
                files.append(os.path.join(root, f))
    src = {}
    for f in files:
        with io.open(f, encoding="utf-8") as fh:
            src[f] = fh.read()
    return src


def rel(p):
    return os.path.relpath(p, JAVA).replace("\\", "/")


def package_of(text):
    m = re.search(r'^package\s+([\w.]+);', text, re.M)
    return m.group(1) if m else ""


def main():
    S = load()
    ok = True

    # ---------- 1. 死代码 ----------
    print("=" * 62)
    print("1. 未使用 import / 死方法 / 死字段")
    n = 0
    for p, t in S.items():
        body = re.sub(r'^import .*$', '', t, flags=re.M)
        for m in re.finditer(r'^import (?:static )?([\w.]+);', t, re.M):
            simple = m.group(1).split(".")[-1]
            if simple != "*" and not re.search(r'\b' + re.escape(simple) + r'\b', body):
                print("   [未用import] %-46s %s" % (rel(p), m.group(1)))
                n += 1
        cls = os.path.basename(p)[:-5]
        for i, ln in enumerate(t.split("\n")):
            m = re.search(r'^\s+private (?:static )?(?:final )?[\w<>\[\], .?]+\s+(\w+)\s*\(', ln)
            if m and m.group(1) != cls and len(re.findall(r'\b' + m.group(1) + r'\b', t)) <= 1:
                print("   [死方法]     %-46s %s (第 %d 行)" % (rel(p), m.group(1), i + 1))
                n += 1
            m2 = re.search(r'^\s+private (?:static )?(?:final )?[\w<>\[\], .?]+\s+(\w+)\s*(?:=|;)', ln)
            if m2 and len(re.findall(r'\b' + m2.group(1) + r'\b', t)) <= 1:
                print("   [死字段]     %-46s %s (第 %d 行)" % (rel(p), m2.group(1), i + 1))
                n += 1
    print("   合计 %d %s" % (n, "OK" if n == 0 else ""))
    ok &= n == 0

    # ---------- 2. ModBalance ----------
    print("=" * 62)
    print("2. ModBalance 常量接线情况")
    mb = os.path.join(PKG, "ModBalance.java")
    consts = re.findall(r'public static final \w+ (\w+)\s*=', S.get(mb, ""))
    unused = [c for c in consts
              if sum(len(re.findall(r'\b' + c + r'\b', v)) for v in S.values()) <= 1]
    print("   共 %d 个，未接线 %d 个 %s" % (len(consts), len(unused), "OK" if not unused else unused))
    ok &= not unused

    # ---------- 3. 跨层级干涉 ----------
    print("=" * 62)
    print("3. 跨层级干涉检查（%s <-> %s）" % (L0, L1))
    bad = 0
    for p, t in S.items():
        pkg = package_of(t)
        if L0 in pkg:
            for m in re.finditer(r'\b' + L1.replace(".", r"\.") + r'\.(\w+)', t):
                print("   [L0 -> L1] %-44s 引用 %s" % (rel(p), m.group(1)))
                bad += 1
            if "Level1" in t:
                print("   [L0 -> L1] %-44s 出现 'Level1'" % rel(p))
                bad += 1
        if L1 in pkg:
            for m in re.finditer(r'\b' + L0.replace(".", r"\.") + r'\.(\w+)', t):
                print("   [L1 -> L0] %-44s 引用 %s" % (rel(p), m.group(1)))
                bad += 1
            if "BackroomsChunkGenerator" in t or "BackroomsMaze" in t or "Topology" in t:
                print("   [L1 -> L0] %-44s 出现 L0 类名" % rel(p))
                bad += 1
    # 群系 key 是否串层
    l0gen = S.get(os.path.join(PKG, "worldgen", "level0", "BackroomsChunkGenerator.java"), "")
    l1gen = S.get(os.path.join(PKG, "worldgen", "level1", "Level1ChunkGenerator.java"), "")
    for key in ("aquila_sector", "warehouse_aisles", "puddle_zone"):
        if key in l0gen:
            print("   [L0 含 L1 群系] %s" % key)
            bad += 1
    for key in ("yellow_rooms", "concrete_halls", "damp_corridors"):
        if key in l1gen:
            print("   [L1 含 L0 群系] %s" % key)
            bad += 1
    print("   合计 %d %s" % (bad, "OK" if bad == 0 else ""))
    ok &= bad == 0

    # ---------- 4. 外部对某一层内部常量的依赖 ----------
    print("=" * 62)
    print("4. 层级外代码是否依赖某层的内部常量")
    bad = 0
    for p, t in S.items():
        pkg = package_of(t)
        if L0 in pkg or L1 in pkg:
            continue
        for m in re.finditer(r'(?:worldgen\.)?level[01]\.(\w+)\.(\w+)', t):
            # 注册点与事件派发必须能看见各层，属设计允许
            allowed = (rel(p) == "com/the_backrooms_complex/BackroomsComplex.java"
                       or rel(p) == "com/the_backrooms_complex/ModEvents.java")
            if allowed:
                print("   [允许] %-46s -> %s.%s（注册/派发）" % (rel(p), m.group(1), m.group(2)))
                continue
            print("   [越界] %-46s -> %s.%s" % (rel(p), m.group(1), m.group(2)))
            bad += 1
    print("   合计 %d %s" % (bad, "OK" if bad == 0 else "（应改为引用共享包）"))
    ok &= bad == 0

    # ---------- 5. 孤儿 Javadoc ----------
    print("=" * 62)
    print("5. 连续 Javadoc（代码被删后留下的孤儿注释）")
    n = 0
    for p, t in S.items():
        lines = t.split("\n")
        for i in range(len(lines) - 1):
            a, b = lines[i].strip(), lines[i + 1].strip()
            if a.endswith("*/") and b.startswith("/**"):
                print("   %-46s 第 %d/%d 行" % (rel(p), i + 1, i + 2))
                n += 1
    print("   合计 %d %s" % (n, "OK" if n == 0 else ""))

    # ---------- 6. 规模 ----------
    print("=" * 62)
    print("6. 规模总览（worldgen 与最大的几个类）")
    rows = sorted(((len(t.split("\n")), rel(p)) for p, t in S.items()), reverse=True)
    print("   总文件 %d 个，总行数 %d" % (len(rows), sum(r[0] for r in rows)))
    print("   最大的 8 个:")
    for cnt, name in rows[:8]:
        print("      %-56s %4d 行" % (name, cnt))
    wg = [(c, n) for c, n in rows if "worldgen/" in n]
    print("   worldgen 合计 %d 行" % sum(c for c, _ in wg))

    print("=" * 62)
    print("结论: %s" % ("全部通过" if ok else "存在问题，见上"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
