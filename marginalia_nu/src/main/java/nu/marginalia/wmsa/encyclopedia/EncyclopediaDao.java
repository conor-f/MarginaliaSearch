package nu.marginalia.wmsa.encyclopedia;

import com.google.inject.Inject;
import com.zaxxer.hikari.HikariDataSource;
import nu.marginalia.wmsa.edge.assistant.dict.WikiArticles;
import nu.marginalia.wmsa.edge.assistant.dict.WikiSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class EncyclopediaDao {

    private HikariDataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(EncyclopediaDao.class);

    @Inject
    public EncyclopediaDao(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public WikiArticles encyclopedia(String term) {
        WikiArticles response = new WikiArticles();
        response.entries = new ArrayList<>();

        try (var connection = dataSource.getConnection()) {
            var stmt = connection.prepareStatement("SELECT DISTINCT(NAME_LOWER) FROM REF_WIKI_TITLE WHERE NAME_LOWER=?");
            stmt.setString(1, term);

            var rsp = stmt.executeQuery();
            while (rsp.next()) {
                response.entries.add(capitalizeWikiString(rsp.getString(1)));
            }
        }
        catch (Exception ex) {
            logger.error("Failed to fetch articles", ex);
            return new WikiArticles();
        }

        return response;
    }

    public Optional<String> resolveEncylopediaRedirect(String term) {
        final List<String> matches = new ArrayList<>();

        try (var connection = dataSource.getConnection()) {
            try (var stmt = connection.prepareStatement("SELECT NAME, REF_NAME FROM REF_WIKI_TITLE WHERE NAME_LOWER=LOWER(?)")) {
                stmt.setString(1, term);

                var rsp = stmt.executeQuery();
                while (rsp.next()) {
                    if (term.equals(rsp.getString(1))
                            || rsp.getString(2) == null) {
                        return Optional.ofNullable(rsp.getString(2));
                    } else {
                        matches.add(rsp.getString(2));
                    }
                }
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        if (!matches.isEmpty()) {
            return Optional.of(matches.get(0));
        }
        return Optional.empty();
    }


    public Optional<WikiSearchResult> findEncyclopediaPageDirect(String term) {

        try (var connection = dataSource.getConnection()) {

            try (var stmt = connection.prepareStatement("SELECT NAME, REF_NAME FROM REF_WIKI_TITLE WHERE NAME_LOWER=LOWER(?)")) {
                stmt.setString(1, term.replace(' ', '_'));

                var rsp = stmt.executeQuery();
                while (rsp.next()) {
                    String name = rsp.getString(1);
                    String refName = rsp.getString(2);

                    if (refName == null) {
                        return Optional.of(new WikiSearchResult(name, null));
                    }
                }
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return Optional.empty();
    }

    public List<WikiSearchResult> findEncyclopediaPages(String term) {
        final List<WikiSearchResult> directMatches = new ArrayList<>();
        final Set<WikiSearchResult> directSearchMatches = new HashSet<>();
        final Set<WikiSearchResult> indirectMatches = new HashSet<>();

        try (var connection = dataSource.getConnection()) {

            try (var stmt = connection.prepareStatement("SELECT NAME, REF_NAME FROM REF_WIKI_TITLE WHERE NAME_LOWER=LOWER(?)")) {
                stmt.setString(1, term.replace(' ', '_'));

                var rsp = stmt.executeQuery();
                while (rsp.next()) {
                    String name = rsp.getString(1);
                    String refName = rsp.getString(2);

                    if (refName == null) {
                        directMatches.add(new WikiSearchResult(name, null));
                    } else {
                        indirectMatches.add(new WikiSearchResult(name, refName));
                    }
                }
            }

            try (var stmt = connection.prepareStatement("SELECT NAME, REF_NAME FROM REF_WIKI_TITLE WHERE NAME_LOWER LIKE ? LIMIT 10")) {
                stmt.setString(1, term.replace(' ', '_').replaceAll("%", "\\%").toLowerCase() + "%");

                var rsp = stmt.executeQuery();
                while (rsp.next()) {
                    String name = rsp.getString(1);
                    String refName = rsp.getString(2);

                    if (refName == null) {
                        directSearchMatches.add(new WikiSearchResult(name, null));
                    } else {
                        indirectMatches.add(new WikiSearchResult(name, refName));
                    }
                }
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        directMatches.forEach(indirectMatches::remove);
        indirectMatches.removeAll(directSearchMatches);
        directMatches.forEach(directSearchMatches::remove);
        directMatches.addAll(indirectMatches);
        directMatches.addAll(directSearchMatches);
        return directMatches;
    }

    private String capitalizeWikiString(String string) {
        if (string.contains("_")) {
            return Arrays.stream(string.split("_")).map(this::capitalizeWikiString).collect(Collectors.joining("_"));
        }
        if (string.length() < 2) {
            return string.toUpperCase();
        }
        return Character.toUpperCase(string.charAt(0)) + string.substring(1).toLowerCase();
    }


}
