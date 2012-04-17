package net.slipcor.classranks.handlers;

import org.bukkit.entity.Player;

/**
 * Permissions handler class
 * 
 * @version v0.4.3
 * 
 * @author slipcor
 */

public abstract class CRHandler {
	public abstract boolean isInGroup(String world, String permName, String player);
	public abstract boolean setupPermissions();
	public abstract boolean hasPerms(Player comP, String string, String world);
	public abstract void classAdd(String world, String player, String cString);
	public abstract void classAddGlobal(String player, String cString);
	public abstract void rankAdd(String world, String player, String rank);
	public abstract void rankAddGlobal(String player, String rank);
	public abstract void rankRemove(String world, String player, String cString);
	public abstract void rankRemoveGlobal(String player, String cString);
	public abstract String getPermNameByPlayer(String world, String player);
	public abstract String getPermNameByPlayerGlobal(String player);
	public abstract void removeGroups(Player player);
}
