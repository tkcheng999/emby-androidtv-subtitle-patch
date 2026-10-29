package mediabrowser.model.entities;
import mediabrowser.model.dlna.SubtitleDeliveryMethod;
/** Host-test stub: only the MediaStream members used by Bridge (REA evidence ev_936ee307…). */
public class MediaStream {
 private int Index;private String Language,DisplayTitle;private MediaStreamType Type;private SubtitleDeliveryMethod DeliveryMethod;private boolean IsExternal;
 public int getIndex(){return Index;}
 public void setIndex(int v){Index=v;}
 public String getLanguage(){return Language;}
 public void setLanguage(String v){Language=v;}
 public String getDisplayTitle(){return DisplayTitle;}
 public void setDisplayTitle(String v){DisplayTitle=v;}
 public MediaStreamType getType(){return Type;}
 public void setType(MediaStreamType v){Type=v;}
 public SubtitleDeliveryMethod getDeliveryMethod(){return DeliveryMethod;}
 public void setDeliveryMethod(SubtitleDeliveryMethod v){DeliveryMethod=v;}
 public void setIsExternal(boolean v){IsExternal=v;}
}
