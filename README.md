## jmNL, a modular NL platform for dialogue agents

see the [LICENSE](LICENSE) file for information on the license and 3rd party libraries included in this project.

There is no comprehensive documentation, if you have questions please ask.
A [guide](https://confluence.ict.usc.edu/display/VHTK/Creating+a+New+Virtual+Human+with+the+FLoReS+Dialogue+Manager) was written for the [VHTK](https://vhtoolkit.ict.usc.edu/). It is a work in progress, so some aspects are still undocumented and may not be fully in sink with the current capabilities in the trunk. If you have any questions please submit an [Issue](https://github.com/fmorbini/jmNL/issues).

Check out the [example characters](resources/characters).

This project implements a framework to build the classic NL infrastructure needed by dialogue agents. It allows people to
extend it by implementing their own external communication protocols, NLU, NLG and DM modules.
It comes with several implemented classification based NLUs, the [FLoReS](http://ict.usc.edu/pubs/FLoReS-%20A%20Forward%20Looking,%20Reward%20Seeking,%20Dialogue%20Manager.pdf) DM and
some simple NLG modules including a template based one.

if you plan to use nlp4j you'll need to separately download the models from their website: https://github.com/emorynlp/nlp4j
