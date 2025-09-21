package com.petitioner0.filecraft.network.s2c;

import com.petitioner0.filecraft.Filecraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record DirListS2CPayload(BlockPos origin, Direction face, List<RequestEntry> entries)
        implements CustomPacketPayload {

    public static final Type<DirListS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Filecraft.MODID, "dir_list"));

    public record RequestEntry(String name, boolean isDir, String absolutePath, String ext) {
        public static RequestEntry of(String n, boolean d, String p, String e) {
            return new RequestEntry(n, d, p, e == null ? "" : e);
        }
        public static final StreamCodec<ByteBuf, RequestEntry> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, RequestEntry::name,
                        ByteBufCodecs.BOOL,        RequestEntry::isDir,
                        ByteBufCodecs.STRING_UTF8, RequestEntry::absolutePath,
                        ByteBufCodecs.STRING_UTF8, RequestEntry::ext,
                        RequestEntry::new
                );
    }

    public static final StreamCodec<ByteBuf, DirListS2CPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,                                        DirListS2CPayload::origin,
                    Direction.STREAM_CODEC,                                       DirListS2CPayload::face,
                    RequestEntry.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)), DirListS2CPayload::entries,
                    DirListS2CPayload::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}