import java.math.BigDecimal;


/**
 * Represents a match that was played, storing win rate for sides A and B and the result of the match
 */
public class Match {
    private final BigDecimal rateA;
    private final BigDecimal rateB;
    private final char result;  // 'A' if side A won, 'B' if B won and 'D' when a draw. Using char instead of Enum for memory.

    public Match(BigDecimal rateA, BigDecimal rateB, char result) {
        this.rateA = rateA;
        this.rateB = rateB;
        this.result = result;
    }

    public BigDecimal getRateA() {
        return rateA;
    }

    public BigDecimal getRateB() {
        return rateB;
    }

    public char getResult() {
        return result;
    }
}
