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
package forge.game.cost;

import com.google.common.base.Predicate;
import forge.game.GameLogEntryType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Class CostReveal.
 */
public class CostReveal extends CostPartWithList {
    // Reveal<Num/Type/TypeDescription>

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;

    private ZoneType revealFrom = ZoneType.Hand;

    public CostReveal(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    public CostReveal(final String amount, final String type, final String description, final ZoneType zoneType) {
        super(amount, type, description);
        this.revealFrom = zoneType;
    }

    @Override
    public boolean isReusable() { return true; }

    @Override
    public boolean isRenewable() { return true; }

    public ZoneType getRevealFrom() {
        return revealFrom;
    }

    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer) {
        final Card source = ability.getHostCard();

        CardCollectionView handList = payer.getCardsIn(revealFrom);
        final String type = this.getType();
        final Integer amount = this.convertAmount();

        if (this.payCostFromSource()) {
            return source.isInZone(revealFrom);
        } else if (this.getType().equals("Hand")) {
            return true;
        } else if (this.getType().equals("SameColor")) {
            if (amount == null) {
                return false;
            } else {
                for (final Card card : handList) {
                    if (CardLists.filter(handList, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return c.sharesColorWith(card);
                        }
                    }).size() >= amount) {
                        return true;
                    }
                }
                return false;
            }
        } else {
            if (ability.isSpell()) {
                CardCollection modifiedHand = new CardCollection(handList);
                modifiedHand.remove(source); // can't pay for itself
                handList = modifiedHand;
            }
            handList = CardLists.getValidCards(handList, type.split(";"), payer, source, ability);
            // not enough cards in hand to pay
            return (amount == null) || (amount <= handList.size());
            //System.out.println("revealcost - " + amount + type + handList);
        }

    }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Reveal ");

        final Integer i = this.convertAmount();

        if (this.payCostFromSource()) {
            sb.append(this.getType());
        } else if (this.getType().equals("Hand")) {
            return ("Reveal your hand");
        } else if (this.getType().equals("SameColor")) {
            return ("Reveal " + i + " cards from your hand that share a color");
        } else {
            final StringBuilder desc = new StringBuilder();

            if (this.getType().equals("Card")) {
                desc.append("Card");
            } else {
                desc.append(this.getTypeDescription() == null ? this.getType() : this.getTypeDescription()).append(
                        " card");
            }

            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc.toString()));
        }

        sb.append(" from your ");
        sb.append(revealFrom.getTranslatedName());

        return sb.toString();
    }

    @Override
    protected Card doPayment(SpellAbility ability, Card targetCard) {
        targetCard.getGame().getAction().reveal(new CardCollection(targetCard), ability.getActivatingPlayer());
        StringBuilder sb = new StringBuilder();
        sb.append(ability.getActivatingPlayer()).append(" reveals ").append(targetCard).append(" to pay a cost for ");
        sb.append(ability);
        targetCard.getGame().getGameLog().add(GameLogEntryType.INFORMATION, sb.toString());
        return targetCard;
    }

    @Override
    protected boolean canPayListAtOnce() {
        return true;
    }

    @Override
    protected CardCollectionView doListPayment(SpellAbility ability, CardCollectionView targetCards) {
        ability.getActivatingPlayer().getGame().getAction().reveal(targetCards, ability.getActivatingPlayer());
        return targetCards;
    }    

    @Override
    public String getHashForLKIList() {
        return "Revealed";
    }
    @Override
    public String getHashForCardList() {
    	return "RevealedCards";
    }

    // Inputs
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int paymentOrder() {
        // Caller of the Untamed needs the reveal to happen before the mana cost
        if (!revealFrom.equals(ZoneType.Hand)) { return -1; }
        return 5;
    }
}
