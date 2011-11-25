package net.slipcor.classranks.core;

import java.util.ArrayList;

import org.bukkit.ChatColor;

/*
 * class class
 * 
 * v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * History:
 * 
 *     v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * @author slipcor
 */

public class Class {
	public final ArrayList<Rank> ranks = new ArrayList<Rank>();
	public String name;
	
	public Class(String sClassName) {
		this.name = sClassName;
	}

	public void add(String sPermName, String sDispName, ChatColor cColor) {
		this.ranks.add(new Rank(sPermName, sDispName, cColor, this));
	}
	
	public void remove(String sPermName) {
		this.ranks.remove(sPermName);
	}
}
