package com.petitioner0.filecraft.network.s2c;

import com.petitioner0.filecraft.Filecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PickedPathS2CPayload(BlockPos nodePos, String path, boolean isDir) implements CustomPacketPayload {
    public static final Type<PickedPathS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Filecraft.MODID, "picked_path"));

    public static final StreamCodec<FriendlyByteBuf, PickedPathS2CPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PickedPathS2CPayload::nodePos,
                    ByteBufCodecs.STRING_UTF8, PickedPathS2CPayload::path,
                    ByteBufCodecs.BOOL, PickedPathS2CPayload::isDir,
                    PickedPathS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
