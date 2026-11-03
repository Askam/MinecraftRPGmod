package fr.needaname.mc_rpg_mod.skill.model;

import java.util.List;

/**
 * Un filtre de blocs / items / entités : une ou plusieurs entrées, chacune un id
 * ({@code minecraft:bone_meal}) ou un tag préfixé par {@code #} ({@code #forge:ores}).
 * La résolution en {@code TagKey}/id vanilla se fait dans une étape ultérieure : ici on
 * ne garde que les chaînes brutes, pour que le modèle reste testable sans Minecraft.
 */
public record Filter(List<String> entries) {
    public Filter {
        entries = List.copyOf(entries);
    }

    /** Une entrée commençant par {@code #} désigne un tag. */
    public static boolean isTag(String entry) {
        return entry.startsWith("#");
    }
}
