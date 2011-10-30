package praxis.slipcor.classranksBP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import praxis.slipcor.classranksBP.ClassRanks;

/*
 * player class
 * 
 * v0.1.6 - cleanup
 * 
 * History:
 * 
 *     v0.1.5.1 - cleanup
 *     v0.1.5.0 - more fixes, update to CB #1337
 *     v0.1.4.2 - Reagents => Items ; Cooldown ; Sign usage
 *     v0.1.3.3 - Possibility to pay for upranking
 * 	   v0.1.2.7 - consistency tweaks, removed debugging, username autocomplete
 * 
 * @author slipcor
 */

public class CRPlayers {
	
	private final ClassRanks c;
	int coolDown;
	
	public CRPlayers(ClassRanks plugin) {
		c = plugin;
	}
	
	/*
	 * receive a string and search for online usernames containing it
	 */	
	String search(String player) {
		Player[] p = c.getServer().getOnlinePlayers();
		for (int i=0;i<p.length;i++) {
			if (p[i].getName().toLowerCase().contains(player.toLowerCase())) {
				// gotcha!
				return p[i].getName();				
			}
		}
		// not found online, hope that it was right anyways
		return player;
	}
	
	boolean ifHasTakeItems(Player iPlayer, ItemStack[] isItems) {
		ItemStack[] isItemsBackup = null;

        ItemStack[] isPlayerItems = iPlayer.getInventory().getContents();
        ItemStack[] isPlayerItemsBackup = new ItemStack[isPlayerItems.length];

        HashMap<Integer,ItemStack> iiItemsLeftover;

        isItemsBackup = new ItemStack[isItems.length];
        
        for(int i=0;i<isItems.length;i++){
            if(isItems[i] != null){
            	isItemsBackup[i] = new ItemStack(
                    isItems[i].getType(),
                    isItems[i].getAmount(),
                    isItems[i].getDurability()
                );

                if(isItems[i].getData() != null){
                	isItemsBackup[i].setData(isItems[i].getData());
                }                
            }
        }

        // isItems == ItemStack we want to take
        // isItemsBackup == Backup
        
        for(int i=0;i<isPlayerItems.length;i++){
            if(isPlayerItems[i] != null){
                isPlayerItemsBackup[i] = new ItemStack(
                    isPlayerItems[i].getType(),
                    isPlayerItems[i].getAmount(),
                    isPlayerItems[i].getDurability()
                );

                if(isPlayerItems[i].getData() != null){
                    isPlayerItemsBackup[i].setData(isPlayerItems[i].getData());
                }
            }
        }
        // isPlayerItems == Player Inventory
        // isPlayerItemsBackup == Backup
	
        iiItemsLeftover = iPlayer.getInventory().removeItem(isItemsBackup);

        if(!iiItemsLeftover.isEmpty()){
            // player does NOT have the stuff

            iPlayer.getInventory().setContents(isPlayerItemsBackup);
            return false;
        }
        return true;  
	}

	int coolDownCheck(Player comP) {
		if ((comP.isOp()) || (coolDown == 0)) {
			return 0; // if we do not want/need to calculate the cooldown, get out of here!
		}
		
		File fConfig = new File(c.getDataFolder(),"cooldowns.yml");
		YamlConfiguration config = new YamlConfiguration();
        
        
        if(fConfig.isFile()){
        	try {
				config.load(fConfig);
			} catch (FileNotFoundException e) {
				c.log("File not found!", Level.SEVERE);
				e.printStackTrace();
			} catch (IOException e) {
				c.log("IO Exception!", Level.SEVERE);
				e.printStackTrace();
			} catch (InvalidConfigurationException e) {
				c.log("Invalid Configuration!", Level.SEVERE);
				e.printStackTrace();
			}
        	c.log("CoolDown file loaded!", Level.INFO);
        } else {
        	HashMap<String, Integer> cdx = new HashMap<String, Integer>();
        	cdx.put("slipcor", 0);
        	config.set("cooldown", null);
        }
        
        Map<String, Object> cds = (Map<String, Object>) config.getConfigurationSection("cooldown").getValues(true);
        int now = Math.round((System.currentTimeMillis() % (60*60*24*1000)) /1000);

        if (cds.containsKey(comP.getName())) {
        	// Subtract the seconds waited from the needed seconds
        	int cd = coolDown - (now - (Integer) cds.get(comP.getName()));
        	if ((cd <= coolDown) && (cd > 0)) {
        		return cd; // we still have to wait, return how many seconds
        	}
        	cds.remove(comP.getName()); // delete the value
        }

    	cds.put(comP.getName(), now);
    	
        config.set("cooldown", cds);
		try {
			config.save(fConfig);
		} catch (IOException e) {
			c.log("IO Exception!", Level.SEVERE);
			e.printStackTrace();
		}
        
		return 0;
	}
}
