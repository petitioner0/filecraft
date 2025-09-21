package com.petitioner0.filecraft.util;

import com.petitioner0.filecraft.content.FilecraftBlocks;
import com.petitioner0.filecraft.fsblock.FileKind;
import com.petitioner0.filecraft.fsblock.FileNodeBlock;
import com.petitioner0.filecraft.fsblock.FileNodeBlockEntity;
import com.petitioner0.filecraft.network.s2c.DirListS2CPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class PlacementScheduler {
    private static final int TICKS_PER_PLACE = 2;     // 每 2 tick
    private static final int HARD_LIMIT = 256;        // 安全上限

    private static final Map<ServerLevel, Deque<PlaceJob>> QUEUES = new WeakHashMap<>();

    public static void enqueue(ServerLevel level, PlaceJob job) {
        Deque<PlaceJob> q = QUEUES.computeIfAbsent(level, k -> new ArrayDeque<>());
        q.addLast(job.limit(HARD_LIMIT));
    }

    public static void cancelByParent(ServerLevel level, BlockPos parent) {
        Deque<PlaceJob> q = QUEUES.get(level);
        if (q == null) return;
        q.removeIf(j -> j.origin.equals(parent));
    }

    /** 由服务器 tick 事件驱动 */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : QUEUES.keySet().toArray(new ServerLevel[0])) {
            Deque<PlaceJob> q = QUEUES.get(level);
            if (q == null || q.isEmpty()) continue;
            PlaceJob job = q.peekFirst();
            if (job == null) continue;

            if (++job.tickCounter >= TICKS_PER_PLACE) {
                job.tickCounter = 0;
                if (!job.placeNext(level)) {
                    q.pollFirst(); // 完成
                }
            }
        }
    }

    public static class PlaceJob {
        public final BlockPos origin;
        public final Direction dir;
        public final List<DirListS2CPayload.RequestEntry> entries;
        public int index = 0;
        public int tickCounter = 0;

        public PlaceJob(BlockPos origin, Direction dir, List<DirListS2CPayload.RequestEntry> entries) {
            this.origin = origin;
            this.dir = dir;
            this.entries = new ArrayList<>(entries);
        }

        public PlaceJob limit(int max) {
            if (entries.size() > max) entries.subList(max, entries.size()).clear();
            return this;
        }

        /** @return true 还有剩余；false 任务结束 */
        public boolean placeNext(ServerLevel level) {
            if (index >= entries.size()) return false;

            DirListS2CPayload.RequestEntry e = entries.get(index++);
            BlockPos p = origin.relative(dir, index);

            BlockState state = FilecraftBlocks.FILE_NODE.get().defaultBlockState()
                    .setValue(FileNodeBlock.KIND, FileKind.fromExtension(e.ext(), e.isDir()));
            level.setBlock(p, state, 3);

            if (level.getBlockEntity(p) instanceof FileNodeBlockEntity be) {
                be.setPath(e.absolutePath(), e.isDir());
                be.setParent(origin);
                be.setExpandDir(dir);

                // —— 继承所有者
                var originBe = level.getBlockEntity(origin);
                if (originBe instanceof FileNodeBlockEntity ob) {
                    be.setOwner(ob.getOwnerUuid(), ob.getOwnerName());
                    // 建立树关系（用 ID，不是坐标）
                    be.setParentId(ob.getNodeId());
                    com.petitioner0.filecraft.util.FileGraphManager.get(level)
                        .registerOrUpdate(be.getNodeId(), ob.getNodeId(), level, p);
                }
            }
            return index < entries.size();
        }
    }
}