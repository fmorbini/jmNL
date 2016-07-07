The cakeVendor character is teh sample illustrated here: https://confluence.ict.usc.edu/display/VHTK/Creating+a+New+Virtual+Human+with+the+NPCEditor

to run it, you need to send a login event using the DM menu in the chat interface.

The Directable example was used to show the use of context for reference resolution and connection to an abductive server
for NLU and NLG connected to a 3D blocks world based on http://smartbody.ict.usc.edu/ for display and action execution.
It'll not work out of the box as it needs smartbody and also a running abduction pipeline similar to https://github.com/isi-metaphor/Metaphor-ADP

The Wikidata character shows how named entities can be used to query a local lucen wikidata index to answer certain
factual questions.

to use wikidata you need to create lucene local indexes.
For that you need to:

1. download a recent [wikidata json dump](https://dumps.wikimedia.org/wikidatawiki/entities/)
2. run edu.usc.ict.nl.nlu.wikidata.dumps.WikidataJsonProcessing.createItemsFile(8, new File(json_dump_file)) (adjust parameters as needed)
3. run edu.usc.ict.nl.nlu.wikidata.dumps.WikidataJsonProcessing.createPropertiesFile(8, new File(json_dump_file)) (adjust parameters as needed)
