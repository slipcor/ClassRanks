package net.slipcor.classranks.permissions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import net.slipcor.classranks.ClassRanks;
import net.slipcor.classranks.managers.ClassManager;
import net.slipcor.classranks.managers.DebugManager;
import net.slipcor.classranks.managers.PlayerManager;

import org.bukkit.World;
import org.bukkit.entity.Player;

import ru.tehkode.permissions.PermissionManager;

/*
 * PermissionsEX handler class
 * 
 * v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * History:
 * 
 *     v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * @author slipcor
 */

public class PermissionsEXHandler extends CRPermissionHandler {
	private final ClassRanks plugin;
	private PermissionManager permissionHandler;
	private final DebugManager db;
	
	public PermissionsEXHandler(ClassRanks cr) {
		plugin = cr;
		db = new DebugManager(cr);
	}

	@Override
	public boolean isInGroup(String world, String permName, String player) {
		db.i("isInGroup: player " + player + ", world: " + world + ", perms: " + permName + ": " + String.valueOf(permissionHandler.getUser(player).inGroup(permName)));
		return permissionHandler.getUser(player).inGroup(permName);
	}

	@Override
	public boolean setupPermissions() {
		// try to fetch the PEX handler, return result
	    
	     permissionHandler = ru.tehkode.permissions.bukkit.PermissionsEx.getPermissionManager();
	        if (permissionHandler == null) {
	    		db.i("PEX not found");
	            return false;
	        }
	     plugin.log("<3 PEX", Level.INFO); // success!
	     return true;
	}

	@Override
	public boolean hasPerms(Player comP, String string, String world) {
		db.i("player hasPerms: " + comP.getName() + ", world: " + world + ", perm: "  + string + " : " + String.valueOf(comP.hasPermission(string)?true:permissionHandler.has(comP.getName(), string, world)));
		return comP.hasPermission(string)?true:permissionHandler.has(comP.getName(), string, world);
	}

	@Override
	public void classAdd(String world, String player, String cString) {
		db.i("");
		player = PlayerManager.search(player); // auto-complete playername
		if (permissionHandler != null) {
			String[] worlds = {world};
			if (world.equalsIgnoreCase("all")) {
				List<World> lWorlds = plugin.getServer().getWorlds();
		
				worlds = new String[lWorlds.size()];
				for(int i=0;i<lWorlds.size();i++) {
					worlds[i] = lWorlds.get(i).getName();
				}
			}
			for(int i=0;i<worlds.length;i++) {
				try {
					permissionHandler.getUser(player).addGroup(cString, world);
					db.i("added group " + cString + " to player " + player + " in world " + worlds[i]);
				} catch (Exception e) {
					plugin.log("PermName " + cString + " or user " + player + " not found in world " + worlds[i], Level.WARNING);
				}
			}
		}
	}

	@Override
	public void rankAdd(String world, String player, String rank) {
		player = PlayerManager.search(player); // auto-complete playername
		if (permissionHandler != null) {
			String[] worlds = {world};
			if (world.equalsIgnoreCase("all")) {
				List<World> lWorlds = plugin.getServer().getWorlds();
				
				worlds = new String[lWorlds.size()];
				for(int i=0;i<lWorlds.size();i++) {
					worlds[i] = lWorlds.get(i).getName();
				}
			}
			for(int i=0;i<worlds.length;i++) {
				try {
					permissionHandler.getUser(player).addGroup(rank, world);
					db.i("added rank " + rank + " to player " + player + " in world " + worlds[i]);
				} catch (Exception e) {
					plugin.log("PermName " + rank + " or user " + player + " not found in world " + worlds[i], Level.WARNING);
				}
			}
		}
	}

	@Override
	public void rankRemove(String world, String player, String cString) {
		player = PlayerManager.search(player); // auto-complete playername
		if (permissionHandler != null) {
			String[] worlds = {world};
			if (world.equalsIgnoreCase("all")) {
				List<World> lWorlds = plugin.getServer().getWorlds();
				
				worlds = new String[lWorlds.size()];
				for(int i=0;i<lWorlds.size();i++) {
					worlds[i] = lWorlds.get(i).getName();
				}
			}
			for(int i=0;i<worlds.length;i++) {
				try {
					permissionHandler.getUser(player).removeGroup(cString, worlds[i]);
					db.i("removed rank " + cString + " from player " + player + " in world " + worlds[i]);
				} catch (Exception e) {
					plugin.log("PermName " + cString + " or user " + player + " not found in world " + worlds[i], Level.WARNING);
				}
			}
		}
	}

	@Override
	public String getPermNameByPlayer(String world, String player) {
		ArrayList<String> permGroups = new ArrayList<String>();
		player = PlayerManager.search(player); // auto-complete playername
		if (permissionHandler != null) {
			String[] worlds = {world};
			if (world.equalsIgnoreCase("all")) {
				List<World> lWorlds = plugin.getServer().getWorlds();
				
				worlds = new String[lWorlds.size()];
				for(int i=0;i<lWorlds.size();i++) {
					worlds[i] = lWorlds.get(i).getName();
				}
			}
			for(int i=0;i<worlds.length;i++) {
				String[] groups = permissionHandler.getUser(player).getGroupsNames(worlds[i]);
				
				for (String group : groups)
					permGroups.add(group);

			}
		}
		
		db.i("player has groups: " + permGroups.toString());
		return ClassManager.getLastPermNameByPermGroups(permGroups);
	}
}
