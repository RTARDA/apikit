/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit.leagues;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.port;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mule.module.apikit.Configuration.APPLICATION_RAML;
import static org.mule.module.apikit.util.RegexMatcher.matchesPattern;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;

import com.jayway.restassured.RestAssured;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;

@ArtifactClassLoaderRunnerConfig(plugins = {"org.mule.modules:mule-module-sockets", "org.mule.modules:mule-module-http-ext"},
        providedInclusions = "org.mule.modules:mule-module-sockets")
public class LeaguesTestCase extends MuleArtifactFunctionalTestCase
{

    @Rule
    public DynamicPort serverPort = new DynamicPort("serverPort");

    @Override
    public int getTestTimeoutSecs()
    {
        return 6000;
    }

    @Override
    protected void doSetUp() throws Exception
    {
        RestAssured.port = serverPort.getNumber();
        super.doSetUp();
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/apikit/leagues/leagues-http-flow-config.xml";
    }

    @Test
    public void resourceNotFound() throws Exception
    {
        given().header("Accept", "application/json")
        .expect().response().statusCode(404)
                .body(is("resource not found"))
                .when().get("/api/matches");
    }

    @Test
    public void methodNotAllowed() throws Exception
    {
        expect().response().statusCode(405)
                .body(is("method not allowed"))
                .when().delete("/api/leagues");
    }

    @Test
    public void unsupportedMediaType() throws Exception
    {
        given().body("Liga Criolla").contentType("text/plain")
        .expect().response().statusCode(415)
                .body(is("unsupported media type"))
                .when().post("/api/leagues");
    }

    @Test
    public void notAcceptable() throws Exception
    {
        given().header("Accept", "text/plain")
        .expect().response().statusCode(406)
                .body(is("not acceptable"))
                .when().get("/api/leagues");
    }

    @Test
    public void badRequestJson() throws Exception
    {
        given().body("{\"liga\": \"Criolla\"}").contentType("application/json")
                .expect().response().statusCode(400)
                .body(is("bad request"))
                .when().post("/api/leagues");
    }

    @Test
    public void badRequestXml() throws Exception
    {
        given().body("<leaguee xmlns=\"http://mulesoft.com/schemas/soccer\"><name>MLS</name></leaguee>")
                .contentType("text/xml")
                .expect().response().statusCode(400)
                .body(is("bad request"))
                .when().post("/api/leagues");
    }

    @Test
    public void getOnLeaguesJson() throws Exception
    {
        given().header("Accept", "application/json")
            .expect()
                .response().body("leagues.name", hasItems("Liga BBVA", "Premier League"))
                .header("Content-type", "application/json").statusCode(200)
            .when().get("/api/leagues");
    }

    @Test
    public void getOnLeaguesJsonTrailingSlash() throws Exception
    {
        given().header("Accept", "application/json")
            .expect()
                .response().body("leagues.name", hasItems("Liga BBVA", "Premier League"))
                .header("Content-type", "application/json").statusCode(200)
            .when().get("/api/leagues/");
    }

    @Test
    public void getOnLeaguesXml() throws Exception
    {
        given().header("Accept", "text/xml")
            .expect()
                .response().body("leagues.league.name", hasItems("Liga BBVA", "Premier League"))
                .header("Content-type", "text/xml").statusCode(200)
            .when().get("/api/leagues");
    }

    @Test
    public void postOnLeaguesJson() throws Exception
    {
        given().body("{ \"name\": \"Major League Soccer\" }")
                .contentType("application/json")
            .expect().statusCode(201)
                .header("Location", "http://localhost:" + serverPort.getValue() + "/api/leagues/4")
                .body(is("")).header("Content-Length", "0")
            .when().post("/api/leagues");
    }

    @Test
    public void postOnLeaguesXml() throws Exception
    {
        given().body("<league xmlns=\"http://mulesoft.com/schemas/soccer\"><name>MLS</name></league>")
                .contentType("text/xml")
            .expect().statusCode(201)
                .header("Location", "http://localhost:" + serverPort.getValue() + "/api/leagues/4")
            .when().post("/api/leagues");
    }

