package com.the_backrooms_complex;

/**
 * 模组配置（内存版本）。
 *
 * <p>本类使用简单的静态布尔字段保存可调选项。如需持久化到配置文件，可在后续版本引入
 * NeoForge 的 {@code net.neoforged.neoforge.common.ModConfigSpec}。</p>
 */
public final class ModConfig {

    /**
     * 使用「阈界的层级密钥」右键「枢纽之门」传送后，是否消耗掉该密钥。
     * 默认 {@code false}（不消耗）。
     */
    public static boolean consumeKeyOnTeleport = false;

    /**
     * 若为 true，则禁止在维度内放置任何非 backrooms_complex 命名空间的方块。
     * 默认 {@code false}（可选功能，默认关闭以便调试与地图编辑）。
     */
    public static boolean enforcePureDimension = false;

    /**
     * 是否在 Level 0 维度内循环播放低音量的荧光灯背景嗡鸣。
     * 默认 {@code true}。音量与间隔见 {@link ModEvents}。
     */
    public static boolean enableAmbientHum = true;

    /**
     * 被刮掉墙纸的墙壁<b>自动恢复</b>所需的秒数。默认 {@code 60} 秒。
     *
     * <p>由 {@code ScraperItem} 在刮墙时按该值换算成 tick 排定方块 tick；
     * 若期间该方块被替换或破坏，则不再恢复。</p>
     */
    public static int wallpaperRestoreSeconds = 60;

    /**
     * 墙纸作为燃料的燃烧时间（tick）。默认 {@code 25}，
     * 相当于 {@code 0.125} 个物品（1 个物品 = 200 tick）。
     */
    public static int wallpaperBurnTicks = 25;

    /**
     * 刮纸刀右键刮墙时是否消耗 1 点耐久。默认 {@code false}（不消耗）。
     */
    public static boolean scraperUsesDurability = true;

    private ModConfig() {
    }
}
