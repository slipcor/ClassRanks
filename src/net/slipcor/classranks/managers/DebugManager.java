package net.slipcor.classranks.managers;

import java.util.logging.Level;

import net.slipcor.classranks.ClassRanks;

/*
 * Debug manager class
 * 
 * author: slipcor
 * 
 * version: v0.2.0.0 - 
 * 
 * history:
 *
 *     v0.2.0.0 - 
 */

public class DebugManager {
	public static boolean active;
	public final ClassRanks plugin;
	
	public DebugManager(ClassRanks plugin) {
		this.plugin = plugin;
	}
	
	/*
	 * info log
	 */
	public void i(String s) {
		if (!active)
			return;
		plugin.log(s, Level.INFO);
	}
	
	/*
	 * warning log
	 */
	public void w(String s) {
		if (!active)
			return;
		plugin.log(s, Level.WARNING);
	}
	
	/*
	 * severe log
	 */
	public void s(String s) {
		if (!active)
			return;
		plugin.log(s, Level.SEVERE);
	}
}
