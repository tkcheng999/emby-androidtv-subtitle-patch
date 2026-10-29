import java.util.*;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride;
import local.emby.subtitles.Bridge;
import mediabrowser.model.dlna.SubtitleDeliveryMethod;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;
import tv.emby.embyatv.api.SubtitleStreamInfo;
/**
 * Behavioural tests for Bridge against host fakes. Field and method names mirror PlaybackController, VideoManager
 * and ExoPlayer 2.18 as reported by REA for tv.emby.embyatv 2.5.03g; the Controller helpers mirror the patched
 * call order (PlaybackController$24.onEvent -> addCCStreamIfNeeded -> switchSubtitleStream(mDefaultSubIndex)).
 */
public class BridgeTest {
 static int checks;
 static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
 static final String SRT="application/x-subrip";
 public static class Fmt {public final String language,label,sampleMimeType;public final int selectionFlags;Fmt(String l,String b,String m,int f){language=l;label=b;sampleMimeType=m;selectionFlags=f;}}
 public static class Group {public final String id;public final int length;private final Fmt[] f;Group(String id,Fmt...f){this.id=id;this.f=f;length=f.length;}public Fmt getFormat(int i){return f[i];}}
 public static class Groups {public final int length;private final Group[] g;Groups(Group...g){this.g=g;length=g.length;}public Group get(int i){return g[i];}}
 public static class Info {
  final int[] types;final Groups[] groups;final Set<String> unsupported=new HashSet<>(),preferred=new HashSet<>();
  Info(int[] types,Groups[] groups){this.types=types;this.groups=groups;}
  public int getRendererCount(){return types.length;}
  public int getRendererType(int r){return types[r];}
  public Groups getTrackGroups(int r){return groups[r];}
  public int getTrackSupport(int r,int g,int t){return unsupported.contains(r+":"+g+":"+t)?3:(preferred.contains(r+":"+g+":"+t)?7:4);}
 }
 public static class Builder {
  final Map<Integer,Boolean> disabled=new HashMap<>();final Map<Integer,SelectionOverride> overrides=new HashMap<>();
  public Builder setRendererDisabled(int r,boolean d){disabled.put(r,d);return this;}
  public Builder clearSelectionOverrides(int r){overrides.remove(r);return this;}
  public Builder setSelectionOverride(int r,Object groups,SelectionOverride o){overrides.put(r,o);return this;}
  Builder copy(){Builder b=new Builder();b.disabled.putAll(disabled);b.overrides.putAll(overrides);return b;}
 }
 public static class TrackSelector {
  Info info;Builder params=new Builder();
  TrackSelector(Info info){this.info=info;}
  public Info getCurrentMappedTrackInfo(){return info;}
  public Builder buildUponParameters(){return params.copy();}
  public void setParameters(Builder b){params=b;}
 }
 public static class Player {final Fmt audio;Player(String lang){audio=new Fmt(lang,null,"audio/ac3",0);}public Fmt getAudioFormat(){return audio;}}
 public static class VideoManager {
  private Object mExoTrackSelector,mExoPlayer=new Player("en");private ArrayList<Object> externalSubSources=new ArrayList<>();int disabled;
  public boolean isNativeMode(){return true;}
  public void disableSubs(){disabled++;}
 }
 public static class Options {Integer sub=-1;public Integer getSubtitleStreamIndex(){return sub;}public void setSubtitleStreamIndex(Integer v){sub=v;}}
 public static class StreamInfo {Integer sub=7;public void setSubtitleStreamIndex(Integer v){sub=v;}}
 public static class Source {String id="source";public String getId(){return id;}ArrayList<MediaStream> streams=new ArrayList<>();public ArrayList<MediaStream> getMediaStreams(){return streams;}public void setMediaStreams(ArrayList<MediaStream> v){streams=v;}}
 public static class Row {Item item=new Item();String source="source";public Item getItem(){return item;}public String getDefaultMediaSourceId(){return source;}}
 public static class Bar {Row mRow=new Row();Object mSubtitles;}
 public static class Listener {Bar this$0;List<SubtitleStreamInfo> val$subs;}
 public static class MediaSelector {Listener mChangedListener;}
 public static class Spinner {MediaSelector parent;public MediaSelector getParent(){return parent;}}
 static Spinner preplay(boolean subtitles){Spinner sp=new Spinner();sp.parent=new MediaSelector();Listener l=new Listener();l.this$0=new Bar();l.this$0.mSubtitles=subtitles?sp.parent:new Object();l.val$subs=list(server(-1,"","Off",SubtitleDeliveryMethod.Embed,false),server(2,"eng","English",SubtitleDeliveryMethod.Embed,false));sp.parent.mChangedListener=l;return sp;}
 public static class Item {String id="movie";public String getId(){return id;}}
 public static class Fragment {int updates;public void updateDisplay(){updates++;}public void addManualSubtitles(Object o){}}
 public static class Controller {
  private VideoManager mVideoManager=new VideoManager();private List<SubtitleStreamInfo> mSubtitleStreams=new ArrayList<>();
  private int mDefaultSubIndex=-1;private boolean burningSubs;private Options mCurrentOptions=new Options();private StreamInfo mCurrentStreamInfo=new StreamInfo();private Fragment mFragment=new Fragment();
  Source source=new Source();Item item=new Item();boolean live;
  public Source getCurrentMediaSource(){return source;}
  public Item getCurrentlyPlayingItem(){return item;}
  public boolean isLiveTv(){return live;}
  /** Patched getSubtitleStreamIndex. */
  int subtitleIndex(){int i=Bridge.current(this);return i>=0?i:mCurrentOptions.sub==null?-1:mCurrentOptions.sub;}
  /** Patched switchSubtitleStream: hook, then the official first statements (mCurrentOptions feeds PlaybackInfo). */
  void switchSub(int i){if(Bridge.select(this,i))return;mCurrentOptions.sub=i>=0?i:null;if(i<0){mCurrentStreamInfo.sub=null;mDefaultSubIndex=-1;}}
  void onPrepared(){Bridge.prepare(this);if(mDefaultSubIndex>=0&&!burningSubs)switchSub(mDefaultSubIndex);}
  /** startItem: new server list and source per playInternal; mDefaultSubIndex replaced only by a server default. VideoManager.init recreates the selector. */
  void startItem(List<SubtitleStreamInfo> server,Integer serverDefault,TrackSelector sel){mSubtitleStreams=server;if(serverDefault!=null)mDefaultSubIndex=serverDefault;mVideoManager.mExoTrackSelector=sel;source=new Source();}
 }
 static Fmt fmt(String lang,String label){return new Fmt(lang,label,SRT,0);}
 static TrackSelector selector(Group...text){return new TrackSelector(new Info(new int[]{2,1,3},new Groups[]{new Groups(),new Groups(),new Groups(text)}));}
 static TrackSelector strmTracks(){return selector(new Group("0",fmt("en","English")),new Group("1",fmt("zh-cn",null)));}
 static SubtitleStreamInfo server(int i,String lang,String title,SubtitleDeliveryMethod d,boolean external){SubtitleStreamInfo s=new SubtitleStreamInfo();s.setIndex(i);s.setLanguage(lang);s.setDisplayTitle(title);s.setDeliveryMethod(d);s.setIsExternalUrl(external);return s;}
 static List<SubtitleStreamInfo> list(SubtitleStreamInfo...s){return new ArrayList<>(Arrays.asList(s));}
 static SelectionOverride text(TrackSelector s){return s.params.overrides.get(2);}

