package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class LifeExchangeVariantEffect extends SpellAbilityEffect {

    // *************************************************************************
    // ************************ EXCHANGE LIFE **********************************
    // *************************************************************************



    // *************************************************************************
    // ************************* LOSE LIFE *************************************
    // *************************************************************************

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Player activatingPlayer = sa.getActivatingPlayer();
        final String mode = sa.getParam("Mode");

        sb.append(activatingPlayer).append(" exchanges life totals with ");
        sb.append(sa.getHostCard());
        sb.append("'s ");
        sb.append(mode.toLowerCase());

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final String mode = sa.getParam("Mode");
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (tgtPlayers.isEmpty()) {
            return;
        }

        Player p = tgtPlayers.get(0);

        Integer power = null;
        Integer toughness = null;

        final Game game = p.getGame();
        final long timestamp = game.getNextTimestamp();

        final int pLife = p.getLife();
        int num = 0;

        if ("Power".equals(mode)) {
            num = source.getNetPower();
            power = pLife;
        } else if ("Toughness".equals(mode)) {
            num = source.getNetToughness();
            toughness = pLife;
        } else {
            return;
        }

        if (!source.isInZone(ZoneType.Battlefield)) {
            return;
        }

        if ((pLife > num) && p.canLoseLife()) {
            final int diff = pLife - num;
            p.loseLife(diff);
            source.addNewPT(power, toughness, timestamp);
            game.fireEvent(new GameEventCardStatsChanged(source));
        } else if ((num > pLife) && p.canGainLife()) {
            final int diff = num - pLife;
            p.gainLife(diff, source, sa);
            source.addNewPT(power, toughness, timestamp);
            game.fireEvent(new GameEventCardStatsChanged(source));
        } else {
            //  do nothing if they are equal
        }
    }

}
