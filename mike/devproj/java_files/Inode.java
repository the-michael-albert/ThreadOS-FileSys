public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers
    private final static int pointerCount = 16;      // block size

    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    Inode( ) {                                     // a default constructor
        length = 0;
        count = 0;
        flag = 1;
        for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
        indirect = -1;
    }

    Inode( short iNumber ) {                       // retrieving inode from disk
        int blockNumber = 1 + iNumber / 16;
        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread( blockNumber, data );

        // 16 inodes per block
        int offset = ( iNumber % pointerCount ) * iNodeSize;

        length = SysLib.bytes2int( data, offset );
        offset += 4; // 4 bytes read
        count = SysLib.bytes2short( data, offset );
        offset += 2; // 6 bytes read
        flag = SysLib.bytes2short( data, offset );
        offset += 2; // 8 bytes read

        for ( int i = 0; i < directSize; i++ ) {
            direct[i] = SysLib.bytes2short( data, offset );
            offset += 2; // 10 bytes read
        }

        indirect = SysLib.bytes2short( data, offset );
    }


    void toDisk( short iNumber ) {                  // save to disk as the i-th inode
        byte[] data = new byte[Disk.blockSize];
        int offset = 0;
        
        SysLib.int2bytes( length, data, offset );
        offset += 4; // 4 bytes written
        SysLib.short2bytes( count, data, offset );
        offset += 2; // 6 bytes written
        SysLib.short2bytes( flag, data, offset );
        offset += 2; // 8 bytes written

        for ( int i = 0; i < directSize; i++ ) {
            SysLib.short2bytes( direct[i], data, offset );
            offset += 2; //write all direct pointers
        }

        SysLib.short2bytes( indirect, data, offset ); // write indirect pointer
        offset += 2; // 32 bytes written at this point (iNodeSize)

        
    }
}
