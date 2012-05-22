package net.slipcor.classranks.handlers;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import net.slipcor.classranks.ClassRanks;
import net.slipcor.classranks.managers.ClassManager;
import net.slipcor.classranks.managers.DebugManager;

/**
 * SuperPermissions handler class
 * 
 * @version v0.4.3
 * 
 * @author slipcor
 */

public class HandleSuperPerms extends CRHandler {
	private final ClassRanks plugin;
	private final DebugManager db;
	
	public HandleSuperPerms(ClassRanks cr) {
		plugin = cr;
		db = new DebugManager(cr);
	}

	@Override
	public boolean isInGroup(String world, String permName, String player) {
		Map<String, Object> nodes = plugin.getConfig().getConfigurationSection("players." + player).getValues(true);
		
		if (nodes != null)
			for (Object key : nodes.values())
				if (((String) key).equals(permName)) {

					db.i("isInGroup: player " + player + ", world: " + world + ", perms: " + permName + ": " + String.valueOf(true));
					return true;
				}

		db.i("isInGroup: player " + player + ", world: " + world + ", perms: " + permName + ": " + String.valueOf(false));
		return false;
	}
	

    /*
     * Function that tries to setup the permissions system, returns result
     */
	@Override
    public boolean setupPermissions() {
	    plugin.log("No permissions plugin found, defaulting to SuperPerms.", Level.INFO); // success!
    	return true;
    }

	@Override
    public boolean hasPerms(Player comP, String string, String world) {
		db.i("player hasPerms: " + comP.getName() + ", world: " + world + ", perm: "  + string + " : " + String.valueOf( comP.hasPermission(string)));
		return comP.hasPermission(string);
	}

    
	/*
     * Add a user to a given class in the given world
     */
	@Override
	public void classAdd(String world, String player, String cString) {
		String firstRank = null;
		if (plugin.trackRanks) {
			firstRank = ClassManager.getFirstPermNameByClassName(cString, player);
		} else {
			firstRank = ClassManager.getFirstPermNameByClassName(cString);
		}
		plugin.getConfig().set("players." + player + "." + cString, firstRank);
		db.i("added group " + cString + " to player " + player + ", no world support");
		plugin.saveConfig();
	}

    /*
     * Add a user to a given rank in the given world
     */
	@Override
	public void rankAdd(String world, String player, String rank) {
		String cString = ClassManager.getClassNameByPermName(rank);
		plugin.getConfig().set("players." + player + "." + cString, rank);
		db.i("added rank " + rank + " to player " + player + ", no world support");
		plugin.saveConfig();
	}

	/*
	 * Remove a user from the class he has in the given world
	 */
	@Override
	public void rankRemove(String world, String player, String cString) {
		plugin.getConfig().set("players." + player, null);
		db.i("removed rank " + cString + " from player " + player + ", no world support");
		plugin.saveConfig();
	}

	@Override
	public String getPermNameByPlayer(String world, String player) {
		ArrayList<String> permGroups = new ArrayList<String>();
		
		Map<String, Object> groups = null;
		try {
			groups = plugin.getConfig().getConfigurationSection("players." + player).getValues(true);
		} catch (Exception e) {
			
		}
		
		if (groups == null) {
			return "";
		}
		
		for (Object group : groups.values())
			permGroups.add((String) group);
		
		db.i("player has groups: " + permGroups.toString());
		return ClassManager.getLastPermNameByPermGroups(permGroups);
	}

	@Override
	public void classAddGlobal(String player, String cString) {
		classAdd(null, player, cString);
	}

	@Override
	public void rankAddGlobal(String player, String rank) {
		rankAdd(null, player, rank);
	}

	@Override
	public void rankRemoveGlobal(String player, String cString) {
		rankRemove(null, player, cString);
	}

	@Override
	public String getPermNameByPlayerGlobal(String player) {
		return getPermNameByPlayer(null, player);
	}

	@Override
	public void removeGroups(Player player) {
		plugin.getConfig().set("players."+player.getName(), null);
		plugin.saveConfig();
	}
}
