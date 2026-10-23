/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class SocialNetworkTest {

    /*
     * Testing strategy
     *
     * guessFollowsGraph(tweets)
     *   Partition on number of tweets:     0, 1, >1
     *   Partition on mentions per tweet:   0, 1, >1
     *   Partition on tweets per author:    1, >1 (mentions must accumulate)
     *   Partition on username case:        same case, mixed case (case-insensitive)
     *   Partition on self-mention:         author mentions self, does not
     *   Partition on malformed mention:    e-mail address (not a mention)
     *
     *   The postcondition is underdetermined (an implementation may use other
     *   evidence than @-mentions), so tests never assert that a user does NOT
     *   follow someone unless the tweets give no possible evidence for it. They
     *   only assert that the required evidence ("Ernie @-mentions Bert" =>
     *   "Ernie follows Bert") is present, that nobody follows themselves, and
     *   that every username in the graph occurs in the tweets.
     *   Usernames are compared in lower case, since they are case-insensitive.
     *
     * influencers(followsGraph)
     *   Partition on graph size:           empty, 1 user, >1 users
     *   Partition on followers:            none, exactly one user with most
     *                                      followers, tie for most followers
     *   Partition on user position:        user only appears as a followee (not a key)
     *   Every distinct username in the graph must appear exactly once in the
     *   result, in non-increasing order of follower count. Ties may be in any order.
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");

    private static Tweet tweet(long id, String author, String text) {
        return new Tweet(id, author, text, d1);
    }

    /** @return the lower-cased set of users that user follows in graph, empty if none. */
    private static Set<String> followsOf(Map<String, Set<String>> graph, String user) {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(user)) {
                for (String followed : entry.getValue()) {
                    result.add(followed.toLowerCase());
                }
            }
        }
        return result;
    }

    /** @return every username (key or value) in graph, lower-cased. */
    private static Set<String> allUsers(Map<String, Set<String>> graph) {
        Set<String> users = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            users.add(entry.getKey().toLowerCase());
            for (String followed : entry.getValue()) {
                users.add(followed.toLowerCase());
            }
        }
        return users;
    }

    private static Set<String> setOf(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    private static Map<String, Set<String>> graphOf(Object... pairs) {
        Map<String, Set<String>> graph = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            @SuppressWarnings("unchecked")
            Set<String> followed = (Set<String>) pairs[i + 1];
            graph.put((String) pairs[i], followed);
        }
        return graph;
    }

    private static int followerCount(Map<String, Set<String>> graph, String user) {
        int count = 0;
        for (Set<String> followed : graph.values()) {
            for (String name : followed) {
                if (name.equalsIgnoreCase(user)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // ---------------------------------------------------------------- guessFollowsGraph

    @Test
    public void testGuessFollowsGraphEmpty() {
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(new ArrayList<>());

        assertTrue("expected empty graph", followsGraph.isEmpty());
    }

    @Test
    public void testGuessFollowsGraphNoMentions() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "alyssa", "is it reasonable to talk about rivest so much?"),
                tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype"));
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue("alyssa has no evidence of following anyone", followsOf(followsGraph, "alyssa").isEmpty());
        assertTrue("bbitdiddle has no evidence of following anyone", followsOf(followsGraph, "bbitdiddle").isEmpty());
        for (Set<String> followed : followsGraph.values()) {
            assertTrue("no tweet mentions anyone", followed.isEmpty());
        }
    }

    @Test
    public void testGuessFollowsGraphSingleMention() {
        List<Tweet> tweets = Arrays.asList(tweet(1, "ernie", "hello @bert"));
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue("expected ernie to follow bert", followsOf(followsGraph, "ernie").contains("bert"));
    }

    @Test
    public void testGuessFollowsGraphMultipleMentionsInOneTweet() {
        List<Tweet> tweets = Arrays.asList(tweet(1, "ernie", "@bert and @elmo and @grover, lunch?"));
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        Set<String> followed = followsOf(followsGraph, "ernie");
        assertTrue("expected ernie to follow bert", followed.contains("bert"));
        assertTrue("expected ernie to follow elmo", followed.contains("elmo"));
        assertTrue("expected ernie to follow grover", followed.contains("grover"));
    }

    @Test
    public void testGuessFollowsGraphMultipleTweetsFromOneUser() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "ernie", "hi @bert"),
                tweet(2, "ernie", "no mention here"),
                tweet(3, "ernie", "also hi @elmo"),
                tweet(4, "ernie", "@bert again"));
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        Set<String> followed = followsOf(followsGraph, "ernie");
        assertTrue("expected ernie to follow bert", followed.contains("bert"));
        assertTrue("expected ernie to follow elmo", followed.contains("elmo"));
    }

    @Test
    public void testGuessFollowsGraphMultipleAuthors() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "ernie", "hi @bert"),
                tweet(2, "bert", "hi @ernie, hi @elmo"));
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue(followsOf(followsGraph, "ernie").contains("bert"));
        assertTrue(followsOf(followsGraph, "bert").contains("ernie"));
        assertTrue(followsOf(followsGraph, "bert").contains("elmo"));
    }

    @Test
    public void testGuessFollowsGraphCaseInsensitive() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "Ernie", "hi @BERT"),
                tweet(2, "ERNIE", "hi @Elmo"),
                tweet(3, "bert", "hi @eRnIe"));
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue(followsOf(followsGraph, "ernie").contains("bert"));
        assertTrue(followsOf(followsGraph, "ernie").contains("elmo"));
        assertTrue(followsOf(followsGraph, "bert").contains("ernie"));

        Set<String> lowerCaseKeys = new HashSet<>();
        for (String key : followsGraph.keySet()) {
            assertTrue("username must appear at most once as a key, ignoring case",
                    lowerCaseKeys.add(key.toLowerCase()));
        }
    }

    @Test
    public void testGuessFollowsGraphNobodyFollowsThemselves() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "ernie", "talking to myself @ernie and @bert"),
                tweet(2, "bert", "@BERT is great"));
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        assertFalse("users can't follow themselves", followsOf(followsGraph, "ernie").contains("ernie"));
        assertFalse("users can't follow themselves", followsOf(followsGraph, "bert").contains("bert"));
        assertTrue(followsOf(followsGraph, "ernie").contains("bert"));
    }

    @Test
    public void testGuessFollowsGraphEmailIsNotAMention() {
        List<Tweet> tweets = Arrays.asList(tweet(1, "ernie", "mail me at bert@sesame.org"));
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        assertFalse("an e-mail address is not a mention", followsOf(followsGraph, "ernie").contains("sesame"));
    }

    @Test
    public void testGuessFollowsGraphOnlyAuthorsAndMentionsAppear() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "ernie", "hi @bert"),
                tweet(2, "elmo", "no mentions"));
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue("graph may only contain authors and mentioned users",
                setOf("ernie", "bert", "elmo").containsAll(allUsers(followsGraph)));
    }

    // ------------------------------------------------------------------- influencers

    @Test
    public void testInfluencersEmpty() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertTrue("expected empty list", influencers.isEmpty());
    }

    @Test
    public void testInfluencersSingleUserWithoutFollowers() {
        Map<String, Set<String>> followsGraph = graphOf("ernie", setOf());
        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals("only user in graph is listed", Arrays.asList("ernie"), influencers);
    }

    @Test
    public void testInfluencersSingleInfluencer() {
        Map<String, Set<String>> followsGraph = graphOf(
                "ernie", setOf("bert"),
                "elmo", setOf("bert"),
                "bert", setOf());
        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals("expected every user exactly once", 3, influencers.size());
        assertEquals("bert has two followers, so is the top influencer", "bert", influencers.get(0));
        assertTrue(influencers.containsAll(setOf("ernie", "elmo", "bert")));
    }

    @Test
    public void testInfluencersMultipleInfluencersAreOrdered() {
        // followers: carol = 3, bob = 2, alice = 1, dave = 0
        Map<String, Set<String>> followsGraph = graphOf(
                "alice", setOf("bob", "carol"),
                "bob", setOf("carol", "alice"),
                "dave", setOf("bob", "carol"));
        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals(Arrays.asList("carol", "bob", "alice", "dave"), influencers);
    }

    @Test
    public void testInfluencersTiedInfluence() {
        // followers: bert = 1, elmo = 1, ernie = 0, grover = 0
        Map<String, Set<String>> followsGraph = graphOf(
                "ernie", setOf("bert"),
                "grover", setOf("elmo"));
        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals(4, influencers.size());
        assertEquals("every user appears exactly once", setOf("ernie", "bert", "grover", "elmo"),
                new HashSet<>(influencers));
        assertEquals("tied users may be in either order", setOf("bert", "elmo"),
                new HashSet<>(influencers.subList(0, 2)));
        assertEquals("tied users may be in either order", setOf("ernie", "grover"),
                new HashSet<>(influencers.subList(2, 4)));
    }

    @Test
    public void testInfluencersUserOnlyAppearingAsFollowee() {
        Map<String, Set<String>> followsGraph = graphOf("ernie", setOf("bert", "elmo"));
        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals("users that are not keys are still included", 3, influencers.size());
        assertEquals(setOf("ernie", "bert", "elmo"), new HashSet<>(influencers));
        assertEquals("ernie has no followers, so is last", "ernie", influencers.get(2));
    }

    @Test
    public void testInfluencersDescendingOrderOnGuessedGraph() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "a", "@z"),
                tweet(2, "b", "@z @y"),
                tweet(3, "c", "@z @y @x"));
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);
        List<String> influencers = SocialNetwork.influencers(followsGraph);

        for (int i = 0; i + 1 < influencers.size(); i++) {
            assertTrue("expected non-increasing follower counts",
                    followerCount(followsGraph, influencers.get(i)) >= followerCount(followsGraph, influencers.get(i + 1)));
        }
        assertEquals("z has the most followers", "z", influencers.get(0));
    }

    /*
     * Warning: all the tests you write here must be runnable against any
     * SocialNetwork class that follows the spec. It will be run against several
     * staff implementations of SocialNetwork, which will be done by overwriting
     * (temporarily) your version of SocialNetwork with the staff's version.
     * DO NOT strengthen the spec of SocialNetwork or its methods.
     *
     * In particular, your test cases must not call helper methods of your own
     * that you have put in SocialNetwork, because that means you're testing a
     * stronger spec than SocialNetwork says. If you need such helper methods,
     * define them in a different class. If you only need them in this test
     * class, then keep them in this test class.
     */

}
