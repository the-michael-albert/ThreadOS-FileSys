/**
 * Is a test program for HW1B. It repeats 100 times of printing out a
 * given word and sleeping for a given millisecond,
 *
 */
public class PingPong extends Thread {
    private String word; // a word to print out
    private int msec;    // a millisecond to wait
    public PingPong( String[] args ) {
	word = args[0];
	msec = Integer.parseInt( args[1] );
    }
    public void run( ) {
	for ( int j = 0; j < 100; j++ ) {
	    SysLib.cout( word + " " );
	    SysLib.sleep( msec );
	}
	SysLib.cout( "\n" );
	SysLib.exit( );
    }
}
