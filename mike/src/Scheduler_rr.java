import java.util.*; // Scheduler_rr.java

public class Scheduler_rr extends Thread
{
    private Vector<TCB> queue;
    private int timeSlice;
    private static final int DEFAULT_TIME_SLICE = 1000;

    // New data added to the original algorithm 
    private boolean[] tids; // Indicate which ids have been used
    private static final int DEFAULT_MAX_THREADS = 10000;

    // A new feature added to the original algorithm 
    // Allocate an ID array, each element indicating if that id has been used
    private int nextId = 0;
    private void initTid( int maxThreads ) {
	tids = new boolean[maxThreads];
	for ( int i = 0; i < maxThreads; i++ )
	    tids[i] = false;
    }

    // A new feature added to the original algorithm 
    // Search an available thread ID and provide a new thread with this ID
    private int getNewTid( ) {
	for ( int i = 0; i < tids.length; i++ ) {
	    int tentative = ( nextId + i ) % tids.length;
	    if ( tids[tentative] == false ) {
		tids[tentative] = true;
		nextId = ( tentative + 1 ) % tids.length;
		return tentative;
	    }
	}
	return -1;
    }

    // A new feature added to the original algorithm 
    // Return the thread ID and set the corresponding tids element to be unused
    private boolean returnTid( int tid ) {
	if ( tid >= 0 && tid < tids.length && tids[tid] == true ) {
	    tids[tid] = false;
	    return true;
	}
	return false;
    }

    // A new feature added to the original algorithm 
    // Retrieve the current thread's TCB from the queue
    public TCB getMyTcb( ) {
	Thread myThread = Thread.currentThread( ); // Get my thread object
	synchronized( queue ) {
	    for ( int i = 0; i < queue.size( ); i++ ) {
		TCB tcb = queue.elementAt( i );
		Thread thread = tcb.getThread( );
		if ( thread == myThread ) // if this is my TCB, return it
		    return tcb;
	    }
	}
	return null;
    }

    // A new feature added to the original algorithm 
    // Return the maximal number of threads to be spawned in the system
    public int getMaxThreads( ) {
	return tids.length;
    }

    public Scheduler_rr( ) {
	timeSlice = DEFAULT_TIME_SLICE;
	queue = new Vector<TCB>( );
	initTid( DEFAULT_MAX_THREADS );
    }

    public Scheduler_rr(int quantum ) {
	timeSlice = quantum;
	queue = new Vector<TCB>( );
	initTid( DEFAULT_MAX_THREADS );
    }

    // A new feature added to the original algorithm 
    // A constructor to receive the max number of threads to be spawned
    public Scheduler_rr(int quantum, int maxThreads ) {
	timeSlice = quantum;
	queue = new Vector<TCB>( );
	initTid( maxThreads );
    }

    private void schedulerSleep( ) {
	try {
	    Thread.sleep( timeSlice );
	} catch ( InterruptedException e ) {
	}
    }

    // A modified addThread of the original algorithm
    public TCB addThread( Thread t ) {
	TCB parentTcb = getMyTcb( ); // get my TCB and find my TID
	int pid = ( parentTcb != null ) ? parentTcb.getTid( ) : -1;
	int tid = getNewTid( ); // get a new TID
	if ( tid == -1)
	    return null;
	TCB tcb = new TCB( t, tid, pid ); // create a new TCB
	queue.add( tcb );
	return tcb;
    }

    // A new feature added to the original algorithm
    // Removing the TCB of a terminating thread
    public boolean deleteThread( ) {
	TCB tcb = getMyTcb( ); 
	if ( tcb!= null ) {
	    this.interrupt( ); 
	    return tcb.setTerminated( );
	} else
	    return false;
    }

    public void sleepThread( int milliseconds ) {
	try {
	    sleep( milliseconds );
	} catch ( InterruptedException e ) { }
    }
    
    // A modified run of the original algorithm
    public void run( ) {
	Thread current = null;

	while ( true ) {
	    try {
		// get the next TCB and its thrad
		if ( queue.size( ) == 0 )
		    continue;
		TCB currentTCB = queue.firstElement( );
		if ( currentTCB.getTerminated( ) == true ) {
		    queue.remove( currentTCB );
		    returnTid( currentTCB.getTid( ) );
		    continue;
		    }
		current = currentTCB.getThread( );

		if ( ( current != null ) ) {
		    if ( current.isAlive( ) )
			current.resume( );
		    else
			// Spawn must be controlled by Scheduler
			// Scheduler must start a new thread
			current.start( ); 
		}
		
		schedulerSleep( );
		// System.out.println("* * * Context Switch * * * ");
		
		synchronized( queue ) {
		    if ( current != null && current.isAlive( ) )
			current.suspend( );
		    queue.remove( currentTCB ); // rotate this TCB to the end
		    queue.add( currentTCB );
		}
	    } catch ( NullPointerException e3 ) { };
	}
    }
}
