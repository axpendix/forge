package forge.game.mana;

import junit.framework.TestCase;

import static forge.card.MagicColor.*;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;

public class ManaCostBeingPaidTest extends TestCase {
    public void testPayManaViaConvoke() {
        runConvokeTest("1 W W", new byte[] { WHITE, COLORLESS, WHITE }, new String[] {"{1}{W}{W}", "{1}{W}", "{W}"});
        runConvokeTest("1 W W", new byte[] { COLORLESS, WHITE, WHITE }, new String[] {"{1}{W}{W}", "{W}{W}", "{W}"});
        runConvokeTest("1 W W", new byte[] { GREEN, WHITE, WHITE }, new String[] {"{1}{W}{W}", "{W}{W}", "{W}"});
        runConvokeTest("1 W G", new byte[] { GREEN, RED, WHITE }, new String[] {"{1}{W}{G}", "{1}{W}", "{W}"});
    }

    private void runConvokeTest(String initialCost, byte[] colorsToPay, String[] expectedRemainder) {
        ManaCostBeingPaid cost = createManaCostBeingPaid(initialCost);
        for (int i = 0; i < colorsToPay.length; i++) {
            assertEquals(expectedRemainder[i], cost.toString());
            cost.payManaViaConvoke(colorsToPay[i]);
        }
        assertEquals("0", cost.toString());
    }

    private ManaCostBeingPaid createManaCostBeingPaid(String cost) {
        ManaCostParser parser = new ManaCostParser(cost);
        return new ManaCostBeingPaid(new ManaCost(parser));
    }
}
