package nu.marginalia.search.command.commands;

import nu.marginalia.client.Context;
import nu.marginalia.search.command.SearchParameters;
import nu.marginalia.search.exceptions.RedirectException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BangCommandTest {
    public BangCommand bangCommand = new BangCommand();

    @Test
    public void testG() {
        try {
            bangCommand.process(Context.internal(),
                    null,
                    new SearchParameters(" !g test",
                    null, null, null, null)
            );
            Assertions.fail("Should have thrown RedirectException");
        }
        catch (RedirectException ex) {
            assertEquals("https://www.google.com/search?q=test", ex.newUrl);
        }
    }

    @Test
    public void testMatchPattern() {
        var match = bangCommand.matchBangPattern("!g test", "!g");

        assertTrue(match.isPresent());
        assertEquals(match.get(), "test");
    }

}