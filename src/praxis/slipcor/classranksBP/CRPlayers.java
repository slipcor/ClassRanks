package praxis.slipcor.classranksBP;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.config.Configuration;

import praxis.slipcor.classranksBP.CRClasses;
import praxis.slipcor.classranksBP.ClassRanks;

/*
 * player class
 * 
 * v0.1.4.2 - Reagents => Items ; Cooldown ; Sign usage
 * 
 * History:
 * 
 *      v0.1.3.3 - Possibility to pay for upranking
 * 		v0.1.2.7 - consistency tweaks, removed debugging, username autocomplete
 * 
 * @author slipcor
 */

public class CRPlayers {
	/*
	 * receive a string and search for online usernames containing it
	 */	
	public static String search(String player) {
		Player[] p = CRClasses.plugin.getServer().getOnlinePlayers();
		for (int i=0;i<p.length;i++) {
			if (p[i].getName().toLowerCase().contains(player.toLowerCase())) {
				// gotcha!
				return p[i].getName();				
			}
			
		}
		// not found online, hope that it was right anyways
		return player;
	}
	
	public static boolean ifHasTakeItems(Player iPlayer, ItemStack[] isItems) {
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

	@SuppressWarnings("unchecked")
	public static int coolDownCheck(Player comP) {
		if ((comP.isOp()) || (CRClasses.coolDown == 0)) {
			return 0; // if we do not want/need to calculate the cooldown, get out of here!
		}
		
		File fConfig = new File(CRClasses.plugin.getDataFolder(),"cooldowns.yml");
        Configuration config = new Configuration(fConfig);
        
        if(fConfig.isFile()){
        	config.load();
        	ClassRanks.log("CoolDown file loaded!", Level.INFO);
        } else {
        	HashMap<String, Integer> cdx = new HashMap<String, Integer>();
        	cdx.put("slipcor", 0);
        	config.setProperty("cooldown", cdx);
        }
        
        HashMap<String, Integer> cds = (HashMap<String, Integer>) config.getProperty("cooldown");
        
        int now = Math.round((System.currentTimeMillis() % (60*60*24*1000)) /1000);

        if (cds.containsKey(comP.getName())) {
        	// Subtract the seconds waited from the needed seconds
        	int cd = CRClasses.coolDown - (now - cds.get(comP.getName()));
        	if ((cd <= CRClasses.coolDown) && (cd > 0)) {
        		return cd; // we still have to wait, return how many seconds
        	}
        	cds.remove(comP.getName()); // delete the value
        }

    	cds.put(comP.getName(), now);
    	
        config.removeProperty("cooldown");
        config.setProperty("cooldown", cds);
        config.save();
        
		return 0;
	}
}