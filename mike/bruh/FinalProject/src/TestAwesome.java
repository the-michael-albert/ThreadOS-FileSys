public class TestAwesome extends Thread{

    public TestAwesome() {
        super();
    }

    public void run() {
        SysLib.cout( "TEST AW3S0M3 Started\n" );
        SysLib.cout( "1 -> Running file system operations test\n" );
        testFileSystemOps();
        SysLib.cout( "2 -> Running Inode test\n" );
        testInode();
    }

    public static void testInode(){
        Inode inode = new Inode();
        inode.toDisk((short)0);
        Inode inode2 = new Inode((short)0);
        if(inode.length == inode2.length && inode.count == inode2.count && inode.flag == inode2.flag){
            SysLib.cout("Inode Test passed\n");
            SysLib.cout("\t-> replication succeeded\n");
        } else {
            SysLib.cout("Inode test failed\n");
        }
    }

    public static void testFileSystemOps(){
        // test the file system operations
        String TestString = "hello world";

        SysLib.cout( "Creating a new file system with 100 blocks\n" );
        FileSystem fs = new FileSystem( 100 );
        SysLib.cout( "Formatting the file system with 64 inodes\n" );
        fs.format( 64 );
        SysLib.cout( "Creating a new file named 'test'\n" );
        FileTableEntry fte = fs.open( "test", "w" );
        
        //ensure the file was created
        if ( fte == null ) {
            SysLib.cerr( "Failed to create the file\n" );
            return;
        }

        SysLib.cout( "Writing " + TestString + " to the file\n" );
        byte[] data = "hello world".getBytes( );
        fs.write( fte, data );
        // close the file
        fs.close( fte );
        
        // test the write using read
        SysLib.cout( "Reading the file\n" );
        byte[] buffer = new byte[TestString.length( )];
        fte = fs.open( "test", "r" );
        fs.read( fte, buffer );
        fs.close( fte );
        if ( new String( buffer ).equals( TestString ) ) {
            SysLib.cout( "Read and write successful\n" );
        } else {
            SysLib.cerr( "Read and write failed\n" );
            return;
        }

        SysLib.cout( "Deleting the file\n" );
        SysLib.cout(fs.delete( "test" ) + "");

        if ( fs.open( "test", "r" ) == null ) {
            SysLib.cout( "File deleted successfully\n" );
        } else {
            SysLib.cerr( "Failed to delete the file\n" );
            return;
        }

        SysLib.cout( "File system operations test successful\n" );
    }
}
