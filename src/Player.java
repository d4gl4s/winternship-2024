import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;


/**
 * Represents a legitimate player whose operations have been processed, storing player-related data such as ID, balance, games won, and placed bets count.
 */

public class Player {
    private final UUID id;
    private final long balance;
    private final int gamesWon;
    private final int placedBetsCount;

    /**
     * Constructs a new legitimate Player whose operations have been processed.
     *
     * @param id               The unique UUID identifier for the player.
     * @param balance          The end balance of the player.
     * @param gamesWon         The number of games won by the player.
     * @param placedBetsCount  The count of bets placed by the player.
     */
    public Player(UUID id, long balance, int gamesWon, int placedBetsCount) {
        this.id = id;
        this.balance = balance;
        this.gamesWon = gamesWon;
        this.placedBetsCount = placedBetsCount;
    }

    public UUID getId() {
        return id;
    }

    public long getBalance() {
        return balance;
    }

    /**
     * Calculates and retrieves the win rate of the player.
     *
     * @return The win rate as a {@code BigDecimal} with scale 2.
     */
    public BigDecimal getWinRate(){
        BigDecimal winRate;
        if (placedBetsCount != 0)
            winRate = BigDecimal.valueOf((double)gamesWon/placedBetsCount).setScale(2, RoundingMode.HALF_UP);
        else winRate = BigDecimal.ZERO;
        return winRate;
    }
}
