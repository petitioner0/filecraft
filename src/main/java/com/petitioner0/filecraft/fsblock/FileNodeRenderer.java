package com.petitioner0.filecraft.fsblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class FileNodeRenderer implements BlockEntityRenderer<FileNodeBlockEntity> {


    // ===== 可调参数 =====
    private static final float BASE_SCALE   = 0.02f;   // 文本基准缩放
    private static final float MAX_TEXT_PX  = 90f;     // 文本最大视觉宽度（像素）
    private static final int   VIEW_DISTANCE = 4;     // 引擎裁剪半径（方块）
    private static final float PUSH_OUT     = 0.7f; // 从方块中心沿朝向法线外推的距离（0.5到面 + 0.5半格 + ε）
    private static final float V_OFFSET_UP  = 0.10f;   // UP 时再略微上抬，像名牌
    private static final float V_OFFSET_DN  = -0.10f;  // DOWN 时再略微下移

    // 颜色（ARGB）
    private static final int TEXT_COLOR = 0xFFFFFFFF;  // 不透明白
    private static final int BG_COLOR   = 0x88000000;  // 半透明黑（名牌底）

    // 字体缓存
    private Font cachedFont = null;

    public FileNodeRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            @Nonnull FileNodeBlockEntity be,
            float partialTicks,
            @Nonnull PoseStack pose,
            @Nonnull MultiBufferSource buffers,
            int light,
            int overlay,
            @Nonnull net.minecraft.world.phys.Vec3 cameraPos // 你现有签名里带这个参数，就保留
    ) {

        final Level level = be.getLevel();
        if (level == null) {
            return;
        }

        final String text = be.getName();
        if (text == null || text.isEmpty()) {
            return;
        }

        final Minecraft mc = Minecraft.getInstance();
        if (cachedFont == null) cachedFont = mc.font;
        final Font font = cachedFont;

        final Direction dir = be.getLabelDir();
        final float camYaw = mc.gameRenderer.getMainCamera().getYRot();


        // 计算朝向法线（用于从方块中心外推“半格+”）
        float nx = 0, ny = 0, nz = 0;
        switch (dir) {
            case SOUTH -> nz =  1;
            case NORTH -> nz = -1;
            case EAST  -> nx =  1;
            case WEST  -> nx = -1;
            case UP    -> ny =  1;
            case DOWN  -> ny = -1;
        }

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);                 // 到方块中心
        pose.translate(nx * PUSH_OUT, ny * PUSH_OUT, nz * PUSH_OUT); // 凸出半格（+ε）

        // 垂直两个方向稍微修饰一下高度，更像名牌
        if (dir == Direction.UP) {
            pose.translate(0, V_OFFSET_UP, 0);
        } else if (dir == Direction.DOWN) {
            pose.translate(0, V_OFFSET_DN, 0);
        }

        // 名牌式朝向：只绕Y轴对准玩家（直立，不随俯仰）
        pose.mulPose(Axis.YP.rotationDegrees(-camYaw));

        // 基准缩放 + 自适应缩放
        pose.scale(-BASE_SCALE, -BASE_SCALE, BASE_SCALE);

        final int pxWidth = font.width(text);
        if (pxWidth > 0 && pxWidth > MAX_TEXT_PX) {
            float scaleAdj = MAX_TEXT_PX / pxWidth;
            pose.scale(scaleAdj, scaleAdj, scaleAdj);
        }

        // 居中绘制
        float x = -pxWidth / 2f;
        float y = 0f;

        // 使用 SEE_THROUGH 以获得类似玩家名牌“穿透可见”的效果；
        // 若你希望被方块遮挡，改成 Font.DisplayMode.NORMAL 即可。
        font.drawInBatch(
                text,
                x, y,
                TEXT_COLOR,
                false,
                pose.last().pose(),
                buffers,
                Font.DisplayMode.SEE_THROUGH,
                BG_COLOR, // 半透明背景条
                light
        );

        pose.popPose();
    }

    /** 交由引擎做可视距离裁剪（半径，方块数）。 */
    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }
}