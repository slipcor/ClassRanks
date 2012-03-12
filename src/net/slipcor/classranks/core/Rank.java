package net.slipcor.classranks.core;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

/**
 * rank class
 * 
 * @version v0.3.0 
 * 
 * @author slipcor
 */

public class Rank {
	String sPermissionName;
	String sDisplayName;
	ChatColor cColor;
	Class crcSuper;

	ItemStack[] items;
	private Double cost;
	int exp;

	/**
	 * create a rank instance
	 * 
	 * @param sPermName
	 *            the permission name to use
	 * @param sDispName
	 *            the display name
	 * @param cC
	 *            the name ChatColor
	 * @param crc
	 *            the class to add to
	 * @param isItems
	 *            the rank items cost
	 * @param dCost
	 *            the rank cost
	 * @param iExp
	 *            the rank experience cost
	 */
	public Rank(String sPermName, String sDispName, ChatColor cC, Class crc,
			ItemStack[] isItems, double dCost, int iExp) {
		this.sPermissionName = sPermName;
		this.sDisplayName = sDispName;
		this.cColor = cC;
		this.crcSuper = crc;
		this.items = isItems;
		this.cost = dCost;
		this.exp = iExp;
	}

	/**
	 * hand over display name
	 * 
	 * @return the display name
	 */
	public String getDispName() {
		return this.sDisplayName;
	}

	/**
	 * hand over the color
	 * 
	 * @return the color
	 */
	public ChatColor getColor() {
		return this.cColor;
	}

	/**
	 * hand over the class
	 * 
	 * @return the class
	 */
	public Class getSuperClass() {
		return this.crcSuper;
	}

	/**
	 * hand over the permission name
	 * 
	 * @return the permission name
	 */
	public String getPermName() {
		return sPermissionName;
	}

	/**
	 * update the display name
	 * 
	 * @param sDisplayName
	 *            the display to use
	 */
	public void setDispName(String sDisplayName) {
		this.sDisplayName = sDisplayName;
	}

	/**
	 * update the color
	 * 
	 * @param cColor
	 *            the color to use
	 */
	public void setColor(ChatColor cColor) {
		this.cColor = cColor;
	}

	public Double getCost() {
		return cost;
	}
	
	public ArrayList<String> getItems() {
		if (items == null) {
			return null;
		}
		ArrayList<String> result = new ArrayList<String>();
		for (ItemStack i : items) {
			if (i == null) {
				continue;
			}
			
			if (i.getDurability() != 0) {
				result.add(i.getType().toString() + ":" + i.getDurability() + ":"+i.getAmount());
			} else {
				if (i.getAmount() != 0) {
					result.add(i.getType().toString() + ":"+i.getAmount());
				} else {
					result.add(i.getType().toString());
				}
			}
		}
		return result;
	}
	
	public int getExp() {
		return exp;
	}

	public ItemStack[] getItemstacks() {
		return items;
	}

	public void debugPrint() {
		//return;
		/*
		 * 
		System.out.print("--"+sPermissionName+"--");
		System.out.print(sDisplayName);
		System.out.print(cColor+"color");
		if (items == null) {
			System.out.print("null");
		} else
			System.out.print(FormatManager.formatItemStacks(items));
		System.out.print(cost);
		System.out.print(exp);
		
		 */
		
	}
}
