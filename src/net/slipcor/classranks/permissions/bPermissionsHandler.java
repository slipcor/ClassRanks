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

import de.bananaco.permissions.Permissions;
import de.bananaco.permissions.interfaces.PermissionSet;
import de.bananaco.permissions.worlds.WorldPermissionsManager;

/*
 * bPermissions handler class
 * 
 * v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * History:
 * 
 *     v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * @author slipcor
 */

public class bPermissionsHandler extends CRPermissionHandler {
	private final ClassRanks plugin;
	private WorldPermissionsManager permissionHandler;
	private final DebugManager db;
	
	public bPermissionsHandler(ClassRanks cr) {
		plugin = cr;
		db = new DebugManager(cr);
	}

	@Override
	public boolean isInGroup(String world, String permName, String player) {
		permName = permName.toLowerCase();
		PermissionSet ps = permissionHandler.getPermissionSet(world);
		List<String> list = ps.getGroups(player);

		db.i("isInGroup: player " + player + ", world: " + world + ", perms: " + permName + ": " + String.valueOf(list.contains(permName)));
		return (list.contains(permName));
	}
	

    /*
     * Function that tries to setup the permissions system, returns result
     */
	@Override
    public boolean setupPermissions() {
    	// try to load permissions, return result
    	
        permissionHandler = Permissions.getWorldPermissionsManager();
        if (permissionHandler == null){
    		db.i("bPerms not found");
            return false;            
        }
        plugin.log("<3 bPermissions", Level.INFO);
    	return true;
    }

	@Override
    public boolean hasPerms(Player comP, String string, String world) {
		db.i("player hasPerms: " + comP.getName() + ", world: " + world + ", perm: "  + string + " : " + String.valueOf( permissionHandler.getPermissionSet(world).getPlayerNodes(comP).contains(string)));
		return comP.hasPermission(string);
	}

    
	/*
     * Add a user to a given class in the given world
     */
	@Override
	public void classAdd(String world, String player, String cString) {
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
					permissionHandler.getPermissionSet(worlds[i]).addGroup(player, cString);
					db.i("added group " + cString + " to player " + player + " in world " + worlds[i]);
				} catch (Exception e) {
					plugin.log("PermName " + cString + " or user " + player + " not found in world " + worlds[i], Level.WARNING);
				}
			}
		}
	}

    /*
     * Add a user to a given rank in the given world
     */
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
					permissionHandler.getPermissionSet(worlds[i]).addGroup(player, rank);
					db.i("added rank " + rank + " to player " + player + " in world " + worlds[i]);
				} catch (Exception e) {
					plugin.log("PermName " + rank + " or user " + player + " not found in world " + worlds[i], Level.WARNING);
				}
			}
		}
	}

	/*
	 * Remove a user from the class he has in the given world
	 */
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
					permissionHandler.getPermissionSet(worlds[i]).removeGroup(player, cString);
					db.i("removed rank " + cString + " from player " + player + " in world " + worlds[i]);
				} catch (Exception e) {
					plugin.log("PermName " + cString + " or user " + player + " not found in world " + worlds[i], Level.WARNING);
				}
			}
		}
	}

	@Override
	public String getPermNameByPlayer(String world, String player) {
		player = PlayerManager.search(player); // auto-complete playername
		ArrayList<String> permGroups = new ArrayList<String>();
		
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
				PermissionSet ps = permissionHandler.getPermissionSet(worlds[i]);
				List<String> list = ps.getGroups(player);
				for (String sRank : list)
					if (ClassManager.rankExists(sRank))
						permGroups.add(sRank);

			}
		}

		db.i("player has groups: " + permGroups.toString());
		return ClassManager.getLastPermNameByPermGroups(permGroups);
	}
}
