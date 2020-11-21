package com.robomwm.generatorofgenerators;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Created on 11/18/2020.
 * A lot of inspiration from MetaGenerator: https://dev.bukkit.org/projects/metagenerator
 * @author RoboMWM
 */
public class SurvivalGenerator extends ChunkGenerator
{
    private Logger logger;

    private String[] GeneratorPluginNames = new String[] {"CityWorld", "WellWorld", "MaxiWorld", "DungeonMaze"};

    Map<String, ChunkGenerator> generators = new HashMap<>();

    public SurvivalGenerator(Plugin plugin, String worldName, String id)
    {
        this.logger = plugin.getLogger();
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        logger.info("Booting SurvivalGenerator");

        for (String pluginName : GeneratorPluginNames)
        {
            Plugin genPlugin = pluginManager.getPlugin(pluginName);
            if (!addGenerator(genPlugin, pluginName, worldName, id))
            {
                logger.severe("SurvivalGenerator: Failed to add " + pluginName);
            }
        }
    }

    private boolean addGenerator(Plugin plugin, String name, String worldName, String id)
    {
        if (plugin == null)
        {
            String error = "Cannot find plugin for generator " + name;
            logger.severe(error);
            return false;
        }

        return generators.put(name, plugin.getDefaultWorldGenerator(worldName, id)) == null;
    }

    private ChunkGenerator getGenerator(int chunkX, int chunkZ)
    {
        int regionX = chunkX / 128;
        int regionZ = chunkZ / 128;

        if (chunkX < 0)
            regionX--;
        if (regionZ < 0)
            regionZ--;

        int section = Math.abs(regionX + regionZ) % (GeneratorPluginNames.length + 1); //another lazy name except this one idk what I should name it
        StackTraceElement e = Thread.currentThread().getStackTrace()[2];
        logger.info("MD: x" + chunkX + " z:" + chunkZ + " regionX:" + regionX + " regionZ:" + regionZ + " section:" + section + " trace:" + e.getClassName() + "#" + e.getMethodName() + "@" + e.getLineNumber());

        if (GeneratorPluginNames.length <= section)
        {
            logger.info("MD: using:NONE");
            return null;
        }

        logger.info("MD: using:" + GeneratorPluginNames[section]);
        return generators.get(GeneratorPluginNames[section]);
    }

    @Override
    public boolean canSpawn(World world, int x, int z)
    {
        ChunkGenerator generator = getGenerator(x, z);
        if (generator != null)
            return generator.canSpawn(world, x, z);

        return super.canSpawn(world, x, z);
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome)
    {
        ChunkGenerator generator = getGenerator(x, z);
        if (generator != null)
            return generator.generateChunkData(world, random, x, z, biome);

        return super.createVanillaChunkData(world, x, z);
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world)
    {
        List<BlockPopulator> populators = new ArrayList<BlockPopulator>();
        populators.add(new SurvivalPopulator());
        return populators;
    }

    class SurvivalPopulator extends BlockPopulator
    {
        @Override
        public void populate(World world, Random random, Chunk source)
        {
            ChunkGenerator generator = getGenerator(source.getX(), source.getZ());
            if (generator != null)
                for(BlockPopulator populator : generator.getDefaultPopulators(world))
                    populator.populate(world, random, source);
        }
    }

    //Probably not safe if we use the vanilla generator anyways, but according to electronicboy:
    //[17:20:23] +DiscordBot: <zzzCat> yes, but no real effect right now
    @Override
    public boolean isParallelCapable()
    {
//        for (ChunkGenerator generator : generators.values())
//        {
//            if (!generator.isParallelCapable())
//                return false;
//        }
//
//        logger.info("isParallelCapable is true!!! :o Async away!!!!");
        return true;
    }

    //I wonder how these will interfere with the other generators. Let's find out c:
    //Yup it messes 'em up alright, especially caves
    //gonna disable tho cuz generating vanilla features consume quite a bit

    @Override
    public boolean shouldGenerateCaves()
    {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations()
    {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures()
    {
        return false;
    }
}
