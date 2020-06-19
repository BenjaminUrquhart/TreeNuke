package net.benjaminurquhart.treenuke;

import org.bukkit.plugin.java.JavaPlugin;

public class TreeNuke extends JavaPlugin {

	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(new Nuker(), this);
	}
}
