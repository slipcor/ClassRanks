package net.slipcor.classranks.listeners;

import net.slipcor.classranks.ClassRanks;
import net.slipcor.classranks.managers.CommandManager;
import net.slipcor.classranks.managers.DebugManager;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

/*
 * player listener class
 * 
 * v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * History:
 * 
 *     v0.1.6 - cleanup
 * 
 * @author slipcor
 */

public class CRPlayerListener extends PlayerListener {
	private final CommandManager cmdMgr;
	private final DebugManager db;
	
	public CRPlayerListener(ClassRanks plugin, CommandManager classes) {
		cmdMgr = classes;
		db = new DebugManager(plugin);
	}
	
    public void onPlayerInteract(PlayerInteractEvent event) {
    	if (event.isCancelled() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
    		return; // event cancelled or no block clicked => OUT
    	}

		db.i("right block click!");
		// we clicked a block
		if (cmdMgr.signCheck[0] == null) {
			return; // no sign usage => OUT
		}
		db.i("we want to check for sign usage!");
		// we want to allow sign usage
		Block bBlock = event.getClickedBlock();
		if (!(bBlock.getState() instanceof Sign)) {
			return; // no sign clicked => OUT
		}
		db.i("we clicked a sign!");
		// we clicked a sign!
		Sign s = (Sign) bBlock.getState();

		if (s.getLine(0).equals(cmdMgr.signCheck[0])) {
    		db.i("parsing command " + s.getLine(1));
			String[] sArgs = {s.getLine(1)}; // parse the command!
			cmdMgr.parseCommand(event.getPlayer(), sArgs);
		} else {
    		db.i("searching for rank commands");
			for (int i = 0 ; i <= 3; i++) {
				if (s.getLine(i).equals(cmdMgr.signCheck[1])) {
		    		db.i("rankup found, parsing...");
					String[] sArgs = {"rankup"};
					cmdMgr.parseCommand(event.getPlayer(), sArgs);
					return;
				} else if (s.getLine(i).equals(cmdMgr.signCheck[2])) {
		    		db.i("rankup found, parsing");
					String[] sArgs = {"rankdown"};
					cmdMgr.parseCommand(event.getPlayer(), sArgs);
					return;
				}
			}
    		db.i("no command found!");
		}
    }
}
