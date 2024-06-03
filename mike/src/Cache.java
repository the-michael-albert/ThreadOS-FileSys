import java.util.*;

public class Cache {
	final int blockSize; // size of a block in bytes
	final int cacheBlocks; // number of blocks in the cache
	Entry[] pageTable = null; // metadata for each block
	List<byte[]> pages = null; // the cache itself
	int pointer = 0; // pointer to the next block to be replaced

	public Cache(int blockSize, int cacheBlocks) {
		this.blockSize = blockSize;
		this.cacheBlocks = cacheBlocks;

		pages = new ArrayList<byte[]>(cacheBlocks);
		pageTable = new Entry[cacheBlocks];

		for (int i = 0; i < cacheBlocks; i++) {
			pageTable[i] = new Entry();
			byte[] buffer = new byte[blockSize];
			pages.add(buffer);
		}

		pointer = cacheBlocks - 1;
	}


	private class Entry {
		public boolean ref = false; // reference bit
		public boolean dirty = false; // dirty bit
		public int index = -1; // block index

		public Entry() {
		}
	}


	/**
	 * Find a free page in the cache to use
	 * @return the index of the free page, or -1 if no free page is found
	 */
	private int findFreePage() {
		for(int i = 0; i < cacheBlocks; ++i) {
			if (this.pageTable[i].index == -1) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Find a page in the cache to replace
	 * @return the index of the page, or -1 if the block is not in the cache
	 */
	private int nextVictim() {
		//loop infinitely until we find a victim (using pointer loop algorithm)
		while(true) {
			this.pointer = (this.pointer + 1) % this.pageTable.length; // increment pointer (circularly)
			if (!this.pageTable[this.pointer].ref) {
				return this.pointer;
			}

			this.pageTable[this.pointer].ref = false;
		}
	}

	/**
	 * Write back the block to disk if it is dirty
	 * @param victimEntry the index of the block to write back
	 */
	private void writeBack(int victimEntry) {
		if (pageTable[victimEntry].index != -1 && pageTable[victimEntry].dirty) {
			byte[] tempBufferedArr = this.pages.get(victimEntry);
			SysLib.rawwrite(this.pageTable[victimEntry].index, tempBufferedArr);
			this.pageTable[victimEntry].dirty = false;
		}
	}

	/**
	 * Read a block from disk into the cache
	 * @param blockId the block to read
	 * @param buffer the buffer to read into (via copy)
	 * @return true if the block was read successfully, false otherwise
	 */
	public synchronized boolean read(int blockId, byte buffer[]) {
		if (blockId < 0) {
			return false;
		} else {
			int rotatedPointer;
			byte[] newByteArray;
			for(rotatedPointer = 0; rotatedPointer < this.pageTable.length; ++rotatedPointer) {
				if (this.pageTable[rotatedPointer].index == blockId) {
					newByteArray = this.pages.get(rotatedPointer);
					System.arraycopy(newByteArray, 0, buffer, 0, this.blockSize);
					this.pageTable[rotatedPointer].ref = true;
					return true;
				}
			}
			rotatedPointer = this.findFreePage();
			if (rotatedPointer == -1) {
				rotatedPointer = this.nextVictim();
			}

			//write back the block to disk if it is dirty
			this.writeBack(rotatedPointer);

			//read block from disk
			SysLib.rawread(blockId, buffer);

			//copy buffer into cache
			newByteArray = new byte[this.blockSize];
			System.arraycopy(buffer, 0, newByteArray, 0, this.blockSize);
			this.pages.set(rotatedPointer, newByteArray);

			//update page table (CLEANUP)
			this.pageTable[rotatedPointer].ref = true;
			this.pageTable[rotatedPointer].index = blockId;
			return true;
		}
	}

	/**
	 * Write a block to disk from the cache
	 * @param blockId the block to write
	 * @param buffer the buffer to write from (via copy)
	 * @return true if the block was written successfully, false otherwise
	 */
	public synchronized boolean write(int blockId, byte buffer[]) {
		if (blockId < 0) {
			return false;
		} else {
			int rotatedPointer;
			byte[] newByteArray;
			for(rotatedPointer = 0; rotatedPointer < this.pageTable.length; ++rotatedPointer) {
				if (this.pageTable[rotatedPointer].index == blockId) {
					newByteArray = this.pages.get(rotatedPointer);
					System.arraycopy(buffer, 0, newByteArray, 0, this.blockSize);
					this.pageTable[rotatedPointer].ref = true;
					this.pageTable[rotatedPointer].dirty = true;
					return true;
				}
			}

			if ((rotatedPointer = this.findFreePage()) == -1) {
				rotatedPointer = this.nextVictim();
			}

			//write back the block to disk if it is dirty
			this.writeBack(rotatedPointer);

			//copy buffer into cache
			newByteArray = new byte[this.blockSize];
			System.arraycopy(buffer, 0, newByteArray, 0, this.blockSize);
			this.pages.set(rotatedPointer, newByteArray);

			//update page table (CLEANUP)
			this.pageTable[rotatedPointer].ref = true;
			this.pageTable[rotatedPointer].index = blockId;
			this.pageTable[rotatedPointer].dirty = true;
			return true;
		}
	}

	//flush all dirty blocks to disk and then sync the disk

	public synchronized void sync() {
		for (int i = 0; i < cacheBlocks; i++) {
			writeBack(i);
		}
		SysLib.sync();
	}

	//flush all dirty blocks to disk
	public synchronized void flush() {
		for (int i = 0; i < cacheBlocks; i++) {
			writeBack(i);
			pageTable[i].ref = false;
			pageTable[i].index = -1;
		}
		SysLib.sync();
	}
}
