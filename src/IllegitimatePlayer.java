import java.util.UUID;

/**
 * Represents a player who has made an illegal operation, storing player ID and the first illegal operation.
 */
public class IllegitimatePlayer {
    private final UUID playerId;
    private final String firstIllegalOperation;     // First illegal operation made by the player

    public IllegitimatePlayer(UUID playerId, String firstIllegalOperation) {
        this.playerId = playerId;
        this.firstIllegalOperation = firstIllegalOperation;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getFirstIllegalOperation() {
        return firstIllegalOperation;
    }
}
