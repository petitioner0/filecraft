package com.petitioner0.filecraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.*;

public class FileGraphManager {

    public record NodeLoc(java.util.UUID nodeId, ResourceKey<Level> levelKey, BlockPos pos) {}

    private final Map<java.util.UUID, NodeLoc> nodeLocs = new HashMap<>();
    private final Map<java.util.UUID, java.util.UUID> parentOf = new HashMap<>();
    private final Map<java.util.UUID, Set<java.util.UUID>> childrenOf = new HashMap<>();

    // 用服务器实例做弱键，跟随服务器生命周期
    private static final Map<MinecraftServer, FileGraphManager> INSTANCES = new WeakHashMap<>();

    public static FileGraphManager get(ServerLevel level) {
        MinecraftServer server = level.getServer();
        return INSTANCES.computeIfAbsent(server, s -> new FileGraphManager());
    }

    // —— 注册/更新位置和父关系
    public synchronized void registerOrUpdate(java.util.UUID nodeId, java.util.UUID parentId,
                                              ServerLevel level, BlockPos pos) {
        // 更新位置
        nodeLocs.put(nodeId, new NodeLoc(nodeId, level.dimension(), pos));

        // 更新父子关系
        java.util.UUID oldParent = parentOf.get(nodeId);
        if (!Objects.equals(oldParent, parentId)) {
            
            if (oldParent != null) {
                var set = childrenOf.get(oldParent);
                if (set != null) set.remove(nodeId);
            }
            parentOf.put(nodeId, parentId);
            if (parentId != null) {
                childrenOf.computeIfAbsent(parentId, k -> new HashSet<>()).add(nodeId);
                com.petitioner0.filecraft.Filecraft.LOGGER.info("Establishing parent-child relationship: parent {} -> child {}", parentId, nodeId);
            } else {
                com.petitioner0.filecraft.Filecraft.LOGGER.info("Registering root node: {}", nodeId);
            }
        }
    }

    public synchronized void updatePosition(java.util.UUID nodeId, ServerLevel level, BlockPos pos) {
        NodeLoc cur = nodeLocs.get(nodeId);
        if (cur == null || !cur.levelKey.equals(level.dimension()) || !cur.pos.equals(pos)) {
            nodeLocs.put(nodeId, new NodeLoc(nodeId, level.dimension(), pos));
        }
    }

    public synchronized void unregister(java.util.UUID nodeId) {
        
        var parent = parentOf.remove(nodeId);
        if (parent != null) {
            var set = childrenOf.get(parent);
            if (set != null) set.remove(nodeId);
        }
       
        var kids = childrenOf.remove(nodeId);
        if (kids != null) {
            for (var k : kids) parentOf.remove(k); 
        }
        nodeLocs.remove(nodeId);
    }

    // —— 取当前孩子
    public synchronized Set<java.util.UUID> getChildren(java.util.UUID nodeId) {
        var set = childrenOf.get(nodeId);
        Set<java.util.UUID> result = set == null ? Set.of() : Set.copyOf(set);
        com.petitioner0.filecraft.Filecraft.LOGGER.info("Getting children of node {}: {}", nodeId, result);
        return result;
    }

    // —— 后序遍历返回所有后代的 NodeLoc（深度优先，不包含自己）
    public synchronized java.util.List<NodeLoc> getDescendantsPostOrder(java.util.UUID root) {
        java.util.List<NodeLoc> out = new ArrayList<>();
        dfsChildren(root, out);
        return out;
    }

    private void dfsChildren(java.util.UUID u, java.util.List<NodeLoc> out) {
        var ch = childrenOf.get(u);
        if (ch != null) {
            for (var v : ch) {
                dfsChildren(v, out);
                var loc = nodeLocs.get(v);
                if (loc != null) out.add(loc);
            }
        }
    }
}