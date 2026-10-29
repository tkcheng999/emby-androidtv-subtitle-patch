package local.emby.subtitles;
import java.util.Locale;
/** Scores subtitle tracks. A negative score means "neither recognisably Chinese nor English" and is never auto-selected. */
public final class Selector {
 private static final String CHINESE=".*(chinese|中文|中字|简体|簡體|繁体|繁體|简中|繁中|简英|繁英|中英|\\bchs\\b|\\bcht\\b).*";
 private static final String SIMPLIFIED=".*(simplified|简体|簡體|简中|简英|\\bchs\\b).*";
 private static final String TRADITIONAL=".*(traditional|繁体|繁體|繁中|繁英|\\bcht\\b).*";
 public static int score(String language,String label,int flags){return score(language,label,flags,"");}
 public static int score(String language,String label,int flags,String audio){
  String l=language==null?"":language.toLowerCase(Locale.ROOT).replace('_','-');
  String n=label==null?"":label.toLowerCase(Locale.ROOT);
  String a=audio==null?"":audio.toLowerCase(Locale.ROOT);
  boolean chinese=l.matches("(zh|chi|zho|cmn|yue)(-.*)?")||n.matches(CHINESE);
  if(!chinese){
   boolean english=l.matches("(en|eng)(-.*)?")||n.matches(".*(english|英文|英语|英語|\\beng\\b).*");
   if(!english)return -1;
   return 100-(((flags&2)!=0||n.contains("forced"))?30:0);
  }
  int score=1000;
  if((flags&2)!=0||n.contains("forced"))score-=500;
  // Subtitles authored for a Chinese dub do not follow the original non-Chinese audio.
  if(!a.isEmpty()&&!a.matches("(zh|chi|zho|cmn|yue)(-.*)?")&&n.matches(".*(国语|國語|粤语|粵語).*"))score-=200;
  if(l.startsWith("zh-hans")||l.equals("zh-cn")||l.equals("zh-sg")||n.matches(SIMPLIFIED))score+=100;
  if(l.startsWith("zh-hant")||l.equals("zh-tw")||l.equals("zh-hk")||l.equals("zh-mo")||n.matches(TRADITIONAL))score-=50;
  return score;
 }
}
