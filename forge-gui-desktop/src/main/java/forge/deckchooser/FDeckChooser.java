package forge.deckchooser;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.google.common.base.Predicate;
import forge.deck.*;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

import forge.FThreads;
import forge.UiCommand;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerContainer;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.quest.QuestEventChallenge;
import forge.quest.QuestUtil;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;

@SuppressWarnings("serial")
public class FDeckChooser extends JPanel implements IDecksComboBoxListener {
    private DecksComboBox decksComboBox;
    private DeckType selectedDeckType;
    private ItemManagerContainer lstDecksContainer;
    private NetDeckCategory netDeckCategory;
    private boolean refreshingDeckType;
    private boolean isForCommander;

    private final DeckManager lstDecks;
    final Localizer localizer = Localizer.getInstance();

    private final FLabel btnViewDeck = new FLabel.ButtonBuilder().text(localizer.getMessage("lblViewDeck")).fontSize(14).build();
    private final FLabel btnRandom = new FLabel.ButtonBuilder().fontSize(14).build();

    private boolean isAi;

    private final ForgePreferences prefs = FModel.getPreferences();
    private FPref stateSetting = null;

    //Show dialog to select a deck
    public static Deck promptForDeck(final CDetailPicture cDetailPicture, final String title, final DeckType defaultDeckType, final boolean forAi) {
        FThreads.assertExecutedByEdt(true);
        final FDeckChooser chooser = new FDeckChooser(cDetailPicture, forAi, GameType.Constructed, false);
        chooser.initialize(defaultDeckType);
        chooser.populate();
        final Dimension parentSize = JOptionPane.getRootFrame().getSize();
        chooser.setMinimumSize(new Dimension((int)(parentSize.getWidth() / 2), (int)parentSize.getHeight() - 200));
        final Localizer localizer = Localizer.getInstance();
        final FOptionPane optionPane = new FOptionPane(null, title, null, chooser, ImmutableList.of(localizer.getMessage("lblOk"), localizer.getMessage("lblCancel")), 0);
        optionPane.setDefaultFocus(chooser);
        chooser.lstDecks.setItemActivateCommand(new UiCommand() {
            @Override
            public void run() {
                optionPane.setResult(0); //accept selected deck on double click or Enter
            }
        });
        optionPane.setVisible(true);
        final int dialogResult = optionPane.getResult();
        optionPane.dispose();
        if (dialogResult == 0) {
            return chooser.getDeck();
        }
        return null;
    }

    public FDeckChooser(final CDetailPicture cDetailPicture, final boolean forAi, GameType gameType, boolean forCommander) {
        lstDecks = new DeckManager(gameType, cDetailPicture);
        setOpaque(false);
        isAi = forAi;
        isForCommander = forCommander;
        final UiCommand cmdViewDeck = new UiCommand() {
            @Override public void run() {
                if (selectedDeckType != DeckType.COLOR_DECK && selectedDeckType != DeckType.THEME_DECK) {
                    FDeckViewer.show(getDeck());
                }
            }
        };
        lstDecks.setItemActivateCommand(cmdViewDeck);
        btnViewDeck.setCommand(cmdViewDeck);
    }

    public void initialize() {
        initialize(DeckType.COLOR_DECK);
    }
    public void initialize(final DeckType defaultDeckType) {
        initialize(null, defaultDeckType);
    }
    public void initialize(final FPref savedStateSetting, final DeckType defaultDeckType) {
        stateSetting = savedStateSetting;
        selectedDeckType = defaultDeckType;
    }

    public DeckType getSelectedDeckType() { return selectedDeckType; }
    public void setSelectedDeckType(final DeckType selectedDeckType0) {
        refreshDecksList(selectedDeckType0, false, null);
    }

    public DeckManager getLstDecks() { return lstDecks; }

