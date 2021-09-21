package com.elmakers.mine.bukkit.utility.help;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

import org.geysermc.connector.common.ChatColor;

import com.elmakers.mine.bukkit.ChatUtils;

public class HelpTopicMatch implements Comparable<HelpTopicMatch> {
    public static double CONTENT_FACTOR = 1;
    public static double TAG_FACTOR = 1.5;
    public static double TITLE_FACTOR = 0.75;

    private static final int MAX_WIDTH = 50;
    private final double relevance;
    private final HelpTopic topic;
    private final Map<String, HelpTopicKeywordMatch> wordMatches = new HashMap<>();
    private double wordsRelevance;
    private double titleRelevance;
    private double tagRelevance;

    @Nonnull
    public static HelpTopicMatch match(Help help, HelpTopic topic, Collection<String> keywords) {
        return new HelpTopicMatch(help, topic, keywords);
    }

    private HelpTopicMatch(Help help, HelpTopic topic, Collection<String> keywords) {
        this.topic = topic;

        double relevance = 0;
        for (String keyword : keywords) {
            relevance += computeRelevance(help, keyword);
        }
        if (!keywords.isEmpty()) {
            relevance = relevance / keywords.size();
            wordsRelevance = wordsRelevance / keywords.size();
            titleRelevance = titleRelevance / keywords.size();
            tagRelevance = tagRelevance / keywords.size();
        }
        this.relevance = relevance;
    }

    public String getDebugText() {
        String debugText = "";
        if (wordsRelevance > 0) {
            debugText += "Word: " + ChatUtils.printPercentage(wordsRelevance);
        }
        if (titleRelevance > 0) {
            if (!debugText.isEmpty()) debugText += " ";
            debugText += "Title: " + ChatUtils.printPercentage(titleRelevance);
        }
        if (tagRelevance > 0) {
            if (!debugText.isEmpty()) debugText += " ";
            debugText += "Tags: " + ChatUtils.printPercentage(tagRelevance);
        }
        return debugText;
    }

    private double computeRelevance(Help help, String keyword) {
        double maxRelevance = 0;
        double wordsRelevance = computeWordsRelevance(help, keyword);
        if (wordsRelevance > 0) {
            wordsRelevance = Math.pow(wordsRelevance, CONTENT_FACTOR);
            this.wordsRelevance += wordsRelevance;
            maxRelevance = Math.max(wordsRelevance, maxRelevance);
        }
        double titleRelevance = computeSetRelevance(help, topic.titleWords, keyword);
        if (titleRelevance > 0) {
            titleRelevance = Math.pow(titleRelevance, TITLE_FACTOR);
            this.titleRelevance += titleRelevance;
            maxRelevance = Math.max(titleRelevance, maxRelevance);
        }
        double tagRelevance = computeSetRelevance(help, topic.tagWords, keyword);
        if (tagRelevance > 0) {
            tagRelevance = Math.pow(tagRelevance, TAG_FACTOR);
            this.tagRelevance += tagRelevance;
            maxRelevance = Math.max(tagRelevance, maxRelevance);
        }
        return maxRelevance;
    }

    private double computeSetRelevance(Help help, Set<String> words, String keyword) {
        HelpTopicKeywordMatch match = HelpTopicKeywordMatch.match(keyword, words, topic, help);
        return match == null ? 0 : match.getRelevance();
    }

    private HelpTopicKeywordMatch getWordMatch(Help help, String keyword) {
        HelpTopicKeywordMatch match = wordMatches.get(keyword);
        if (match == null) {
            match = HelpTopicKeywordMatch.match(keyword, topic, help);
            if (match != null) {
                wordMatches.put(keyword, match);
            }
        }
        return match;
    }

    private double computeWordsRelevance(Help help, String keyword) {
        HelpTopicKeywordMatch match = getWordMatch(help, keyword);
        if (match == null) {
            return 0;
        }
        return match.getRelevance();
    }

    @Override
    public int compareTo(HelpTopicMatch o) {
        return o.relevance > relevance ? 1 : (o.relevance < relevance ? -1 : 0);
    }

    @Nonnull
    public HelpTopic getTopic() {
        return topic;
    }

    public String getSummary(boolean forConsole) {
        return getSummary(MAX_WIDTH, ChatColor.AQUA, ChatColor.RESET, !forConsole);
    }

    public String getSummary(int maxWidth, String matchPrefix, String matchSuffix) {
        return getSummary(maxWidth, matchPrefix, matchSuffix, false);
    }

