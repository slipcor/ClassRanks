package net.slipcor.classranks.managers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import net.slipcor.classranks.ClassRanks;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/*
 * Player manager class
 * 
 * v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * History:
 * 
 *     v0.1.6 - cleanup
 *     v0.1.5.1 - cleanup
 *     v0.1.5.0 - more fixes, update to CB #1337
 * 
 * @author slipcor
 */

public class PlayerManager {
	private final ClassRanks plugin;
	private static DebugManager db;
	public static int coolDown;
	
	public PlayerManager(ClassRanks plugin) {
		this.plugin = plugin;
		db = new DebugManager(plugin);
	}
	
	/*
	 * receive a string and search for online usernames containing it
	 */	
	public static String search(String player) {
		Player[] p = Bukkit.getServer().getOnlinePlayers();
		for (int i=0;i<p.length;i++)
			if (p[i].getName().toLowerCase().contains(player.toLowerCase()))
				return p[i].getName(); // gotcha!

		db.i("player not online: " + player);
		// not found online, hope that it was right anyways
		return player;
	}
	
	boolean ifHasTakeItems(Player iPlayer, ItemStack[] isItems) {
		ItemStack[] isItemsBackup = null;

        ItemStack[] isPlayerItems = iPlayer.getInventory().getContents();
        ItemStack[] isPlayerItemsBackup = new ItemStack[isPlayerItems.length];

        HashMap<Integer,ItemStack> iiItemsLeftover;

        isItemsBackup = new ItemStack[isItems.length];
        
        for (int i=0;i<isItems.length;i++) {
            if (isItems[i] != null) {
            	isItemsBackup[i] = isItems[i].clone(); //TODO: needed??
            }
        }

        // isItems == ItemStack we want to take
        // isItemsBackup == Backup
        
        for (int i=0;i<isPlayerItems.length;i++) {
            if (isPlayerItems[i] != null) {
                isPlayerItemsBackup[i] = isPlayerItems[i].clone();
            }
        }
        // isPlayerItems == Player Inventory
        // isPlayerItemsBackup == Backup
	
        iiItemsLeftover = iPlayer.getInventory().removeItem(isItemsBackup);

        if(!iiItemsLeftover.isEmpty()){
            // player does NOT have the stuff

    		db.i("player does not have the items");
            iPlayer.getInventory().setContents(isPlayerItemsBackup);
            return false;
        }
		db.i("player has the items");
        return true;  
	}

	int coolDownCheck(Player comP) {
		if ((comP.isOp()) || (coolDown == 0)) {
			return 0; // if we do not want/need to calculate the cooldown, get out of here!
		}

		db.i("calculating cooldown");
		
		File fConfig = new File(plugin.getDataFolder(),"cooldowns.yml");
		YamlConfiguration config = new YamlConfiguration();
        
        if(fConfig.isFile()){
        	try {
				config.load(fConfig);
			} catch (FileNotFoundException e) {
				plugin.log("File not found!", Level.SEVERE);
				e.printStackTrace();
			} catch (IOException e) {
				plugin.log("IO Exception!", Level.SEVERE);
				e.printStackTrace();
			} catch (InvalidConfigurationException e) {
				plugin.log("Invalid Configuration!", Level.SEVERE);
				e.printStackTrace();
			}
        	plugin.log("CoolDown file loaded!", Level.INFO);
        } else {
        	Map<String, Object> cdx = new HashMap<String, Object>();
        	cdx.put("slipcor", 0);
        	config.addDefault("cooldown", cdx);
        }
        
        Map<String, Object> cds = (Map<String, Object>) config.getConfigurationSection("cooldown").getValues(true);
        int now = Math.round((System.currentTimeMillis() % (60*60*24*1000)) /1000);

        if (cds.containsKey(comP.getName())) {
    		db.i("player cooldown found!");
        	// Subtract the seconds waited from the needed seconds
        	int cd = coolDown - (now - (Integer) cds.get(comP.getName()));
        	if ((cd <= coolDown) && (cd > 0)) {
        		db.i("cooldown still is: "+cd);
        		return cd; // we still have to wait, return how many seconds
        	}
        	cds.remove(comP.getName()); // delete the value
    		db.i("value deleted");
        }

    	cds.put(comP.getName(), now);
		db.i("value set");
    	
        config.set("cooldown", cds);
		try {
			config.save(fConfig);
		} catch (IOException e) {
			plugin.log("IO Exception!", Level.SEVERE);
			e.printStackTrace();
		}
        
		return 0;
	}
	

}
