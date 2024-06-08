public class TestAwesome extends Thread{

    public TestAwesome() {
        super();
    }

    public void run() {

        SysLib.cout( "_________________________________\n" );
        SysLib.cout( "TEST AW3S0M3 Started\n" );
        SysLib.cout( "_________________________________\n" );
        SysLib.cout( "1 -> Running file system operations test\n" );
        testFileSystemOps();
        SysLib.cout( "_________________________________\n" );
        SysLib.cout( "2 -> Running Inode test\n" );
        testInode();
        SysLib.cout( "_________________________________\n" );
        SysLib.cout( "3 -> Running Directory test\n" );
        testDirectory();
        SysLib.cout( "_________________________________\n" );
        SysLib.cout( "4 -> Running SuperBlock test\n" );
        testSuperBlock();

        SysLib.exit();

    }

    public static void testDirectory() {
        // test directory operations
        SysLib.cout("\t1. Creating new directory with 64 inodes\n");
        Directory dir = new Directory(64);
        SysLib.cout("\t2. Creating new file named 'test'\n");
        short iNumber = dir.ialloc("test");
        if (iNumber >= 0) {
          SysLib.cout("\t\t✓ File created successfully\n");
        } else {
          SysLib.cerr("\t\tFailed to create file\n");
          return;
        }
    
        SysLib.cout("\t3. Finding inode number for 'test'\n");
        short iNumber2 = dir.namei("test");
        if (iNumber == iNumber2) {
          SysLib.cout("\t\t✓ Inode number found successfully\n");
        } else {
          SysLib.cerr("\t\tFailed to find inode number\n");
          return;
        }
    
        SysLib.cout("\t4. Deleting file\n");
        if (dir.ifree(iNumber)) {
          SysLib.cout("\t\t✓ File deleted successfully\n");
        } else {
          SysLib.cerr("\t\tFailed to delete file\n");
          return;
        }
    
        SysLib.cout("\t✓ Directory operations test successful\n");
      }

    public static void testInode(){
        Inode inode = new Inode();
        inode.toDisk((short)0);
        Inode inode2 = new Inode((short)0);
        if(inode.length == inode2.length && inode.count == inode2.count && inode.flag == inode2.flag){
            SysLib.cout("\t✓ Inode Test passed\n");
            SysLib.cout("\t\t-> ✓ replication succeeded\n");
        } else {
            SysLib.cerr("\tInode test failed\n");
        }
    }

    public static void testFileSystemOps(){
        // test the file system operations
        String TestString = "hello world";

        SysLib.cout( "\t1. Creating a new file system with 100 blocks\n" );
        FileSystem fs = new FileSystem( 100 );
        SysLib.cout( "\t2. Formatting the file system with 64 inodes\n" );
        fs.format( 64 );
        SysLib.cout( "\t3. Creating a new file named 'test'\n" );
        FileTableEntry fte = fs.open( "test", "w" );
        
        //ensure the file was created
        if ( fte == null ) {
            SysLib.cerr( "\t\tFailed to create the file\n" );
            return;
        }

        SysLib.cout( "\t4. Writing " + TestString + " to the file\n" );
        byte[] data = "hello world".getBytes( );
        fs.write( fte, data );
        // close the file
        fs.close( fte );
        
        // test the write using read
        SysLib.cout( "\t5. Reading the file\n" );
        byte[] buffer = new byte[TestString.length( )];
        fte = fs.open( "test", "r" );
        fs.read( fte, buffer );
        fs.close( fte );
        if ( new String( buffer ).equals( TestString ) ) {
            SysLib.cout( "\t\t✓ Read and write successful\n" );
        } else {
            SysLib.cerr( "\t\tRead and write failed\n" );
            return;
        }

        SysLib.cout( "\t6. Deleting the file\n" );
        SysLib.cout(fs.delete( "test" ) + "");

        if ( fs.open( "test", "r" ) == null ) {
            SysLib.cout( "\t\t✓ File deleted successfully\n" );
        } else {
            SysLib.cerr( "\t\tFailed to delete the file\n" );
            return;
        }

        SysLib.cout( "\t✓ File system operations test successful\n" );
    }

    public static void testSuperBlock(){
        SysLib.cout("\t1. Creating a new superblock with 100 blocks\n");
        SuperBlock sb = new SuperBlock(100);
        if(sb.totalBlocks == 100 && sb.inodeBlocks == 64 && sb.freeList == 66){
            SysLib.cout("\t\t-> ✓replication succeeded\n");
            SysLib.cout("\t✓ SuperBlock Test passed\n");
        } else {
            SysLib.cerr("\tSuperBlock test failed\n");
        }
    }
}
