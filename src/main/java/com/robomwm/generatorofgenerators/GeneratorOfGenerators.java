package com.robomwm.generatorofgenerators;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 11/20/2020.
 *
 * @author RoboMWM
 */
public class GeneratorOfGenerators extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        List<String> defaultGenerators = new ArrayList<>();
        defaultGenerators.add("CityWorld");
        defaultGenerators.add("CityWorld,PILLARS");
        defaultGenerators.add("WellWorld");
        getConfig().addDefault("default", defaultGenerators);
        getConfig().addDefault("cellSize.default", 64);
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
    {
        String[] worldNameSplit = worldName.split("_");
        String suffix = worldNameSplit[worldNameSplit.length - 1];
        switch (suffix)
        {
            case "nether":
                return new SurvivalGenerator(this, worldName, id); //TODO
            case "end":
                return new SurvivalGenerator(this, worldName, id); //TODO
            default:
                return new SurvivalGenerator(this, worldName, id);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length < 3)
            return false;

        World world = getServer().getWorld(args[0]);
        if (world == null)
        {
            sender.sendMessage(ChatColor.RED + "World is null");
            return false;
        }

        world.getChunkAt(Integer.parseInt(args[1]), Integer.parseInt(args[2])); //not async so we can test spigot and etc. And it's a debug command anyway.
        return true;
    }
}