    @Test
    public void postOnLeaguesXmlWindows1252() throws Exception
    {
        InputStream inputStrem = this.getClass().getClassLoader().getResourceAsStream("org/mule/module/apikit/leagues/league-windows1252.xml");
        String body = IOUtils.toString(inputStrem, "windows-1252");
        Matcher matcher = Pattern.compile(".*<description>(.*)</description>.*").matcher(body);
        matcher.find();
        String responseBody = matcher.group(1);
        given().body(body)
                .contentType("text/xml;charset=windows-1252")
                .header("Accept", "text/plain")
            .expect().statusCode(201)
                .body(is(responseBody))
                .header("Location", "http://localhost:" + serverPort.getValue() + "/api/leagues/4")
                .when().post("/api/leagues");
    }

    @Test
    public void postCustomStatus() throws Exception
    {
        given().body("{ \"name\": \"(invlid name)\" }")
                .contentType("application/json")
            .expect()
                .statusCode(400)
                .body(is("Invalid League Name"))
            .when().post("/api/leagues");
    }

    @Test
    public void getOnSingleLeagueJson() throws Exception
    {
        given().header("Accept", "application/json")
            .expect()
                .response().body("name", is("Liga BBVA"))
                .header("Content-type", "application/json").statusCode(200)
            .when().get("/api/leagues/liga-bbva");
    }

    @Test
    public void getOnSingleLeagueXml() throws Exception
    {
        given().header("Accept", "text/xml")
            .expect()
                .response().body("league.name", is("Liga BBVA"))
                .header("Content-type", "text/xml").statusCode(200)
            .when().get("/api/leagues/liga-bbva");
    }

    @Test
    public void putOnSingleLeagueJson() throws Exception
    {
        given().body("{ \"name\": \"Liga Hispanica\" }")
                .contentType("application/json")
            .expect()
                .statusCode(204)
                .body(is(""))
            .when().put("/api/leagues/liga-bbva");

        given().header("Accept", "application/json")
                .expect()
                .response().body("name", is("Hispanic League"))
                .header("Content-type", "application/json").statusCode(200)
                .when().get("/api/leagues/liga-bbva");
    }

    @Test
    public void putOnSingleLeagueXml() throws Exception
    {
        given().body("<league xmlns=\"http://mulesoft.com/schemas/soccer\"><name>Hispanic League</name></league>")
                .contentType("text/xml")
            .expect()
                .statusCode(204)
                .body(is(""))
            .when().put("/api/leagues/liga-bbva");

        given().header("Accept", "application/json")
            .expect()
                .response().body("name", is("Hispanic League"))
                .header("Content-type", "application/json").statusCode(200)
            .when().get("/api/leagues/liga-bbva");
    }

    @Test
    public void putCustomStatus() throws Exception
    {
        given().body("{ \"name\": \"(invlid name)\" }")
                .contentType("application/json")
            .expect()
                .statusCode(400)
                .body(is(""))
            .when().put("/api/leagues/liga-bbva");
    }

    @Test
    public void putMultiPartFormData() throws Exception
    {
        given().multiPart("description", "Barcelona Badge")
                .multiPart("image", "bbva.jpg", this.getClass().getClassLoader().getResourceAsStream("org/mule/module/apikit/leagues/bbva.jpg"))
                .expect().statusCode(200)
                .body("upload", is("OK"))
                .when().put("/api/leagues/liga-bbva/badge");
    }

    @Test
    public void deleteOnSingleLeague() throws Exception
    {
        expect().response()
            .statusCode(204).body(is(""))
            .when().delete("/api/leagues/liga-bbva");
    }

    @Test
    public void uriParamMaxLenError() throws Exception
    {
        given().header("Accept", "application/json")
            .expect()
                .response().body(is("bad request"))
                .statusCode(400)
            .when().get("/api/leagues/a-name-long-enough-not-to-be-valid");
    }

    @Test
    public void uriParamPatternError() throws Exception
    {
        given().header("Accept", "application/json")
            .expect()
                .response().body(is("bad request"))
                .statusCode(400)
            .when().get("/api/leagues/invalid_name");
    }

    @Test
    public void getTeamsQueryParams() throws Exception
    {
        given().header("Accept", "application/json")
            .expect()
                .response().body("name", hasItems("Barcelona", "Real Madrid", "Valencia", "Athletic Bilbao", "Atletico Madrid"))
                .header("Content-type", "application/json").header("preferred-team", "BCN").statusCode(200)
            .when().get("/api/leagues/liga-bbva/teams");
    }

