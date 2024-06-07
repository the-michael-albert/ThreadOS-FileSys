public class Inode {
    public final static int iNodeSize = 32;       // fix to 32 bytes
    public final static int directSize = 11;      // # direct pointers
    public final static int pointerCount = 16;      // block size

    public static final int ERROR_BLOCK_SET = -1;
    public static final int ERROR_BLOCK_NOT_SET = -2;

    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    Inode( ) {                                     // a default constructor (provided by the system)
        length = 0;
        count = 0;
        flag = 1;
        for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
        indirect = -1;
    }

    /**
     * Retrieve the inode from disk
     * @param iNumber the inode number to retrieve from disk
     */
    Inode( short iNumber ) {                       // retrieving inode from disk
        int blockNumber = 1 + iNumber / 16;
        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread( blockNumber, data );

        // 16 inodes per block
        int offset = ( iNumber % pointerCount ) * iNodeSize;

        length = SysLib.bytes2int( data, offset ); // the length of the corresponding file 
        offset += 4; // 4 bytes read
        count = SysLib.bytes2short( data, offset ); // the number of file (structure) table entries that point to this inode
        offset += 2; // 6 bytes read
        flag = SysLib.bytes2short( data, offset ); // the flag to indicate if it is unused (= 0), used(= 1), or in some other status
        offset += 2; // 8 bytes read

        for ( int i = 0; i < directSize; i++ ) {
            direct[i] = SysLib.bytes2short( data, offset );
            offset += 2; // 10 bytes read
        }

        indirect = SysLib.bytes2short( data, offset );
    }


    /**
     * Save the inode to disk
     * @param iNumber the inode number to save to disk
     */
    void toDisk(int iNumber){
        toDisk((short)iNumber);
    }

    /**
     * Save the inode to disk
     * @param iNumber the inode number
     */
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

        int blockNumber = 1 + iNumber / 16;
        byte[] readData = new byte[Disk.blockSize];
        SysLib.rawread( blockNumber, readData );

        offset = ( iNumber % pointerCount ) * iNodeSize; // offset to the i-th inode
        System.arraycopy( data, 0, readData, offset, iNodeSize ); // copy data to readData
        SysLib.rawwrite( blockNumber, readData ); // write back to disk
    }

    /**
     * Find the target block of the inode
     * @param offset the offset of the block
     * @return the target block number
     */
    int findTargetBlock( int offset ) {
        int destBlock = offset / Disk.blockSize;
        if ( destBlock < directSize ) { //if target block is in the direct pointers
            return direct[destBlock]; // return direct pointer
        } else if ( indirect < 0 ) { // failsafe if indirect pointer is not set
            return -1;
        } else { // search indirect pointers
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread( indirect, data );
            return SysLib.bytes2short( 
                data,  // data
                ( destBlock - directSize ) * 2 // offset is the block number
            ); 
        }
    }

    /**
     * Create or Register a target block to the inode
     * @param offset the offset of the block
     * @param targetBlockNum the target block to register
     * @return 0 if successful, -1 if block is already set, -2 if previous block is not set, -3 if indirect pointer is not set
     */
    int registerTargetBlock(int offset, short targetBlockNum) { 
        int destBlock = offset / Disk.blockSize;
        if ( destBlock < directSize ) { //if target block is in the direct pointers
            if ( direct[destBlock] != -1 ) { // if direct pointer is already set
                return Inode.ERROR_BLOCK_SET;
            } else if ( destBlock > 0 && direct[destBlock - 1] == -1 ) { // if previous block is not set
                return Inode.ERROR_BLOCK_NOT_SET;
            } else {
                // set direct pointer to target block
                direct[destBlock] = targetBlockNum; 
                return 0;
            }
        } else if ( indirect < 0 ) { // failsafe if indirect pointer is not set
            return -3;
        } else { // search indirect pointers
            
            // read indirect block
            byte[] data = new byte[Disk.blockSize]; 
            SysLib.rawread( indirect, data );

            // calculate indirect block
            int indirectBlock = destBlock - directSize;
            if ( SysLib.bytes2short( data, indirectBlock * 2 ) != -1 ) { // if indirect pointer is already set
                return -1;
            } else {
                SysLib.short2bytes( targetBlockNum, data, indirectBlock * 2 );
                SysLib.rawwrite( indirect, data );
                return 0;
            }
        }
    }

    /**
     * Unregister a target block from the inode
     * @param offset the offset of the block
     * @return the target block number
     */
    boolean registerIndexBlock( short indexBlockNum ) {
        if ( indirect != -1 ) { // if indirect pointer is already set
            return false; // return false (fail)
        } else {
            indirect = indexBlockNum;
            byte[] data = new byte[Disk.blockSize];
            // set all indirect pointers to -1
            for ( int i = 0; i < Disk.blockSize / 2; i++ ) {
                SysLib.short2bytes( (short) -1, data, i * 2 );
            }
            SysLib.rawwrite( indirect, data );
            return true; // return true (success)
        }
    }

    /**
     * Unregister the index block
     * @return the data of the index block
     */
    byte[] unregisterIndexBlock() {
        if ( indirect < 0 ) { // if indirect pointer is not set
            return null; // return null
        } else {
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread( indirect, data ); // read indirect block
            indirect = -1; // reset indirect pointer
            return data; // return indirect block data
        }
    }
}
