public class Directory {
  private static int maxChars = 30; // max characters of each file name

  // Directory entries
  private int fsize[];     // each element stores a different file size.
  private char fnames[][]; // each element stores a different file name.

  public Directory(int maxInumber) { // directory constructor
    fsizes = new int[maxInumber];    // maxInumber = max files
    for (int i = 0; i < maxInumber; i++)
      fsize[i] = 0; // all file size initialized to 0
    fnames = new char[maxInumber][maxChars];
    String root = "/";                         // entry(inode) 0 is "/"
    fsize[0] = root.length();                  // fsize[0] is the size of "/".
    root.getChars(0, fsizes[0], fnames[0], 0); // fnames[0] includes "/"
  }

  public int bytes2directory(byte data[]) {
    // assumes data[] received directory information from disk
    // initializes the Directory instance with this data[]

    // read in the fsize[] array
    for (int i = 0; i < f.size.length; i++)
      fsize[i] = SysLib.bytes2int(data, i * 4);

    // read in the fnames[] array
    // offset is the size of fsize[] array times 4 to account for the 4 bytes
    // per int
    int offset = fsize.length * 4;
    for (int i = 0; i < fnames.length; i++) {
      // create a new string from the data array for each file name
      // read in the maxChars * 2 bytes to account for the 2 bytes per char
      String fname = new String(data, offset, maxChars * 2);
      // copy the characters from the string to the fnames array
      // getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin)
      // fsize[i] is equal to the number of characters in the file name
      fname.getChars(0, fsize[i], fnames[i], 0);
      // increment the offset by the number of characters read in
      offset += maxChars * 2;
    }
    return 0;
  }

  public byte[] directory2bytes() {
    // converts and return Directory information into a plain byte array
    // this byte array will be written back to disk
    // note: only meaningfull directory information should be converted
    // into bytes.

    // create a new byte array to store the directory information
    byte[] data = new byte[(fsize.length * 4) + (fnames.length * maxChars * 2)];

    // write the fsize[] array to the data array
    for (int i = 0; i < fsize.length; i++)
      SysLib(int2bytes(fsize[i]), data, i * 4);

    // write the fnames[] array to the data array
    // offset is the size of fsize[] array times 4 to account for the 4 bytes
    // per int
    int offset = fsize.length * 4;
    for (int i = 0; i < fnames.length; i++) {
      // create a new string from the fnames array
      String fname = new String(fnames[i], 0, fsize[i]);
      // copy the characters from the string to the data array
      // getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin)
      // fsize[i] is equal to the number of characters in the file name
      fname.getBytes(0, fsize[i], data, offset);
      // increment the offset by the number of characters written
      offset += maxChars * 2;
    }
    return data;
  }

  public short ialloc(String filename) {
    // filename is the one of a file to be created.
    // allocates a new inode number for this filename
    return 0;
  }

  public boolean ifree(short iNumber) {
    // deallocates this inumber (inode number)
    // the corresponding file will be deleted.
    return true;
  }

  public short namei(String filename) {
    // returns the inumber corresponding to this filename
    return 0;
  }
}