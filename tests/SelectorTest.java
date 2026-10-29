import local.emby.subtitles.Selector;
public class SelectorTest {
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
 public static void main(String[] a){
 check(Selector.score("chi","简体",0)>Selector.score("chi","繁体",0),"simplified preference");
 check(Selector.score("chi","简体 forced",2)<Selector.score("chi","繁体",0),"full before forced");
 check(Selector.score("eng","English",0)>=0,"English fallback recognized");
 check(Selector.score(null,"简英双语",0)>=0,"recognize untagged Chinese");
 check(Selector.score("zh-Hans","",0)>Selector.score("zh-Hant","",0),"script language tags");
 check(Selector.score(null,null,0)<0,"unknown not guessed");
 check(Selector.score("chi","简英双语[英语]",0,"eng")>Selector.score("chi","简体[国语]",0,"eng"),"match original audio");
 // ExoPlayer normalises ISO 639-2 audio codes to "en"; region/script variants are prefixes.
 check(Selector.score("chi","简英双语[英语]",0,"en")>Selector.score("chi","简体[国语]",0,"en"),"match normalised audio code");
 check(Selector.score("zh-Hans-CN","",0)>Selector.score("zh-Hant-TW","",0),"script tag with region");
 check(Selector.score("zh-cn","",0)>Selector.score("zh","",0),"zh-cn counts as simplified");
 check(Selector.score(null,"CHS&ENG",0)>Selector.score(null,"CHT&ENG",0),"chs/cht label tokens");
 check(Selector.score(null,"简中",0)>=0,"简中 recognised");
 check(Selector.score(null,"Chinese Simplified",0)>Selector.score(null,"Chinese Traditional",0),"english labels");
 check(Selector.score("und","Torchsong",0)<0,"chs token must be a whole word");
 check(Selector.score("zh","简体[国语]",0,"zh")==Selector.score("zh","简体",0,"zh"),"no dub penalty for Chinese audio");
 check(Selector.score("zh-tw","繁体 forced",2)>Selector.score("eng","English",0),"any Chinese remains above English");
 check(Selector.score("en-US","",0)>Selector.score("en-US","forced",2),"full English above forced");
 check(Selector.score("und","英文",0)>=0,"English label recognised");
 check(Selector.score("fr","French",0)<0,"other language not guessed");
 System.out.println("19 selection checks passed");
 }
}
