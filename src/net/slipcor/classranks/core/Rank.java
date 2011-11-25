package net.slipcor.classranks.core;

import org.bukkit.ChatColor;

/*
 * rank class
 * 
 * v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * History:
 * 
 *     v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * @author slipcor
 */

public class Rank {
	String sPermissionName;
	String sDisplayName;
	ChatColor cColor;
	Class crcSuper;
	
	public Rank(String sPermName, String sDispName, ChatColor cC, Class crc) {
		this.sPermissionName = sPermName;
		this.sDisplayName = sDispName;
		this.cColor = cC;
		this.crcSuper = crc;
	}
	
	public String getPermName() {
		return this.getPermissionName();
	}
	
	public String getDispName() {
		return this.sDisplayName;
	}
	
	public ChatColor getColor() {
		return this.cColor;
	}
	
	public Class getSuperClass() {
		return this.crcSuper;
	}

	public String getPermissionName() {
		return sPermissionName;
	}

	public void setDispName(String sDisplayName) {
		this.sDisplayName = sDisplayName;
	}

	public void setColor(ChatColor cColor) {
		this.cColor = cColor;
	}
}
