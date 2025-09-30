package com.petitioner0.filecraft.network;

import com.petitioner0.filecraft.Filecraft;
import com.petitioner0.filecraft.client.ui.PathInputScreen;
import com.petitioner0.filecraft.fsblock.FileNodeBlockEntity;
import com.petitioner0.filecraft.network.c2s.RequestDirListC2SPayload;
import com.petitioner0.filecraft.network.c2s.RequestOpenFileC2SPayload;
import com.petitioner0.filecraft.network.c2s.RequestPickPathC2SPayload;
import com.petitioner0.filecraft.network.s2c.DirListS2CPayload;
import com.petitioner0.filecraft.network.s2c.PickedPathS2CPayload;
import com.petitioner0.filecraft.util.FileLister;
import com.petitioner0.filecraft.util.PlacementScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.List;

@EventBusSubscriber(modid = Filecraft.MODID)
public final class FilecraftNetwork {

    public static final String PROTOCOL = "1";

    /** 公共端注册：声明消息类型 + 编解码 + 方向 */
    @SubscribeEvent
    public static void registerCommon(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(PROTOCOL);

        // 【服务端 -> 客户端】请求客户端去列目录
        reg.playToClient(
                RequestDirListC2SPayload.TYPE,
                RequestDirListC2SPayload.STREAM_CODEC);

        // 【服务端 -> 客户端】请求客户端打开文件
        reg.playToClient(
                RequestOpenFileC2SPayload.TYPE,
                RequestOpenFileC2SPayload.STREAM_CODEC);

        // 【服务端 -> 客户端】请求选择路径
        reg.playToClient(
                RequestPickPathC2SPayload.TYPE,
                RequestPickPathC2SPayload.STREAM_CODEC);

        // 【客户端 -> 服务端】客户端把列出的目录回传给服务器
        reg.playToServer(
                DirListS2CPayload.TYPE,
                DirListS2CPayload.STREAM_CODEC,
                (payload, ctx) -> {
                    
                    ServerPlayer sp = (ServerPlayer) ctx.player();
                    if (sp == null)
                        return;
                    ServerLevel level = sp.level();

                    // 调度批量放置
                    PlacementScheduler.enqueue(
                            level,
                            new PlacementScheduler.PlaceJob(
                                    payload.origin(), payload.face(), payload.entries()));

                    // 记录 origin 的展开方向
                    if (level.getBlockEntity(payload.origin()) instanceof FileNodeBlockEntity originBe) {
                        originBe.setExpandDir(payload.face());
                    }
                });

        // 【客户端 -> 服务端】回传已选择的路径
        reg.playToServer(
                PickedPathS2CPayload.TYPE,
                PickedPathS2CPayload.STREAM_CODEC,
                (payload, ctx) -> {
                    var sp = (ServerPlayer) ctx.player();
                    if (sp == null) return;
                    var level = sp.level();
                    var be = level.getBlockEntity(payload.nodePos());
                    if (!(be instanceof FileNodeBlockEntity node)) return;

                    // 仅允许所有者或未绑定的节点
                    var owner = node.getOwnerUuid();
                    if (owner != null && !owner.equals(sp.getUUID())) {
                        sp.displayClientMessage(Component.translatable("message.filecraft.owner_only_bind"), true);
                        return;
                    }
                    if (owner == null) node.setOwner(sp.getUUID(), sp.getGameProfile().getName());

                    // 替换为新路径（先回收子节点，避免残留）
                    node.collapse(level);

                    node.setParent(null);
                    node.setParentId(null);
                    node.setPath(payload.path(), payload.isDir());

                    sp.displayClientMessage(
                        Component.translatable("message.filecraft.bound_to_path", node.getName()),
                        true
                    );
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
                        // 直接使用包里的真实信息
                        String basePath = payload.path();
                        boolean isDir = payload.isDir();

                        // 列出子项（按路径）
                        List<DirListS2CPayload.RequestEntry> list = FileLister
                                .list(basePath, isDir)
                                .stream()
                                .map(e -> DirListS2CPayload.RequestEntry.of(
                                        e.name(), e.isDir(), e.absolutePath(), e.ext()))
                                .toList();

                        // 回传给服务端
                        FilecraftNetwork.sendToServer(
                                new DirListS2CPayload(payload.nodePos(), payload.face(), list));
                    });

            event.register(
                RequestOpenFileC2SPayload.TYPE,
                (payload, ctx) -> {
                    String path = payload.path();
                    // 在主线程执行
                    Minecraft.getInstance().execute(() -> {
                        boolean ok = false;
                        try {
                            if (!GraphicsEnvironment.isHeadless()
                                    && Desktop.isDesktopSupported()) {
                                Desktop desk = Desktop.getDesktop();
                                if (desk.isSupported(Desktop.Action.OPEN)) {
                                    desk.open(new File(path));
                                    ok = true;
                                }
                            }
                        } catch (Throwable t) {
                            Filecraft.LOGGER.warn("Desktop.open failed: {}", path, t);
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
                                Filecraft.LOGGER.warn("OS fallback open failed: {}", path, t2);
                            }
                        }

                        var mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.displayClientMessage(
                                ok ? Component.translatable("message.filecraft.file_open_success", path) 
                                   : Component.translatable("message.filecraft.file_open_failed", path),
                                true
                            );
                        }
                    });
                }
            );

            event.register(
                RequestPickPathC2SPayload.TYPE,
                (payload, ctx) -> {
                    var mc = Minecraft.getInstance();
                    mc.execute(() -> mc.setScreen(new PathInputScreen(
                        Component.translatable("screen.filecraft.enter_path"),
                        (String text) -> {
                            File f = new File(text);
                            if (!f.exists()) {
                                if (mc.player != null) mc.player.displayClientMessage(Component.translatable("message.filecraft.path_not_exists"), true);
                                return;
                            }
                            boolean isDir = f.isDirectory();
                            FilecraftNetwork.sendToServer(
                                new PickedPathS2CPayload(
                                    payload.nodePos(), f.getAbsolutePath(), isDir
                                )
                            );
                            if (mc.player != null) {
                                mc.player.displayClientMessage(
                                    Component.translatable("message.filecraft.path_picked", f.getAbsolutePath()),
                                    true
                                );
                            }
                        }
                    )));
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

    // 服务器 -> 客户端：让某个玩家客户端选择路径
    public static void sendToPlayer(ServerPlayer player, RequestPickPathC2SPayload msg) {
        PacketDistributor.sendToPlayer(player, msg);
    }

    // 客户端 -> 服务器：把客户端列出来的目录回传
    public static void sendToServer(DirListS2CPayload msg) {
        ClientPacketDistributor.sendToServer(msg);
    }

    // 客户端 -> 服务器：回传已选择的路径
    public static void sendToServer(PickedPathS2CPayload msg) {
        ClientPacketDistributor.sendToServer(msg);
    }
}