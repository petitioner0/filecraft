package com.petitioner0.filecraft.fsblock;

import com.petitioner0.filecraft.network.FilecraftNetwork;
import com.petitioner0.filecraft.network.c2s.RequestDirListC2SPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class FileNodeBlock extends Block implements EntityBlock {
    public static final EnumProperty<FileKind> KIND = EnumProperty.create("kind", FileKind.class);

    public FileNodeBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(KIND, FileKind.FOLDER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FileNodeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        // 空手时没有 hand 参数，这里当作主手传入
        return handleUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    private InteractionResult handleUse(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FileNodeBlockEntity node))
            return InteractionResult.PASS;

        Direction face = hit.getDirection();

        if (state.getValue(KIND) == FileKind.FOLDER) {
            // 目标玩家：优先所有者；无所有者则回退到点击者并写入 owner
            java.util.UUID targetUuid = node.getOwnerUuid();
            net.minecraft.server.level.ServerPlayer target = null;
            if (targetUuid != null) {
                target = ((net.minecraft.server.level.ServerLevel) level).getServer()
                        .getPlayerList().getPlayer(targetUuid);
                if (target == null) {
                    player.displayClientMessage(Component.translatable("message.filecraft.owner_offline"), true);
                    return InteractionResult.CONSUME;
                }
            } else {
                target = (net.minecraft.server.level.ServerPlayer) player;
                node.setOwner(target.getUUID(), target.getGameProfile().getName());
            }

            boolean removeSelf = player.isShiftKeyDown(); // ← 按住Shift则尝试删除自己

            var graph = com.petitioner0.filecraft.util.FileGraphManager
                    .get((net.minecraft.server.level.ServerLevel) level);
            var children = graph.getChildren(node.getNodeId());
            boolean isExpanded = !children.isEmpty();

            com.petitioner0.filecraft.Filecraft.LOGGER.info(
                    "Node ID: {}, Children count: {}, Is expanded: {}, Shift: {}",
                    node.getNodeId(), children.size(), isExpanded, removeSelf);

            // === 删除自己（仅限所有者） ===
            if (removeSelf) {

                // 1) 回收子节点（collapse 只清子树，不删自己）
                node.collapse(level);

                // 2) 注销并移除自己（加双重保险）
                graph.unregister(node.getNodeId());
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    // 仅当当前位置的方块实体还是这个节点时才移除，避免误删
                    if (sl.getBlockEntity(pos) == be) {
                        sl.removeBlock(pos, /* isMoving */ false);
                    }
                }

                player.displayClientMessage(Component.translatable("message.filecraft.directory_recycled"), true);
                return InteractionResult.CONSUME;
            }

            // === 不删除自己：展开/回收 子节点 ===
            if (isExpanded) {
                node.collapse(level); // 只清子树，保留自己
                if (!player.getUUID().equals(target.getUUID())) {
                    player.displayClientMessage(Component.translatable("message.filecraft.requested_owner_collapse", node.getOwnerName()), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.filecraft.directory_collapsed"), true);
                }
            } else {
                // 请求对"目标玩家"展开（保持你原有的跨端交互）
                FilecraftNetwork.sendToPlayer(target, new RequestDirListC2SPayload(
                        pos, face, node.getPath(), node.isDirectory(), node.getNodeId()));
                if (!player.getUUID().equals(target.getUUID())) {
                    player.displayClientMessage(Component.translatable("message.filecraft.requested_owner_expand", node.getOwnerName()), true);
                }
            }
            return InteractionResult.CONSUME;
        } else {
            // 仅所有者可触发
            java.util.UUID owner = node.getOwnerUuid();
            if (owner == null || !owner.equals(player.getUUID())) {
                player.displayClientMessage(Component.translatable("message.filecraft.owner_only_open"), true);
                return InteractionResult.CONSUME;
            }

            // 仅对在线的自身客户端发包（已经在服务端）
            if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) {
                return InteractionResult.CONSUME;
            }

            // 保险：如果这个节点被标记为目录，就不处理
            if (node.isDirectory()) {
                player.displayClientMessage(Component.translatable("message.filecraft.cannot_open_directory"), true);
                return InteractionResult.CONSUME;
            }

            // 发包到所有者客户端，请求在该机器上打开文件
            FilecraftNetwork.sendToPlayer(
                sp,
                new com.petitioner0.filecraft.network.c2s.RequestOpenFileC2SPayload(node.getPath())
            );

            player.displayClientMessage(Component.translatable("message.filecraft.file_open_requested"), true);
            return InteractionResult.CONSUME;
        }
    }

    // 相邻方块变化时，重新评估文字朝向
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
            Block block, Orientation fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        if (level.isClientSide)
            return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FileNodeBlockEntity node)
            node.recomputeLabelDir();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer,
            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof net.minecraft.server.level.ServerPlayer sp) {
            var be = level.getBlockEntity(pos);
            if (be instanceof FileNodeBlockEntity node) {
                node.setOwner(sp.getUUID(), sp.getGameProfile().getName());
                // 确保 parentId=null 会被 registerOrUpdate 写入为根
                node.setParentId(null);
                // 手动注册根节点位置
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    com.petitioner0.filecraft.util.FileGraphManager.get(sl)
                            .registerOrUpdate(node.getNodeId(), null, sl, pos);
                }
            }
        }
    }
}