package dk.digitalidentity.indberetning.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SearchUtil {

	private SearchUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static double calculateTokenMatchScore(String fullName, String query) {
		// Tokenize the full name and query
		Set<String> fullNameTokens = tokenize(fullName);
		Set<String> queryTokens = tokenize(query);

		// Calculate the number of exact matches
		long exactMatches = queryTokens.stream()
				.filter(fullNameTokens::contains)
				.count();

		// Calculate partial matches by checking if any full name token starts with a query token
		long partialMatches = queryTokens.stream()
				.filter(queryToken -> fullNameTokens.stream()
						.anyMatch(fullNameToken -> fullNameToken.startsWith(queryToken)))
				.count();

		// Assign different weights to exact and partial matches
		double exactMatchWeight = 2.0;  // Give more weight to exact matches
		double partialMatchWeight = 1.0; // Less weight for partial matches

		// Calculate the final score as a weighted sum of exact and partial matches
		return (exactMatches * exactMatchWeight) + (partialMatches * partialMatchWeight);
	}

	// Tokenize a string by splitting on spaces and converting to lowercase
	private static Set<String> tokenize(String input) {
		return new HashSet<>(Arrays.asList(input.toLowerCase().split(" ")));
	}
}
