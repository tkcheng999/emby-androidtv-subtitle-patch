package local.emby.subtitles;
import java.lang.reflect.*;
import java.util.*;
import android.util.Log;
/**
 * Exposes subtitle tracks that ExoPlayer actually found but the server did not describe, and picks a default.
 * Hooks (see patch.py): prepare() at the start of PlaybackController.addCCStreamIfNeeded (first READY of each
 * playInternal), select() at the start of switchSubtitleStream, current() at the start of getSubtitleStreamIndex.
 * Player-only indices start at Policy.BASE. They are always consumed by select() and are never written to
 * mCurrentOptions / mCurrentStreamInfo, which feed PlaybackInfo, transcoding and progress requests.
 */
public final class Bridge {
 /** Correct the native ordinal mapping for external files when the MKV contains undeclared embedded tracks. */
 public static boolean selectExternal(Object vm,List<?> subs,int index,boolean ordinal){
  if(ordinal||index<0||index>=Policy.CLIENT_ONLY)return false;
  try{
   List<?> ext=(List<?>)get(vm,"externalSubSources");if(ext==null||ext.isEmpty())return false;
   int n=0,target=-1;
   for(Object sub:subs){
    String format=str(call(sub,"getFormat"));
    if(!Boolean.TRUE.equals(call(sub,"getIsExternalUrl"))||call(sub,"getUrl")==null||!Arrays.asList("subrip","srt","dvb_teletext","dvb","ass","ssa","vtt").contains(format))continue;
    n++;if(num(call(sub,"getIndex"))==index)target=n;
   }
   if(target<0||n!=ext.size())return false;
   Object selector=get(vm,"mExoTrackSelector"),info=call(selector,"getCurrentMappedTrackInfo");
   if(info==null)return false;
   for(int r=0;r<num(call(info,"getRendererCount"));r++){
    if(num(call(info,"getRendererType",r))!=TEXT)continue;
    Object groups=call(info,"getTrackGroups",r);
    for(int g=0;g<num(get(groups,"length"));g++){
     Object group=call(groups,"get",g);
     if(child(str(get(group,"id")))!=target||num(get(group,"length"))!=1)continue;
     Object f=call(group,"getFormat",0);Track tr=new Track();tr.r=r;tr.g=g;tr.t=0;
     tr.lang=str(get(f,"language"));tr.label=str(get(f,"label"));tr.mime=str(get(f,"sampleMimeType"));
     if(applyToSelector(selector,tr)){Log.i(TAG,"Mapped external subtitle index="+index+" child="+target+" group="+g);return true;}
    }
   }
  }catch(Throwable e){Log.w(TAG,"External mapping not applied: "+e.getClass().getSimpleName());}
  return false;
 }
 private static class Preplay {String item,source;int index;long at;}
 private static Preplay preplay;
 /** Called only by the native popup's row-click handler, never by Spinner's initial selection callback. */
 public static synchronized void preplayClick(Object spinner,int position){
  try{
   Object selector=call(spinner,"getParent"),listener=get(selector,"mChangedListener");
   Object bar=get(listener,"this$0");
   if(get(bar,"mSubtitles")!=selector)return; // Not the audio or version selector.
   List<?> subs=(List<?>)get(listener,"val$subs");
   if(position<0||position>=subs.size())return;
   int index=num(call(subs.get(position),"getIndex"));if(index>=Policy.CLIENT_ONLY)return;
   Object row=get(bar,"mRow");Preplay p=new Preplay();
   p.item=str(call(call(row,"getItem"),"getId"));p.source=str(call(row,"getDefaultMediaSourceId"));
   if(p.item.isEmpty()||p.source.isEmpty())return;
   p.index=index;p.at=System.nanoTime();preplay=p;
   Log.i(TAG,"Explicit preplay subtitle index="+index);
  }catch(Throwable e){Log.w(TAG,"Preplay choice not captured: "+e.getClass().getSimpleName());}
 }
 private static final String TAG="EmbySubtitlePatch";
 private static final int TEXT=3,FORMAT_HANDLED=4,FORMAT_HANDLED_PREFERRED=7,FORCED=2,NONE=Integer.MIN_VALUE;
 private static final String OVERRIDE="com.google.android.exoplayer2.trackselection.DefaultTrackSelector$SelectionOverride";
 private static final Map<Object,Session> sessions=new WeakHashMap<>();
 private static class Track {int id,r,g,t,flags;String lang,label,mime,key;}
 /** Holds no reference to the controller, player or track selector, so the weak key can be collected. */
 private static class Session {List<?> list;List<Track> tracks=new ArrayList<>();int selected=-1,pendingAuto=NONE;String item;Policy.Choice manual;}
 private static Field field(Object o,String n)throws Exception {Class<?> c=o.getClass();while(c!=null){try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(n);}
 private static Object get(Object o,String n)throws Exception{return field(o,n).get(o);}
 private static void put(Object o,String n,Object v)throws Exception{field(o,n).set(o,v);}
 private static Object call(Object o,String n,Object...a)throws Exception{
  for(Method m:o.getClass().getMethods())if(m.getName().equals(n)&&m.getParameterTypes().length==a.length){
   Class<?>[] p=m.getParameterTypes();boolean ok=true;
   for(int i=0;i<a.length;i++){Class<?> c=p[i];if(a[i]==null){if(c.isPrimitive())ok=false;continue;}if(c==int.class)c=Integer.class;if(c==boolean.class)c=Boolean.class;if(c==long.class)c=Long.class;if(!c.isInstance(a[i]))ok=false;}
   if(ok){m.setAccessible(true);return m.invoke(o,a);}
  }throw new NoSuchMethodException(n);
 }
 @SuppressWarnings({"unchecked","rawtypes"})
 private static Object enumValue(String c,String n)throws Exception{return Enum.valueOf((Class)Class.forName(c),n);}
 private static int num(Object v){return ((Number)v).intValue();}
 private static String str(Object v){return v==null?"":v.toString();}
 private static Session session(Object c){Session s=sessions.get(c);if(s==null){s=new Session();sessions.put(c,s);}return s;}
 private static void strip(List<?> l)throws Exception{if(l==null)return;for(Iterator<?> i=l.iterator();i.hasNext();)if(num(call(i.next(),"getIndex"))>=Policy.BASE)i.remove();}
 /** MergingMediaPeriod renames groups "<child>:<id>"; VideoManager puts the video at child 0 and server external files after it. */
 private static int child(String id){int p=id.indexOf(':');if(p<=0)return 0;try{return Integer.parseInt(id.substring(0,p));}catch(NumberFormatException e){return 0;}}
 /** A server entry for a subtitle inside the media file; external files are flagged by VideoManager.prepareExoPlayer. */
 private static boolean serverEmbedded(Object x)throws Exception{
  int i=num(call(x,"getIndex"));
  return i<Policy.CLIENT_ONLY&&!Boolean.TRUE.equals(call(x,"getIsExternalUrl"))&&!"External".equals(str(call(x,"getDeliveryMethod")));
 }
 private static List<Track> discover(Object info,boolean merged)throws Exception{
  List<Track> found=new ArrayList<>();
  for(int r=0;r<num(call(info,"getRendererCount"));r++){
   if(num(call(info,"getRendererType",r))!=TEXT)continue;
   Object groups=call(info,"getTrackGroups",r);
   for(int g=0;g<num(get(groups,"length"));g++){
    Object group=call(groups,"get",g);
    Log.i(TAG,"Text group renderer="+r+" group="+g+" id="+str(get(group,"id"))+" merged="+merged+" tracks="+num(get(group,"length")));
    if(merged&&child(str(get(group,"id")))>0)continue;
    for(int t=0;t<num(get(group,"length"));t++){
     int support=num(call(info,"getTrackSupport",r,g,t));
     Log.i(TAG,"Text capability r="+r+" g="+g+" t="+t+" support="+support+" mime="+str(get(call(group,"getFormat",t),"sampleMimeType")));
     if(support!=FORMAT_HANDLED&&support!=FORMAT_HANDLED_PREFERRED)continue;
     Object f=call(group,"getFormat",t);String mime=str(get(f,"sampleMimeType"));
     // Closed captions keep the official 99/100 path.
     if(mime.equals("application/cea-608")||mime.equals("application/cea-708"))continue;
     Track tr=new Track();tr.id=Policy.BASE+found.size();tr.r=r;tr.g=g;tr.t=t;tr.mime=mime;
     tr.lang=str(get(f,"language"));tr.label=str(get(f,"label"));tr.flags=num(get(f,"selectionFlags"));
     int same=0;for(Track x:found)if(x.lang.equals(tr.lang)&&x.label.equals(tr.label)&&x.mime.equals(tr.mime))same++;
     tr.key=tr.lang+"|"+tr.label+"|"+tr.mime+"|"+same;found.add(tr);
    }
   }
  }
  return found;
 }
 private static String audioLanguage(Object vm){try{Object f=call(get(vm,"mExoPlayer"),"getAudioFormat");return f==null?"":str(get(f,"language"));}catch(Throwable e){return "";}}
 @SuppressWarnings({"unchecked","rawtypes"})
 public static synchronized void prepare(Object c){
  Session s=session(c);
  s.list=null;s.tracks=new ArrayList<>();s.selected=-1;s.pendingAuto=NONE;
  List subs=null,media=null;
  try{
   int original=num(get(c,"mDefaultSubIndex"));
   // playInternal restarts (audio switch while transcoding) do not reset mDefaultSubIndex.
   if(original>=Policy.BASE){original=Policy.OFF;put(c,"mDefaultSubIndex",original);}
   Object item=call(c,"getCurrentlyPlayingItem");String id=item==null?"":str(call(item,"getId"));
   if(!id.equals(s.item)){s.item=id;s.manual=null;}
   subs=(List)get(c,"mSubtitleStreams");Object source=call(c,"getCurrentMediaSource");
   media=source==null?null:(List)call(source,"getMediaStreams");
   Preplay p=preplay;preplay=null;
   if(p!=null&&source!=null&&p.item.equals(id)&&p.source.equals(str(call(source,"getId")))&&System.nanoTime()-p.at<600000000000L){
    s.manual=p.index<0?Policy.Choice.off():Policy.Choice.server(p.index);
   }
   strip(subs);strip(media);
   Object vm=get(c,"mVideoManager");
   if(vm==null||subs==null||source==null||!Boolean.TRUE.equals(call(vm,"isNativeMode"))||Boolean.TRUE.equals(get(c,"burningSubs"))||Boolean.TRUE.equals(call(c,"isLiveTv")))return;
   Object selector=get(vm,"mExoTrackSelector");Object info=selector==null?null:call(selector,"getCurrentMappedTrackInfo");
   if(info==null)return;
   List ext=(List)get(vm,"externalSubSources");
   List<Track> found=discover(info,ext!=null&&!ext.isEmpty());
   int serverEmbedded=0;for(Object x:subs)if(serverEmbedded(x))serverEmbedded++;
   boolean expose=Policy.needsPlayerTracks(serverEmbedded,found.size());
   if(expose){
    if(media==null){media=new ArrayList();call(source,"setMediaStreams",media);}
    for(Track tr:found){
     String title=tr.label.isEmpty()?tr.lang:tr.label;if(title.isEmpty())title="Subtitle "+(tr.id-Policy.BASE+1);title+=" [Player]";
     Object sub=Class.forName("tv.emby.embyatv.api.SubtitleStreamInfo").getConstructor().newInstance();
     call(sub,"setIndex",tr.id);call(sub,"setLanguage",tr.lang);call(sub,"setDisplayTitle",title);call(sub,"setFormat",tr.mime.equals("application/pgs")?"pgssub":tr.mime);call(sub,"setIsForced",(tr.flags&FORCED)!=0);
     call(sub,"setDeliveryMethod",enumValue("mediabrowser.model.dlna.SubtitleDeliveryMethod","Embed"));subs.add(sub);
     // Marker for the overlay's has-subtitles check (Utils.GetSubtitleStreams), like the official CC entry 99.
     Object ms=Class.forName("mediabrowser.model.entities.MediaStream").getConstructor().newInstance();
     call(ms,"setIndex",tr.id);call(ms,"setLanguage",tr.lang);call(ms,"setDisplayTitle",title);call(ms,"setType",enumValue("mediabrowser.model.entities.MediaStreamType","Subtitle"));
     call(ms,"setDeliveryMethod",enumValue("mediabrowser.model.dlna.SubtitleDeliveryMethod","Embed"));media.add(ms);
    }
    s.list=subs;s.tracks=found;
   }
   List<Policy.Candidate> all=new ArrayList<>();
   for(Object x:subs){
    int i=num(call(x,"getIndex"));if(i>=Policy.BASE)continue;
    String label=str(call(x,"getDisplayTitle"));if(label.isEmpty())label=str(call(x,"getName"));
    int flags=Boolean.TRUE.equals(call(x,"getIsForced"))?FORCED:0;
    // Never auto-pick a burn-in subtitle: it restarts playback as a transcode.
    all.add(new Policy.Candidate(i,str(call(x,"getLanguage")),label,flags,false,!"Encode".equals(str(call(x,"getDeliveryMethod"))),null));
   }
   for(Track tr:s.tracks)all.add(new Policy.Candidate(tr.id,tr.lang,tr.label,tr.flags,true,true,tr.key));
   Object opts=get(c,"mCurrentOptions");Object req=opts==null?null:call(opts,"getSubtitleStreamIndex");
   Integer requested=req==null?null:Integer.valueOf(num(req));
   int chosen=Policy.choose(all,original,requested,s.manual,audioLanguage(vm));
   put(c,"mDefaultSubIndex",chosen);
   if(chosen>=0)s.pendingAuto=chosen;
   if(expose){Object f=get(c,"mFragment");if(f!=null)call(f,"updateDisplay");}
   Log.i(TAG,"Discovered "+found.size()+" player subtitle tracks; server embedded="+serverEmbedded+" exposed="+expose+" default "+original+" -> "+chosen+" requested="+requested+" manual="+(s.manual!=null));
  }catch(Throwable e){
   Log.e(TAG,"Discovery failed: "+e.getClass().getSimpleName()+": "+e.getMessage());
   s.list=null;s.tracks=new ArrayList<>();s.pendingAuto=NONE;
   try{strip(subs);strip(media);if(num(get(c,"mDefaultSubIndex"))>=Policy.BASE)put(c,"mDefaultSubIndex",Policy.OFF);}catch(Throwable ignored){}
  }
 }
 /** @return true when the call was consumed and the official switchSubtitleStream body must not run. */
 public static synchronized boolean select(Object c,int id){
  Session s=sessions.get(c);
  boolean auto=s!=null&&s.pendingAuto!=NONE&&s.pendingAuto==id;
  if(s!=null)s.pendingAuto=NONE;
  if(id<Policy.BASE){
   if(s!=null){s.selected=-1;if(!auto&&id<Policy.CLIENT_ONLY)s.manual=id<0?Policy.Choice.off():Policy.Choice.server(id);}
   return false;
  }
  try{
   Track tr=null;
   if(s!=null&&s.list!=null&&s.list==get(c,"mSubtitleStreams"))for(Track x:s.tracks)if(x.id==id)tr=x;
   if(tr==null||!apply(c,tr)){
    Log.w(TAG,"Ignoring player subtitle "+id+" that no longer matches the current tracks");
    if(auto)call(get(c,"mVideoManager"),"disableSubs");
    return true;
   }
   s.selected=id;if(!auto)s.manual=Policy.Choice.player(tr.key);
   // The server knows nothing about this track: tell it no server subtitle is active.
   Object opts=get(c,"mCurrentOptions");if(opts!=null)call(opts,"setSubtitleStreamIndex",Integer.valueOf(Policy.OFF));
   Object info=get(c,"mCurrentStreamInfo");if(info!=null)call(info,"setSubtitleStreamIndex",(Object)null);
   Object f=get(c,"mFragment");if(f!=null){call(f,"addManualSubtitles",(Object)null);call(f,"updateDisplay");}
   Log.i(TAG,"Selected player subtitle "+id+" language="+tr.lang+" label="+tr.label+(auto?" (default)":" (manual)"));
  }catch(Throwable e){Log.e(TAG,"Selection failed: "+e.getClass().getSimpleName()+": "+e.getMessage());}
  return true;
 }
 /** Re-validates the exact renderer/group/track against the live selector, which VideoManager.init recreates per playInternal. */
 private static boolean apply(Object c,Track tr)throws Exception{
  Object selector=get(get(c,"mVideoManager"),"mExoTrackSelector");if(selector==null)return false;
  return applyToSelector(selector,tr);
 }
 private static boolean applyToSelector(Object selector,Track tr)throws Exception{
  Object info=call(selector,"getCurrentMappedTrackInfo");if(info==null)return false;
  int count=num(call(info,"getRendererCount"));
  if(tr.r>=count||num(call(info,"getRendererType",tr.r))!=TEXT)return false;
  Object groups=call(info,"getTrackGroups",tr.r);if(tr.g>=num(get(groups,"length")))return false;
  Object group=call(groups,"get",tr.g);if(tr.t>=num(get(group,"length")))return false;
  int support=num(call(info,"getTrackSupport",tr.r,tr.g,tr.t));
  if(support!=FORMAT_HANDLED&&support!=FORMAT_HANDLED_PREFERRED)return false;
  Object f=call(group,"getFormat",tr.t);
  if(!tr.lang.equals(str(get(f,"language")))||!tr.label.equals(str(get(f,"label")))||!tr.mime.equals(str(get(f,"sampleMimeType"))))return false;
  Object builder=call(selector,"buildUponParameters");
  for(int r=0;r<count;r++)if(num(call(info,"getRendererType",r))==TEXT){call(builder,"setRendererDisabled",r,r!=tr.r);if(r!=tr.r)call(builder,"clearSelectionOverrides",r);}
  Object override=Class.forName(OVERRIDE).getConstructor(int.class,int[].class).newInstance(tr.g,new int[]{tr.t});
  call(builder,"setSelectionOverride",tr.r,groups,override);call(selector,"setParameters",builder);
  return true;
 }
 public static synchronized int current(Object c){
  try{Session s=sessions.get(c);return s!=null&&s.selected>=Policy.BASE&&s.list!=null&&s.list==get(c,"mSubtitleStreams")?s.selected:-1;}catch(Throwable e){return -1;}
 }
}
