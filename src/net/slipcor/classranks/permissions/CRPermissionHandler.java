package net.slipcor.classranks.permissions;

import org.bukkit.entity.Player;

/*
 * Permissions handler class
 * 
 * v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * History:
 * 
 *     v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * @author slipcor
 */

public abstract class CRPermissionHandler {
	public abstract boolean isInGroup(String world, String permName, String player);
	public abstract boolean setupPermissions();
	public abstract boolean hasPerms(Player comP, String string, String world);
	public abstract void classAdd(String world, String player, String cString);
	public abstract void rankAdd(String world, String player, String rank);
	public abstract void rankRemove(String world, String player, String cString);
	public abstract String getPermNameByPlayer(String world, String player);
}
