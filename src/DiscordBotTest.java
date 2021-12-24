import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DiscordBotTest {
	DiscordBot fakeBot = new DiscordBot("","","");
	@Test
	void itShouldSendWhoAmI() {
		
	}
	@Test
	void itShouldEnterRPS() {
		fakeBot.messageReceived("!rps", "Badguy");
		assertTrue(fakeBot.rpsMode);
	}
}
