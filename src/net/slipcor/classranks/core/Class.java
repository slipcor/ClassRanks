package net.slipcor.classranks.core;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

/**
 * class class
 * 
 * @version v0.3.0 
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
	 * @param isItems the rank items cost
	 * @param dCost the rank cost
	 * @param iExp the rank exp cost
	 */
	public void add(String sPermName, String sDispName, ChatColor cColor, ItemStack[] isItems, double dCost, int iExp) {
		this.ranks.add(new Rank(sPermName, sDispName, cColor, this, isItems, dCost, iExp));
	}
	
	/**
	 * remove a string from class
	 * @param sPermName the rank name to remove
	 */
	public void remove(String sPermName) {
		this.ranks.remove(sPermName);
	}
}
