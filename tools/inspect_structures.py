"""解析用户提供的 .nbt 结构文件，输出尺寸 / 方块调色板 / 是否含基岩等信息。"""
import gzip
import os
import struct
from collections import Counter

DIR = r"C:\Users\lin13\Desktop\结构"


class Reader:
    def __init__(self, data):
        self.d = data
        self.p = 0

    def u1(self):
        v = self.d[self.p]
        self.p += 1
        return v

    def i1(self):
        v = struct.unpack_from(">b", self.d, self.p)[0]
        self.p += 1
        return v

    def i2(self):
        v = struct.unpack_from(">h", self.d, self.p)[0]
        self.p += 2
        return v

    def i4(self):
        v = struct.unpack_from(">i", self.d, self.p)[0]
        self.p += 4
        return v

    def i8(self):
        v = struct.unpack_from(">q", self.d, self.p)[0]
        self.p += 8
        return v

    def f4(self):
        v = struct.unpack_from(">f", self.d, self.p)[0]
        self.p += 4
        return v

    def f8(self):
        v = struct.unpack_from(">d", self.d, self.p)[0]
        self.p += 8
        return v

    def s(self):
        n = struct.unpack_from(">H", self.d, self.p)[0]
        self.p += 2
        v = self.d[self.p:self.p + n].decode("utf-8", "replace")
        self.p += n
        return v

    def payload(self, t):
        if t == 1:
            return self.i1()
        if t == 2:
            return self.i2()
        if t == 3:
            return self.i4()
        if t == 4:
            return self.i8()
        if t == 5:
            return self.f4()
        if t == 6:
            return self.f8()
        if t == 7:
            n = self.i4()
            v = list(self.d[self.p:self.p + n])
            self.p += n
            return v
        if t == 8:
            return self.s()
        if t == 9:
            et = self.u1()
            n = self.i4()
            return [self.payload(et) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = self.u1()
                if tt == 0:
                    break
                name = self.s()
                d[name] = self.payload(tt)
            return d
        if t == 11:
            n = self.i4()
            return [self.i4() for _ in range(n)]
        if t == 12:
            n = self.i4()
            return [self.i8() for _ in range(n)]
        raise ValueError("unknown tag %d at %d" % (t, self.p))

    def root(self):
        t = self.u1()
        assert t == 10, "root is not compound: %d" % t
        self.s()
        return self.payload(10)


for name in sorted(os.listdir(DIR)):
    if not name.endswith(".nbt"):
        continue
    path = os.path.join(DIR, name)
    with open(path, "rb") as f:
        raw = f.read()
    try:
        data = gzip.decompress(raw)
    except OSError:
        data = raw
    nbt = Reader(data).root()

    size = nbt.get("size")
    palette = nbt.get("palette", [])
    blocks = nbt.get("blocks", [])
    print("=" * 70)
    print("%s   DataVersion=%s" % (name, nbt.get("DataVersion")))
    print("  size (x,y,z) = %s   方块数 = %d   调色板条目 = %d   entities = %d"
          % (size, len(blocks), len(palette), len(nbt.get("entities", []))))
    used = Counter(b.get("state", 0) for b in blocks)
    print("  --- 调色板（按用量） ---")
    for idx, cnt in used.most_common():
        entry = palette[idx] if idx < len(palette) else {}
        props = entry.get("Properties")
        pstr = ""
        if props:
            pstr = " " + ",".join("%s=%s" % (k, v) for k, v in sorted(props.items()))
        print("    %-52s x%-5d%s" % (entry.get("Name", "?"), cnt, pstr))
    # 每层（Y）的方块数分布
    if size and len(size) == 3:
        per_y = Counter(b["pos"][1] for b in blocks)
        print("  --- 每 Y 层方块数 ---")
        for y in range(size[1]):
            print("    y=%-3d : %d" % (y, per_y.get(y, 0)))
