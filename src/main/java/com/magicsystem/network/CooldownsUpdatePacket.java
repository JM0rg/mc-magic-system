package com.magicsystem.network;

import com.magicsystem.MagicSystemMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record CooldownsUpdatePacket(List<Entry> entries) implements CustomPayload {
    public static final CustomPayload.Id<CooldownsUpdatePacket> ID =
        new CustomPayload.Id<>(MagicSystemMod.id("cooldowns_update"));

    public static final PacketCodec<PacketByteBuf, CooldownsUpdatePacket> CODEC =
        PacketCodec.of(CooldownsUpdatePacket::write, CooldownsUpdatePacket::read);

    public record Entry(String spellId, String spellName, int remainingMs) {}

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeString(e.spellId());
            buf.writeString(e.spellName());
            buf.writeVarInt(e.remainingMs());
        }
    }

    public static CooldownsUpdatePacket read(PacketByteBuf buf) {
        int n = buf.readVarInt();
        List<Entry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String id = buf.readString();
            String name = buf.readString();
            int rem = buf.readVarInt();
            list.add(new Entry(id, name, rem));
        }
        return new CooldownsUpdatePacket(list);
    }
}


