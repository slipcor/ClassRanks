package net.slipcor.classranks.handlers;

import java.util.ArrayList;
import java.util.logging.Level;

import net.slipcor.classranks.ClassRanks;
import net.slipcor.classranks.managers.ClassManager;
import net.slipcor.classranks.managers.DebugManager;
import net.slipcor.classranks.managers.PlayerManager;

import org.bukkit.entity.Player;

import ru.tehkode.permissions.PermissionManager;

/**
 * PermissionsEX handler class
 * 
 * @version v0.4.3
 * 
 * @author slipcor
 */

public class HandlePEX extends CRHandler {
	private final ClassRanks plugin;
	private PermissionManager permissionHandler;
	private final DebugManager db;

	public HandlePEX(ClassRanks cr) {
		plugin = cr;
		db = new DebugManager(cr);
	}

	@Override
	public boolean isInGroup(String world, String permName, String player) {
		db.i("isInGroup: player "
				+ player
				+ ", world: "
				+ world
				+ ", perms: "
				+ permName
				+ ": "
				+ String.valueOf(permissionHandler.getUser(player).inGroup(
						permName)));

		return permissionHandler.getUser(player).inGroup(permName);
	}

	@Override
	public boolean setupPermissions() {
		// try to fetch the PEX handler, return result

		permissionHandler = ru.tehkode.permissions.bukkit.PermissionsEx
				.getPermissionManager();
		if (permissionHandler == null) {
			db.i("PEX not found");
			return false;
		}
		plugin.log("<3 PEX", Level.INFO); // success!
		return true;
	}

	@Override
	public boolean hasPerms(Player comP, String string, String world) {
		db.i("player hasPerms: "
				+ comP.getName()
				+ ", world: "
				+ world
				+ ", perm: "
				+ string
				+ " : "
				+ String.valueOf(comP.hasPermission(string) ? true
						: permissionHandler.has(comP.getName(), string, world)));
		return comP.hasPermission(string) ? true : permissionHandler.has(
				comP.getName(), string, world);
	}

	@Override
	public void classAdd(String world, String player, String cString) {
		player = PlayerManager.search(player); // auto-complete playername

		if (world.equalsIgnoreCase("all")) {
			classAddGlobal(player, cString);
			return;
		}
		try {
			permissionHandler.getUser(player).addGroup(cString, world);
			db.i("added group " + cString + " to player " + player
					+ " in world " + world);
		} catch (Exception e) {
			plugin.log("PermName " + cString + " or user " + player
					+ " not found in world " + world, Level.WARNING);
		}
	}

	@Override
	public void rankAdd(String world, String player, String rank) {
		player = PlayerManager.search(player); // auto-complete playername
		
		if (world.equalsIgnoreCase("all")) {
			rankAddGlobal(player, rank);
			return;
		}
		try {
			permissionHandler.getUser(player).addGroup(rank, world);
			db.i("added rank " + rank + " to player " + player
					+ " in world " + world);
		} catch (Exception e) {
			plugin.log("PermName " + rank + " or user " + player
					+ " not found in world " + world, Level.WARNING);
		}
	}

	@Override
	public void rankRemove(String world, String player, String cString) {
		player = PlayerManager.search(player); // auto-complete playername
		
		if (world.equalsIgnoreCase("all")) {
			rankRemoveGlobal(player, cString);
			return;
		}
		try {
			permissionHandler.getUser(player).removeGroup(cString,
					world);
			db.i("removed rank " + cString + " from player " + player
					+ " in world " + world);
		} catch (Exception e) {
			plugin.log("PermName " + cString + " or user " + player
					+ " not found in world " + world, Level.WARNING);
		}
	}

	@Override
	public String getPermNameByPlayer(String world, String player) {
		ArrayList<String> permGroups = new ArrayList<String>();
		player = PlayerManager.search(player); // auto-complete playername
		
		if (world.equalsIgnoreCase("all")) {
			return getPermNameByPlayerGlobal(player);
		}
		String[] groups = permissionHandler.getUser(player)
				.getGroupsNames(world);

		for (String group : groups)
			permGroups.add(group);

		db.i("player has groups: " + permGroups.toString());
		return ClassManager.getLastPermNameByPermGroups(permGroups);
	}

	@Override
	public void classAddGlobal(String player, String cString) {
		player = PlayerManager.search(player); // auto-complete playername

		try {
			permissionHandler.getUser(player).addGroup(cString);
			db.i("added group " + cString + " to player " + player);
		} catch (Exception e) {
			plugin.log("PermName " + cString + " or user " + player
					+ " not found", Level.WARNING);
		}
	}

	@Override
	public void rankAddGlobal(String player, String rank) {
		player = PlayerManager.search(player); // auto-complete playername
		
		try {
			permissionHandler.getUser(player).addGroup(rank);
			db.i("added rank " + rank + " to player " + player);
		} catch (Exception e) {
			plugin.log("PermName " + rank + " or user " + player
					+ " not found", Level.WARNING);
		}
	}

	@Override
	public void rankRemoveGlobal(String player, String cString) {
		player = PlayerManager.search(player); // auto-complete playername
		
		try {
			permissionHandler.getUser(player).removeGroup(cString);
			db.i("added rank " + cString + " to player " + player);
		} catch (Exception e) {
			plugin.log("PermName " + cString + " or user " + player
					+ " not found", Level.WARNING);
		}
	}

	@Override
	public String getPermNameByPlayerGlobal(String player) {
		ArrayList<String> permGroups = new ArrayList<String>();
		player = PlayerManager.search(player); // auto-complete playername
		
		String[] groups = permissionHandler.getUser(player)
				.getGroupsNames();

		for (String group : groups)
			permGroups.add(group);

		db.i("player has groups: " + permGroups.toString());
		return ClassManager.getLastPermNameByPermGroups(permGroups);
	}

	@Override
	public void removeGroups(Player player) {
		String[] groups = permissionHandler.getUser(player)
				.getGroupsNames();

		for (String group : groups) {
			permissionHandler.getUser(player).removeGroup(group);
		}
	}
}
