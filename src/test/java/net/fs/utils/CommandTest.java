package net.fs.utils;

import org.junit.Test;

/**
 * Created by Poison on 7/11/2016.
 */
public class CommandTest {

    @Test
    public void execute() throws Exception {
        Command.execute("java -version");
    }

}