/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.trigger;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardPredicates;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;

public class TriggerFightOnce extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Fight.
     * </p>
     *
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerFightOnce(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        @SuppressWarnings("unchecked")
        final List<Card> fighters = (List<Card>) runParams.get(AbilityKey.Fighters);

        if (hasParam("ValidCard")) {
            if (!Iterables.any(fighters,
                    CardPredicates.restriction(getParam("ValidCard").split(","),
                        getHostCard().getController(), getHostCard(), null))) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Fighters);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked")
        List<Card> fighters = (List<Card>)sa.getTriggeringObject(AbilityKey.Fighters);
        sb.append(Localizer.getInstance().getMessage("lblFighter")).append(" 1: ").append(fighters.get(0)).append(", ");
        sb.append(Localizer.getInstance().getMessage("lblFighter")).append(" 2: ").append(fighters.get(1));
        return sb.toString();
    }
}

