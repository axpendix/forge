package forge.game.ability.effects;

import com.google.common.collect.Maps;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;

import java.util.Map;

public class ImmediateTriggerEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("TriggerDescription")) {
            return sa.getParam("TriggerDescription");
        }

        return "";

    }

    @Override
    public void resolve(SpellAbility sa) {
        Map<String, String> mapParams = Maps.newHashMap(sa.getMapParams());

        mapParams.remove("Cost");

        if (mapParams.containsKey("SpellDescription")) {
            mapParams.put("TriggerDescription", mapParams.get("SpellDescription"));
            mapParams.remove("SpellDescription");
        }

        String triggerRemembered = null;

        // Set Remembered
        if (sa.hasParam("RememberObjects")) {
            triggerRemembered = sa.getParam("RememberObjects");
        }

        mapParams.put("Mode", TriggerType.Immediate.name());

        final Trigger immediateTrig = TriggerHandler.parseTrigger(mapParams, sa.getHostCard(), sa.isIntrinsic());

        // Need to copy paid costs

        if (triggerRemembered != null) {
            for (final String rem : triggerRemembered.split(",")) {
                for (final Object o : AbilityUtils.getDefinedObjects(sa.getHostCard(), rem, sa)) {
                    if (o instanceof SpellAbility) {
                        // "RememberObjects$ Remembered" don't remember spellability
                        continue;
                    }
                    immediateTrig.addRemembered(o);
                }
            }
        }

        if (sa.hasParam("RememberDefinedNumber")) {
            immediateTrig.addRemembered((Integer) AbilityUtils.calculateAmount(sa.getHostCard(),
                    sa.getParam("RememberDefinedNumber"), sa));
        }

        if (mapParams.containsKey("Execute") || sa.hasAdditionalAbility("Execute")) {
            AbilitySub overridingSA = sa.getAdditionalAbility("Execute");
            overridingSA.setActivatingPlayer(sa.getActivatingPlayer());
            // need to set Parent to null, otherwise it might have wrong root ability
            overridingSA.setParent(null);

            if (sa.hasParam("CopyTriggeringObjects")) {
                overridingSA.setTriggeringObjects(sa.getTriggeringObjects());
            }

            immediateTrig.setOverridingAbility(overridingSA);
        }
        final TriggerHandler trigHandler  = sa.getActivatingPlayer().getGame().getTriggerHandler();

        // Instead of registering this, add to the delayed triggers as an immediate trigger type? Which means it'll fire as soon as possible
        trigHandler.registerDelayedTrigger(immediateTrig);
    }
}
