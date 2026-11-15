package dev.saad.activitytrio;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.json.*;
import static org.junit.Assert.*;

/** Optional integration with freshly downloaded public responses, never account credentials. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class LiveFeedTest {
    private Path folder;
    @Before public void setup(){String path=System.getenv("ACTIVITY_TRIO_LIVE_FIXTURES");Assume.assumeTrue("Set the optional public-response fixture folder",path!=null);folder=Paths.get(path);}
    @Test public void currentGithubResponseParses()throws Exception{
        String html=new String(Files.readAllBytes(folder.resolve("github.html")),StandardCharsets.UTF_8);ActivityData data=Parsers.github(html,System.getenv().getOrDefault("ACTIVITY_TRIO_GITHUB_USER","demo-user"),2026,"America/New_York",Instant.now().toString());assertEquals(365,data.cells.size());assertTrue(data.total>0);Files.write(folder.resolve("github-seed.json"),data.json().toString().getBytes(StandardCharsets.UTF_8));
    }
    @Test public void currentCodeforcesResponseParses()throws Exception{
        JSONObject response=new JSONObject(new String(Files.readAllBytes(folder.resolve("codeforces.json")),StandardCharsets.UTF_8));assertEquals("OK",response.getString("status"));assertTrue(response.getJSONArray("result").length()<1000);ActivityData data=Parsers.codeforces(response.getJSONArray("result"),System.getenv().getOrDefault("ACTIVITY_TRIO_CODEFORCES_USER","demo-user"),2026,"America/New_York",Instant.now().toString());assertTrue(data.total>0);Files.write(folder.resolve("codeforces-seed.json"),data.json().toString().getBytes(StandardCharsets.UTF_8));
    }
    @Test public void currentLeetcodeResponseParses()throws Exception{
        Path file=folder.resolve("leetcode.json");Assume.assumeTrue(Files.exists(file));JSONObject response=new JSONObject(new String(Files.readAllBytes(file),StandardCharsets.UTF_8));String user=response.getJSONObject("data").getJSONObject("matchedUser").getString("username");ActivityData data=Parsers.leetcode(response,user,2026,"UTC",Instant.now().toString());assertNotNull(data.problems);assertTrue(data.problems.getInt("available")>0);assertEquals("leetcode",data.provider);
    }
}
