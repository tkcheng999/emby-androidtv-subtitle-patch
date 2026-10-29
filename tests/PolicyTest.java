import java.util.*;
import local.emby.subtitles.Policy;
import local.emby.subtitles.Policy.Candidate;
import local.emby.subtitles.Policy.Choice;
public class PolicyTest {
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
 static Candidate server(int i,String lang,String label){return new Candidate(i,lang,label,0,false,true,null);}
 static Candidate burn(int i,String lang,String label){return new Candidate(i,lang,label,0,false,false,null);}
 static Candidate player(int i,String lang,String label,String key){return new Candidate(i,lang,label,0,true,true,key);}
 public static void main(String[] a){
  List<Candidate> strm=Arrays.asList(player(10000,"en","English","en|English|t|0"),player(10001,"zh-cn","简体","zh-cn|简体|t|0"),player(10002,"zh-tw","繁體","zh-tw|繁體|t|0"));
  check(Policy.choose(strm,-1,-1,null,"en")==10001,"auto picks simplified player track");
  check(Policy.choose(strm,10001,-1,null,"en")==10001,"stale synthetic default is recomputed");
  check(Policy.choose(Arrays.asList(player(10000,"en","English","k")),10003,-1,null,"en")==10000,"stale synthetic default recomputes English fallback");
  check(Policy.choose(Arrays.asList(server(2,"eng","English")),2,-1,null,"en")==2,"no-Chinese fallback keeps server default");
  check(Policy.choose(Arrays.asList(server(2,"eng","English")),-1,null,null,"en")==2,"no-Chinese fallback enables English");
  check(Policy.choose(Arrays.asList(server(2,"eng","English"),server(3,"chi","Chinese")),2,-1,null,"en")==3,"Chinese server stream beats non-Chinese default");
  check(Policy.choose(Arrays.asList(server(2,"eng","English"),burn(3,"chi","简体")),2,-1,null,"en")==2,"burn-in stream never auto-picked");
  check(Policy.choose(Arrays.asList(server(2,"eng","English"),server(3,"chi","Chinese")),2,2,null,"en")==3,"server default carried in request must not block Chinese");
  check(Policy.choose(strm,-1,-1,Choice.off(),"en")==-1,"manual off survives restart");
  check(Policy.choose(strm,-1,-1,Choice.player("en|English|t|0"),"en")==10000,"manual player track survives restart");
  check(Policy.choose(strm,-1,-1,Choice.player("gone"),"en")==10001,"vanished manual track falls back to auto");
  check(Policy.choose(Arrays.asList(server(2,"eng","English"),server(3,"chi","Chinese")),3,3,Choice.server(2),"en")==2,"manual server choice beats request");
  check(Policy.choose(Arrays.asList(server(3,"chi","简体")),-1,-1,Choice.server(10000),"en")==3,"server choice never matches a player key");
  check(Policy.choose(strm,-1,99,null,"en")==10001,"CC pseudo request is not an explicit server request");
  check(Policy.needsPlayerTracks(0,2)&&!Policy.needsPlayerTracks(2,2)&&!Policy.needsPlayerTracks(3,2),"expose only when server metadata is short");
  System.out.println("15 policy checks passed");
 }
}
