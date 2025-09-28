package com.petitioner0.filecraft.network.c2s;

import com.petitioner0.filecraft.Filecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestPickPathC2SPayload(BlockPos nodePos) implements CustomPacketPayload {
    public static final Type<RequestPickPathC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Filecraft.MODID, "req_pick_path"));

    public static final StreamCodec<FriendlyByteBuf, RequestPickPathC2SPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestPickPathC2SPayload::nodePos,
                    RequestPickPathC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
