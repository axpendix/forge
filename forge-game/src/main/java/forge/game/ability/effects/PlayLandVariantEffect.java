package forge.game.ability.effects;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.card.CardUtil;
import forge.game.event.GameEventLandPlayed;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;

import java.util.List;

public class PlayLandVariantEffect extends SpellAbilityEffect {

    @Override
    public void resolve(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = source.getGame();
        final String landType = sa.getParam("Clone");
        List<PaperCard> cards = Lists.newArrayList(StaticData.instance().getCommonCards().getUniqueCards());
        if ("BasicLand".equals(landType)) {
            final Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, PaperCard.FN_GET_RULES);
            cards = Lists.newArrayList(Iterables.filter(cards, cpp));
        }
        // current color of source card
        final ColorSet color = CardUtil.getColors(source);
        if (color.isColorless()) {
            return;
        }
        // find basic lands that can produce mana of one of the card's colors
        final List<String> landNames = Lists.newArrayList();
        for (byte i = 0; i < MagicColor.WUBRG.length; i++) {
            if (color.hasAnyColor(MagicColor.WUBRG[i])) {
                landNames.add(MagicColor.Constant.BASIC_LANDS.get(i));
                landNames.add(MagicColor.Constant.SNOW_LANDS.get(i));
            }
        }

        final Predicate<PaperCard> cp = Predicates.compose(new Predicate<String>() {
            @Override
            public boolean apply(final String name) {
                return landNames.contains(name);
            }
        }, PaperCard.FN_GET_NAME);
        cards = Lists.newArrayList(Iterables.filter(cards, cp));
        // get a random basic land
        PaperCard ran = Aggregates.random(cards);
        Card random = CardFactory.getCard(ran, activator, source.getGame());
        // if activator cannot play the random land, loop
        while (!activator.canPlayLand(random, false) && !cards.isEmpty()) {
            cards.remove(ran);
            if (cards.isEmpty()) return;
            ran = Aggregates.random(cards);
            random = CardFactory.getCard(ran, activator, game);
        }

        source.addCloneState(CardFactory.getCloneStates(random, source, sa), game.getNextTimestamp());
        source.updateStateForView();

        source.setController(activator, 0);
        game.getAction().moveTo(activator.getZone(ZoneType.Battlefield), source, sa);

        // play a sound
        game.fireEvent(new GameEventLandPlayed(activator, source));
        
        // Run triggers
        game.getTriggerHandler().runTrigger(TriggerType.LandPlayed, AbilityKey.mapFromCard(source), false);
        game.getStack().unfreezeStack();
        activator.addLandPlayedThisTurn();
    }
}
