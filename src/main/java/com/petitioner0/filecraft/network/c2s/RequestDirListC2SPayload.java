package com.petitioner0.filecraft.network.c2s;

import com.petitioner0.filecraft.Filecraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record RequestDirListC2SPayload(
        BlockPos nodePos,
        Direction face,
        String path,
        boolean isDir,
        UUID nodeId
) implements CustomPacketPayload {

    public static final Type<RequestDirListC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Filecraft.MODID, "request_dir_list"));

    // 用 STRING_UTF8 映射出 UUID 的 StreamCodec
    private static final StreamCodec<ByteBuf, UUID> UUID_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString);

    public static final StreamCodec<ByteBuf, RequestDirListC2SPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,              RequestDirListC2SPayload::nodePos,
                    Direction.STREAM_CODEC,             RequestDirListC2SPayload::face,
                    ByteBufCodecs.STRING_UTF8,          RequestDirListC2SPayload::path,
                    ByteBufCodecs.BOOL,                 RequestDirListC2SPayload::isDir,
                    UUID_STREAM_CODEC,                  RequestDirListC2SPayload::nodeId,
                    RequestDirListC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}