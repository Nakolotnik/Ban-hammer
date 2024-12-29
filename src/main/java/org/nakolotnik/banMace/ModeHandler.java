package org.nakolotnik.banMace;

import org.bukkit.entity.Player;

public interface ModeHandler {
    void execute(Player damager, Player target);
    String getModeName();
}
