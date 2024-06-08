import java.util.Vector;

public class FileTable {

  private Vector<FileTableEntry> table;  // the actual entity of this file table
  private Directory dir; // the root directory

  public final static int UNUSED = 0; // file is unused
  public final static int USED = 1;   // file is in use (not read/write)
  public final static int READ = 2;   // file is open for read
  public final static int WRITE = 3;  // file is open for write

  public FileTable(Directory directory) { // constructor
    table = new Vector<FileTableEntry>(); // instantiate a file (structure) table
    dir = directory;      // receive a reference to the Director
  }                       // from the file system

  // major public methods
  public synchronized FileTableEntry falloc(String filename, String mode) {
    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry

    short iNumber = -1; // inode's number
    Inode inode = null; // inode

    // loop until we find the file or create it
    while (true) {
      // if the given file is root...
      if (filename.equals("/")) {
        // the inode number must be 0
        iNumber = 0;
      }

      // if the given file is not root, get the inode number of the given file
      // from the directory
      else {
        // this should return either -1 if the file does not already exist or
        // the inode number of the file if it does exist
        iNumber = dir.namei(filename);
      }

      // if the file exists...
      if (iNumber >= 0) {
        // create a reference to the inode of the given file
        inode = new Inode(iNumber);

        // if the file is open for reading...
        if (mode.equals("r")) {
          // if the file is not currently being written to...
          if (inode.flag == UNUSED || inode.flag == USED ||
              inode.flag == READ) {
            inode.flag = READ;
            // end the loop
            break;
          }

          // if the file is currently being written to by another thread...
          else if (inode.flag == WRITE) {
            // wait for the file to be written to by the other thread
            try {
              wait();
            } catch (InterruptedException e) {
              // do nothing
            }
          }
        }

        // if the file is open for writing...
        else {
          // if the file is not currently being read or written to...
          if (inode.flag == UNUSED || inode.flag == USED) {
            inode.flag = WRITE;
            // end the loop
            break;
          }
          // if the file is currently being read or written to by another
          // thread...
          else {
            // wait for the file to be read or written to by the other thread
            try {
              wait();
            } catch (InterruptedException e) {
              // do nothing
            }
          }
        }
      }

      // if the file does not exist and is NOT open for reading...
      else if (!mode.equals("r")) {
        // allocate a new inode for the file and get it's inode number
        iNumber = dir.ialloc(filename);

        // if the inode was not successfully allocated...
        if (iNumber < 0) {
          // print an error message
          SysLib.cerr("Error: FileTable.falloc(): "
                      + "Could not allocate inode for file " + filename);
          return null;
        }

        // create a reference to the inode of the given file
        inode = new Inode(iNumber);
        inode.flag = WRITE;
        // end the loop
        break;
      }

      // if the file does not exist and IS open for reading...
      else {
        // should not create a new file if it was opened for reading
        // print an error message
        SysLib.cerr("Error: FileTable.falloc(): "
                    + "File " + filename + " does not exist");
        return null;
      }
    }

    // by this point, an inode has been successfully found or created
    inode.count++;         // increment the inode's count
    inode.toDisk(iNumber); // write the inode to the disk

    // create a new file table entry for the file
    FileTableEntry entry = new FileTableEntry(inode, iNumber, mode);
    table.addElement(entry); // add the new file table entry to the table
    return entry;            // return the new file table entry
  }

  public synchronized boolean ffree(FileTableEntry e) {
    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table

    // if the file table entry is not in the table...
    if (!table.contains(e)) {
      // return false
      return false;
    }

    // get a reference to the inode of the file table entry
    Inode inode = new Inode(e.iNumber);

    // remove the file table entry from the table
    table.removeElement(e);

    // if the inode is flagged as read...
    if (inode.flag == READ) {
      // if the inode is only being read by one thread...
      if (inode.count == 1) {
        notify(); // notify the thread that the file is no longer being read
        // set the inode's flag to used
        inode.flag = USED;
      }
    }

    // if the inode is flagged as write...
    else if (inode.flag == WRITE) {
      // set the inode's flag to used
      inode.flag = USED;
      notifyAll(); // notify all threads that the file is no longer being
                   // written to
    }

    inode.count--;           // decrement the inode's count
    inode.toDisk(e.iNumber); // write the inode to the disk
    return true;             // return true
  }

  public synchronized boolean fempty() {
    return table.isEmpty(); // return if table is empty
  }                         // should be called before starting a format
}