package com.petitioner0.filecraft.fsblock;


import com.petitioner0.filecraft.content.FilecraftBlockEntities;
import com.petitioner0.filecraft.util.SystemPathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.checkerframework.checker.nullness.qual.NonNull;

public class FileNodeBlockEntity extends BlockEntity {
    private String path = SystemPathUtil.getFirstRootPath();
    private String name = SystemPathUtil.getFirstRootPath();
    private boolean isDirectory = true;
    private Direction expandDir = Direction.NORTH;
    private BlockPos parent = null;

    // 渲染文字的朝向缓存（上→东→南→西→北→下）
    private Direction labelDir = Direction.UP;

    
    private java.util.UUID ownerUuid = null;
    private String ownerName = "";

    // 唯一标识 & 父关系（用 UUID 表示树）
    private java.util.UUID nodeId = java.util.UUID.randomUUID();
    private java.util.UUID parentId = null;

    public FileNodeBlockEntity(BlockPos pos, BlockState state) {
        super(FilecraftBlockEntities.FILE_NODE.get(), pos, state);
    }

    public void setPath(String path, boolean isDir) {
        this.path = path;
        this.isDirectory = isDir;
        this.name = extractName(path, isDir);
        Level currentLevel = this.level;
        if (currentLevel != null && !currentLevel.isClientSide) {
            currentLevel.setBlock(getBlockPos(),
                getBlockState().setValue(FileNodeBlock.KIND, FileKind.fromExtension(ext(path), isDir)),
                Block.UPDATE_ALL);
            recomputeLabelDir();
            sync();
        }
    }

    public void setParent(BlockPos parent) { this.parent = parent; }
    public BlockPos getParent() { return parent; }
    public void setExpandDir(Direction d) { this.expandDir = d; }
    public Direction getExpandDir() { return expandDir; }

