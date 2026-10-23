/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extract consists of methods that extract information from a list of tweets.
 * 
 * DO NOT change the method signatures and specifications of these methods, but
 * you should implement their method bodies, and you may add new public or
 * private methods or classes if you like.
 */
public class Extract {

    /**
     * Get the time period spanned by tweets.
     * 
     * @param tweets
     *            list of tweets with distinct ids, not modified by this method.
     * @return a minimum-length time interval that contains the timestamp of
     *         every tweet in the list.
     */
    public static Timespan getTimespan(List<Tweet> tweets) {
        Instant start = tweets.get(0).getTimestamp();
        Instant end = start;

        for (Tweet tweet : tweets) {
            Instant timestamp = tweet.getTimestamp();
            if (timestamp.isBefore(start)) {
                start = timestamp;
            }
            if (timestamp.isAfter(end)) {
                end = timestamp;
            }
        }

        return new Timespan(start, end);
    }

    /**
     * Get usernames mentioned in a list of tweets.
     * 
     * @param tweets
     *            list of tweets with distinct ids, not modified by this method.
     * @return the set of usernames who are mentioned in the text of the tweets.
     *         A username-mention is "@" followed by a Twitter username (as
     *         defined by Tweet.getAuthor()'s spec).
     *         The username-mention cannot be immediately preceded or followed by any
     *         character valid in a Twitter username.
     *         For this reason, an email address like bitdiddle@mit.edu does NOT 
     *         contain a mention of the username mit.
     *         Twitter usernames are case-insensitive, and the returned set may
     *         include a username at most once.
     */
    public static Set<String> getMentionedUsers(List<Tweet> tweets) {
        Set<String> mentionedUsers = new HashSet<>();
        Set<String> seenLowercase = new HashSet<>();

        for (Tweet tweet : tweets) {
            String text = tweet.getText();

            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) != '@') {
                    continue;
                }

                // "@" must not be immediately preceded by a valid username character,
                // otherwise it's part of something like an email address.
                if (i > 0 && isUsernameChar(text.charAt(i - 1))) {
                    continue;
                }

                int start = i + 1;
                int end = start;
                while (end < text.length() && isUsernameChar(text.charAt(end))) {
                    end++;
                }

                if (end > start) {
                    String username = text.substring(start, end);
                    String lowercaseUsername = username.toLowerCase();
                    if (seenLowercase.add(lowercaseUsername)) {
                        mentionedUsers.add(username);
                    }
                    i = end - 1;
                }
            }
        }

        return mentionedUsers;
    }

    /**
     * @param c character to check
     * @return true if c is a character valid in a Twitter username
     *         (letter, digit, underscore, or hyphen).
     */
    private static boolean isUsernameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

}
