/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit.apifiles;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.port;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mule.module.apikit.AbstractConfiguration.APPLICATION_RAML;

import org.mule.module.apikit.util.FunctionalAppDeployTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import com.jayway.restassured.RestAssured;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

public class ApiTestCase extends FunctionalAppDeployTestCase
{
    private static final String CONSOLE_PATH = "/konsole";

    @Rule
    public DynamicPort serverPort = new DynamicPort("serverPort");
    @Rule
    public DynamicPort serverPort2 = new DynamicPort("serverPort2");

    @Override
    public int getTestTimeoutSecs()
    {
        return 6000;
    }

    @Override
    public void doSetUp() throws Exception
    {
        RestAssured.port = serverPort2.getNumber();
        super.doSetUp();
    }

    @Test
    public void ramlApi() throws Exception
    {
        File appDir = new File("src/test/resources", "org/mule/module/apikit/apifiles");
        String appName = "ramlApi";
        deployExplodedApp(appDir, appName);
        deploymentService.start();
        assertApplicationDeploymentSuccess(applicationDeploymentListener, appName);
        assertRaml();
    }

    private void assertRaml() throws Exception
    {
        given().header("Accept", APPLICATION_RAML)
                .expect()
                .response().body(containsString("baseUri: http://localhost:" + port + "/api"), containsString("!include example.json"))
                .header("Content-type", APPLICATION_RAML).statusCode(200)
                .when().get("/api/?raml");
    }

}
