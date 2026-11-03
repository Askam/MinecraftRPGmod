package fr.needaname.mc_rpg_mod.effect.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.needaname.mc_rpg_mod.effect.ActiveEffect;
import fr.needaname.mc_rpg_mod.skill.model.Effect;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlagMultipliersTest {

    private static ActiveEffect flag(String json) {
        JsonObject fields = JsonParser.parseString(json).getAsJsonObject();
        return new ActiveEffect("owner", new Effect(EffectType.FLAG, fields), false, Optional.empty());
    }

    @Test
    void productOfMatchingMultipliers() {
        List<ActiveEffect> effects = List.of(
                flag("{ \"flag\": \"dig_speed\", \"multiplier\": 1.1 }"),
                flag("{ \"flag\": \"dig_speed\", \"multiplier\": 1.2 }"),
                flag("{ \"flag\": \"durability\", \"multiplier\": 1.15 }"));
        assertEquals(1.1 * 1.2, FlagMultipliers.product(effects, "dig_speed"), 1e-9);
        assertEquals(1.15, FlagMultipliers.product(effects, "durability"), 1e-9);
    }

    @Test
    void absentFlagGivesNeutralOne() {
        assertEquals(1.0, FlagMultipliers.product(List.of(), "dig_speed"), 1e-9);
    }

    @Test
    void flagWithoutMultiplierIsIgnored() {
        List<ActiveEffect> effects = List.of(flag("{ \"flag\": \"no_leaf_break\" }"));
        assertEquals(1.0, FlagMultipliers.product(effects, "no_leaf_break"), 1e-9);
    }
}
