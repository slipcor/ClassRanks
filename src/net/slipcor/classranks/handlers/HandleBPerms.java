package net.slipcor.classranks.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import net.slipcor.classranks.ClassRanks;
import net.slipcor.classranks.managers.ClassManager;
import net.slipcor.classranks.managers.DebugManager;
import net.slipcor.classranks.managers.PlayerManager;

import org.bukkit.World;
import org.bukkit.entity.Player;

import de.bananaco.bpermissions.api.ApiLayer;
import de.bananaco.bpermissions.api.util.CalculableType;

/**
 * bPermissions handler class
 * 
 * @version v0.4.3
 * 
 * @author slipcor
 */

public class HandleBPerms extends CRHandler {
	private final ClassRanks plugin;
	private final DebugManager db;

	public HandleBPerms(ClassRanks cr) {
		plugin = cr;
		db = new DebugManager(cr);
	}

	@Override
	public boolean isInGroup(String world, String permName, String player) {
		return ApiLayer.hasGroupRecursive(world, CalculableType.USER, player,
				permName);
	}

	/*
	 * Function that tries to setup the permissions system, returns result
	 */
	@Override
	public boolean setupPermissions() {
		// try to load permissions, return result
		try {
			ApiLayer al = new ApiLayer();
			plugin.log("<3 bPermissions", Level.INFO);
			return (al.toString() != null);
		} catch (Exception e) {
			db.i("bPerms not found");
			return false;
		}
	}

	@Override
	public boolean hasPerms(Player comP, String string, String world) {
		return comP.hasPermission(string);
	}

	/*
	 * Add a user to a given class in the given world
	 */
	@Override
	public void classAdd(String world, String player, String cString) {
		player = PlayerManager.search(player); // auto-complete playername

		String[] worlds = { world };
		if (world.equalsIgnoreCase("all")) {
			List<World> lWorlds = plugin.getServer().getWorlds();

			worlds = new String[lWorlds.size()];
			for (int i = 0; i < lWorlds.size(); i++) {
				worlds[i] = lWorlds.get(i).getName();
			}
		}
		for (int i = 0; i < worlds.length; i++) {
			try {
				ApiLayer.addGroup(worlds[i], CalculableType.USER, player,
						cString);
				db.i("added group " + cString + " to player " + player
						+ " in world " + worlds[i]);
			} catch (Exception e) {
				plugin.log("PermName " + cString + " or user " + player
						+ " not found in world " + worlds[i], Level.WARNING);
			}
		}
	}

	/*
	 * Add a user to a given rank in the given world
	 */
	@Override
	public void rankAdd(String world, String player, String rank) {
		player = PlayerManager.search(player); // auto-complete playername

		String[] worlds = { world };
		if (world.equalsIgnoreCase("all")) {
			List<World> lWorlds = plugin.getServer().getWorlds();

			worlds = new String[lWorlds.size()];
			for (int i = 0; i < lWorlds.size(); i++) {
				worlds[i] = lWorlds.get(i).getName();
			}
		}
		for (int i = 0; i < worlds.length; i++) {
			try {
				ApiLayer.addGroup(worlds[i], CalculableType.USER, player, rank);
				db.i("added rank " + rank + " to player " + player
						+ " in world " + worlds[i]);
			} catch (Exception e) {
				plugin.log("PermName " + rank + " or user " + player
						+ " not found in world " + worlds[i], Level.WARNING);
			}
		}
	}

	/*
	 * Remove a user from the class he has in the given world
	 */
	@Override
	public void rankRemove(String world, String player, String cString) {
		player = PlayerManager.search(player); // auto-complete playername

		String[] worlds = { world };
		if (world.equalsIgnoreCase("all")) {
			List<World> lWorlds = plugin.getServer().getWorlds();

			worlds = new String[lWorlds.size()];
			for (int i = 0; i < lWorlds.size(); i++) {
				worlds[i] = lWorlds.get(i).getName();
			}
		}
		for (int i = 0; i < worlds.length; i++) {
			try {
				ApiLayer.removeGroup(worlds[i], CalculableType.USER, player,
						cString);
				db.i("removed rank " + cString + " from player " + player
						+ " in world " + worlds[i]);
			} catch (Exception e) {
				plugin.log("PermName " + cString + " or user " + player
						+ " not found in world " + worlds[i], Level.WARNING);
			}
		}
	}

	@Override
	public String getPermNameByPlayer(String world, String player) {
		player = PlayerManager.search(player); // auto-complete playername
		ArrayList<String> permGroups = new ArrayList<String>();

		String[] worlds = { world };
		if (world.equalsIgnoreCase("all")) {
			List<World> lWorlds = plugin.getServer().getWorlds();

			worlds = new String[lWorlds.size()];
			for (int i = 0; i < lWorlds.size(); i++) {
				worlds[i] = lWorlds.get(i).getName();
			}
		}
		for (int i = 0; i < worlds.length; i++) {
			db.i("checking world "+worlds[i]);
			String[] list = ApiLayer.getGroups(worlds[i], CalculableType.USER,
					player);
			for (String sRank : list) {
				db.i("checking rank "+sRank);
				if (ClassManager.rankExists(sRank)) {
					permGroups.add(sRank);
				}
			}

		}

		db.i("player has groups: " + permGroups.toString());
		return ClassManager.getLastPermNameByPermGroups(permGroups);
	}

	@Override
	public void classAddGlobal(String player, String cString) {
		
	}

	@Override
	public void rankAddGlobal(String player, String rank) {
		
	}

	@Override
	public void rankRemoveGlobal(String player, String cString) {
		
	}

	@Override
	public String getPermNameByPlayerGlobal(String player) {
		return null;
	}

	@Override
	public void removeGroups(Player player) {
		String[] list = ApiLayer.getGroups(player.getWorld().getName(), CalculableType.USER, player.getName());
		for (String group : list) {
			ApiLayer.removeGroup(player.getWorld().getName(), CalculableType.USER, player.getName(),
					group);
		}
	}
}
