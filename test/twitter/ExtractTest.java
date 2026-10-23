/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

public class ExtractTest {

    /*
     * Testing strategy
     * =================
     *
     * getTimespan(tweets):
     *   Partition on number of tweets:
     *      - exactly one tweet
     *      - two tweets, in chronological order
     *      - two tweets, in reverse chronological order
     *      - more than two tweets, with min/max timestamps in the middle of the list
     *      - multiple tweets that share the exact same timestamp (span of zero length)
     *   Partition on the resulting Timespan:
     *      - start == end (single tweet / all tweets same time)
     *      - start < end
     *
     * getMentionedUsers(tweets):
     *   Partition on number of mentions in the tweet list:
     *      - no tweets contain an "@"
     *      - a single tweet with a single mention
     *      - a single tweet with multiple distinct mentions
     *      - multiple tweets, mentions spread across them
     *   Partition on mention validity / boundaries:
     *      - "@" preceded by a letter/digit (email-like address, e.g. bitdiddle@mit.edu) -> NOT a mention
     *      - "@" at the very start of the text -> IS a mention
     *      - "@" followed immediately by a non-username character (e.g. "@ hello", "@@", "@.") -> NOT a mention
     *      - "@" followed by a valid username immediately followed by another valid
     *        username character later (shouldn't split false positives)
     *   Partition on case:
     *      - same username mentioned with different cases across tweets -> counted once
     *   Partition on duplicate mentions:
     *      - same username mentioned more than once -> appears once in the returned set
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:00:00Z");

    private static final Tweet tweet1 = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 = new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    @Test
    public void testGetTimespanSingleTweet() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d1, timespan.getEnd());
    }

    @Test
    public void testGetTimespanTwoTweets() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1, tweet2));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    @Test
    public void testGetTimespanTwoTweetsReverseOrder() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet2, tweet1));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    @Test
    public void testGetTimespanMinMaxInMiddle() {
        Tweet early = new Tweet(3, "ben", "early tweet", d1);
        Tweet middle = new Tweet(4, "ben", "middle tweet", d2);
        Tweet late = new Tweet(5, "ben", "late tweet", d3);

        Timespan timespan = Extract.getTimespan(Arrays.asList(middle, early, late));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d3, timespan.getEnd());
    }

    @Test
    public void testGetTimespanSameTimestamp() {
        Tweet a = new Tweet(6, "ben", "first", d1);
        Tweet b = new Tweet(7, "ben", "second", d1);

        Timespan timespan = Extract.getTimespan(Arrays.asList(a, b));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d1, timespan.getEnd());
    }

    @Test
    public void testGetMentionedUsersNoMention() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet1));

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    @Test
    public void testGetMentionedUsersSingleMention() {
        Tweet tweet = new Tweet(10, "ben", "talking to @alyssa about rivest", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));

        assertEquals("expected one mention", Collections.singleton("alyssa"), lowercase(mentionedUsers));
    }

    @Test
    public void testGetMentionedUsersMultipleMentionsOneTweet() {
        Tweet tweet = new Tweet(11, "ben", "@alyssa and @bbitdiddle should talk", d1);

        Set<String> mentionedUsers = lowercase(Extract.getMentionedUsers(Arrays.asList(tweet)));

        assertEquals("expected two mentions", 2, mentionedUsers.size());
        assertTrue(mentionedUsers.contains("alyssa"));
        assertTrue(mentionedUsers.contains("bbitdiddle"));
    }

    @Test
    public void testGetMentionedUsersAcrossMultipleTweets() {
        Tweet a = new Tweet(12, "ben", "@alyssa hi", d1);
        Tweet b = new Tweet(13, "louis", "@bbitdiddle hi", d2);

        Set<String> mentionedUsers = lowercase(Extract.getMentionedUsers(Arrays.asList(a, b)));

        assertEquals("expected two mentions", 2, mentionedUsers.size());
        assertTrue(mentionedUsers.contains("alyssa"));
        assertTrue(mentionedUsers.contains("bbitdiddle"));
    }

    @Test
    public void testGetMentionedUsersEmailIsNotMention() {
        Tweet tweet = new Tweet(14, "ben", "email me at bitdiddle@mit.edu please", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));

        assertTrue("email address must not be treated as a mention", mentionedUsers.isEmpty());
    }

    @Test
    public void testGetMentionedUsersAtStartOfText() {
        Tweet tweet = new Tweet(15, "ben", "@alyssa are you there?", d1);

        Set<String> mentionedUsers = lowercase(Extract.getMentionedUsers(Arrays.asList(tweet)));

        assertEquals(Collections.singleton("alyssa"), mentionedUsers);
    }

    @Test
    public void testGetMentionedUsersAtNotFollowedByUsernameChar() {
        Tweet tweet = new Tweet(16, "ben", "look over here @ nothing to see, also @@ and @.", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));

        assertTrue("no valid username follows '@'", mentionedUsers.isEmpty());
    }

    @Test
    public void testGetMentionedUsersCaseInsensitiveDeduplication() {
        Tweet a = new Tweet(17, "ben", "@Alyssa is great", d1);
        Tweet b = new Tweet(18, "louis", "@alyssa agreed", d2);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(a, b));

        assertEquals("same user mentioned with different case should be counted once",
                1, mentionedUsers.size());
    }

    private static Set<String> lowercase(Set<String> usernames) {
        Set<String> result = new java.util.HashSet<>();
        for (String username : usernames) {
            result.add(username.toLowerCase());
        }
        return result;
    }

    /*
     * Warning: all the tests you write here must be runnable against any
     * Extract class that follows the spec. It will be run against several staff
     * implementations of Extract, which will be done by overwriting
     * (temporarily) your version of Extract with the staff's version.
     * DO NOT strengthen the spec of Extract or its methods.
     *
     * In particular, your test cases must not call helper methods of your own
     * that you have put in Extract, because that means you're testing a
     * stronger spec than Extract says. If you need such helper methods, define
     * them in a different class. If you only need them in this test class, then
     * keep them in this test class.
     */

}
