package tv.emby.embyatv.api;
import mediabrowser.model.dlna.SubtitleDeliveryMethod;
/** Host-test stub with the members REA reported for 2.5.03g (evidence ev_871076fb…). */
public class SubtitleStreamInfo {
 private SubtitleDeliveryMethod DeliveryMethod;private String DisplayTitle,Format,Language,Name,Url;private int Index;private boolean IsExternalUrl,IsForced;private long Offset;
 public SubtitleDeliveryMethod getDeliveryMethod(){return DeliveryMethod;}
 public void setDeliveryMethod(SubtitleDeliveryMethod v){DeliveryMethod=v;}
 public String getDisplayTitle(){return DisplayTitle;}
 public void setDisplayTitle(String v){DisplayTitle=v;}
 public String getFormat(){return Format;}
 public void setFormat(String v){Format=v;}
 public int getIndex(){return Index;}
 public void setIndex(int v){Index=v;}
 public boolean getIsExternalUrl(){return IsExternalUrl;}
 public void setIsExternalUrl(boolean v){IsExternalUrl=v;}
 public boolean getIsForced(){return IsForced;}
 public void setIsForced(boolean v){IsForced=v;}
 public String getLanguage(){return Language;}
 public void setLanguage(String v){Language=v;}
 public String getName(){return Name;}
 public void setName(String v){Name=v;}
 public long getOffset(){return Offset;}
 public void setOffset(long v){Offset=v;}
 public String getUrl(){return Url;}
 public void setUrl(String v){Url=v;}
}
