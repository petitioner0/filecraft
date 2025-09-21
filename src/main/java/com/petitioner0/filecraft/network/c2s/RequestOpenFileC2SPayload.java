package com.petitioner0.filecraft.network.c2s;

import com.petitioner0.filecraft.Filecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestOpenFileC2SPayload(String path) implements CustomPacketPayload {
    public static final Type<RequestOpenFileC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Filecraft.MODID, "request_open_file"));

    public static final StreamCodec<FriendlyByteBuf, RequestOpenFileC2SPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, RequestOpenFileC2SPayload::path,
                    RequestOpenFileC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