    public void setOwner(java.util.UUID uuid, String name) {
        this.ownerUuid = uuid;
        this.ownerName = name == null ? "" : name;
        sync();
    }
    public java.util.UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }

    public java.util.UUID getNodeId() { return nodeId; }
    public java.util.UUID getParentId() { return parentId; }
    public void setParentId(java.util.UUID pid) {
        this.parentId = pid;
        sync();
        registerOrUpdateIndex();
    }

    public String getPath() { return path; }
    public String getName() { return name; }
    public boolean isDirectory() { return isDirectory; }

    public Direction getLabelDir() {
        Level currentLevel = this.level;
        if (currentLevel != null && currentLevel.isClientSide && !isValidLabelDir(labelDir)) {
            recomputeLabelDir(); // 客户端渲染时发现无效则懒更新
        }
        return labelDir;
    }

    
    public void recomputeLabelDir() {
        Level currentLevel = this.level;
        if (currentLevel == null) return;
        Direction[] order = { Direction.UP, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.DOWN };
        for (Direction d : order) {
            if (currentLevel.getBlockState(worldPosition.relative(d)).isAir()) {
                labelDir = d;
                sync();
                return;
            }
        }
        labelDir = Direction.UP;
        sync();
    }

    private boolean isValidLabelDir(Direction d) {
        Level currentLevel = this.level;
        if (currentLevel == null) return true;
        BlockPos n = worldPosition.relative(d);
        return currentLevel.getBlockState(n).isAir();
    }

    public static String extractName(String path, boolean isDir) {
        if (path == null || path.isEmpty()) return "";
        String p = path.replace('\\', '/');
        if (isDir && (p.endsWith("/") || p.endsWith(":"))) p = p.substring(0, p.length()-1);
        int idx = p.lastIndexOf('/');
        return idx >= 0 ? p.substring(idx+1) : p;
    }

    private static String ext(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot+1) : "";
    }

    // —— 在服务器上把自己的"当前坐标"注册/更新到图管理器
    private void registerOrUpdateIndex() {
        Level currentLevel = this.level;
        if (currentLevel instanceof net.minecraft.server.level.ServerLevel sl && !currentLevel.isClientSide) {
            com.petitioner0.filecraft.util.FileGraphManager.get(sl)
                .registerOrUpdate(nodeId, parentId, sl, worldPosition);
        }
    }

    // 方块加载到世界时，刷新一次索引
    @Override
    public void onLoad() {
        super.onLoad();
        registerOrUpdateIndex();
    }

    // 方块被移除时，从索引里注销
    @Override
    public void setRemoved() {
        Level currentLevel = this.level;
        if (currentLevel instanceof net.minecraft.server.level.ServerLevel sl && !currentLevel.isClientSide) {
            com.petitioner0.filecraft.util.FileGraphManager.get(sl).unregister(nodeId);
        }
        super.setRemoved();
    }

    // 将来支持"移动节点"，在坐标发生变化完成后调用：
    public void notifyMoved() { registerOrUpdateIndex(); }

    
    public void collapse(Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl) || level.isClientSide) return;

        // 取消自身挂起的放置任务
        com.petitioner0.filecraft.util.PlacementScheduler.cancelByParent(sl, worldPosition);

        var graph = com.petitioner0.filecraft.util.FileGraphManager.get(sl);
        // 后序遍历返回顺序位置（不包括自己）
        java.util.List<com.petitioner0.filecraft.util.FileGraphManager.NodeLoc> order =
                graph.getDescendantsPostOrder(this.nodeId);

        // 依次删除
        for (var loc : order) {
            if (loc.levelKey().equals(sl.dimension())) {
                var be = sl.getBlockEntity(loc.pos());
                if (be instanceof FileNodeBlockEntity child) {
                    // 递归取消它下面的放置任务
                    com.petitioner0.filecraft.util.PlacementScheduler.cancelByParent(sl, child.getBlockPos());
                }
                sl.removeBlock(loc.pos(), false);
                graph.unregister(loc.nodeId()); // 从索引移除
            }
        }
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput out) {
        super.saveAdditional(out);
        out.putString("path", path);
        out.putString("name", name);
        out.putBoolean("isDir", isDirectory);
        out.putInt("expandDir", expandDir.get3DDataValue());
        if (parent != null) out.putLong("parent", parent.asLong());
        out.putInt("labelDir", labelDir.get3DDataValue());
        out.putString("ownerName", ownerName == null ? "" : ownerName);
        out.putString("ownerUuid", ownerUuid == null ? "" : ownerUuid.toString());
        out.putString("nodeId", nodeId == null ? "" : nodeId.toString());
        out.putString("parentId", parentId == null ? "" : parentId.toString());
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput in) {
        super.loadAdditional(in);
        this.path = in.getStringOr("path", SystemPathUtil.getFirstRootPath());
        this.name = in.getStringOr("name", SystemPathUtil.getFirstRootPath());
        this.isDirectory = in.getBooleanOr("isDir", true);
        this.expandDir = Direction.from3DDataValue(in.getIntOr("expandDir", Direction.UP.get3DDataValue()));
        this.parent = in.getLong("parent").map(BlockPos::of).orElse(null);
        this.labelDir = Direction.from3DDataValue(in.getIntOr("labelDir", Direction.UP.get3DDataValue()));
        this.ownerName = in.getStringOr("ownerName", "");
        String ou = in.getStringOr("ownerUuid", "");
        this.ownerUuid = ou.isEmpty() ? null : java.util.UUID.fromString(ou);
        try {
            String nid = in.getStringOr("nodeId", "");
            this.nodeId = nid.isEmpty() ? java.util.UUID.randomUUID() : java.util.UUID.fromString(nid);
        } catch (Exception e) { this.nodeId = java.util.UUID.randomUUID(); }
        try {
            String pid = in.getStringOr("parentId", "");
            this.parentId = pid.isEmpty() ? null : java.util.UUID.fromString(pid);
        } catch (Exception e) { this.parentId = null; }
    }

    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        
        return this.saveWithoutMetadata(registries);
    }

    
    @Override
    public void handleUpdateTag(@NonNull ValueInput input) {
        super.handleUpdateTag(input);
    }

    
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        
        return ClientboundBlockEntityDataPacket.create(this);
    }

    
    @Override
    public void onDataPacket(@NonNull Connection connection, @NonNull ValueInput input) {
        super.onDataPacket(connection, input);
    }

    
    private void sync() {
        setChanged();
        Level currentLevel = this.level;
        if (currentLevel != null && !currentLevel.isClientSide) {
            currentLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}