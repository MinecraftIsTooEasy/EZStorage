package com.zerofall.ezstorage.network;

import com.zerofall.ezstorage.network.C2S.C2SClearCraftingGridPacket;
import com.zerofall.ezstorage.network.C2S.C2SInvSlotClickedPacket;
import com.zerofall.ezstorage.network.C2S.C2SReqCraftingPacket;
import com.zerofall.ezstorage.network.S2C.S2CCraftingPreviewPacket;
import com.zerofall.ezstorage.network.S2C.S2CCursorItemPacket;
import com.zerofall.ezstorage.network.S2C.S2COpenGuiPacket;
import com.zerofall.ezstorage.network.S2C.S2CStoragePacket;
import moddedmite.rustedironcore.network.PacketReader;

public class EZStoragePacketHandler {

    public static void registerAllPackets() {
        // Server-bound (C2S)
        PacketReader.registerServerPacketReader(C2SInvSlotClickedPacket.CHANNEL, C2SInvSlotClickedPacket::new);
        PacketReader.registerServerPacketReader(C2SReqCraftingPacket.CHANNEL, C2SReqCraftingPacket::new);
        PacketReader.registerServerPacketReader(C2SClearCraftingGridPacket.CHANNEL, C2SClearCraftingGridPacket::new);

        // Client-bound (S2C)
        PacketReader.registerClientPacketReader(S2CStoragePacket.CHANNEL, S2CStoragePacket::new);
        PacketReader.registerClientPacketReader(S2COpenGuiPacket.CHANNEL, S2COpenGuiPacket::new);
        PacketReader.registerClientPacketReader(S2CCursorItemPacket.CHANNEL, S2CCursorItemPacket::new);
        PacketReader.registerClientPacketReader(S2CCraftingPreviewPacket.CHANNEL, S2CCraftingPreviewPacket::new);
    }
}