package net.slipcor.classranks.commands;

import net.slipcor.classranks.ClassRanks;
import net.slipcor.classranks.managers.CommandManager;
import net.slipcor.classranks.managers.DebugManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * classadmin command class
 * 
 * @version v0.2.1
 * 
 * @author slipcor
 */

public class ClassAdminCommand implements CommandExecutor {
	private final ClassRanks plugin;
	private final CommandManager cmdMgr;
	private final DebugManager db;
	
	/**
	 * create a class admin command instance
	 * @param cr the plugin instance to hand over
	 * @param cm the command manager instance to hand over
	 */
	public ClassAdminCommand(ClassRanks cr, CommandManager cm) {
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
    	
		// admin class command, parse it!
		db.i("/classadmin detected! parsing...");
		return cmdMgr.parseAdminCommand((Player) sender, args);
	}

}
