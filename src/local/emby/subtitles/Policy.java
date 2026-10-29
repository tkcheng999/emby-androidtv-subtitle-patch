package local.emby.subtitles;
import java.util.List;
/** Default-subtitle policy. Free of Android, Emby and ExoPlayer types so it can be tested on a host JVM. */
public final class Policy {
 /** First player-only index. Utils.ReportProgress already drops indices >= 99; menu orders must stay below 65536. */
 public static final int BASE=10000;
 /** Official client-only CC pseudo indices start here (99 and 100). */
 public static final int CLIENT_ONLY=99;
 public static final int OFF=-1;
 public static final class Candidate {
  final int index,flags;final String language,label,key;final boolean player,auto;
  /** auto=false keeps a track selectable in the menu but never picks it by default (e.g. burn-in delivery). */
  public Candidate(int index,String language,String label,int flags,boolean player,boolean auto,String key){this.index=index;this.language=language;this.label=label;this.flags=flags;this.player=player;this.auto=auto;this.key=key;}
 }
 /** A choice made in the subtitle menu, remembered for the same item while the same controller replays it. */
 public static final class Choice {
  final boolean off;final int server;final String key;
  private Choice(boolean off,int server,String key){this.off=off;this.server=server;this.key=key;}
  public static Choice off(){return new Choice(true,OFF,null);}
  public static Choice server(int index){return new Choice(false,index,null);}
  public static Choice player(String key){return new Choice(false,OFF,key);}
  boolean matches(Candidate c){return c.player?key!=null&&key.equals(c.key):key==null&&!off&&server==c.index;}
 }
 /** Player tracks are exposed only when the server describes fewer embedded subtitles than the player found. */
 public static boolean needsPlayerTracks(int serverEmbedded,int playerEmbedded){return playerEmbedded>serverEmbedded;}
 /**
  * @param original  mDefaultSubIndex as computed by the official code (server default or burn-in index)
  * @param requested subtitle index sent in the PlaybackInfo request (mCurrentOptions), null when unset
  */
 public static int choose(List<Candidate> all,int original,Integer requested,Choice manual,String audio){
  if(original>=BASE)original=OFF;
  if(manual!=null){
   if(manual.off)return OFF;
   for(Candidate c:all)if(manual.matches(c))return c.index;
  }
  // Details-page defaults are also sent as requested indices; this is not proof of a manual choice.
  // Honour actual in-player choices above, otherwise apply the requested Chinese-first policy.
  int best=original,bestScore=-1;
  for(Candidate c:all){
   if(!c.auto)continue;
   int s=Selector.score(c.language,c.label,c.flags,audio);
   if(s>bestScore){bestScore=s;best=c.index;}
  }
  return best;
 }
}
