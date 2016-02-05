## jmNL, a modular NL platform for dialogue agents

see the [LICENSE](LICENSE) file for information on the license and 3rd party libraries included in this project.

There is no comprehensive documentation, if you have questions please ask.
A somewhat out-of-date [guide](doc/vhtk-guide.pdf) was written for the [VHTK](https://vhtoolkit.ict.usc.edu/).

This project implements a framework to build the classic NL infrastructure needed by dialogue agents. It allows people to
extend it by implementing their own external communication protocols, NLU, NLG and DM modules.
It comes with several implemented classification based NLUs, the [FLoReS](http://ict.usc.edu/pubs/FLoReS-%20A%20Forward%20Looking,%20Reward%20Seeking,%20Dialogue%20Manager.pdf) DM and
some simple NLG modules including a template based one.

if you plan to use clearnlp you'll need to separately download the models: https://github.com/clir/clearnlp-guidelines/blob/master/md/quick_start/models.md

to use wikidata (see [this demo character](resources/characters/WikiData/)) you need to create lucene local indexes.
For that you need to:

1. download a recent [wikidata json dump](https://dumps.wikimedia.org/wikidatawiki/entities/)
2. run edu.usc.ict.nl.nlu.wikidata.dumps.WikidataJsonProcessing.createItemsFile(8, new File(json_dump_file)) (adjust parameters as needed)
3. run edu.usc.ict.nl.nlu.wikidata.dumps.WikidataJsonProcessing.createPropertiesFile(8, new File(json_dump_file)) (adjust parameters as needed)
