package fr.needaname.mc_rpg_mod.effect;

import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectDispatcherTest {

    private static EffectHandler handlerFor(EffectType type) {
        return () -> type;
    }

    @Test
    void registerAndLookup() {
        EffectDispatcher table = new EffectDispatcher();
        EffectHandler attribute = handlerFor(EffectType.ATTRIBUTE);
        table.register(attribute);

        assertTrue(table.isHandled(EffectType.ATTRIBUTE));
        assertFalse(table.isHandled(EffectType.FLAG));
        assertEquals(attribute, table.handler(EffectType.ATTRIBUTE).orElseThrow());
        assertTrue(table.handler(EffectType.FLAG).isEmpty());
        assertEquals(1, table.handledTypes().size());
    }

    @Test
    void doubleRegistrationIsRejected() {
        EffectDispatcher table = new EffectDispatcher();
        table.register(handlerFor(EffectType.AIR));
        assertThrows(IllegalStateException.class, () -> table.register(handlerFor(EffectType.AIR)));
    }
}
