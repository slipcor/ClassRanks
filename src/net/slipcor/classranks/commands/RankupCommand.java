package net.slipcor.classranks.commands;

import net.slipcor.classranks.ClassRanks;
import net.slipcor.classranks.managers.CommandManager;
import net.slipcor.classranks.managers.DebugManager;
import net.slipcor.classranks.managers.FormatManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/*
 * rankup command class
 * 
 * v0.2.1 - internal command restructuring
 * 
 * History:
 * 
 *     v0.2.1 - internal command restructuring
 * 
 * @author slipcor
 */

public class RankupCommand implements CommandExecutor {
	private final ClassRanks plugin;
	private final CommandManager cmdMgr;
	private final DebugManager db;
	
	public RankupCommand(ClassRanks cr, CommandManager cm) {
		plugin = cr;
		cmdMgr = cm;
		db = new DebugManager(cr);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
    	if (!(sender instanceof Player)) {
    		plugin.msg(sender, "Console access is not implemented. If you want that, visit:");
    		plugin.msg(sender, "http://dev.bukkit.org/server-mods/classranks/");
    		return true;
    	}

		// if we use the shortcut /rankup or /rankdown, shift the array
		String[] tStr = new String[args.length+1];
		System.arraycopy(args, 0, tStr, 1, args.length);
		tStr[0] = cmd.getName();
		db.i("shortcut detected, parsed '" + FormatManager.formatStringArray(args) + "' to '" + tStr.toString() + "'");
		return cmdMgr.parseCommand((Player) sender, tStr);
	}

}
