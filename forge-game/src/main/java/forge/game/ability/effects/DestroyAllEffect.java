package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

public class DestroyAllEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {

        if (sa.hasParam("SpellDescription")) {
            return sa.getParam("SpellDescription");
        }

        final StringBuilder sb = new StringBuilder();
        final boolean noRegen = sa.hasParam("NoRegen");
        sb.append(sa.getHostCard().getName()).append(" - Destroy permanents.");

        if (noRegen) {
            sb.append(" They can't be regenerated");
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {

        final boolean noRegen = sa.hasParam("NoRegen");
        final Card card = sa.getHostCard();
        final Game game = sa.getActivatingPlayer().getGame();

        Player targetPlayer = sa.getTargets().getFirstTargetedPlayer();

        String valid = "";

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        // Ugh. If calculateAmount needs to be called with DestroyAll it _needs_
        // to use the X variable
        // We really need a better solution to this
        if (valid.contains("X")) {
            valid = TextUtil.fastReplace(valid,
                    "X", Integer.toString(AbilityUtils.calculateAmount(card, "X", sa)));
        }

        CardCollectionView list = game.getCardsIn(ZoneType.Battlefield);

        if (targetPlayer != null) {
            list = CardLists.filterControlledBy(list, targetPlayer);
        }

        list = AbilityUtils.filterListByType(list, valid, sa);

        final boolean remDestroyed = sa.hasParam("RememberDestroyed");
        if (remDestroyed) {
            card.clearRemembered();
        }

        if (sa.hasParam("RememberAllObjects")) {
            card.addRemembered(list);
        }

        // exclude cards that can't be destroyed at this moment
        list = CardLists.filter(list, CardPredicates.Presets.CAN_BE_DESTROYED);

        if (list.size() > 1) {
            list = GameActionUtil.orderCardsByTheirOwners(game, list, ZoneType.Graveyard);
        }

        CardZoneTable table = new CardZoneTable();

        for (Card c : list) {
            if (game.getAction().destroy(c, sa, !noRegen, table) && remDestroyed) {
                card.addRemembered(CardUtil.getLKICopy(c));
            }
        }
        table.triggerChangesZoneAll(game);
    }

}