    private void updateDecks(final Iterable<DeckProxy> decks, final ItemManagerConfig config) {
        lstDecks.setAllowMultipleSelections(false);

        lstDecks.setPool(decks);
        lstDecks.setup(config);

        btnRandom.setText(localizer.getMessage("lblRandomDeck"));
        btnRandom.setCommand(new UiCommand() {
            @Override
            public void run() {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });

        lstDecks.setSelectedIndex(0);
    }

    private void updateCustom() {
        DeckFormat deckFormat = lstDecks.getGameType().getDeckFormat();
        switch (deckFormat) {
        case Commander:
            updateDecks(DeckProxy.getAllCommanderDecks(), ItemManagerConfig.COMMANDER_DECKS);
            break;
        case Oathbreaker:
            updateDecks(DeckProxy.getAllOathbreakerDecks(), ItemManagerConfig.COMMANDER_DECKS);
            break;
        case Brawl:
            updateDecks(DeckProxy.getAllBrawlDecks(), ItemManagerConfig.COMMANDER_DECKS);
            break;
        case TinyLeaders:
            updateDecks(DeckProxy.getAllTinyLeadersDecks(), ItemManagerConfig.COMMANDER_DECKS);
            break;
        default:
            updateDecks(DeckProxy.getAllConstructedDecks(), ItemManagerConfig.CONSTRUCTED_DECKS);
            break;
        }
    }

    private void updateColors(Predicate<PaperCard> formatFilter) {
        lstDecks.setAllowMultipleSelections(true);

        lstDecks.setPool(ColorDeckGenerator.getColorDecks(lstDecks, formatFilter, isAi));
        lstDecks.setup(ItemManagerConfig.STRING_ONLY);

        btnRandom.setText(localizer.getMessage("lblRandomColors"));
        btnRandom.setCommand(new UiCommand() {
            @Override
            public void run() {
                DeckgenUtil.randomSelectColors(lstDecks);
            }
        });

        // default selection = basic two color deck
        lstDecks.setSelectedIndices(new Integer[]{0, 1});
    }

    private void updateMatrix(GameFormat format) {
        lstDecks.setAllowMultipleSelections(false);

        lstDecks.setPool(ArchetypeDeckGenerator.getMatrixDecks(format, isAi));
        lstDecks.setup(ItemManagerConfig.STRING_ONLY);

        btnRandom.setText("Random");
        btnRandom.setCommand(new UiCommand() {
            @Override
            public void run() {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });

        // default selection = basic two color deck
        lstDecks.setSelectedIndices(new Integer[]{0});
    }

    private void updateRandomCommander() {
        DeckFormat deckFormat = lstDecks.getGameType().getDeckFormat();
        if (!deckFormat.hasCommander()) {
            return;
        }

        lstDecks.setAllowMultipleSelections(false);
        lstDecks.setPool(CommanderDeckGenerator.getCommanderDecks(deckFormat, isAi, false));
        lstDecks.setup(ItemManagerConfig.STRING_ONLY);

        btnRandom.setText("Random");
        btnRandom.setCommand(new UiCommand() {
            @Override
            public void run() {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });

        // default selection = basic two color deck
        lstDecks.setSelectedIndices(new Integer[]{0});
    }

    private void updateRandomCardGenCommander() {
        DeckFormat deckFormat = lstDecks.getGameType().getDeckFormat();
        if (!deckFormat.hasCommander()) {
            return;
        }

        lstDecks.setAllowMultipleSelections(false);
        lstDecks.setPool(CommanderDeckGenerator.getCommanderDecks(deckFormat, isAi, true));
        lstDecks.setup(ItemManagerConfig.STRING_ONLY);

        btnRandom.setText("Random");
        btnRandom.setCommand(new UiCommand() {
            @Override
            public void run() {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });

        // default selection = basic two color deck
        lstDecks.setSelectedIndices(new Integer[]{0});
    }

    private void updateThemes() {
        updateDecks(DeckProxy.getAllThemeDecks(), ItemManagerConfig.STRING_ONLY);
    }

    private void updatePrecons() {
        updateDecks(DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons()), ItemManagerConfig.PRECON_DECKS);
    }

    private void updateQuestEvents() {
        updateDecks(DeckProxy.getAllQuestEventAndChallenges(), ItemManagerConfig.QUEST_EVENT_DECKS);
    }

    private void updateRandom() {
        updateDecks(RandomDeckGenerator.getRandomDecks(lstDecks, isAi), ItemManagerConfig.STRING_ONLY);
    }

    private void updateNetDecks() {
        if (netDeckCategory != null) {
            decksComboBox.setText(netDeckCategory.getDeckType());
        }
        updateDecks(DeckProxy.getNetDecks(netDeckCategory), ItemManagerConfig.NET_DECKS);
    }

    public Deck getDeck() {
        final DeckProxy proxy = lstDecks.getSelectedItem();
        if (proxy == null) {
            return null;
        }
        return proxy.getDeck();
    }

    /** Generates deck from current list selection(s). */
    public RegisteredPlayer getPlayer() {
        if (lstDecks.getSelectedIndex() < 0) { return null; }

        // Special branch for quest events
        if (selectedDeckType == DeckType.QUEST_OPPONENT_DECK) {
            final QuestEvent event = DeckgenUtil.getQuestEvent(lstDecks.getSelectedItem().getName());
            final RegisteredPlayer result = new RegisteredPlayer(event.getEventDeck());
            if (event instanceof QuestEventChallenge) {
                result.setStartingLife(((QuestEventChallenge) event).getAiLife());
            }
            result.setCardsOnBattlefield(QuestUtil.getComputerStartingCards(event));
            return result;
        }

        return new RegisteredPlayer(getDeck());
    }

    public void populate() {
        if (decksComboBox == null) { //initialize components with delayed initialization the first time this is populated
            decksComboBox = new DecksComboBox();
            lstDecksContainer = new ItemManagerContainer(lstDecks);
            decksComboBox.addListener(this);
            restoreSavedState();
        } else {
            removeAll();
        }
        this.setLayout(new MigLayout("insets 0, gap 0"));
        decksComboBox.addTo(this, "w 100%, h 30px!, gapbottom 5px, spanx 2, wrap");
        this.add(lstDecksContainer, "w 100%, growy, pushy, spanx 2, wrap");
        this.add(btnViewDeck, "w 50%-3px, h 30px!, gaptop 5px, gapright 6px");
        this.add(btnRandom, "w 50%-3px, h 30px!, gaptop 5px");
        if (isShowing()) {
            revalidate();
            repaint();
        }
    }

    public final boolean isAi() {
        return isAi;
    }

    public void setIsAi(final boolean isAiDeck) {
        isAi = isAiDeck;
    }

    @Override
    public void deckTypeSelected(final DecksComboBoxEvent ev) {
        if ((ev.getDeckType() == DeckType.NET_DECK || ev.getDeckType() == DeckType.NET_COMMANDER_DECK) && !refreshingDeckType) {
            FThreads.invokeInBackgroundThread(new Runnable() { //needed for loading net decks
                @Override
                public void run() {
                    final NetDeckCategory category = NetDeckCategory.selectAndLoad(lstDecks.getGameType());

                    FThreads.invokeInEdtLater(new Runnable() {
                        @Override
                        public void run() {
                            if (category == null) {
                                decksComboBox.setDeckType(selectedDeckType); //restore old selection if user cancels
                                if (selectedDeckType == DeckType.NET_DECK && netDeckCategory != null) {
                                    decksComboBox.setText(netDeckCategory.getDeckType());
                                }
                                return;
                            }

                            netDeckCategory = category;
                            refreshDecksList(ev.getDeckType(), true, ev);
                        }
                    });
                }
            });
            return;
        }
        refreshDecksList(ev.getDeckType(), false, ev);
    }

    public void refreshDeckListForAI(){
        //remember current deck by name, refresh decklist for AI/Human then reselect if possible
        String currentName= lstDecks.getSelectedItem().getName();
        refreshDecksList(selectedDeckType,true,null);
        lstDecks.setSelectedString(currentName);
        saveState();
    }

    private void refreshDecksList(final DeckType deckType, final boolean forceRefresh, final DecksComboBoxEvent ev) {
        if (decksComboBox == null) { return; } // Not yet populated
        if (selectedDeckType == deckType && !forceRefresh) { return; }
        selectedDeckType = deckType;

        if (ev == null) {
            refreshingDeckType = true;
            decksComboBox.refresh(deckType, isForCommander);
            refreshingDeckType = false;
        }
        lstDecks.setCaption(deckType.toString());

        switch (deckType) {
            case CUSTOM_DECK:
                updateCustom();
                break;
            case COMMANDER_DECK:
                updateCustom();
                break;
            case COLOR_DECK:
                updateColors(null);
                break;
            case STANDARD_COLOR_DECK:
                updateColors(FModel.getFormats().getStandard().getFilterPrinted());
                break;
            case MODERN_COLOR_DECK:
                updateColors(FModel.getFormats().getModern().getFilterPrinted());
                break;
            case STANDARD_CARDGEN_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateMatrix(FModel.getFormats().getStandard());
                }
                break;
            case PIONEER_CARDGEN_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateMatrix(FModel.getFormats().getPioneer());
                }
                break;
            case MODERN_CARDGEN_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateMatrix(FModel.getFormats().getModern());
                }
                break;
            case LEGACY_CARDGEN_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateMatrix(FModel.getFormats().get("Legacy"));
                }
                break;
            case VINTAGE_CARDGEN_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateMatrix(FModel.getFormats().get("Vintage"));
                }
            break;
            case RANDOM_COMMANDER_DECK:
                updateRandomCommander();
                break;
            case RANDOM_CARDGEN_COMMANDER_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateRandomCardGenCommander();
                }
                break;
            case THEME_DECK:
                updateThemes();
                break;
            case QUEST_OPPONENT_DECK:
                updateQuestEvents();
                break;
            case PRECONSTRUCTED_DECK:
                updatePrecons();
                break;
            case RANDOM_DECK:
                updateRandom();
                break;
            case NET_DECK:
                updateNetDecks();
                break;
            case NET_COMMANDER_DECK:
                updateNetDecks();
                break;
            default:
                break; //other deck types not currently supported here
        }
    }

    private final String SELECTED_DECK_DELIMITER = "::";

    public void saveState() {
        if (stateSetting == null) {
            throw new NullPointerException("State setting missing. Specify first using the initialize() method.");
        }
        prefs.setPref(stateSetting, getState());
        prefs.save();
    }

    private String getState() {
        final StringBuilder state = new StringBuilder();
        if (decksComboBox.getDeckType() == null || decksComboBox.getDeckType() == DeckType.NET_DECK) {
            //handle special case of net decks
            if (netDeckCategory == null) { return ""; }
            state.append(NetDeckCategory.PREFIX).append(netDeckCategory.getName());
        }
        else {
            state.append(decksComboBox.getDeckType().name());
        }
        state.append(";");
        joinSelectedDecks(state, SELECTED_DECK_DELIMITER);
        return state.toString();
    }

    private void joinSelectedDecks(final StringBuilder state, final String delimiter) {
        final Iterable<DeckProxy> selectedDecks = lstDecks.getSelectedItems();
        boolean isFirst = true;
        if (selectedDecks != null) {
            for (final DeckProxy deck : selectedDecks) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    state.append(delimiter);
                }
                state.append(deck.toString());
            }
        }
    }

    public void restoreSavedState() {
        final DeckType oldDeckType = selectedDeckType;
        if (stateSetting == null) {
            //if can't restore saved state, just refresh deck list
            refreshDecksList(oldDeckType, true, null);
            return;
        }

        final String savedState = prefs.getPref(stateSetting);
        refreshDecksList(getDeckTypeFromSavedState(savedState), true, null);
        if (!lstDecks.setSelectedStrings(getSelectedDecksFromSavedState(savedState))) {
            //if can't select old decks, just refresh deck list
            refreshDecksList(oldDeckType, true, null);
        }
    }

    private DeckType getDeckTypeFromSavedState(final String savedState) {
        try {
            if (StringUtils.isBlank(savedState)) {
                return selectedDeckType;
            } else {
                final String deckType = savedState.split(";")[0];
                if (deckType.startsWith(NetDeckCategory.PREFIX)) {
                    netDeckCategory = NetDeckCategory.selectAndLoad(lstDecks.getGameType(), deckType.substring(NetDeckCategory.PREFIX.length()));
                    return DeckType.NET_DECK;
                }
                return DeckType.valueOf(deckType);
            }
        } catch (final IllegalArgumentException ex) {
            System.err.println(ex.getMessage() + ". Using default : " + selectedDeckType);
            return selectedDeckType;
        }
    }

    private List<String> getSelectedDecksFromSavedState(final String savedState) {
        try {
            if (StringUtils.isBlank(savedState)) {
                return new ArrayList<>();
            } else {
                return Arrays.asList(savedState.split(";")[1].split(SELECTED_DECK_DELIMITER));
            }
        } catch (final Exception ex) {
            System.err.println(ex + " [savedState=" + savedState + "]");
            return new ArrayList<>();
        }
    }

    public DecksComboBox getDecksComboBox() {
        return decksComboBox;
    }
}
