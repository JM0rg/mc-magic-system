package com.magicsystem.network;

import com.magicsystem.MagicSystemMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ManaUpdatePacket(int currentMana, int maxMana) implements CustomPayload {
    public static final CustomPayload.Id<ManaUpdatePacket> ID = 
        new CustomPayload.Id<>(MagicSystemMod.id("mana_update"));
    
    public static final PacketCodec<PacketByteBuf, ManaUpdatePacket> CODEC = 
        PacketCodec.of(ManaUpdatePacket::write, ManaUpdatePacket::read);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(currentMana);
        buf.writeVarInt(maxMana);
    }

    public static ManaUpdatePacket read(PacketByteBuf buf) {
        int currentMana = buf.readVarInt();
        int maxMana = buf.readVarInt();
        return new ManaUpdatePacket(currentMana, maxMana);
    }
}
