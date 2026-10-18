package com.yourname.backrooms_complex;

/**
 * 数值平衡表 —— 所有影响游戏玩法的数值都集中在这里。
 *
 * <p>把数值从各业务类里抽出来，好处是：</p>
 * <ul>
 *   <li><b>调参只改一处</b>：想整体调整难度时不必在十几个文件里翻找；</li>
 *   <li><b>便于平衡性审查</b>：所有会改变游戏体验的数字都在这张表里；</li>
 *   <li><b>避免魔法数字</b>：业务代码里只出现有语义的常量名。</li>
 * </ul>
 *
 * <p>注意：纯视觉参数（模糊半径、颜色、抖动幅度等）不在此表，
 * 它们由各自的客户端特效类维护。</p>
 */
public final class ModBalance {

    // ==================== 数值上限 ====================

    /** 饮水值与精神值的上限。 */
    public static final int STAT_MAX = 100;

    // ==================== 自然消耗 ====================

    /** 饮水值基础消耗间隔（20 秒 1 点）。 */
    public static final int THIRST_BASE_INTERVAL = 400;
    /** 奔跑时额外消耗间隔（8 秒 1 点）。 */
    public static final int THIRST_SPRINT_INTERVAL = 160;
    /** 潮湿环境额外消耗间隔（6 秒 1 点）。 */
    public static final int THIRST_DAMP_INTERVAL = 120;
    /** 潮湿环境精神值下降间隔（10 秒 1 点）。 */
    public static final int SANITY_DAMP_INTERVAL = 200;
    /** 黑暗中精神值下降间隔（6 秒 1 点）。 */
    public static final int SANITY_DARK_INTERVAL = 120;
    /** 黄色房间精神值恢复间隔（5 秒 1 点）。 */
    public static final int SANITY_YELLOW_INTERVAL = 100;

    // ==================== 状态阈值 ====================

    /** 精神值低于此值时方块与雾变暗红。 */
    public static final int DARK_RED_THRESHOLD = 5;
    /** 跨过暗红阈值时给予的黑暗效果时长（2 秒）。 */
    public static final int DARKNESS_TICKS = 40;

    // ==================== 脱水伤害 ====================

    /** 饮水值归零时每次扣除的血量。 */
    public static final float DEHYDRATION_DAMAGE = 1.0F;
    /** 脱水伤害间隔（3 秒 = 60 tick）。 */
    public static final int DEHYDRATION_INTERVAL_TICKS = 60;

    // ==================== 饮用恢复 ====================

    /** 未知污水瓶恢复的饮水值。 */
    public static final int SEWAGE_THIRST_RESTORE = 35;
    /** 未知污水瓶扣除的精神值。 */
    public static final int SEWAGE_SANITY_COST = 12;
    /** 药水 / 水瓶 / 牛奶 / 汤类恢复的饮水值。 */
    public static final int DRINK_THIRST_RESTORE = 30;

    // ==================== 地毯纤维 ====================

    /** 食用地毯纤维造成的伤害。 */
    public static final float FIBER_DAMAGE = 6.0F;
    /** 食用地毯纤维回复的饥饿值。 */
    public static final int FIBER_NUTRITION = 1;
    /** 食用地毯纤维附带的饥饿效果时长（10 秒）。 */
    public static final int FIBER_HUNGER_TICKS = 200;
    /** 食用地毯纤维附带的反胃效果时长（3 秒）。 */
    public static final int FIBER_NAUSEA_TICKS = 60;

    // ==================== 罐装杏仁水 ====================

    /** 受潮罐装杏仁水：饮用时长（2 秒）。 */
    public static final int DAMP_CAN_USE_TICKS = 40;
    /** 受潮罐装杏仁水：恢复精神值。 */
    public static final int DAMP_CAN_SANITY = 20;
    /** 受潮罐装杏仁水：恢复饮水值。 */
    public static final int DAMP_CAN_THIRST = 30;
    /** 受潮罐装杏仁水：恢复饥饿值。 */
    public static final int DAMP_CAN_HUNGER = 2;
    /** 受潮罐装杏仁水：是否附带反胃。 */
    public static final boolean DAMP_CAN_NAUSEA = true;

    /** 完好罐装杏仁水：饮用时长（1.5 秒）。 */
    public static final int PRISTINE_CAN_USE_TICKS = 30;
    /** 完好罐装杏仁水：恢复精神值。 */
    public static final int PRISTINE_CAN_SANITY = 40;
    /** 完好罐装杏仁水：恢复饮水值。 */
    public static final int PRISTINE_CAN_THIRST = 30;
    /** 完好罐装杏仁水：恢复饥饿值。 */
    public static final int PRISTINE_CAN_HUNGER = 4;
    /** 完好罐装杏仁水：是否附带反胃。 */
    public static final boolean PRISTINE_CAN_NAUSEA = false;

    // ==================== 地板刮取 ====================

    /** 刮开地板时地毯纤维的最小掉落数。 */
    public static final int FIBER_DROP_MIN = 1;
    /** 刮开地板时地毯纤维的最大掉落数。 */
    public static final int FIBER_DROP_MAX = 2;
    /** 剥离地板的复原延迟（60 秒）。 */
    public static final int CARPET_RESTORE_TICKS = 1200;

    // ==================== 地物概率 ====================

    /** 混凝土大厅：蜘蛛网生成概率。 */
    public static final double DECOR_COBWEB_CHANCE = 0.0001D;
    /** 混凝土大厅：贴墙砂砾堆（燧石）生成概率。 */
    public static final double DECOR_GRAVEL_CHANCE = 0.00003D;
    /** 混凝土大厅：熄灭的篝火生成概率（含砂砾区间之后的增量）。 */
    public static final double DECOR_CAMPFIRE_CHANCE = 0.00015D;

    private ModBalance() {
    }
}