    @Test
    public void getTeamsHeaderCaseInsensitive() throws Exception
    {
        given().header("Accept", "application/json").header("Preferred", "RMD")
            .expect()
                .response().body("name", hasItems("Barcelona", "Real Madrid", "Valencia", "Athletic Bilbao", "Atletico Madrid"))
                .header("Content-type", "application/json").header("preferred-team", "RMD").statusCode(200)
            .when().get("/api/leagues/liga-bbva/teams");
    }

    @Test
    public void getTeamsQueryParamsOffset3() throws Exception
    {
        given().header("Accept", "application/json")
            .expect()
                .response().body("name", allOf(hasItems("Athletic Bilbao", "Atletico Madrid"), not(hasItem("Barcelona")),
                                               not(hasItem("Real Madrid")), not(hasItem("Valencia"))))
                .header("Content-type", "application/json").statusCode(200)
                .when().get("/api/leagues/liga-bbva/teams?offset=3");
    }

    @Test
    public void getTeamsQueryParamsMinimumError() throws Exception
    {
        given().header("Accept", "application/json")
            .expect()
                .response().body(is("bad request"))
                .statusCode(400)
                .when().get("/api/leagues/liga-bbva/teams?offset=-1");
    }

    @Test
    public void getTeamsQueryParamsMaximumError() throws Exception
    {
        given().header("Accept", "application/json")
            .expect()
                .response().body(is("bad request"))
                .statusCode(400)
                .when().get("/api/leagues/liga-bbva/teams?limit=11");
    }

    @Test
    public void getRamlFromApi() throws Exception
    {
        given().header("Accept", APPLICATION_RAML)
            .expect()
                .response().body(matchesPattern("(?s).*baseUri: \"http://[localhost0-9.]+:" + port + "/api\".*"))
                .header("Content-type", APPLICATION_RAML).statusCode(200)
            .when().get("/api");
    }

    @Test
    public void getRamlFromEmbeddedConsole() throws Exception
    {
        given().header("Accept", APPLICATION_RAML)
            .expect()
                .response().body(matchesPattern("(?s).*baseUri: \"http://[localhost0-9.]+:" + port + "/api\".*"))
                .header("Content-type", APPLICATION_RAML).statusCode(200)
            .when().get("/api/console");
    }

    @Test
    public void getRamlWrongContentType() throws Exception
    {
        given().header("Accept", "application/json")
            .expect()
                .response().body(containsString("resource not found"))
                .statusCode(404)
            .when().get("/api");
    }

    @Test
    public void console() throws Exception
    {
        given().header("Accept", "text/html")
            .expect()
                .response().body(allOf(containsString("<title>API Console</title>"),
                                       matchesPattern("(?s).*src=\"\\./\\?\".*")))
                .header("Content-type", "text/html").statusCode(200)
            .when().get("/api/console/index.html");
    }

    @Test
    public void consoleDirectory() throws Exception
    {
        given().header("Accept", "text/html")
            .expect()
                .response().body(allOf(containsString("<title>API Console</title>"),
                                       matchesPattern("(?s).*src=\"\\./\\?\".*")))
                .header("Content-type", "text/html").statusCode(200)
            .when().get("/api/console/");
    }

    @Test
    public void consoleDirectoryNoSlash() throws Exception
    {
        given().redirects().follow(false).header("Accept", "text/html")
            .expect()
                .response().statusCode(301)
                .header("Location", "http://localhost:" + serverPort.getValue() + "/api/console/")
            .when().get("/api/console");
    }

    @Test
    public void getAnyResponse() throws Exception
    {
        given().header("Accept", "application/json")
            .expect()
                .response().body("name", is("Liga BBVA"))
                .header("Content-type", "application/json").statusCode(200)
            .when().get("/api/leagues/liga-bbva/badge");
    }

    @Test
    public void getDefaultResponseCode() throws Exception
    {
        given()
            .expect()
                .response().statusCode(200)
            .when().get("/api/leagues/liga-bbva/teams/leader");
    }

    @Test
    public void resolveTemplateWithSpecialChars() throws Exception
    {
        given()
            .expect()
                .response().statusCode(200)
            .when().get("/api/leagues/league+one");
    }


}
