package com.the_backrooms_complex.worldgen;

/**
 * 并查集（Union-Find / Disjoint Set）。
 *
 * <p>支持路径压缩（find）与按秩合并（union），用于验证迷宫空气单元格的连通性。
 * 在 {@code BackroomsChunkGenerator} 中作为递归回溯算法之后的安全网，在
 * {@code /backrooms check} 命令中用于统计连通分量数量。</p>
 */
public final class UnionFind {

    private final int[] parent;
    private final int[] rank;

    public UnionFind(int size) {
        parent = new int[size];
        rank = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }
    }

    /** 查找根节点，带路径压缩。 */
    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }

    /** 合并两个集合，按秩合并。 */
    public void union(int a, int b) {
        int rootA = find(a);
        int rootB = find(b);
        if (rootA == rootB) {
            return;
        }
        if (rank[rootA] < rank[rootB]) {
            int tmp = rootA;
            rootA = rootB;
            rootB = tmp;
        }
        parent[rootB] = rootA;
        if (rank[rootA] == rank[rootB]) {
            rank[rootA]++;
        }
    }
}
