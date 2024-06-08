import java.io.File;

public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    public FileSystem( int diskBlocks ) {
        // create superblock, and format disk with 64 inodes in default
        superblock = new SuperBlock( diskBlocks );
    
        // create directory, and register "/" in directory entry 0
        directory = new Directory( superblock.inodeBlocks );
    
        // file table is created, and store directory in the file table
        filetable = new FileTable( directory );
    
        // directory reconstruction
        FileTableEntry dirEnt = open( "/", "r" );
        int dirSize = fsize( dirEnt );
        if ( dirSize > 0 ) {
            byte[] dirData = new byte[dirSize];
            read( dirEnt, dirData );
            directory.bytes2directory( dirData );
        }
        close( dirEnt );
    }

    //DONE
    void sync( ) {
        // directory synchronizatioin
        FileTableEntry dirEnt = open( "/", "w" );
        byte[] dirData = directory.directory2bytes( );
        write( dirEnt, dirData );
        close( dirEnt );
    
        // superblock synchronization
        superblock.sync( );
    }

    //DONE
    boolean format( int files ) {
        // wait until all filetable entries are destructed
        while ( filetable.fempty( ) == false ){} 
        // ; I do not like the semicolon approach for line format safety
    
        // format superblock, initialize inodes, and create a free list
        superblock.format( files );
    
        // create directory, and register "/" in directory entry 0
        directory = new Directory( superblock.inodeBlocks );
    
        // file table is created, and store directory in the file table
        filetable = new FileTable( directory );
    
        return true;
    }

    //DONE
    FileTableEntry open( String filename, String mode ) {
        FileTableEntry ftEnt = filetable.falloc( filename, mode );
        if ( mode == "w" ) {
            if ( !deallocAllBlocks( ftEnt ) ){
                return null;
            } 
        }
        return ftEnt;
    }

    //DONE
    boolean close( FileTableEnty ftEnt ) {
        // filetable entry is freed
        synchronized ( ftEnt ) {
            // need to decrement count; also: changing > 1 to > 0 below
            ftEnt.count--;
            if ( ftEnt.count > 0 ) // my children or parent are(is) using it
                return true;
        }
        return filetable.ffree( ftEnt );
    }
	
	
    //DONE
    int fsize( FileTableEntry ftEnt ) {
        synchronized ( ftEnt ) {
            return ftEnt.inode.length;
        }
    }


    //DONE?
    int read( FileTableEntry ftEnt, byte[] buffer ) {
        if ( ftEnt.mode == "w" || ftEnt.mode == "a" ){
            return -1;
        }
    
        int offset   = 0;              // buffer offset
        int left     = buffer.length;  // the remaining data of this buffer
    
        synchronized ( ftEnt ) {
			// repeat reading until no more data  or reaching EOF
            while ( left > 0 && ftEnt.seekPtr < fsize( ftEnt ) ) {
                int blockNum = ftEnt.inode.findTargetBlock( ftEnt.seekPtr );
                if ( blockNum == -1 ) {
                    break;
                }
                byte[] block = new byte[Disk.blockSize];
                SysLib.rawread( blockNum, block );
                int blockOffset = ftEnt.seekPtr % Disk.blockSize;
                int blockLeft = Disk.blockSize - blockOffset;
                int amount = Math.min( blockLeft, left );
                System.arraycopy( block, blockOffset, buffer, offset, amount );
                ftEnt.seekPtr += amount;
                offset += amount;
                left -= amount;
            }

            return offset;
        }
    }

    int write( FileTableEntry ftEnt, byte[] buffer ) {
        // at this point, ftEnt is only the one to modify the inode
        if ( ftEnt.mode == "r" )
            return -1;
    
        synchronized ( ftEnt ) {
            int offset   = 0;              // buffer offset
            int left     = buffer.length;  // the remaining data of this buffer
    
            while ( left > 0 ) {
                int blockNum = ftEnt.inode.findTargetBlock( ftEnt.seekPtr );
                // if the block number is not set...
                if ( blockNum == -1 ) {
                    short newBlockNum = (short)superblock.getFreeBlock( );
                    // register the new block number and get operations the status code
                    int statuscode = ftEnt.inode.registerTargetBlock( ftEnt.seekPtr, newBlockNum )
                    
                    // if the block registration fails...
                    if ( statuscode == -1 ) {
                        // terminate the write operation
                        return -1;
                    }
                    
                    // if the indirect pointer is not set...
                    if ( statuscode == -3) {
                        // allocate a new block for the indirect pointer
                        short indirectBlockNum = (short)superblock.getFreeBlock( );
                        // register index block at block indirect number
                        // if the registration fails...
                        if ( !ftEnt.inode.registerIndexBlock( indirectBlockNum ) ) {
                            // terminate the write operation
                            return -1;
                        }
                        // register the target block number
                        // if the registration fails...
                        if ( ftEnt.inode.registerTargetBlock( ftEnt.seekPtr, newBlockNum ) != 0 ) {
                            // terminate the write operation
                            return -1;
                        }
                    }
            
                    blockNum = newBlockNum;
                }

                // if the block number is set, read the block
                byte[] block = new byte[Disk.blockSize];
                SysLib.rawread( blockNum, block );

                // write data to the block
                int blockOffset = ftEnt.seekPtr % Disk.blockSize; // offset of the block
                int blockLeft = Disk.blockSize - blockOffset; // remaining space of the block
                int amount = Math.min( blockLeft, left ); // amount of data to write

                // copy data to the block 
                System.arraycopy( buffer, offset, block, blockOffset, amount );
                
                // write the block to the disk
                SysLib.rawwrite( blockNum, block );

                // update the seek point to the position after the block
                ftEnt.seekPtr += amount;
                // update the offset to the position after the block
                offset += amount;
                //dec left by amount of data written
                left -= amount;

                if ( ftEnt.seekPtr > ftEnt.inode.length ) {
                    ftEnt.inode.length = ftEnt.seekPtr;
                }
            }
            // write the inode to the disk
            ftEnt.inode.toDisk( ftEnt.iNumber );
            return offset;
        }
    }

    private boolean deallocAllBlocks( FileTableEntry ftEnt ) {
        if ( ftEnt.inode.count != 1 ) { // not the only one using it
            return false;
        }


        // deallocate all blocks
        byte[] data = ftEnt.inode.unregisterIndexBlock( );
        if ( data != null ) {
            short blockNum = SysLib.bytes2short( data, 0 );
            while ( blockNum != -1 ) {
                superblock.returnBlock( blockNum );
                blockNum = SysLib.bytes2short( data, 2 );
            }
        }

        // deallocate direct blocks
        for ( short i = 0; i < ftEnt.inode.directSize; i++ ) {
            if ( ftEnt.inode.direct[i] != -1 ) { // if the block is set
                superblock.returnBlock( ftEnt.inode.direct[i] );
                ftEnt.inode.direct[i] = -1;
            }
        }

        //once we run out of direct blocks, we need to write back the inode
        ftEnt.inode.toDisk( ftEnt.iNumber );
        return true;
    }

	
	
	
    boolean delete( String filename ) {
        FileTableEntry ftEnt = open( filename, "w" );
        short iNumber = ftEnt.iNumber;
        return close( ftEnt ) && directory.ifree( iNumber );
    }

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    boolean isOffsetWithinBounds( FileTableEntry ftEnt, int offset ) {
        return offset >= 0 && offset <= fsize( ftEnt );
    }

    int seek( FileTableEntry ftEnt, int offset, int whence ) {
        synchronized ( ftEnt ) {
            if ( whence == SEEK_SET ) {
                //offset is within bounds of file
                if ( isOffsetWithinBounds( ftEnt, offset )) {
                    ftEnt.seekPtr = offset;
                } else {
                    return -1; //error
                }

            } else if ( whence == SEEK_CUR ) {
                //offset is within bounds of file
                if ( isOffsetWithinBounds( ftEnt, ftEnt.seekPtr + offset )) {
                    ftEnt.seekPtr += offset;
                } else {
                    return -1; //error
                }
            } else if ( whence == SEEK_END ) {
                //offset is within bounds of file
                if ( isOffsetWithinBounds( ftEnt, fsize( ftEnt ) + offset )) {
                    ftEnt.seekPtr = fsize( ftEnt ) + offset;
                } else {
                    return -1; //error
                }
            }
            /*
            System.out.println( "seek: offset=" + offset +
                    " fsize=" + fsize( ftEnt ) +
                    " seekptr=" + ftEnt.seekPtr +
                    " whence=" + whence );
            */
		}
        return -1;
    }
}
