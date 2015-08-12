package edu.usc.ict.nl.nlu.wikidata;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WikiLanguage {
	public static final Set<String> LANG=new HashSet<String>(Arrays.asList("aa", "ab", "ace", "aeb", "aeb-arab", "aeb-latn", "af", "ak", "aln", "als", "am", "an", "ang", "anp", "ar", "arc", "arn", "arq", "ary", "arz", "as", "ast", "av", "avk", "awa", "ay", "az", "azb", "ba", "bar", "bat-smg", "bbc", "bbc-latn", "bcc", "bcl", "be", "be-tarask", "be-x-old", "bg", "bgn", "bh", "bho", "bi", "bjn", "bm", "bn", "bo", "bpy", "bqi", "br", "brh", "bs", "bto", "bug", "bxr", "ca", "cbk-zam", "cdo", "ce", "ceb", "ch", "cho", "chr", "chy", "ckb", "co", "cps", "cr", "crh", "crh-cyrl", "crh-latn", "cs", "csb", "cu", "cv", "cy", "da", "de", "de-at", "de-ch", "de-formal", "din", "diq", "dsb", "dtp", "dty", "dv", "dz", "ee", "egl", "el", "eml", "en", "en-ca", "en-gb", "eo", "es", "et", "eu", "ext", "fa", "ff", "fi", "fit", "fiu-vro", "fj", "fo", "fr", "frc", "frp", "frr", "fur", "fy", "ga", "gag", "gan", "gan-hans", "gan-hant", "gd", "gl", "glk", "gn", "gom", "gom-deva", "gom-latn", "got", "grc", "gsw", "gu", "gv", "ha", "hak", "haw", "he", "hi", "hif", "hif-latn", "hil", "ho", "hr", "hrx", "hsb", "ht", "hu", "hy", "hz", "ia", "id", "ie", "ig", "ii", "ik", "ike-cans", "ike-latn", "ilo", "inh", "io", "is", "it", "iu", "ja", "jam", "jbo", "jut", "jv", "ka", "kaa", "kab", "kbd", "kbd-cyrl", "kg", "khw", "ki", "kiu", "kj", "kk", "kk-arab", "kk-cn", "kk-cyrl", "kk-kz", "kk-latn", "kk-tr", "kl", "km", "kn", "ko", "ko-kp", "koi", "kr", "krc", "kri", "krj", "ks", "ks-arab", "ks-deva", "ksh", "ku", "ku-arab", "ku-latn", "kv", "kw", "ky", "la", "lad", "lb", "lbe", "lez", "lfn", "lg", "li", "lij", "liv", "lmo", "ln", "lo", "loz", "lrc", "lt", "ltg", "lus", "luz", "lv", "lzh", "lzz", "mai", "map-bms", "mdf", "mg", "mh", "mhr", "mi", "min", "mk", "ml", "mn", "mo", "mr", "mrj", "ms", "mt", "mus", "mwl", "my", "myv", "mzn", "na", "nah", "nan", "nap", "nb", "nds", "nds-nl", "ne", "new", "ng", "niu", "nl", "nl-informal", "nn", "no", "nov", "nrm", "nso", "nv", "ny", "oc", "om", "or", "os", "ota", "pa", "pag", "pam", "pap", "pcd", "pdc", "pdt", "pfl", "pi", "pih", "pl", "pms", "pnb", "pnt", "prg", "ps", "pt", "pt-br", "qu", "qug", "rgn", "rif", "rm", "rmy", "rn", "ro", "roa-rup", "roa-tara", "ru", "rue", "rup", "ruq", "ruq-cyrl", "ruq-latn", "rw", "rwr", "sa", "sah", "sat", "sc", "scn", "sco", "sd", "sdc", "se", "sei", "ses", "sg", "sgs", "sh", "shi", "shi-latn", "shi-tfng", "si", "simple", "sk", "sl", "sli", "sm", "sma", "sn", "so", "sq", "sr", "sr-ec", "sr-el", "srn", "ss", "st", "stq", "su", "sv", "sw", "szl", "ta", "tcy", "te", "tet", "tg", "tg-cyrl", "tg-latn", "th", "ti", "tk", "tl", "tly", "tn", "to", "tokipona", "tpi", "tr", "tru", "ts", "tt", "tt-cyrl", "tt-latn", "tum", "tw", "ty", "tyv", "tzm", "udm", "ug", "ug-arab", "ug-latn", "uk", "ur", "uz", "uz-cyrl", "uz-latn", "ve", "vec", "vep", "vi", "vls", "vmf", "vo", "vot", "vro", "wa", "war", "wo", "wuu", "xal", "xh", "xmf", "yi", "yo", "yue", "za", "zea", "zh", "zh-classical", "zh-cn", "zh-hans", "zh-hant", "zh-hk", "zh-min-nan", "zh-mo", "zh-my", "zh-sg", "zh-tw", "zh-yue", "zu"));
	
	public static WikiLanguage EN;
	static {
		try {
			EN=get("en");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String getLang(String l) {
		return LANG.contains(l)?l:null;
	}

	private static Map<String,WikiLanguage> cache=null;
	public static WikiLanguage get(String l) throws Exception {
		if (cache==null) cache=new HashMap<String,WikiLanguage>();
		WikiLanguage ret=cache.get(l);
		if (ret==null) cache.put(l, ret=new WikiLanguage(l));
		return ret;
	}
	
	private String lcode=null;
	private WikiLanguage(String l) throws Exception {
		if (getLang(l)!=null) this.lcode=l;
		else throw new Exception("invalid language code: "+l+". Valid codes are: "+LANG);
	}
	public String getLcode() {
		return lcode;
	}
	@Override
	public String toString() {
		return getLcode();
	}
}
