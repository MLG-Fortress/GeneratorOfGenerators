package com.robomwm.multigenerator;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Created on 11/18/2020.
 * A lot of inspiration from MetaGenerator: https://dev.bukkit.org/projects/metagenerator
 * @author RoboMWM
 */
public class SquareGridGenerator extends ChunkGenerator
{
    private Logger logger;
    private Plugin plugin;
    private int gridLength = 1;
    private int cellSize;
    private boolean vanillaCaves;
    private boolean vanillaDecorations;
    private boolean vanillaStructures;

    private ArrayList<ChunkGenerator> generators = new ArrayList<>();

    public SquareGridGenerator(Plugin plugin, String worldName, String id)
    {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        logger.info("Booting " + this.getClass().getSimpleName());

        plugin.reloadConfig();

        if (id == null || id.isEmpty())
            id = "default";

        ConfigurationSection section = plugin.getConfig().getConfigurationSection(id);

        if (section == null)
            throw new RuntimeException("[MultiGenerator] " + id + " is not an ID specified in the config.yml");

        cellSize = section.getInt("cellSize");
        vanillaCaves = section.getBoolean("vanillaCaves", false);
        vanillaDecorations = section.getBoolean("vanillaDecorations", false);
        vanillaStructures = section.getBoolean("vanillaStructures", false);

        logger.info("Generating Vanilla Caves: " + vanillaCaves);
        logger.info("Generating Vanilla Decorations: " + vanillaDecorations);
        logger.info("Generating Vanilla Structures: " + vanillaStructures);

        for (String generatorName : section.getStringList("generators"))
        {
            String[] splitName = generatorName.split(",");
            String pluginName = splitName[0];
            String configId = null;
            if (splitName.length > 1)
                configId = splitName[1];

            Plugin genPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
            if (!addGenerator(genPlugin, pluginName, worldName, configId))
                logger.severe("SurvivalGenerator: Failed to add " + pluginName);
        }

        if (generators.isEmpty())
            throw new RuntimeException("[MultiGenerator] No Generators found (or successfully loaded) for ID " + id + ", please check config.yml's " + id + " section and the generator plugins.");

        //Increase our square grid size until it's large enough to accommodate all generators
        while (gridLength * gridLength < generators.size())
            gridLength++;
        logger.info("Mapping generator cells to a " + gridLength + "x" + gridLength + " grid.");
        logger.info("Each cell is " + cellSize * 16 + "x" + cellSize * 16);
    }

    private boolean addGenerator(Plugin plugin, String name, String worldName, String id)
    {
        if (plugin == null)
        {
            String error = "Cannot find plugin for generator " + name;
            logger.severe(error);
            return false;
        }

        if (id == null)
            logger.info("Adding generator " + name);
        else
            logger.info("Adding generator " + name + " with id " + id);

        return generators.add(plugin.getDefaultWorldGenerator(worldName, id));
    }

    //Choose a generator based off chunk coordinates
    private ChunkGenerator getGenerator(int chunkX, int chunkZ)
    {
        //This will mirror the grid across both x and y axes
        chunkX = Math.abs(chunkX);
        chunkZ = Math.abs(chunkZ);

        //Convert chunk coordinates to our custom grid coordinates
        int regionX = chunkX / cellSize;
        int regionZ = (chunkZ / cellSize) * gridLength;
        int cellIndex = (regionX + regionZ) % (generators.size());

        StackTraceElement e = Thread.currentThread().getStackTrace()[3];
        logger.info("SG" + cellIndex + ": " + generators.get(cellIndex).getClass().getSimpleName() + " x:" + chunkX + " z:" + chunkZ + " regionX:" + regionX + " regionZ:" + regionZ + " trace:" + e.getClassName() + "#" + e.getMethodName() + "@" + e.getLineNumber());

        return generators.get(cellIndex);
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
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace.length > 900)
            {
                logger.info("SG: Stack too large, terminating populator calls. Size: " + stackTrace.length);
                return;
            }

            ChunkGenerator generator = getGenerator(source.getX(), source.getZ());
                if (generator != null)
                    for(BlockPopulator populator : generator.getDefaultPopulators(world))
                        try
                        {
                            populator.populate(world, random, source);
                        }
                        catch (IllegalArgumentException e)
                        {
                            logger.info(e.getMessage());
                            int length = e.getStackTrace().length;
                            e.printStackTrace();
                            if (length > 3)
                            {
                                String name = e.getStackTrace()[3].getClassName();
                                int line = e.getStackTrace()[3].getLineNumber();
                                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "communicationconnector The " + name + " errored because of " + e.getMessage() + ". It happened at line " + line);
                            }
                        }
        }
    }

    //Probably not safe if we use the vanilla generator anyways, but according to electronicboy:
    //[17:20:23] +DiscordBot: <zzzCat> yes, but no real effect right now
    //May be cause of stack overflow tho https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/generator/CustomChunkGenerator.java#118
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
        return false;
    }

    //I wonder how these will interfere with the other generators. Let's find out c:
    //Yup it messes 'em up alright, especially caves
    //gonna disable tho cuz generating vanilla features consume quite a bit

    @Override
    public boolean shouldGenerateCaves()
    {
        return vanillaCaves;
    }

    @Override
    public boolean shouldGenerateDecorations()
    {
        return vanillaDecorations;
    }

    @Override
    public boolean shouldGenerateStructures()
    {
        return vanillaStructures;
    }
}
