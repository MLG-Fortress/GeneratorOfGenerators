package com.robomwm.generatorofgenerators;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created on 11/20/2020.
 *
 * @author RoboMWM
 */
public class GeneratorOfGenerators extends JavaPlugin
{
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
}
