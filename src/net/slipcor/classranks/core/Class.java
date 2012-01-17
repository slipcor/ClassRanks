package net.slipcor.classranks.core;

import java.util.ArrayList;

import org.bukkit.ChatColor;

/**
 * class class
 * 
 * @version v0.2.0
 * 
 * @author slipcor
 */

public class Class {
	public final ArrayList<Rank> ranks = new ArrayList<Rank>();
	public String name;
	
	/**
	 * create a class instance
	 * @param sClassName the class name
	 */
	public Class(String sClassName) {
		this.name = sClassName;
	}
	
	/**
	 * add a rank to class
	 * @param sPermName the rank permissions name
	 * @param sDispName the rank display name
	 * @param cColor the rank color
	 */
	public void add(String sPermName, String sDispName, ChatColor cColor) {
		this.ranks.add(new Rank(sPermName, sDispName, cColor, this));
	}
	
	/**
	 * remove a string from class
	 * @param sPermName the rank name to remove
	 */
	public void remove(String sPermName) {
		this.ranks.remove(sPermName);
	}
}
