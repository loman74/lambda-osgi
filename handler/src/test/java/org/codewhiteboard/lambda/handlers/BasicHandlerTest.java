package org.codewhiteboard.lambda.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codewhiteboard.lambda.handlers.BasicHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class BasicHandlerTest {

    private Map<String,String> input;

    @Before
    public void createInput() throws IOException {
        // set up your sample input object here.
        input = new HashMap<String,String>();
        input.put("jar", "s3://xxxxxxx/xx.jar");
        input.put("class" , "org.xx.xx.xx");
        input.put("method","mainProxy");
        input.put("command_line" , "arg 1 arg2 arg3");
        input.put("log_debug" , "");
        
    }

    private Context createContext() {
        TestContext ctx = new TestContext();

        // TODO: customize your context here if needed.
        ctx.setFunctionName("Your Function Name");

        return ctx;
    }

    @Test
    public void testGenericLambda() {
        BasicHandler handler = new BasicHandler();
        Context ctx = createContext();


        String output = handler.handleRequest(input, ctx);
        System.out.print(output);
        Assert.assertTrue(output.contains("Exit code: 0"));
    }
}