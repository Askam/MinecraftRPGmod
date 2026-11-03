package fr.needaname.mc_rpg_mod.skill.player;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La classe de données est pure : on peut vérifier l'aller-retour NBT et la recopie sans démarrer
 * Minecraft (le NBT vanilla se charge seul). C'est ce qui garantit que les données survivent au
 * respawn — le point le plus coûteux des pièges connus.
 */
class PlayerSkillsTest {

    private static PlayerSkills sample() {
        PlayerSkills s = new PlayerSkills();
        s.setLevel("combat", 12);
        s.setXp("combat", 3456L);
        s.setLevel("mort", 3);
        s.addNode("poigne");
        s.addNode("riposte");
        s.setPowerSlot("R", "tourbillon");
        s.setLastUsed("tourbillon", 240000L);
        s.setStatBaseline("minecraft:sprint_one_cm", 123456L);
        s.addActiveTicks(600);
        return s;
    }

    @Test
    void nbtRoundTripPreservesEverything() {
        CompoundTag tag = sample().save();
        PlayerSkills reloaded = new PlayerSkills();
        reloaded.load(tag);

        assertEquals(12, reloaded.level("combat"));
        assertEquals(3456L, reloaded.xp("combat"));
        assertEquals(3, reloaded.level("mort"));
        assertEquals(0, reloaded.level("inconnu"));
        assertTrue(reloaded.hasNode("poigne"));
        assertTrue(reloaded.hasNode("riposte"));
        assertFalse(reloaded.hasNode("absent"));
        assertEquals("tourbillon", reloaded.powerSlot("R").orElseThrow());
        assertTrue(reloaded.powerSlot("G").isEmpty());
        assertEquals(240000L, reloaded.lastUsed("tourbillon").orElseThrow());
        assertTrue(reloaded.lastUsed("jamais").isEmpty());
        assertEquals(123456L, reloaded.statBaseline("minecraft:sprint_one_cm"));
        assertEquals(0L, reloaded.statBaseline("minecraft:walk_one_cm"));
        assertEquals(600, reloaded.activeTicksSinceDeath());
    }

    @Test
    void copyFromReplacesState() {
        PlayerSkills target = new PlayerSkills();
        target.setLevel("natation", 99);
        target.addNode("obsolete");

        target.copyFrom(sample());

        // L'état précédent de la cible est entièrement remplacé.
        assertEquals(0, target.level("natation"));
        assertFalse(target.hasNode("obsolete"));
        assertEquals(12, target.level("combat"));
        assertTrue(target.hasNode("poigne"));
        assertEquals("tourbillon", target.powerSlot("R").orElseThrow());
        assertEquals(123456L, target.statBaseline("minecraft:sprint_one_cm"));
        assertEquals(600, target.activeTicksSinceDeath());
    }

    @Test
    void loadResetsPreviousState() {
        PlayerSkills s = sample();
        s.load(new CompoundTag());
        assertEquals(0, s.level("combat"));
        assertTrue(s.nodes().isEmpty());
        assertTrue(s.powerSlots().isEmpty());
        assertTrue(s.cooldowns().isEmpty());
    }
}
