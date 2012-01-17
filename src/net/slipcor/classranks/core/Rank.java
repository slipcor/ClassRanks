package net.slipcor.classranks.core;

import org.bukkit.ChatColor;

/**
 * rank class
 * 
 * @version v0.2.0
 * 
 * @author slipcor
 */

public class Rank {
	String sPermissionName;
	String sDisplayName;
	ChatColor cColor;
	Class crcSuper;
	
	/**
	 * create a rank instance
	 * @param sPermName the permission name to use
	 * @param sDispName the display name
	 * @param cC the name ChatColor
	 * @param crc the class to add to
	 */
	public Rank(String sPermName, String sDispName, ChatColor cC, Class crc) {
		this.sPermissionName = sPermName;
		this.sDisplayName = sDispName;
		this.cColor = cC;
		this.crcSuper = crc;
	}
	
	/**
	 * hand over display name
	 * @return the display name
	 */
	public String getDispName() {
		return this.sDisplayName;
	}
	
	/**
	 * hand over the color
	 * @return the color
	 */
	public ChatColor getColor() {
		return this.cColor;
	}
	
	/**
	 * hand over the class
	 * @return the class
	 */
	public Class getSuperClass() {
		return this.crcSuper;
	}
	
	/**
	 * hand over the permission name
	 * @return the permission name
	 */
	public String getPermName() {
		return sPermissionName;
	}
	
	/**
	 * update the display name
	 * @param sDisplayName the display to use
	 */
	public void setDispName(String sDisplayName) {
		this.sDisplayName = sDisplayName;
	}
	
	/**
	 * update the color
	 * @param cColor the color to use
	 */
	public void setColor(ChatColor cColor) {
		this.cColor = cColor;
	}
}
