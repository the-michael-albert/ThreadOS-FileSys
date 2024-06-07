import java.util.Arrays;

public class SuperBlock {
    public int totalBlocks; // the number of disk blocks
    public int inodeBlocks; // the number of inodes
    public int freeList;    // the block number of the free list's head

    public static final int INODE_DEF = 64;



    public SuperBlock( int diskSize ) { // diskSize is the number of blocks in the disk
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread( 0, superBlock );
        totalBlocks = SysLib.bytes2int( superBlock, 0 );
        inodeBlocks = SysLib.bytes2int( superBlock, 4 );
        freeList = SysLib.bytes2int( superBlock, 8 );

        if (
                totalBlocks == diskSize && // check if the total blocks is equal to the disk size
                        inodeBlocks > 0 && // check if the inode blocks is greater than 0
                        freeList >= 2  // check if the free list is greater than or equal to 2
        ) {
            return;
        } else {
            totalBlocks = diskSize; // set the total blocks to the disk size
            format( inodeBlocks ); // format the disk
        }

        inodeBlocks = diskSize;
    }

    // format the disk
    public void format( int totalInodes ) {
        if ( totalInodes <= 0 ) {
            totalInodes = INODE_DEF;
        }
        SysLib.cerr( "formatting with " + totalInodes + " inodes\n" );

        this.inodeBlocks = totalInodes;
        for (int i = 0; i < totalInodes; i++ ) {
            Inode inode = new Inode();
            inode.flag = 0;
            inode.toDisk( i );
        }

        this.freeList = 2 + (this.inodeBlocks * Inode.iNodeSize) / Disk.blockSize;

        // write the free list to the disk
        for (int i = this.freeList; i < this.totalBlocks; i++ ) {
            byte[] block = new byte[Disk.blockSize];


            // write the block as free to the disk
            for (int j = 0; j < Disk.blockSize; j++ ) {
                block[j] = 0;
            }

            SysLib.cerr( "writing block [free]" + i + " to disk\n" );

            // write the block to the disk
            SysLib.int2bytes( i + 1, block, 0 );
            SysLib.rawwrite( i, block );
        }

        sync();
    }

    // sync the superblock to disk
    public void sync() {
        byte[] superBlock = new byte[Disk.blockSize];
        // write the blocks to the superblock buffer
        SysLib.int2bytes( totalBlocks, superBlock, 0 );
        // write the inode count to the superblock buffer
        SysLib.int2bytes( inodeBlocks, superBlock, 4 );
        // write the free list to the superblock buffer (all the free blocks)
        SysLib.int2bytes( freeList, superBlock, 8 );

        // write the superblock to disk (all 16 bytes of it)
        SysLib.rawwrite( 0, superBlock );

        SysLib.cerr( "synced superblock" );
    }


}
