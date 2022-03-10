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
//        input.put("jar", "s3://4652-5751-2377-application-dev-staging/CRISP/crisp-dm-client/crisp-dm-client-0.0.1-SNAPSHOT-DM.jar");
        input.put("jar", "s3://4652-5751-2377-application-dev-staging/CRISP/crisp-data-compare/vdiff/vdiff-0.0.1-SNAPSHOT.jar");
//        input.put("class" , "org.finra.crisp.CrispDMClient");
        input.put("class" , "org.springframework.boot.loader.JarLauncher");
//        input.put("method","mainProxy");
//        input.put("command_line" , "get_data -e dev -k stats_dt -n crisp -o replication_slots_hs -p 2022-03-08 -t txt -u devintenvironment ");
        input.put("command_line" , "data-compare --s-schema=mrws_surv_owner --s-table=pattern_alert_type_mkt_class_map  --t-schema=mrws_surv_owner --t-table=pattern_alert_type_mkt_class_map --s-user=gk_k26363_ro --s-password=4glQUpDPW2ZFVcJ4  --t-user=gk_k26363_ro --t-password=ZkmqIHthKTYYgK0M  --s-engine=postgresql --s-host=crisp-prod-pg-dsh-c.cszkeajok0so.us-east-1.rds.amazonaws.com --s-db=crisp --t-db=daslp --t-engine=postgresql --t-host=dasl-pg-prod.cszkeajok0so.us-east-1.rds.amazonaws.com -e manmadh.lodugu@finra.org --fetchLimit=30000");
        input.put("log_debug" , "org.finra.crisp.vdiff.comparators.DataCompareImpl");
        
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