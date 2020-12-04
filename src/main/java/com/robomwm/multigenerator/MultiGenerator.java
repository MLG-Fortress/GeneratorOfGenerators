package com.robomwm.multigenerator;

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
public class MultiGenerator extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        List<String> defaultGenerators = new ArrayList<>();
        defaultGenerators.add("CityWorld");
        defaultGenerators.add("CityWorld,METRO");
        defaultGenerators.add("WellWorld");
        getConfig().addDefault("default.generators", defaultGenerators);
        getConfig().addDefault("default.cellLengthInChunks", 64);
        getConfig().addDefault("default.vanillaCaves", false);
        getConfig().addDefault("default.vanillaDecorators", false);
        getConfig().addDefault("default.vanillaStructures", false);
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
    {
        if (id == null)
            return new SquareGridGenerator(this, worldName, id);

        switch (id)
        {
            case "SequentialGrid":
                return new SquareGridGenerator(this, worldName, id);
            default:
                return new SquareGridGenerator(this, worldName, id);
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
