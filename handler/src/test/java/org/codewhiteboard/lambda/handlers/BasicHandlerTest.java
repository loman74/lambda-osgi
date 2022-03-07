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
        input.put("jar", "s3://4652-5751-2377-application-dev-staging/CRISP/crisp-dm-client/crisp-dm-client-0.0.1-SNAPSHOT-DM.jar");
        input.put("class" , "org.finra.crisp.CrispDMClient");
        input.put("method","mainProxy");
        input.put("command_line" , "get_data -e dev -k stats_dt -n crisp -o replication_slots_hs -p 2021-10-31 -t txt -u devintenvironment ");
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