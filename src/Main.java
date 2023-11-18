import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


/**
 * This class represents a casino application that processes player data,
 * handles matches, and calculates results.
 *
 * I'm assuming that all input data is correct and formatted appropriately
 */
public class Main {
    private static final String PLAYER_DATA_FILE = "resources/player_data.txt";
    private static final String MATCH_DATA_FILE = "resources/match_data.txt";
    private static final String RESULTS_FILE = "src/results.txt";
    private static Long hostBalance = 0L; //keeps track of casino host balance, starts at 0

    /**
     * The entry point of the casino application.
     *
     * @param args Command line arguments (not used in this application).
     */
    public static void main(String[] args) {
        Map<UUID, Match> matches = readMatchesFromFile();    //reads all match input data into list sorted from oldest to newest
        List<IllegitimatePlayer> illegitimatePlayers = new ArrayList<>();   //stores all players who have made illegal operations
        List<Player> legitimatePlayers = processPlayerOperations(matches, illegitimatePlayers);  //finds all players data who are legitimate

        // Sorting illegitimatePlayers and legitimatePlayers based on player UUID values
        illegitimatePlayers.sort(Comparator.comparing(IllegitimatePlayer::getPlayerId));    // O(n log n)
        legitimatePlayers.sort(Comparator.comparing(Player::getId));    // O(n log n)

        writeResultsToFile(legitimatePlayers, illegitimatePlayers); //writes all data to output file
    }


    /**
     * Processes player operations, such as bets, withdrawals, and deposits.
     * It generates a list of legitimate players and collects information
     * about illegitimate players.
     *
     * @param matches             The list of matches players bet on, sorted from oldest to newest.
     * @param illegitimatePlayers The list to store illegitimate players.
     * @return The list of legitimate players.
     */
    private static List<Player> processPlayerOperations(Map<UUID, Match> matches, List<IllegitimatePlayer> illegitimatePlayers) {
        String line;
        List<Player> legitimatePlayers = new ArrayList<>();

        // Storing player data as variables here and not in the Player class object gives slight efficiency win
        UUID previousPlayerUUID = null;     // Holds the UUID of the previous player we processed
        boolean previousPlayerIllegal = false;  // True if player who we process previously has made an illegal operation
        long playerCoins = 0;   // Stores the players coin count
        int playerPlacedBets = 0;
        int playerBetsWon = 0;
        long currentPlayerHostChanges = 0;  // Keeps track of how much current player has changed casino host balance (negative or positive)

        try (BufferedReader br = Files.newBufferedReader(Paths.get(PLAYER_DATA_FILE), StandardCharsets.UTF_8)) {
            while ((line = br.readLine()) != null) {
                String[] lineSplits = line.split(",");
                UUID playerUUID = UUID.fromString(lineSplits[0]);
                if(previousPlayerUUID == null) previousPlayerUUID = playerUUID;     // Required for first player in file
                if(previousPlayerIllegal && playerUUID.equals(previousPlayerUUID)) continue;    // If the current player has made an illegal operation in the past, we skip their operations

                if(!previousPlayerUUID.equals(playerUUID)){     // If the previous player is not the same as the one currently, we have looked through all previous player operations
                    // Add only legitimate players to our players list
                    if(!previousPlayerIllegal){
                        legitimatePlayers.add(new Player(previousPlayerUUID, playerCoins, playerBetsWon,playerPlacedBets));
                        hostBalance += currentPlayerHostChanges;    // Remove previous legitimate player operations host balance changes from casino balance
                    }

                    // Reset all player variables
                    previousPlayerIllegal = false;
                    currentPlayerHostChanges = 0;
                    playerCoins = 0;
                    playerPlacedBets = 0;
                    playerBetsWon = 0;

                    // Add the new player as the previousPlayer
                    previousPlayerUUID = playerUUID;
                }

                Operation operation = Operation.valueOf(lineSplits[1]); // Operation that the player made
                int coinsUsed = Integer.parseInt(lineSplits[3]);    // How many coins the player used for given operation

                switch (operation) {
                    case BET -> {
                        if (coinsUsed > playerCoins) { //bet is illegal, when bet amount is higher than balance
                            illegitimatePlayers.add(new IllegitimatePlayer(playerUUID, line));   // Add player to illegitimate players list
                            previousPlayerIllegal = true;
                        } else {
                            playerPlacedBets++;
                            currentPlayerHostChanges+=coinsUsed;    // Move the coins used to casino balance
                            playerCoins-=coinsUsed;     // Remove coins used for bet from player

                            UUID matchId = UUID.fromString(lineSplits[2]);  // UUID of the match the player bet on
                            char sideOfBet = lineSplits[4].charAt(0);   // What side the player bet on
                            Match match = matches.get(matchId);     // The match the user bet on

                            if (match.getResult() == sideOfBet) {   // Player won the bet
                                BigDecimal rate = sideOfBet == 'A' ? match.getRateA() : match.getRateB();
                                int gain = rate.multiply(BigDecimal.valueOf(coinsUsed)).intValue();     // How much the player gained from the bet
                                int pay = gain + coinsUsed;     // The payback for the player
                                currentPlayerHostChanges -= pay;    // remove payback amount from host balance
                                playerCoins += pay;   // transfer the payback amount to player
                                playerBetsWon++;
                            } else if(match.getResult() == 'D'){
                                // Math was a draw, return coins to player
                                currentPlayerHostChanges-=coinsUsed;
                                playerCoins+=coinsUsed;
                            } // In case of a lost bet, the coins have been already added to host balance
                        }
                    }
                    case WITHDRAW -> {
                        if (coinsUsed > playerCoins) {  // Withdraw is illegal, if player balance is lower than withdraw amount
                            line = playerUUID + " WITHDRAW null " + coinsUsed + " null";    // Replace empty values in operation with nulls in output
                            illegitimatePlayers.add(new IllegitimatePlayer(playerUUID, line));  // Add current player to illegitimate players list
                            previousPlayerIllegal = true;
                        } else playerCoins -= coinsUsed;
                    }
                    case DEPOSIT -> playerCoins += coinsUsed;
                }

            }
        }catch (IOException e){
            System.err.println("Error: Unable to read file - " + e.getMessage());
        }
        return legitimatePlayers;
    }

