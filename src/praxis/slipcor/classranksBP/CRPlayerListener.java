package praxis.slipcor.classranksBP;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

/*
 * player listener class
 * 
 * v0.1.4.2 - Reagents => Items ; Cooldown ; Sign usage
 * 
 * History:
 * 
 * 		v0.1.2.7 - consistency tweaks, removed debugging, username autocomplete
 * 		v0.1.2.0 - renaming for release
 * 
 * @author slipcor
 */

public class CRPlayerListener extends PlayerListener {    
    public void onPlayerInteract(PlayerInteractEvent event) {
    	if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
    		// we clicked a block
    		if (CRClasses.signCheck[0] != null) {
    			// we want to allow sign usage
    			Block bBlock = event.getClickedBlock();
    			Material bMat = bBlock.getType();
    			
    			if ((bMat == Material.SIGN) || (bMat == Material.SIGN_POST)) {
    				// we clicked a sign!
    				Sign s = (Sign) bBlock.getState();

    				if (s.getLine(0).equals(CRClasses.signCheck[0])) {
    					String[] sArgs = {s.getLine(1)};
    					CRClasses.parseCommand(event.getPlayer(), sArgs);
    					return;
    				} else {
    					for (int i = 0 ; i <= 3; i++) {
    						if (s.getLine(i).equals(CRClasses.signCheck[1])) {
    							String[] sArgs = {"rankup"};
    							CRClasses.parseCommand(event.getPlayer(), sArgs);
    							return;
    						} else if (s.getLine(i).equals(CRClasses.signCheck[2])) {
    							String[] sArgs = {"rankdown"};
    							CRClasses.parseCommand(event.getPlayer(), sArgs);
    							return;
    						}
    					}
    				}
    			}
    		}
    	}
    }
}