 static void metadataLessStrm(){
  Controller c=new Controller();TrackSelector sel=strmTracks();
  c.startItem(list(),null,sel);c.onPrepared();
  check(c.mSubtitleStreams.size()==2&&c.mSubtitleStreams.get(0).getIndex()==10000&&c.mSubtitleStreams.get(1).getIndex()==10001,"player tracks exposed with synthetic indices");
  check("English [Player]".equals(c.mSubtitleStreams.get(0).getDisplayTitle())&&"zh-cn [Player]".equals(c.mSubtitleStreams.get(1).getDisplayTitle()),"menu titles fall back to language");
  check(c.mSubtitleStreams.get(1).getDeliveryMethod()==SubtitleDeliveryMethod.Embed,"synthetic entries are embedded, never external");
  check(c.source.streams.size()==2&&c.source.streams.get(0).getType()==MediaStreamType.Subtitle,"markers enable the subtitle button");
  check(c.mDefaultSubIndex==10001&&c.subtitleIndex()==10001,"simplified Chinese selected by default");
  check(text(sel)!=null&&text(sel).groupIndex==1&&text(sel).tracks[0]==0&&Boolean.FALSE.equals(sel.params.disabled.get(2)),"exact renderer/group/track override");
  check(c.mCurrentOptions.sub==-1&&c.mCurrentStreamInfo.sub==null,"no synthetic index in server-bound state");
  check(c.mFragment.updates>0,"overlay refreshed");
 }
 static void manualOffSurvivesRestart(){
  Controller c=new Controller();c.startItem(list(),null,strmTracks());c.onPrepared();
  c.switchSub(-1);
  check(c.subtitleIndex()==-1&&c.mCurrentOptions.sub==null,"manual off handled by official path");
  // Audio switch while transcoding: playInternal keeps the stale default, VideoManager.init recreates the selector.
  TrackSelector sel=strmTracks();c.mDefaultSubIndex=10001;c.startItem(list(),null,sel);c.onPrepared();
  check(c.mDefaultSubIndex==-1&&text(sel)==null,"manual off survives restart and stale default is dropped");
 }
 static void manualPlayerChoiceSurvivesRestart(){
  Controller c=new Controller();TrackSelector sel=strmTracks();c.startItem(list(),null,sel);c.onPrepared();
  c.switchSub(10000);
  check(c.subtitleIndex()==10000&&text(sel).groupIndex==0,"manual player switch applied");
  check(c.mCurrentOptions.sub==-1,"manual player switch keeps server-bound index clear");
  TrackSelector next=strmTracks();c.startItem(list(),null,next);c.onPrepared();
  check(c.mDefaultSubIndex==10000&&text(next).groupIndex==0,"manual player choice survives restart on the new selector");
 }
 static void staleSyntheticIndexIsConsumed(){
  Controller c=new Controller();c.startItem(list(),null,strmTracks());c.onPrepared();
  c.mSubtitleStreams=list(); // list rebuilt before READY (startItem / live media info refresh)
  c.mCurrentOptions.sub=-1;c.switchSub(10001);
  check(c.mCurrentOptions.sub==-1&&c.subtitleIndex()==-1,"stale synthetic index never reaches mCurrentOptions");
  c.startItem(list(),null,selector());c.mDefaultSubIndex=10001;c.onPrepared();
  check(c.mDefaultSubIndex==-1&&c.mCurrentOptions.sub==-1,"stale default dropped when no player tracks remain");
 }
 static void mappingIsRevalidated(){
  Controller c=new Controller();c.startItem(list(),null,strmTracks());c.onPrepared();
  TrackSelector other=selector(new Group("0",fmt("en","English")),new Group("1",fmt("ja",null)));
  c.mVideoManager.mExoTrackSelector=other;c.switchSub(10001);
  check(text(other)==null&&c.mCurrentOptions.sub==-1,"changed track layout is not selected blindly");
 }
 static void serverMetadataPreferred(){
  Controller c=new Controller();TrackSelector sel=strmTracks();
  c.startItem(list(server(2,"eng","English",SubtitleDeliveryMethod.Embed,false),server(3,"chi","Chinese Simplified",SubtitleDeliveryMethod.Embed,false)),2,sel);
  c.onPrepared();
  check(c.mSubtitleStreams.size()==2&&c.source.streams.isEmpty(),"no duplicate player entries when the server lists the embedded tracks");
  check(c.mDefaultSubIndex==3&&c.mCurrentOptions.sub==3&&c.subtitleIndex()==3,"Chinese server stream chosen through the official path");
 }
 static void serverExternalNotDuplicated(){
  Controller c=new Controller();
  TrackSelector sel=selector(new Group("0:0",fmt("en","English")),new Group("1:0",fmt("zh",null)));
  c.mVideoManager.externalSubSources.add(new Object());
  // VideoManager.prepareExoPlayer turned the server's external entry into Embed + isExternalUrl.
  c.startItem(list(server(5,"chi","简体",SubtitleDeliveryMethod.Embed,true)),null,sel);c.onPrepared();
  check(c.mSubtitleStreams.size()==2&&c.mSubtitleStreams.get(1).getIndex()==10000&&"English [Player]".equals(c.mSubtitleStreams.get(1).getDisplayTitle()),"merged external file not re-listed as a player track");
  check(c.mDefaultSubIndex==5&&c.mCurrentOptions.sub==5,"external Chinese server subtitle chosen");
 }
 static void noChineseFallback(){
  Controller c=new Controller();TrackSelector sel=selector(new Group("0",fmt("en","English")));
  c.startItem(list(),null,sel);c.onPrepared();
  check(c.mSubtitleStreams.size()==1&&c.mDefaultSubIndex==10000&&text(sel).groupIndex==0&&c.mCurrentOptions.sub==-1,"no Chinese: English enabled locally without server index leak");
 }
 static void preferredRendererPgs(){
  Controller c=new Controller();TrackSelector sel=selector(new Group("0",new Fmt("en","English-PGS","application/pgs",0)));
  sel.info.preferred.add("2:0:0");c.startItem(list(),null,sel);c.onPrepared();
  check(c.mSubtitleStreams.size()==1&&c.mDefaultSubIndex==10000&&text(sel).groupIndex==0,"Emby preferred support 7 PGS is playable and selected as English fallback");
  check("pgssub".equals(c.mSubtitleStreams.get(0).getFormat()),"PGS menu uses official codec name to hide text-only position control");
 }
 static void ignoredTracks(){
  Controller c=new Controller();
  TrackSelector sel=selector(new Group("0",new Fmt("en",null,"application/cea-608",0)),new Group("1",fmt("zh-cn","简体")),new Group("2",fmt("zh-tw","繁體")));
  sel.info.unsupported.add("2:1:0");
  c.startItem(list(),null,sel);c.onPrepared();
  check(c.mSubtitleStreams.size()==1&&"繁體 [Player]".equals(c.mSubtitleStreams.get(0).getDisplayTitle()),"CEA-608 and unsupported tracks skipped");
 }
 static void burningAndLiveUntouched(){
  Controller c=new Controller();c.burningSubs=true;c.startItem(list(),4,strmTracks());c.onPrepared();
  check(c.mSubtitleStreams.isEmpty()&&c.mDefaultSubIndex==4,"burn-in playback untouched");
  Controller l=new Controller();l.live=true;l.startItem(list(),null,strmTracks());l.onPrepared();
  check(l.mSubtitleStreams.isEmpty()&&l.mDefaultSubIndex==-1,"live TV untouched");
 }
 static void nextItemResetsManualChoice(){
  Controller c=new Controller();c.startItem(list(),null,strmTracks());c.onPrepared();c.switchSub(-1);
  c.item=new Item();c.item.id="episode2";TrackSelector sel=strmTracks();c.startItem(list(),null,sel);c.onPrepared();
  check(c.mDefaultSubIndex==10001&&text(sel).groupIndex==1,"next item auto-selects Chinese again");
 }
 static void inheritedRequestDoesNotBlockChinese(){
  Controller c=new Controller();c.mCurrentOptions.sub=2;
  c.startItem(list(server(2,"eng","English",SubtitleDeliveryMethod.Embed,false),server(3,"chi","简体",SubtitleDeliveryMethod.Embed,false)),2,strmTracks());c.onPrepared();
  check(c.mDefaultSubIndex==3,"inherited server request must not block Chinese");
 }
 static void explicitPreplay(){
  Controller c=new Controller();Spinner sp=preplay(true);
  Bridge.preplayClick(sp,1);
  c.startItem(list(server(2,"eng","English",SubtitleDeliveryMethod.Embed,false),server(3,"chi","简体",SubtitleDeliveryMethod.Embed,false)),3,strmTracks());c.onPrepared();
  check(c.mDefaultSubIndex==2,"actual preplay English click overrides automatic Chinese");
  Bridge.preplayClick(sp,0);Controller off=new Controller();off.startItem(list(),null,strmTracks());off.onPrepared();
  check(off.mDefaultSubIndex==-1,"actual preplay Off survives absence of an intent extra");
  Controller next=new Controller();next.startItem(list(),null,strmTracks());next.onPrepared();
  check(next.mDefaultSubIndex==10001,"preplay choice consumed once, next independent playback automatic");
  Bridge.preplayClick(preplay(false),0);Controller audio=new Controller();audio.startItem(list(),null,strmTracks());audio.onPrepared();
  check(audio.mDefaultSubIndex==10001,"audio/version clicks never become subtitle choices");
  Bridge.preplayClick(sp,0);Controller changed=new Controller();changed.startItem(list(),null,strmTracks());changed.source.id="other";changed.onPrepared();
  check(changed.mDefaultSubIndex==10001,"preplay choice scoped to exact media source");
 }
 static void externalMappingWithUnknownEmbedded(){
  VideoManager vm=new VideoManager();TrackSelector sel=selector(new Group("0:2",fmt("en","English")),new Group("0:3",new Fmt("en","English PGS","application/pgs",0)),new Group("1:",fmt("zh-cn",null)),new Group("2:",fmt("zh-tw",null)));
  vm.mExoTrackSelector=sel;vm.externalSubSources.add(new Object());vm.externalSubSources.add(new Object());
  SubtitleStreamInfo simp=server(0,"zh-CN","简体",SubtitleDeliveryMethod.Embed,true),trad=server(1,"zh-TW","繁體",SubtitleDeliveryMethod.Embed,true);
  for(SubtitleStreamInfo x:Arrays.asList(simp,trad)){x.setFormat("srt");x.setUrl("https://example.invalid/subtitle");}
  List<SubtitleStreamInfo> subs=list(simp,trad);
  check(Bridge.selectExternal(vm,subs,1,false)&&text(sel).groupIndex==3,"external traditional maps to child 2, not undeclared embedded ordinal 1");
  check(Bridge.selectExternal(vm,subs,0,false)&&text(sel).groupIndex==2,"external simplified maps to child 1");
  check(!Bridge.selectExternal(vm,subs,0,true),"CC ordinal selection remains official");
  vm.externalSubSources.remove(1);
  check(!Bridge.selectExternal(vm,subs,1,false),"ambiguous external merge count fails closed to official mapping");
 }
 public static void main(String[] a){
  metadataLessStrm();manualOffSurvivesRestart();manualPlayerChoiceSurvivesRestart();staleSyntheticIndexIsConsumed();mappingIsRevalidated();
  serverMetadataPreferred();serverExternalNotDuplicated();noChineseFallback();preferredRendererPgs();ignoredTracks();burningAndLiveUntouched();nextItemResetsManualChoice();inheritedRequestDoesNotBlockChinese();
  explicitPreplay();externalMappingWithUnknownEmbedded();System.out.println(checks+" bridge checks passed");
 }
}
