"""按 Y 层打印 .nbt 结构的方块布局（ASCII），用于判断结构本体的形状。"""
import gzip
import os
import struct

DIR = r"C:\Users\lin13\Desktop\结构"

CODE = {
    "minecraft:air": ".",
    # 新命名空间（模组内已修正的副本）
    "backrooms_complex:floor_tile": "F",
    "backrooms_complex:ceiling_tile": "C",
    "backrooms_complex:wall_yellow": "W",
    "backrooms_complex:wall_yellow_plinth": "p",
    "backrooms_complex:fluorescent_lamp": "L",
    # 旧命名空间（桌面上的原始文件仍使用，便于对照排查）
    "backrooms_level0:floor_tile": "F",
    "backrooms_level0:ceiling_tile": "C",
    "backrooms_level0:wall_yellow": "W",
    "backrooms_level0:wall_yellow_plinth": "p",
    "backrooms_level0:fluorescent_lamp": "L",
    "minecraft:bamboo_mosaic": "b",
    "minecraft:bamboo_mosaic_stairs": "s",
    "minecraft:bamboo_slab": "l",
    "minecraft:oak_log": "O",
    "minecraft:oak_leaves": "T",
    "minecraft:lectern": "R",
    "minecraft:potted_lily_of_the_valley": "v",
    "minecraft:potted_oxeye_daisy": "d",
    "minecraft:potted_azalea_bush": "a",
    "minecraft:potted_allium": "u",
}


class R:
    def __init__(s, d):
        s.d = d
        s.p = 0

    def u1(s):
        v = s.d[s.p]
        s.p += 1
        return v

    def i2(s):
        v = struct.unpack_from(">h", s.d, s.p)[0]
        s.p += 2
        return v

    def i4(s):
        v = struct.unpack_from(">i", s.d, s.p)[0]
        s.p += 4
        return v

    def i8(s):
        v = struct.unpack_from(">q", s.d, s.p)[0]
        s.p += 8
        return v

    def f4(s):
        v = struct.unpack_from(">f", s.d, s.p)[0]
        s.p += 4
        return v

    def f8(s):
        v = struct.unpack_from(">d", s.d, s.p)[0]
        s.p += 8
        return v

    def st(s):
        n = struct.unpack_from(">H", s.d, s.p)[0]
        s.p += 2
        v = s.d[s.p:s.p + n].decode("utf-8", "replace")
        s.p += n
        return v

    def pl(s, t):
        if t == 1:
            return struct.unpack(">b", bytes([s.u1()]))[0]
        if t == 2:
            return s.i2()
        if t == 3:
            return s.i4()
        if t == 4:
            return s.i8()
        if t == 5:
            return s.f4()
        if t == 6:
            return s.f8()
        if t == 7:
            n = s.i4()
            v = list(s.d[s.p:s.p + n])
            s.p += n
            return v
        if t == 8:
            return s.st()
        if t == 9:
            et = s.u1()
            n = s.i4()
            return [s.pl(et) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = s.u1()
                if tt == 0:
                    break
                k = s.st()
                d[k] = s.pl(tt)
            return d
        if t == 11:
            n = s.i4()
            return [s.i4() for _ in range(n)]
        if t == 12:
            n = s.i4()
            return [s.i8() for _ in range(n)]
        raise ValueError(t)


def load(name):
    raw = open(os.path.join(DIR, name), "rb").read()
    try:
        raw = gzip.decompress(raw)
    except OSError:
        pass
    r = R(raw)
    assert r.u1() == 10
    r.st()
    return r.pl(10)


for name in ["level0_1.nbt", "level0_3.nbt"]:
    n = load(name)
    pal = n["palette"]
    blocks = n["blocks"]
    sx, sy, sz = n["size"]
    grid = [[["?" for _ in range(sx)] for _ in range(sz)] for _ in range(sy)]
    for b in blocks:
        x, y, z = b["pos"]
        nm = pal[b["state"]]["Name"]
        grid[y][z][x] = CODE.get(nm, "#")
    print("=" * 72)
    print("%s  size=%dx%dx%d  blocks=%d" % (name, sx, sy, sz, len(blocks)))
    print("图例: .=空气 F=地板 C=天花板 W=墙 p=踢脚线 L=荧光灯 b=竹马赛克 s=竹楼梯 O=橡木 T=树叶 R=讲台 v/d/a/u=盆栽")
    for y in range(sy):
        print("--- y=%d ---" % y)
        for z in range(sz):
            print("   " + "".join(grid[y][z][x] for x in range(sx)))
