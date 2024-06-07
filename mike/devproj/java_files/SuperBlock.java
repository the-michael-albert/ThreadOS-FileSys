import java.util.Arrays;

public class SuperBlock {
    public int totalBlocks; // the number of disk blocks
    public int inodeBlocks; // the number of inodes
    public int freeList;    // the block number of the free list's head

    

    public SuperBlock( int diskSize ) { // diskSize is the number of blocks in the disk
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread( 0, superBlock );
        totalBlocks = SysLib.bytes2int( superBlock, 0 );
        inodeBlocks = SysLib.bytes2int( superBlock, 4 );
        freeList = SysLib.bytes2int( superBlock, 8 );
        System.out.println( Arrays.toString( superBlock ));

        if ( 
            totalBlocks == diskSize && // check if the total blocks is equal to the disk size
            inodeBlocks > 0 && // check if the inode blocks is greater than 0
            freeList >= 2  // check if the free list is greater than or equal to 2
        ) {
            return;
        } else {
            totalBlocks = diskSize; // set the total blocks to the disk size
            System.out.println( "INODEBLOCKS:" + inodeBlocks );
            format( inodeBlocks ); // format the disk
        }

        inodeBlocks = diskSize;
    }

    // format the disk
    public void format( int totalInodes ) {
        this.inodeBlocks = totalInodes;
        Inode inode = new Inode();
        inode.flag = 2;
        inode.toDisk( 0 );
        
        // initialize all inodes as unused
        for ( short i = 1; i < totalInodes; i++ ) {
            inode = new Inode(); // must create a new inode object for each iteration
            inode.toDisk( i ); // write to disk
        }
        
        // initialize the free list
        freeList = ( totalInodes * 32 ) / Disk.blockSize;

        // loop through all blocks and write the free list
        for ( int i = freeList; i < totalBlocks; i++ ) {
            byte[] data = new byte[Disk.blockSize];
            for ( int j = 0; j < Disk.blockSize; j++ ) {
                data[j] = 0;
            }
            SysLib.int2bytes( i + 1, data, 0 );
            SysLib.rawwrite( i, data );
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
    }


 }