    public String getSummary(int maxWidth, String matchPrefix, String matchSuffix, boolean addTooltips) {
        String title = getTopic().getTitle();
        int titleLength = title.length();
        if (titleLength > maxWidth - 4) {
            return "";
        }
        int remainingLength = maxWidth - titleLength;
        String[] lines = topic.getLines();
        if (lines.length == 0) {
            return "";
        }

        // Look for matches on each line separately, tracking the number of matches
        double mostRelevant = 0;
        String summary = null;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            // Case-insensitive searching
            String matchLine = line.toLowerCase();
            // Match against each search keyword, keeping a range of text between them
            int firstMatchIndex = -1;
            int lastMatchEnd = -1;
            double relevance = 0;
            int matchCount = 0;
            for (HelpTopicKeywordMatch match : wordMatches.values()) {
                if (!match.allowHighlight(topic)) continue;
                String keyword = match.getWord();
                int startIndex = matchLine.indexOf(keyword);
                if (startIndex >= 0) {
                    // Track match count and relevance
                    double matchRelevance = match.getRelevance();
                    if (matchRelevance > 0) {
                        relevance += matchRelevance;
                        matchCount++;
                    }
                    // Track range of all keywords
                    int endIndex = startIndex + keyword.length();
                    if (firstMatchIndex == -1) {
                        firstMatchIndex = startIndex;
                        lastMatchEnd = endIndex;
                    } else {
                        firstMatchIndex = Math.min(firstMatchIndex, startIndex);
                        lastMatchEnd = Math.max(lastMatchEnd, endIndex);
                    }
                }
            }

            // If there are more matches than we currently have, use this line
            if (relevance > 0 && relevance > mostRelevant) {
                boolean fitAllMatches = true;
                mostRelevant = relevance;
                // Trim this line if it is too long
                if (line.length() > remainingLength) {
                    // Trim from the start if we can fit everything
                    if (lastMatchEnd < remainingLength) {
                        line = line.substring(0, remainingLength) + ChatColor.GRAY + "...";
                    } else if (line.length() - firstMatchIndex < remainingLength) {
                        // Get everything from the end
                        line =  ChatColor.GRAY + "..."  + ChatColor.RESET + line.substring(line.length() - remainingLength);
                    } else if (lastMatchEnd - firstMatchIndex < remainingLength) {
                        // If the whole segment can fit, center it
                        int padding = remainingLength - (lastMatchEnd - firstMatchIndex);
                        int startIndex = firstMatchIndex - padding / 2;
                        line =  ChatColor.GRAY + "..."  + ChatColor.RESET + line.substring(startIndex, startIndex + remainingLength) + ChatColor.GRAY + "...";
                    } else {
                        // Just start at the first match
                        line = ChatColor.GRAY + "..." + line.substring(firstMatchIndex, firstMatchIndex + remainingLength) + ChatColor.GRAY + "...";
                        fitAllMatches = false;
                    }
                }
                summary = line;
                if (fitAllMatches && matchCount == wordMatches.size()) break;
            }
        }
        // Fall back to first line
        if (summary == null) {
            summary = lines[0];

            // Trim if needed
            if (summary.length() > remainingLength) {
                summary = summary.substring(0, remainingLength) + ChatColor.GRAY + "...";
            }
        }
        // Highlight matches
        for (HelpTopicKeywordMatch match : wordMatches.values()) {
            if (!match.allowHighlight(topic)) continue;
            String keyword = match.getWord();
            Pattern pattern = Pattern.compile("((?i)" + Pattern.quote(keyword) + ")");
            Matcher matcher = pattern.matcher(summary);
            StringBuffer highlighted = new StringBuffer();
            while (matcher.find()) {
                String replacement = matcher.group(1);
                if (addTooltips) {
                    String hover = "Matched " + match.getKeyword() + " at "
                        + ChatUtils.printPercentage(match.getSimilarity()) + " with "
                        + ChatUtils.printPercentage(match.getRelevance()) + " relevance";
                    replacement = "`{\"text\":\"" + replacement.replace("\"", "\\\"") + "\",";
                    replacement += "\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"" + hover + "\"}}`";
                }
                replacement = matchPrefix + replacement + matchSuffix;
                matcher.appendReplacement(highlighted, replacement);
            }
            matcher.appendTail(highlighted);
            summary = highlighted.toString();
        }
        return summary;
    }

    public HelpTopicKeywordMatch getKeywordMatch(String keyword) {
        return wordMatches.get(keyword);
    }

    public double getRelevance() {
        return relevance;
    }

    public boolean isRelevant() {
        return relevance > 0;
    }
}
