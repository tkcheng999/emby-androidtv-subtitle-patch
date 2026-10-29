package android.util;
/** Host-test stub only. Never put tests/stubs on the APK classpath. */
public final class Log {
 public static int i(String tag,String msg){System.out.println("I/"+tag+": "+msg);return 0;}
 public static int w(String tag,String msg){System.out.println("W/"+tag+": "+msg);return 0;}
 public static int e(String tag,String msg){System.out.println("E/"+tag+": "+msg);return 0;}
}
