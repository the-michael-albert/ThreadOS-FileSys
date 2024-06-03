// PingPong 4 4 & PingPong 5 5 & PingPong 6 6
// PingPong 4 4 ; PingPong 5 5 ; PingPong 6 6
// PingPong 4 4 ; PingPong 5 5 & PingPong 6 6

import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

class Shell extends Thread
{
    boolean runnable = true;
    static final boolean DEBUG = false;
    static final boolean ERR = true;

    static Stack<String> commandStack = new Stack<>();

    public Shell()
    {
        SysLib.cout("      _          _ _  \n");
        SysLib.cout("     | |        | | | \n");
        SysLib.cout("  ___| |__   ___| | | \n");
        SysLib.cout(" / __| '_ \\ / _ \\ | | \n");
        SysLib.cout(" \\__ \\ | | |  __/ | | \n");
        SysLib.cout(" |___/_| |_|\\___|_|_| \n");
        SysLib.cout("                     \n");
    }

    public void run()
    {
        int count = 1;
        while(runnable)
        {
            SysLib.cout("Shell["+ count +"]: ");
            StringBuffer buffer = new StringBuffer();
            SysLib.cin(buffer);
            String[] commands = SysLib.stringToArgs(buffer.toString());
            if(commands.length > 0)
            {
                // Execute the command and get the result
                int result = execute(buffer.toString(), Arrays.copyOfRange(commands, 1, commands.length));
                debug("Result: " + result);
                count++;
            }
        }
        SysLib.cout("__________                                            __  \n");
        SysLib.cout("\\______   \\ ____ _____    ____  ____     ____  __ ___/  |_ \n");
        SysLib.cout(" |     ___// __ \\\\__  \\ _/ ___\\/ __ \\   /  _ \\|  |  \\   __\\\n");
        SysLib.cout(" |    |   \\  ___/ / __ \\\\  \\__\\  ___/  (  <_> )  |  /|  |  \n");
        SysLib.cout(" |____|    \\___  >____  /\\___  >___  >  \\____/|____/ |__|  \n");
        SysLib.cout("               \\/     \\/     \\/    \\/                      \n");
        SysLib.sync();
        SysLib.exit();
    }


    /**
     * Executes a command with the given arguments and returns the result of the command
     * @param command The command to execute (the whole string)
     * @param args The arguments to the command as arr
     * @return The result of the command (System Code) or 0 if the command is exit
     */
    public int execute(String command, String[] args)
    {
//        debug("Command: " + command);
//        debug("Args: " + Arrays.toString(args));
        if(command.equals("exit"))
        {
            runnable = false;
            return 0;
        }
        else if(command.contains(";") && command.contains("&")){ // if command contains both ; and &
            return execCombo(command);
        }
        else if(command.contains(";")){ // if command contains ;
            return execSemiColon(command);
        }
        else if(command.contains("&")){ // if command contains &
            return execAmpersand(command);
        }
        return sysExec(SysLib.stringToArgs(command));
    }

    /**
     * Executes a command with a semicolon and an ampersand
     * @param command The command to execute
     * @return The result of the command
     */
    public int execCombo(String command){
        // if the command contains both ; and & then we need to check which one comes first
        if (command.indexOf(';') < command.indexOf('&')){ // if ; comes first then execute the command with ;
            return execSemiColon(command);
        } else { // if & comes first then execute the command with &
            return execAmpersand(command);
        }
    }

    /**
     * Executes a command with a semicolon
     * @param command The command to execute
     * @return The result of the command
     */
    public int execSemiColon(String command){
        String[] commands = command.split(";");
        String importantCommand = commands[0];

        //combine the rest of the commands and execute them
        String restOfCommands = "";
        for (int i = 1; i < commands.length; i++){
            restOfCommands += commands[i] + ";";
        }
        restOfCommands = restOfCommands.substring(0, restOfCommands.length() - 1);

        debug("FORKING " + importantCommand);
        execute(importantCommand, SysLib.stringToArgs(importantCommand));
        debug("Running command: " + importantCommand + " %THEN%" + restOfCommands);

        sysJoin();
        debug("JOINING " + importantCommand);

        debug("FORKING " + restOfCommands);
        execute(restOfCommands, SysLib.stringToArgs(restOfCommands));
        if ((!restOfCommands.contains("&") || !restOfCommands.contains(";"))  && commandStack.size() > 0){ // join only if restOf does not contain & or ; (let that command handle it)
            sysJoin();
            debug("JOINING " + restOfCommands);
        }
        // join only if restOf
        return 0;
    }


    /**
     * Executes a command with an ampersand
     * @param command The command to execute
     * @return The result of the command
     */
    public int execAmpersand(String command){
        String[] commands = command.split("&");
        String importantCommand = commands[0];
        String restOfCommands = "";
        for (int i = 1; i < commands.length; i++){
            restOfCommands += commands[i] + "&";
        }
        restOfCommands = restOfCommands.substring(0, restOfCommands.length() - 1);

        debug("FORKING " + importantCommand);
        execute(importantCommand, SysLib.stringToArgs(importantCommand));
        debug("FORKING " + restOfCommands);
        execute(restOfCommands, SysLib.stringToArgs(restOfCommands));
        debug("Concurrently running commands: " + importantCommand + " %AND% " + restOfCommands);
        sysJoin();
        debug("JOINING " + importantCommand );
        if ((!restOfCommands.contains("&") || !restOfCommands.contains(";"))  && commandStack.size() > 0){ // join only if restOf does not contain & or ; (let that command handle it)
            // join only if restOf does not contain & or ; (let that command handle it)
            sysJoin();
            debug("JOINING " + restOfCommands);
        }

        return 0;
    }



    /**
     * Debugging method to print out debug messages
     * @param message The message to print out
     */
    public static void debug(String message)
    {
        if(DEBUG){
            SysLib.cout("DEBUG: " + message + "\n");
        }
    }

    public static void error(String message)
    {
        if (ERR){
            SysLib.cerr("ERROR: " + message + "\n");
        }
    }

    public static void sysJoin()
    {
        error("Joining " + commandStack.peek());
        commandStack.pop();
        SysLib.join();
    }

    public static int sysExec( String[] args )
    {
        error("Executing" + Arrays.toString(args));
        commandStack.push(Arrays.toString(args));
        return SysLib.exec(args);
    }



}