# -*- coding: utf-8 -*-
import os, json, io, zipfile
from PIL import Image

ROOT = r"C:\Users\lin13\Desktop\模型"
A = r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.0\src\main\resources\assets\backrooms_complex"
JAR = r"C:\Users\lin13\.mcreator\gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar"
z = zipfile.ZipFile(JAR)

# 木材名 -> 原版贴图；按 key 长度倒序匹配，避免 "橡木" 抢走 "深色橡木"
WOOD = [
    ("深色橡木", "dark_oak_planks"), ("金合欢木", "acacia_planks"), ("白桦木", "birch_planks"),
    ("丛林木", "jungle_planks"), ("云杉木", "spruce_planks"), ("樱花木", "cherry_planks"),
    ("绯红木", "crimson_planks"), ("诡异木", "warped_planks"), ("红树木", "mangrove_planks"),
    ("红木", "mangrove_planks"), ("竹子", "bamboo_planks"), ("竹", "bamboo_planks"),
    ("桦木", "birch_planks"), ("橡木", "oak_planks"),
]
KIND = [("木凳子","stool"), ("木桌子","table"), ("木椅子","chair"), ("木梯子","ladder"), ("木篱笆","fence"), ("杂项","misc")]

def match_wood(text):
    for cn, tex in WOOD:
        if cn in text:
            return tex
    return "oak_planks"

def ids(tex):
    return tex.replace("_planks","")

def rebuild_uv(p0, p1, face):
    w = p1[0]-p0[0]; h = p1[1]-p0[1]; d = p1[2]-p0[2]
    if face in ("north","south"): u,v = w,h
    elif face in ("east","west"): u,v = d,h
    else: u,v = w,d
    return {"uv":[0,0,u,v], "texture":"#tex", "tintindex":0}

written = []
for dp, dn, fn in os.walk(ROOT):
    kind_cn = os.path.basename(dp)
    kind_en = None
    for cn, en in KIND:
        if cn == kind_cn: kind_en = en
    if kind_en is None: continue
    for f in sorted(fn):
        if not f.endswith(".bbmodel"): continue
        model_cn = f[:-8]
        vanilla = match_wood(model_cn)
        d = json.load(open(os.path.join(dp,f), encoding="utf-8"))
        elements = []
        for e in d.get("elements", []):
            if e.get("type") not in (None,"cube"): continue
            p0 = [round(v,3) for v in e["from"]]; p1 = [round(v,3) for v in e["to"]]
            faces = {}
            for name, fd in (e.get("faces") or {}).items():
                if fd is None: continue
                faces[name] = rebuild_uv(p0,p1,name)
            if faces: elements.append({"from":p0,"to":p1,"faces":faces})
        if not elements: continue
        # 家具名（中文去掉"木"和类别词）
        extra = ""
        if kind_en == "misc":
            extra = "office_chair" if "办公椅" in model_cn else "site_light"
            bid = extra
        else:
            bid = "%s_%s" % (kind_en, ids(vanilla))
        if kind_en == "misc":
            bid = extra
            vanilla = "sea_lantern" if "灯" in model_cn else "oak_planks"
        # 输出
        os.makedirs(os.path.join(A,"blockstates"), exist_ok=True)
        os.makedirs(os.path.join(A,"models/block"), exist_ok=True)
        os.makedirs(os.path.join(A,"models/item"), exist_ok=True)
        model = {"parent":"minecraft:block/block",
                 "textures":{"tex":"minecraft:block/"+vanilla,"particle":"minecraft:block/"+vanilla},
                 "elements":elements}
        open(os.path.join(A,"models/block",bid+".json"),"w",encoding="utf-8").write(json.dumps(model, ensure_ascii=False, indent=2))
        open(os.path.join(A,"blockstates",bid+".json"),"w",encoding="utf-8").write(
            json.dumps({"variants":{"":{"model":"backrooms_complex:block/"+bid}}}, ensure_ascii=False, indent=2))
        open(os.path.join(A,"models/item",bid+".json"),"w",encoding="utf-8").write(
            json.dumps({"parent":"backrooms_complex:block/"+bid}, ensure_ascii=False, indent=2))
        written.append((bid, vanilla, len(elements), model_cn))

for w in written: print(" | ".join(str(x) for x in w))
print("TOTAL", len(written))
json.dump([w[0] for w in written], open(r"C:\Users\lin13\Documents\deepseek-harness\default-workspace\backrooms_complex-1.0.0\tools\bb_ids.json","w",encoding="utf-8"), ensure_ascii=False)