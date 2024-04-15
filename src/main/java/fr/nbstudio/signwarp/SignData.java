package fr.nbstudio.signwarp;

import net.md_5.bungee.api.ChatColor;

public class SignData {
    static final String HEADER_WARP = "[Warp]";
    static final String HEADER_TARGET = "[WarpTarget]";

    private String header;
    String warpName;

    SignData(String[] lines) {
        header = ChatColor.stripColor(lines[0]);
        warpName = lines[1];
    }

    Boolean isValidWarpName() {
        return warpName != null && !warpName.isEmpty();
    }

    Boolean isWarp() {
        return header.equalsIgnoreCase(HEADER_WARP);
    }

    Boolean isWarpTarget() {
        return header.equalsIgnoreCase(HEADER_TARGET);
    }

    Boolean isWarpSign() {
        return isWarp() || isWarpTarget();
    }
}
