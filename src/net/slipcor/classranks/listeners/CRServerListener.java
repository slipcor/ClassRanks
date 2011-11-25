package net.slipcor.classranks.listeners;

import java.util.logging.Level;

import net.slipcor.classranks.ClassRanks;
import net.slipcor.classranks.register.payment.Methods;

import org.bukkit.Bukkit;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

/*
 * server listener class
 * 
 * v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * History:
 * 
 *     v0.1.6 - cleanup
 * 
 * @author slipcor
 */

public class CRServerListener extends ServerListener {
	private final Methods methods;
	private final ClassRanks plugin;

    public CRServerListener(ClassRanks plugin) {
    	this.plugin = plugin;
    	this.methods = new Methods();
    }

    @Override
    public void onPluginDisable(PluginDisableEvent event) {
        // Check to see if the plugin thats being disabled is the one we are using
        if (this.methods != null && Methods.hasMethod()) {
            Boolean check = Methods.checkDisabled(event.getPlugin());

            if(check) {
                plugin.method = null;
                plugin.log("</3 eConomy",Level.INFO);
            }
        }
    }

	@Override
    public void onPluginEnable(PluginEnableEvent event) {
        // Check to see if we need a payment method
        if (!Methods.hasMethod()) {
            if(Methods.setMethod(Bukkit.getServer().getPluginManager())) {
                plugin.method = Methods.getMethod();
                plugin.log("<3 " + plugin.method.getName() + " version: " + plugin.method.getVersion(),Level.INFO);
            } else {
		       	plugin.log("</3 eConomy",Level.INFO);
            }
        }
    }
}