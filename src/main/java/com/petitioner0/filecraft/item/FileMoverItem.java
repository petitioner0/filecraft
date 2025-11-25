package com.petitioner0.filecraft.item;

import com.petitioner0.filecraft.content.FilecraftBlocks;
import com.petitioner0.filecraft.fsblock.FileKind;
import com.petitioner0.filecraft.fsblock.FileNodeBlock;
import com.petitioner0.filecraft.fsblock.FileNodeBlockEntity;
import com.petitioner0.filecraft.util.FileGraphManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

public class FileMoverItem extends Item {
    private static final String NBT_KEY_NODE_ID = "filecraft:mover_nodeId";
    private static final String NBT_KEY_PATH = "filecraft:mover_path";
    private static final String NBT_KEY_IS_DIR = "filecraft:mover_isDir";
    private static final String NBT_KEY_OWNER_UUID = "filecraft:mover_ownerUuid";
    private static final String NBT_KEY_POS = "filecraft:mover_pos";

    public FileMoverItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = customData.copyTag();
        boolean hasStoredNode = nbt.contains(NBT_KEY_NODE_ID);

        if (player == null)
            return InteractionResult.PASS;

        if (!(level.getBlockState(pos).getBlock() instanceof FileNodeBlock)) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS; // 等待服务端逻辑
        }

        if (!(level instanceof ServerLevel sl)) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        FileNodeBlockEntity targetNode = null;
        if (!hasStoredNode) {
            if (!(be instanceof FileNodeBlockEntity)) {
                return InteractionResult.PASS;
            }
            targetNode = (FileNodeBlockEntity) be;
        }

        if (!hasStoredNode) {
            // 第一次点击：删除节点并保存信息
            var owner = targetNode.getOwnerUuid();
            if (owner != null && !owner.equals(sp.getUUID())) {
                sp.displayClientMessage(Component.translatable("message.filecraft.owner_only_move"), true);
                return InteractionResult.CONSUME;
            }

            // 保存节点信息
            nbt.putString(NBT_KEY_NODE_ID, targetNode.getNodeId().toString());
            nbt.putString(NBT_KEY_PATH, targetNode.getPath());
            nbt.putBoolean(NBT_KEY_IS_DIR, targetNode.isDirectory());
            if (owner != null) {
                nbt.putString(NBT_KEY_OWNER_UUID, owner.toString());
            } else {
                nbt.putString(NBT_KEY_OWNER_UUID, "");
            }
            nbt.putLong(NBT_KEY_POS, pos.asLong());

            // 更新 ItemStack 的数据组件
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

            // 先回收子树
            targetNode.collapse(level);

            // 从图管理器注销
            FileGraphManager graph = FileGraphManager.get(sl);
            graph.unregister(targetNode.getNodeId());

            // 删除方块
            sl.removeBlock(pos, false);

            sp.displayClientMessage(Component.translatable("message.filecraft.node_picked_for_move"), true);
            return InteractionResult.CONSUME;
        }
        // 第二次点击：尝试移动到目标节点
        if (!(be instanceof FileNodeBlockEntity)) {
            return InteractionResult.PASS;
        }
        targetNode = (FileNodeBlockEntity) be;

        String nodeIdStr = nbt.getStringOr(NBT_KEY_NODE_ID, "");
        if (nodeIdStr.isEmpty()) {
            return InteractionResult.CONSUME;
        }
        UUID storedNodeId = UUID.fromString(nodeIdStr);
        String storedPath = nbt.getStringOr(NBT_KEY_PATH, "");
        boolean storedIsDir = nbt.getBooleanOr(NBT_KEY_IS_DIR, false);

        // 检查目标节点
        if (targetNode.getNodeId().equals(storedNodeId)) {
            // 点击的是同一个节点（虽然已经被删除，但可能是其他情况）
            sp.displayClientMessage(Component.translatable("message.filecraft.cannot_move_to_self"), true);
            return InteractionResult.CONSUME;
        }

        // 检查目标节点是否为文件夹
        if (!targetNode.isDirectory()) {
            sp.displayClientMessage(Component.translatable("message.filecraft.target_must_be_folder"), true);
            return InteractionResult.CONSUME;
        }

        // 检查目标节点拥有者是否为自己
        UUID targetOwner = targetNode.getOwnerUuid();
        if (targetOwner == null || !targetOwner.equals(sp.getUUID())) {
            sp.displayClientMessage(Component.translatable("message.filecraft.target_owner_mismatch"), true);
            return InteractionResult.CONSUME;
        }

        // 也检查目标节点是否在存储节点的后代中（通过图管理器）
        FileGraphManager graph = FileGraphManager.get(sl);

        try {
            File source = new File(storedPath).getCanonicalFile();
            File target = new File(targetNode.getPath()).getCanonicalFile();

            if (storedIsDir) {
                Path sourceCanonical = source.toPath();
                Path targetCanonical = target.toPath();

                // 判断目标是否是源目录的子目录（系统级比较，不会误判）
                if (targetCanonical.startsWith(sourceCanonical)) {
                    sp.displayClientMessage(
                            Component.translatable("message.filecraft.cannot_move_to_descendant"),
                            true);
                    clearStoredNode(stack);
                    return InteractionResult.CONSUME;
                }
            }
        } catch (IOException ex) {
            sp.displayClientMessage(Component.literal("路径解析失败: " + ex.getMessage()), true);
            clearStoredNode(stack);
            return InteractionResult.CONSUME;
        }

        // 所有检查通过，执行移动
        try {
            // 1. 移动真实文件
            File sourceFile = new File(storedPath);
            if (!sourceFile.exists()) {
                sp.displayClientMessage(Component.translatable("message.filecraft.source_file_not_exists"), true);
                clearStoredNode(stack);
                return InteractionResult.CONSUME;
            }

            String targetDirPath = targetNode.getPath();
            File targetDir = new File(targetDirPath);
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                sp.displayClientMessage(Component.translatable("message.filecraft.target_dir_not_exists"), true);
                clearStoredNode(stack);
                return InteractionResult.CONSUME;
            }

            File targetFile = new File(targetDir, sourceFile.getName());
            if (targetFile.exists()) {
                sp.displayClientMessage(
                        Component.translatable("message.filecraft.target_file_exists", targetFile.getName()), true);
                clearStoredNode(stack);
                return InteractionResult.CONSUME;
            }

            // 使用 Files.move 移动文件
            Path sourcePath = sourceFile.toPath();
            Path targetPath = targetFile.toPath();
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 2. 检查文件夹是否已展开，如果已展开则放置节点
            net.minecraft.core.Direction expandDir = targetNode.getExpandDir();
            java.util.Set<UUID> children = graph.getChildren(targetNode.getNodeId());
            boolean isExpanded = !children.isEmpty();

            if (isExpanded) {
                // 找到已展开文件夹的最末端位置
                // PlacementScheduler 使用 origin.relative(dir, index)，index 从 1 开始
                // 所以如果有 n 个子节点，它们分别在 offset 1, 2, ..., n
                // 新节点应该放在 offset (n + 1)
                int childCount = children.size();
                BlockPos newPos = pos.relative(expandDir, childCount + 1);

                // 放置新节点
                net.minecraft.world.level.block.state.BlockState newState = com.petitioner0.filecraft.content.FilecraftBlocks.FILE_NODE
                        .get().defaultBlockState()
                        .setValue(FileNodeBlock.KIND,
                                com.petitioner0.filecraft.fsblock.FileKind.fromExtension(
                                        getExtension(targetFile.getName()), storedIsDir));

                sl.setBlock(newPos, newState, 3);

                BlockEntity newBe = sl.getBlockEntity(newPos);
                if (newBe instanceof FileNodeBlockEntity newNode) {
                    newNode.setPath(targetFile.getAbsolutePath(), storedIsDir);
                    newNode.setParent(pos);
                    newNode.setExpandDir(expandDir);
                    newNode.setOwner(sp.getUUID(), sp.getGameProfile().getName());
                    newNode.setParentId(targetNode.getNodeId());

                    // 注册到图管理器
                    graph.registerOrUpdate(newNode.getNodeId(), targetNode.getNodeId(), sl, newPos);
                }

                sp.displayClientMessage(Component.translatable("message.filecraft.node_moved_success",
                        sourceFile.getName(), targetDirPath), true);
            } else {
                // 文件夹未展开，只移动文件，不放置节点
                sp.displayClientMessage(Component.translatable("message.filecraft.node_moved_success",
                        sourceFile.getName(), targetDirPath), true);
            }

            clearStoredNode(stack);
            return InteractionResult.CONSUME;

        } catch (IOException e) {
            com.petitioner0.filecraft.Filecraft.LOGGER.error("Failed to move file: {}", storedPath, e);
            sp.displayClientMessage(Component.translatable("message.filecraft.file_move_failed", e.getMessage()),
                    true);
            clearStoredNode(stack);
            return InteractionResult.CONSUME;
        }
    }

    private void clearStoredNode(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = customData.copyTag();
        nbt.remove(NBT_KEY_NODE_ID);
        nbt.remove(NBT_KEY_PATH);
        nbt.remove(NBT_KEY_IS_DIR);
        nbt.remove(NBT_KEY_OWNER_UUID);
        nbt.remove(NBT_KEY_POS);

        // 如果 NBT 为空，删除组件以避免冗余
        if (nbt.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
    }
}
