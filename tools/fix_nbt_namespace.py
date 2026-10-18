"""把模组内 .nbt 结构副本里的旧命名空间 backrooms_level0: 更新为 backrooms_complex:。

只改 NBT 字符串里的方块 ID 命名空间（字符串自带 2 字节大端长度前缀，一并修正），
几何、方块种类、方块属性、方块实体 NBT 全部保持不变。
用户桌面上的原始文件不动。
"""
import gzip
import os

DIR = r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.0\src\main\resources\data\backrooms_complex\structure"
OLD = b"backrooms_level0:"
NEW = b"backrooms_complex:"

for name in sorted(os.listdir(DIR)):
    if not name.endswith(".nbt"):
        continue
    path = os.path.join(DIR, name)
    raw = open(path, "rb").read()
    gz = True
    try:
        data = gzip.decompress(raw)
    except OSError:
        gz = False
        data = raw

    out = bytearray()
    i = 0
    patched = 0
    n = len(data)
    while i < n:
        # 在当前位置往后找 OLD 的出现
        j = data.find(OLD, i)
        if j < 0:
            out += data[i:]
            break
        # 该字符串的长度前缀紧邻其前 2 字节
        if j < 2:
            out += data[i:j + len(OLD)]
            i = j + len(OLD)
            continue
        length = int.from_bytes(data[j - 2:j], "big")
        full_old = data[j:j + length]
        # 校验：长度前缀确实描述了以 OLD 开头的这个字符串
        if length < len(OLD) or not full_old.startswith(OLD) or j + length > n:
            out += data[i:j + len(OLD)]
            i = j + len(OLD)
            continue
        full_new = NEW + full_old[len(OLD):]
        out += data[i:j - 2]                       # 前缀之前的内容
        out += len(full_new).to_bytes(2, "big")    # 新的长度前缀
        out += full_new                            # 新的方块 ID
        i = j + length
        patched += 1

    new_raw = gzip.compress(bytes(out), mtime=0) if gz else bytes(out)
    open(path, "wb").write(new_raw)
    print("%-18s 修正方块 ID %d 处，%d -> %d 字节" % (name, patched, len(raw), len(new_raw)))

# 校验：不再残留旧命名空间
print()
left = 0
for name in sorted(os.listdir(DIR)):
    if not name.endswith(".nbt"):
        continue
    data = gzip.decompress(open(os.path.join(DIR, name), "rb").read())
    c = data.count(b"backrooms_level0")
    left += c
    print("%-18s 残留 backrooms_level0 次数 = %d" % (name, c))
print("\n合计残留 = %d（应为 0）" % left)
