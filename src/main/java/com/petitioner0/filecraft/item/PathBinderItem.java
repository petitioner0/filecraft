package com.petitioner0.filecraft.item;

import com.petitioner0.filecraft.fsblock.FileNodeBlock;
import com.petitioner0.filecraft.fsblock.FileNodeBlockEntity;
import com.petitioner0.filecraft.network.FilecraftNetwork;
import com.petitioner0.filecraft.network.c2s.RequestPickPathC2SPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public class PathBinderItem extends Item implements net.neoforged.neoforge.common.extensions.IItemExtension {
    public PathBinderItem(Properties props) { super(props); }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (!(level.getBlockState(pos).getBlock() instanceof FileNodeBlock)) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS; // 等待服务端逻辑
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FileNodeBlockEntity node)) return InteractionResult.PASS;

        var owner = node.getOwnerUuid();
        if (owner != null && !owner.equals(sp.getUUID())) {
            sp.displayClientMessage(Component.translatable("message.filecraft.owner_only_bind"), true);
            return InteractionResult.CONSUME;
        }
        if (owner == null) {
            node.setOwner(sp.getUUID(), sp.getGameProfile().getName());
        }

        // 防止展开状态下导致混乱：先回收子树
        node.collapse(level);

        // 请求客户端弹出选择器
        FilecraftNetwork.sendToPlayer(sp, new RequestPickPathC2SPayload(pos));
        return InteractionResult.CONSUME;
    }
}
