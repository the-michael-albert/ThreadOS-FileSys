    public class Boot {
   static final int OK = 0;
   static final int ERROR = -1;

   public static void main(String[] var0) {
      SysLib.cerr("threadOS ver 1.0:\n");
      SysLib.boot();
      SysLib.cerr("Type ? for help\n");
      String[] var1 = new String[]{"Loader"};
      SysLib.exec(var1);
   }
}