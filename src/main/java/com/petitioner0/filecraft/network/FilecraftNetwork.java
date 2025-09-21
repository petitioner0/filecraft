package com.petitioner0.filecraft.network;

import com.petitioner0.filecraft.Filecraft;
import com.petitioner0.filecraft.fsblock.FileNodeBlockEntity;
import com.petitioner0.filecraft.network.c2s.RequestDirListC2SPayload;
import com.petitioner0.filecraft.network.c2s.RequestOpenFileC2SPayload;
import com.petitioner0.filecraft.network.s2c.DirListS2CPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Filecraft.MODID)
public final class FilecraftNetwork {

    public static final String PROTOCOL = "1";

    /** 公共端注册（两端都会跑到）：声明消息类型 + 编解码 + 方向 */
    @SubscribeEvent
    public static void registerCommon(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(PROTOCOL);

        // 【服务端 -> 客户端】请求客户端去列目录（你原来的注释逻辑）
        reg.playToClient(
                RequestDirListC2SPayload.TYPE,
                RequestDirListC2SPayload.STREAM_CODEC);

        // 【服务端 -> 客户端】请求客户端打开文件
        reg.playToClient(
                RequestOpenFileC2SPayload.TYPE,
                RequestOpenFileC2SPayload.STREAM_CODEC);

        // 【客户端 -> 服务端】客户端把列出的目录回传给服务器
        reg.playToServer(
                DirListS2CPayload.TYPE,
                DirListS2CPayload.STREAM_CODEC,
                (payload, ctx) -> {
                    // == 这里等价于你原来 DirListS2CPacket.handle(...) 的服务端逻辑 ==
                    ServerPlayer sp = (ServerPlayer) ctx.player();
                    if (sp == null)
                        return;
                    ServerLevel level = sp.level();

                    // 调度批量放置（你的原逻辑）
                    com.petitioner0.filecraft.util.PlacementScheduler.enqueue(
                            level,
                            new com.petitioner0.filecraft.util.PlacementScheduler.PlaceJob(
                                    payload.origin(), payload.face(), payload.entries()));

                    // 记录 origin 的展开方向
                    if (level.getBlockEntity(payload.origin()) instanceof FileNodeBlockEntity originBe) {
                        originBe.setExpandDir(payload.face());
                    }
                });
    }

    /** 客户端端注册：处理“服务端下发的 RequestDirListC2S” */
    @EventBusSubscriber(modid = Filecraft.MODID, value = Dist.CLIENT)
    public static final class ClientHandlers {
        @SubscribeEvent
        public static void registerClient(final RegisterClientPayloadHandlersEvent event) {
            event.register(
                    RequestDirListC2SPayload.TYPE,
                    (payload, ctx) -> {
                        // 直接使用包里的真实信息，避免读取客户端 BE 的 path
                        String basePath = payload.path();
                        boolean isDir = payload.isDir();

                        // 列出子项（按路径）
                        java.util.List<DirListS2CPayload.RequestEntry> list = com.petitioner0.filecraft.util.FileLister
                                .list(basePath, isDir)
                                .stream()
                                .map(e -> DirListS2CPayload.RequestEntry.of(
                                        e.name(), e.isDir(), e.absolutePath(), e.ext()))
                                .toList();

                        // 回传给服务端（origin/face 仍用原包里的）
                        FilecraftNetwork.sendToServer(
                                new DirListS2CPayload(payload.nodePos(), payload.face(), list));
                    });

            event.register(
                RequestOpenFileC2SPayload.TYPE,
                (payload, ctx) -> {
                    String path = payload.path();
                    // 在主线程执行，避免与渲染/网络线程打架
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        boolean ok = false;
                        try {
                            if (!java.awt.GraphicsEnvironment.isHeadless()
                                    && java.awt.Desktop.isDesktopSupported()) {
                                java.awt.Desktop desk = java.awt.Desktop.getDesktop();
                                if (desk.isSupported(java.awt.Desktop.Action.OPEN)) {
                                    desk.open(new java.io.File(path));
                                    ok = true;
                                }
                            }
                        } catch (Throwable t) {
                            com.petitioner0.filecraft.Filecraft.LOGGER.warn("Desktop.open failed: {}", path, t);
                        }

                        if (!ok) {
                            // 平台回退
                            try {
                                String os = System.getProperty("os.name").toLowerCase();
                                ProcessBuilder pb;
                                if (os.contains("win")) {
                                    pb = new ProcessBuilder("cmd", "/c", "start", "", "\"" + path + "\"");
                                } else if (os.contains("mac")) {
                                    pb = new ProcessBuilder("open", path);
                                } else {
                                    pb = new ProcessBuilder("xdg-open", path);
                                }
                                pb.start();
                                ok = true;
                            } catch (Throwable t2) {
                                com.petitioner0.filecraft.Filecraft.LOGGER.warn("OS fallback open failed: {}", path, t2);
                            }
                        }

                        var mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.displayClientMessage(
                                ok ? net.minecraft.network.chat.Component.translatable("message.filecraft.file_open_success", path) 
                                   : net.minecraft.network.chat.Component.translatable("message.filecraft.file_open_failed", path),
                                true
                            );
                        }
                    });
                }
            );
        }
    }

    /* ===== 发送工具 ===== */

    // 服务器 -> 客户端：让某个玩家客户端去列目录
    public static void sendToPlayer(ServerPlayer player, RequestDirListC2SPayload msg) {
        PacketDistributor.sendToPlayer(player, msg);
    }

    // 服务器 -> 客户端：让某个玩家客户端打开文件
    public static void sendToPlayer(ServerPlayer player, RequestOpenFileC2SPayload msg) {
        PacketDistributor.sendToPlayer(player, msg);
    }

    // 客户端 -> 服务器：把客户端列出来的目录回传
    public static void sendToServer(DirListS2CPayload msg) {
        ClientPacketDistributor.sendToServer(msg);
    }
}