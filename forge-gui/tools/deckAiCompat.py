#!/usr/bin/env python

import argparse, os, re

print("Agetian's MTG Forge Deck AI Compatibility Analyzer v1.0\n")

parser = argparse.ArgumentParser(description="Analyze MTG Forge decks for AI compatibility.")
parser.add_argument("-p", action="store_true", help="print only AI-playable decks")
parser.add_argument("-u", action="store_true", help="print only AI-unplayable decks")
parser.add_argument("-d", action="store_true", help="physically delete unplayable decks")

args = parser.parse_args()

# simple structural self-test (can this tool work?)
if not (os.access(os.path.join("cardsfolder","a","abu_jafar.txt"),os.F_OK) or os.access(os.path.join("decks"),os.F_OK)):
    print("Fatal error:\n    This utility requires the 'cardsfolder' folder with unpacked card files and the 'decks' folder with .dck files in the current directory in order to operate. Exiting.")
    exit(1)
if args.p and args.u:
    print("Fatal error:\n    The -p and -u options are mutually exclusive, please specify one of these options and not both of them at the same time.")
    exit(1)

# basic variables
cardlist = {}
total_cards = 0
ai_playable_cards = 0
total_decks = 0
playable_decks = 0
nonplayable_in_deck = 0

# main algorithm
print("Loading cards...")
for root, dirs, files in os.walk("cardsfolder"):
    for name in files:
	if name.find(".txt") != -1:
	    total_cards += 1
	    fullpath = os.path.join(root, name)
	    cardtext = open(fullpath).read()
	    cardtext_lower = cardtext.lower()
	    cardname_literal = cardtext.replace('\r','').split('\n')[0].split(':')
	    cardname = ":".join(cardname_literal[1:]).strip()
	    if (cardtext_lower.find("alternatemode:split") != -1) or (cardtext_lower.find("alternatemode: split") != -1):
		# split card, special handling needed
		cardsplittext = cardtext.replace('\r','').split('\n')
		cardnames = []
		for line in cardsplittext:
		    if line.lower().find("name:") != -1:
			cardnames.extend([line.split('\n')[0].split(':')[1]])
		cardname = " // ".join(cardnames)
	    if cardtext.lower().find("removedeck:all") != -1:
		cardlist[cardname] = 0
	    else:
		cardlist[cardname] = 1
		ai_playable_cards += 1

perc_playable = (float(ai_playable_cards) / total_cards) * 100
perc_unplayable = ((float(total_cards) - ai_playable_cards) / total_cards) * 100

print("Loaded %d cards, among them %d playable by the AI (%d%%), %d unplayable by the AI (%d%%).\n" % (total_cards, ai_playable_cards, perc_playable, total_cards - ai_playable_cards, perc_unplayable))

print("Scanning decks...")
for root, dirs, files in os.walk("decks"):
    for name in files:
	if name.find(".dck") != -1:
	    total_decks += 1
	    nonplayable_in_deck = 0
	    cardnames = []
	    fullpath = os.path.join(root, name)
	    deckdata = open(fullpath).readlines()
	    for line in deckdata:
		regexobj = re.search('^([0-9]+) +([^|]+)', line)
		if regexobj:
		    cardname = regexobj.groups()[1].replace('\n','').replace('\r','').strip()
		    if cardlist[cardname] == 0:
			cardnames.extend([cardname])
			nonplayable_in_deck += 1
	    if nonplayable_in_deck == 0:
		if not args.u:
		    playable_decks += 1
		    print("%s is PLAYABLE by the AI." % name)
	    else:
		if not args.p:
		    print("%s is UNPLAYABLE by the AI (%d unplayable cards: %s)." % (name, nonplayable_in_deck, str(cardnames)))
		if args.d:
		    os.remove(os.path.join(root, name))

perc_playable_decks = (float(playable_decks) / total_decks) * 100
perc_unplayable_decks = ((float(total_decks) - playable_decks) / total_decks) * 100

print("\nScanned %d decks, among them %d playable by the AI (%d%%), %d unplayable by the AI (%d%%)." % (total_decks, playable_decks, perc_playable_decks, total_decks - playable_decks, perc_unplayable_decks))
