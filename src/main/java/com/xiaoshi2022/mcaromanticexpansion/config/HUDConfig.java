package com.xiaoshi2022.mcaromanticexpansion.config;

import com.xiaoshi2022.mcaromanticexpansion.client.AffectionHUD;
import net.neoforged.neoforge.common.ModConfigSpec;

public class HUDConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec SPEC;

    // 配置值
    private static ModConfigSpec.ConfigValue<String> hudPosition;
    private static ModConfigSpec.IntValue hudOffsetX;
    private static ModConfigSpec.IntValue hudOffsetY;
    private static ModConfigSpec.BooleanValue showDebug;

    // ★★★ 新增：自定义坐标 ★★★
    private static ModConfigSpec.IntValue customX;
    private static ModConfigSpec.IntValue customY;

    static {
        BUILDER.comment("HUD Settings").push("hud");

        hudPosition = BUILDER
                .comment("HUD display position")
                .comment("Available values: TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, CUSTOM")
                .define("hudPosition", "BOTTOM_CENTER");

        hudOffsetX = BUILDER
                .comment("Horizontal offset in pixels (applies to all positions)")
                .defineInRange("hudOffsetX", 0, -500, 500);

        hudOffsetY = BUILDER
                .comment("Vertical offset in pixels (applies to all positions)")
                .defineInRange("hudOffsetY", -30, -500, 500);

        // ★★★ 自定义坐标配置 ★★★
        customX = BUILDER
                .comment("Custom X position (only used when hudPosition = CUSTOM)")
                .comment("Position relative to top-left corner of the screen")
                .defineInRange("customX", 100, 0, 1920);

        customY = BUILDER
                .comment("Custom Y position (only used when hudPosition = CUSTOM)")
                .comment("Position relative to top-left corner of the screen")
                .defineInRange("customY", 100, 0, 1080);

        showDebug = BUILDER
                .comment("Show debug information")
                .define("showDebug", false);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void markLoaded() {
        // 配置已由 FML 加载
    }

    public static void applyConfig() {
        try {
            // 1. 应用位置
            String posStr = getHudPosition();
            if (posStr != null && !posStr.isEmpty()) {
                try {
                    AffectionHUD.HUDPosition position = AffectionHUD.HUDPosition.valueOf(posStr);
                    AffectionHUD.setPosition(position);

                    // ★★★ 如果是自定义模式，应用自定义坐标 ★★★
                    if (position == AffectionHUD.HUDPosition.CUSTOM) {
                        AffectionHUD.setCustomPosition(getCustomX(), getCustomY());
                    }
                } catch (IllegalArgumentException e) {
                    AffectionHUD.setPosition(AffectionHUD.HUDPosition.BOTTOM_CENTER);
                }
            }
        } catch (Exception e) {
            AffectionHUD.setPosition(AffectionHUD.HUDPosition.BOTTOM_CENTER);
        }

        // 2. 应用偏移（对所有模式都有效）
        try {
            AffectionHUD.setCustomOffset(getHudOffsetX(), getHudOffsetY());
        } catch (Exception e) {
            AffectionHUD.setCustomOffset(0, -30);
        }

        // 3. 应用调试模式
        try {
            AffectionHUD.setShowDebugInfo(showDebug());
        } catch (Exception e) {
            AffectionHUD.setShowDebugInfo(false);
        }
    }

    // ========== Getter ==========
    public static String getHudPosition() {
        try {
            return hudPosition != null ? hudPosition.get() : "BOTTOM_CENTER";
        } catch (Exception e) {
            return "BOTTOM_CENTER";
        }
    }

    public static int getHudOffsetX() {
        try {
            return hudOffsetX != null ? hudOffsetX.get() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getHudOffsetY() {
        try {
            return hudOffsetY != null ? hudOffsetY.get() : -30;
        } catch (Exception e) {
            return -30;
        }
    }

    public static int getCustomX() {
        try {
            return customX != null ? customX.get() : 100;
        } catch (Exception e) {
            return 100;
        }
    }

    public static int getCustomY() {
        try {
            return customY != null ? customY.get() : 100;
        } catch (Exception e) {
            return 100;
        }
    }

    public static boolean showDebug() {
        try {
            return showDebug != null && showDebug.get();
        } catch (Exception e) {
            return false;
        }
    }

    // ========== Setter ==========
    public static void setHudPosition(String position) {
        try {
            if (hudPosition != null) {
                hudPosition.set(position);
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    public static void setHudPosition(AffectionHUD.HUDPosition position) {
        setHudPosition(position.name());
    }

    public static void setHudOffsetX(int x) {
        try {
            if (hudOffsetX != null) {
                hudOffsetX.set(x);
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    public static void setHudOffsetY(int y) {
        try {
            if (hudOffsetY != null) {
                hudOffsetY.set(y);
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    public static void setCustomX(int x) {
        try {
            if (customX != null) {
                customX.set(x);
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    public static void setCustomY(int y) {
        try {
            if (customY != null) {
                customY.set(y);
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    public static void setShowDebug(boolean debug) {
        try {
            if (showDebug != null) {
                showDebug.set(debug);
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    public static void save() {
        try {
            if (SPEC != null) {
                SPEC.save();
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    public static void resetToDefault() {
        setHudPosition("BOTTOM_CENTER");
        setHudOffsetX(0);
        setHudOffsetY(-30);
        setCustomX(100);
        setCustomY(100);
        setShowDebug(false);
        save();
        applyConfig();
    }
}