    /**
     * Writes the results, including legitimate players' data,
     * illegitimate players represented by their first illegal operations,
     * and host balance change, to the output file.
     *
     * @param legitimatePlayers   The list of legitimate players.
     * @param illegitimatePlayers The list of illegitimate players.
     */
    private static void writeResultsToFile(List<Player> legitimatePlayers, List<IllegitimatePlayer> illegitimatePlayers) {
        StringBuilder results = new StringBuilder();

        // Write legitimate players data to file
        if (!legitimatePlayers.isEmpty()) {
            for (Player p : legitimatePlayers)
                results.append(String.format("%s %s %s%s", p.getId(), p.getBalance(), p.getWinRate(), System.lineSeparator()));
        }else results.append(System.lineSeparator());
        results.append(System.lineSeparator()); //empty line to separate

        // Write illegitimate players represented by their first illegal operations
        if (!illegitimatePlayers.isEmpty()) {
            for (IllegitimatePlayer ip : illegitimatePlayers)
                results.append(String.format("%s%s", ip.getFirstIllegalOperation(), System.lineSeparator()));
        }else results.append(System.lineSeparator());
        results.append(System.lineSeparator()); //empty line to separate

        // Write host balance change
        results.append(hostBalance);

        // Write to file
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(RESULTS_FILE), StandardCharsets.UTF_8)) {
            bw.write(results.toString());
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }


    /**
     * Reads match data from MATCH_DATA_FILE file and returns a list of matches.
     *
     * @return The list of matches.
     */
    private static Map<UUID, Match> readMatchesFromFile() {
        Map<UUID, Match> matches = new HashMap<>();
        String line;
        try (BufferedReader br = Files.newBufferedReader(Paths.get(MATCH_DATA_FILE), StandardCharsets.UTF_8)) {
            while ((line = br.readLine()) != null) {
                String[] lineSplits = line.split(",");
                try {
                    Match match = new Match(
                        new BigDecimal(lineSplits[1]),  // Rate A
                        new BigDecimal(lineSplits[2]),  // Rate B
                        lineSplits[3].charAt(0));    // Match result: 'A' if side A won, 'B' if side B won and 'D' if it was a draw

                    matches.put(UUID.fromString(lineSplits[0]), match);     //store the match in a hashmap for quick access with the matchId as the key
                } catch (Exception e) {
                    // Ideally we would have custom exceptions to throw for NumberFormatException, IllegalArgumentException, StringIndexOutOfBoundsException, ...
                    // and we would check them beforehand, but I'm assuming all input data is correct
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }catch (IOException e){
            System.err.println("Error: Unable to read file - " + e.getMessage());
        }
        return matches;
    }
}
