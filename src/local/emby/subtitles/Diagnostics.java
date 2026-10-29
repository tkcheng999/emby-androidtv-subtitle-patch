package local.emby.subtitles;
import android.util.Log;
import java.util.*;
/** Optional read-only cue diagnostics, enabled only by EMBY_TRACE_CUES at build time. */
public final class Diagnostics {
 private static int count;
 public static void cues(Object view,List<?> cues){
  if(cues==null||cues.isEmpty()||count++>=12)return;
  try{int bitmaps=0;for(Object cue:cues)if(cue.getClass().getField("bitmap").get(cue)!=null)bitmaps++;
   Log.i("EmbySubtitlePatch","Rendered cues="+cues.size()+" bitmaps="+bitmaps+" visibility="+view.getClass().getMethod("getVisibility").invoke(view)+" alpha="+view.getClass().getMethod("getAlpha").invoke(view));
  }catch(Throwable e){Log.w("EmbySubtitlePatch","Cue diagnostic failed: "+e.getClass().getSimpleName());}
 }
}
