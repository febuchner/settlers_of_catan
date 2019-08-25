package ai;

import application.GameStart;
import gameobjects.Elements.Street;
import gameworld.HexagonField;
import gameworld.WayPoint;
import networking.ClientProtocol;
import networking.MessageObjects.MaritimeTrade;
import networking.MessageObjects.Resources;
import player.Player;
import resources.ResourcePointer;
import tools.BuildingType;
import tools.EvolutionType;
import tools.FieldType;
import tools.PortTypes;
import tools.ResourceType;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;

/**
 * Implements the entire AI logic
 *
 * @author Felip, Minh
 */
public class AiLogic {

    /**
     * A reference to the last village we build during the initial phase.
     */
    private WayPoint initialPhaseWayPoint;
    /**
     * A reference to the Ai's associated player;
     */
    private Player player;

    /**
     * A list of all the settlementWayPoints the AI has (used to speed up the performance)
     */
    private ArrayList<WayPoint> settlementWayPoints = new ArrayList<>();
    /**
     * A list of all the streets the AI has (used to speed up the performance)
     */
    private ArrayList<Street> streets = new ArrayList<>();

    /**
     * saves the locations for the move thief action that were already chosen in this turn
     */
    private ArrayList<HexagonField> triedFieldsThief = new ArrayList<>();
    /**
     * Maps every number to their dice probability. NOTE that 7 has here explicitly
     * a probability of 0
     */
    private final HashMap<Integer, Double> numberToDiceProbability = new HashMap<>();

    {
        numberToDiceProbability.put(2, (1.0 / 36));
        numberToDiceProbability.put(3, (2.0 / 36));
        numberToDiceProbability.put(4, (3.0 / 36));
        numberToDiceProbability.put(5, (4.0 / 36));
        numberToDiceProbability.put(6, (5.0 / 36));
        numberToDiceProbability.put(8, (5.0 / 36));
        numberToDiceProbability.put(9, (4.0 / 36));
        numberToDiceProbability.put(10, (3.0 / 36));
        numberToDiceProbability.put(11, (2.0 / 36));
        numberToDiceProbability.put(12, (1.0 / 36));
        numberToDiceProbability.put(7, 0.0);

    }

    /**
     * Constructor
     *
     * @param player
     */
    public AiLogic(Player player) {

        setPlayer(player);
    }

    // Methods

    /**
     * AI logic for placing initialSettlements during pre game phase.
     *
     * @return
     */
    public WayPoint initialSettlementPlacementAI() {
        // Get a list of non-occupied wayPoints
        ArrayList<WayPoint> settlementCandidates = getNonOccupiedWaypoints();
        GameStart.mainLogger.getLOGGER().fine(settlementCandidates.toString());
        // Remove hexagonfield at desert
        settlementCandidates = removeUnappropriateWayPointsForStartPhase(settlementCandidates);
        GameStart.mainLogger.getLOGGER().fine(settlementCandidates.toString());

        // get fields with best probability
        WayPoint wp = getBestWaypointConsideringPorbabilities(settlementCandidates);
        initialPhaseWayPoint = wp;
        return wp;
    }

    /**
     * AI logic for placing an initial street next to the prior placed initial
     * building
     *
     * @return
     */
    public WayPoint[] initialStreetPlacementAI() {
        WayPoint[] wayPoints = new WayPoint[2];
        wayPoints[0] = initialPhaseWayPoint;
        wayPoints[1] = getBestWayPointForStreet(wayPoints[0].getWayPointNeighbours());

        // Not needed anymore
        initialPhaseWayPoint = null;
        return wayPoints;
    }

    /**
     * AI logic for handling the thief.
     * @param secondTry- true if ai already tried a location which then got rejected by the server
     */
    public Object[] handleThiefAction(boolean secondTry) {
    	if(secondTry){
            Object[] listToReturn = new Object[2];
            ArrayList<HexagonField> fields = GameStart.siedlerVonCatan.getGameWorld().getFields();
            for (HexagonField hexagonField : fields) {
                if (GameStart.siedlerVonCatan.getThief().getThiefPosition().isEqualTo(hexagonField.getPosition())||triedFieldsThief.contains(hexagonField))
                    continue;
                triedFieldsThief.add(hexagonField);
                listToReturn[0]=hexagonField;
                listToReturn[1]=null;
            }
            return listToReturn;
    	}
    	else{
    		triedFieldsThief.clear();
    		Object[] objects = getPlayersFromFieldWithHighestPlayerCount();
    		triedFieldsThief.add((HexagonField) objects[0]);
    		return objects;
    	}
    }

    /**
     * Returns players from field with highest player count
     *
     * @return
     */
    private Object[] getPlayersFromFieldWithHighestPlayerCount() {
        Object[] listToReturn = new Object[2];
        Player playerWithLowestResources = null;
        Player playerWithHighestResources = null;

        HexagonField field = null;
        int playerCount = -1;
        ArrayList<HexagonField> fields = GameStart.siedlerVonCatan.getGameWorld().getFields();
        for (HexagonField hexagonField : fields) {
            if (hexagonField.getFieldType() == FieldType.WATER || GameStart.siedlerVonCatan.getThief().getThiefPosition().isEqualTo(hexagonField.getPosition()))
                continue;
            if (hexagonField.getNeighbourWayPointsWithSettlements(player.getPlayerID()).size() > playerCount) {
                playerCount = hexagonField.getNeighbourWayPointsWithSettlements(player.getPlayerID()).size();
                field = hexagonField;
            }
        }
        int resourceCountMax = -1;
        double probability = -1;
        int resourceCountMin = 1000000;
        for (WayPoint wp : field.getNeighbourWayPointsWithSettlements(player.getPlayerID())) {
            Player playerInField = GameStart.siedlerVonCatan.findPlayerByID(wp.getSettlement().getOwnerID());
            if (playerInField.getNumberOfResource(ResourceType.HIDDEN) >= resourceCountMax
                    && numberToDiceProbability.get(field.getChipNumber()) > probability) {
                playerWithHighestResources = playerInField;
                probability = numberToDiceProbability.get(field.getChipNumber());
            }
            if (playerInField.getNumberOfResource(ResourceType.HIDDEN) < resourceCountMin
                    && playerInField.getNumberOfResource(ResourceType.HIDDEN) != 0) {
                playerWithLowestResources = playerInField;
            }
        }
        listToReturn[0] = field;
        listToReturn[1] = playerWithLowestResources == null ? playerWithHighestResources : playerWithLowestResources;
        return listToReturn;
    }

    // Getters and Setters
    public Player getPlayer() {
        return player;
    }

    /**
     * Sets the player associated to this AI
     *
     * @param player
     */
    public void setPlayer(Player player) {
        // GameStart.mainLogger.getLOGGER().fine("A player has been assigned to the ai...");
        this.player = player;
    }

    /**
     * @return An arrayList of non occupied wayPoints
     */
    private ArrayList<WayPoint> getNonOccupiedWaypoints() {
        return getNonOccupiedWaypoints(GameStart.siedlerVonCatan.getGameWorld().getWayPoints());
    }

    /**
     * @return An arrayList of non occupied wayPoints
     */
    private ArrayList<WayPoint> getNonOccupiedWaypoints(ArrayList<WayPoint> wayPoints) {
        ArrayList<WayPoint> nonOccupiedWaypoints = new ArrayList<>();
        wayPoints.forEach(wayPoint -> {
            if (!wayPoint.getSettlement().isOccupied())
                nonOccupiedWaypoints.add(wayPoint);
        });
        return nonOccupiedWaypoints;
    }

    /**
     * @return An arrayList of non occupied wayPoints by enemy settlements
     */
    private ArrayList<WayPoint> getNonOccupiedByEnemySettlementWaypoints(ArrayList<WayPoint> wayPoints) {
        ArrayList<WayPoint> nonOccupiedWaypoints = new ArrayList<>();
        for (WayPoint wayPoint : wayPoints) {
            if (wayPoint.getSettlement().getOwnerID() == player.getPlayerID()
                    || wayPoint.getSettlement().getOwnerID() == null)
                nonOccupiedWaypoints.add(wayPoint);
        }
        return nonOccupiedWaypoints;
    }

    /**
     * @return An arrayList of non occupied wayPoints by any settlement
     */
    private ArrayList<WayPoint> getNonOccupiedByAnySettlement(ArrayList<WayPoint> wayPoints) {
        ArrayList<WayPoint> nonOccupiedWaypoints = new ArrayList<>();
        for (WayPoint wayPoint : wayPoints) {
            if (wayPoint.getSettlement().getOwnerID() == null)
                nonOccupiedWaypoints.add(wayPoint);
        }
        return nonOccupiedWaypoints;
    }

    /**
     * Removes inappropriate way points for start phase
     *
     * @param wayPointsArrayList
     * @return
     */
    private ArrayList<WayPoint> removeUnappropriateWayPointsForStartPhase(ArrayList<WayPoint> wayPointsArrayList) {
        ArrayList<WayPoint> wayPointsNotAtDesert = new ArrayList<>();
        for (WayPoint wp : wayPointsArrayList) {
            boolean addWayPoint = true;
            for (HexagonField hexagonField : wp.getFieldNeighbours()) {
                if (hexagonField.getChipNumber() == null || hexagonField.getChipNumber().intValue() == 7) { // Since 7
                    // == Desert
                    addWayPoint = false;
                    break;
                }
            }
            if (addWayPoint)
                wayPointsNotAtDesert.add(wp);
        }
        return wayPointsNotAtDesert.size() == 0 ? wayPointsArrayList : wayPointsNotAtDesert;
    }

    /**
     * Given a list of wayPoints, select the one with the best numbers (highest
     * probability).
     *
     * @param wayPoints Arraylist of wayPoint candidates.
     * @return
     */
    private WayPoint getBestWaypointConsideringPorbabilities(ArrayList<WayPoint> wayPoints) {
        GameStart.mainLogger.getLOGGER().fine(numberToDiceProbability.toString());
        WayPoint currentBestWayPoint = null;
        double currentMaxScore = -1;
        for (WayPoint wp : wayPoints) {
            // Calculate score.
            double tempScore = 0;
            // Each weaypoint has a max. of 3 neighbours (coast for instance has less)
            for (HexagonField hexagon : wp.getFieldNeighbours()) {
                GameStart.mainLogger.getLOGGER().fine(hexagon.getChipNumber().toString());
                // Check if not water (water has no number)
                if (hexagon.getChipNumber() != null && hexagon.getChipNumber() != 0)
                    tempScore += numberToDiceProbability.get(hexagon.getChipNumber());
            }
            if (currentMaxScore < tempScore) {
                currentBestWayPoint = wp;
                currentMaxScore = tempScore;
            }

            GameStart.mainLogger.getLOGGER().fine(wp.toString() + " Score: " + currentMaxScore);
        }
        return currentBestWayPoint;

    }

    /**
     * @param wayPoints A pre-defined list of certain wayPoints.
     * @return The best wayPoint for building a street form the list.
     */
    private WayPoint getBestWayPointForStreet(ArrayList<WayPoint> wayPoints) {
        if (wayPoints.size() == 0)
            return null;
        WayPoint currentBestWayPoint = null;
        ArrayList<WayPoint> nonOccupiedByAnySettlement = getNonOccupiedByAnySettlement(wayPoints);
        if (nonOccupiedByAnySettlement.size() > 0) {
            currentBestWayPoint = getBestWaypointConsideringPorbabilities(nonOccupiedByAnySettlement);
        } else {
            ArrayList<WayPoint> nonOccupiedwayPointsByEnemySettlement = getNonOccupiedByEnemySettlementWaypoints(
                    wayPoints);
            if (nonOccupiedwayPointsByEnemySettlement.size() > 0) {
                currentBestWayPoint = getBestWaypointConsideringPorbabilities(nonOccupiedwayPointsByEnemySettlement);
            } else {
                ArrayList<WayPoint> nonOccupiedwayPoints = getNonOccupiedWaypoints(wayPoints);
                if (nonOccupiedwayPoints.size() > 0) {
                    currentBestWayPoint = getBestWaypointConsideringPorbabilities(nonOccupiedwayPoints);
                } else {
                    for (WayPoint wp : wayPoints) {
                        for (WayPoint wp2 : wp.getWayPointNeighbours()) {
                            if (!wp2.getSettlement().isOccupied())
                                    nonOccupiedwayPoints.add(wp);
                        }
                        if (nonOccupiedwayPoints.size() > 0)
                            currentBestWayPoint = getBestWaypointConsideringPorbabilities(nonOccupiedwayPoints);
                        else
                            currentBestWayPoint = getBestWaypointConsideringPorbabilities(wayPoints);
                    }
                }
            }
        }
        return currentBestWayPoint;
    }

    /**
     * Drops 50% of the cards
     */

    public Resources dropHalfCards() {
        int cardsNumber = player.getTotalNumberOfResources();
        int cardsToDrop = cardsNumber % 2 == 0 ? cardsNumber / 2 : (cardsNumber - 1) / 2;
        Integer wood = 0;
        Integer loam = 0;
        Integer wool = 0;
        Integer grain = 0;
        Integer stone = 0;

        GameStart.mainLogger.getLOGGER().fine("AI needs to drop: " + cardsToDrop + " cards");

        while (cardsToDrop > 0) {
            if (player.getNumberOfResource(ResourceType.WOOL) - wool > 0 && cardsToDrop > 0) {
                wool++;
                cardsToDrop--;
            }
            if (player.getNumberOfResource(ResourceType.STONE) - stone > 0 && cardsToDrop > 0) {
                stone++;
                cardsToDrop--;
            }
            if (player.getNumberOfResource(ResourceType.GRAIN) - grain > 0 && cardsToDrop > 0) {
                grain++;
                cardsToDrop--;
            }
            if (player.getNumberOfResource(ResourceType.WOOD) - wood > 0 && cardsToDrop > 0) {
                wood++;
                cardsToDrop--;
            }
            if (player.getNumberOfResource(ResourceType.LOAM) - loam > 0 && cardsToDrop > 0) {
                loam++;
                cardsToDrop--;
            }
        }
        return new Resources(wood, loam, wool, grain, stone, 0);
    }

    /**
     * Tries to build a settlement in a good location
     */
    public WayPoint getWayPointToBuildSettlement() {
        // No need to calculate anything if we surely do not have the resources to build
        // a village
        if (!hasResourcesToBuildVillage())
            return null;
        // Here we have resources to buy a village
        ArrayList<WayPoint> possibleWayPointsForVillage = getPossibleWayPointsForVillage();
        // Return if empty
        if (possibleWayPointsForVillage.size() == 0)
            return null;
        WayPoint wayPointToBuild = getBestWaypointConsideringPorbabilities(possibleWayPointsForVillage);
        // Return waypoint
        return wayPointToBuild;
    }

    /**
     * Returns a list of wayPoints where we can construct a village.
     *
     * @return
     */
    private ArrayList<WayPoint> getPossibleWayPointsForVillage() {
        ArrayList<WayPoint> possibleWayPointsForVillage = new ArrayList<>();
//		ArrayList<WayPoint> nonOccupiedWayPoints = getNonOccupiedWaypoints();
        for (Street street : streets) {
            for (WayPoint wayPoint : street.getConnectionPoints()) {
                GameStart.mainLogger.getLOGGER().fine("Is occupied: " + wayPoint + " " + wayPoint.getSettlement().isOccupied());
                if (!wayPoint.getSettlement().isOccupied() && !possibleWayPointsForVillage.contains(wayPoint))
                    possibleWayPointsForVillage.add(wayPoint);
            }
        }
        GameStart.mainLogger.getLOGGER().fine("getPossibleWayPointsForVillage " + possibleWayPointsForVillage);
        return possibleWayPointsForVillage;

    }

    /**
     * @return True, if we have enough resources to build a village.
     */
    private boolean hasResourcesToBuildVillage() {
        GameStart.mainLogger.getLOGGER().fine(player.getResources().toString());
        return player.getResources().get(ResourceType.WOOD) > 0 && player.getResources().get(ResourceType.LOAM) > 0
                && player.getResources().get(ResourceType.GRAIN) > 0
                && player.getResources().get(ResourceType.WOOL) > 0;
    }

    /**
     * @return True, if we have enough resources to build a castle.
     */
    private boolean hasResourcesToBuildCastle() {
        GameStart.mainLogger.getLOGGER().fine(player.getResources().toString());
        return player.getResources().get(ResourceType.STONE) >= 3 && player.getResources().get(ResourceType.GRAIN) >= 2;
    }

    /**
     * @return True, if we have enough resources to build a street.
     */
    private boolean hasResourcesToBuildStreet() {
        GameStart.mainLogger.getLOGGER().fine("TRYING STREET: " + player.getResources().toString());
        GameStart.mainLogger.getLOGGER().fine("TRYING STREET: " + (player.getResources().get(ResourceType.WOOD) > 0 && player.getResources().get(ResourceType.LOAM) > 0));

        return player.getResources().get(ResourceType.WOOD) > 0 && player.getResources().get(ResourceType.LOAM) > 0;
    }

    /**
     * Tries to find a good street to build
     *
     * @return
     */
    public WayPoint[] getStreetToBuild() {
        // No need to calculate anything if we surely do not have the resources to build
        // a village
        if (!hasResourcesToBuildStreet())
            return null;
        WayPoint[] bestWayPoints = null;
        HashSet<WayPoint> streetStartingPoints = new HashSet<>();
        streetStartingPoints.addAll(settlementWayPoints);
        for (Street street : streets) {
            streetStartingPoints.add(street.getConnectionPoints()[0]);
            streetStartingPoints.add(street.getConnectionPoints()[1]);
        }
        for (WayPoint startingPoint : streetStartingPoints) {
            WayPoint[] wayPoints = new WayPoint[2];
            wayPoints[0] = startingPoint;
            ArrayList<WayPoint> candidatesForStreet = new ArrayList<>();
            GameStart.mainLogger.getLOGGER().fine("#### " + getNonOccupiedByEnemySettlementWaypoints(
                    startingPoint.getWayPointNeighbours()));
            for (WayPoint wayPointCandidate : getNonOccupiedByEnemySettlementWaypoints(
                    startingPoint.getWayPointNeighbours())) {
                Street streetToAdd = GameStart.siedlerVonCatan.getGameWorld().findStreetWithWayPoints(startingPoint,
                        wayPointCandidate);
                if (streetToAdd.getOwnerID() == null) {
                    candidatesForStreet.add(wayPointCandidate);
                }
            }
            // In case the list is empty, try to include wayPoints with settlements, as we
            // might still be able to get the longest road
            if (candidatesForStreet.size() == 0) {
                for (WayPoint wayPointCandidate : startingPoint.getWayPointNeighbours()) {
                    Street streetToAdd = GameStart.siedlerVonCatan.getGameWorld().findStreetWithWayPoints(startingPoint,
                            wayPointCandidate);
                    if (streetToAdd.getOwnerID() == null)
                        candidatesForStreet.add(wayPointCandidate);
                }
            }
            GameStart.mainLogger.getLOGGER().fine("Candidates for street building: " + candidatesForStreet);
            wayPoints[1] = getBestWayPointForStreet(candidatesForStreet);
            if (wayPoints[1] != null) {
                if (bestWayPoints == null) {
                    bestWayPoints = wayPoints;
                } else {
                    //
                    ArrayList<WayPoint> candidates = new ArrayList<>();
                    candidates.add(bestWayPoints[1]);
                    candidates.add(wayPoints[1]);
                    // Replace if better
                    if (getBestWayPointForStreet(candidates) == wayPoints[1]
                            && !wayPoints[1].getStreetConnectedWaypoints().contains(wayPoints[0])) {
                        bestWayPoints = wayPoints;
                    }
                }
            }
        }
        return bestWayPoints;
    }

    /**
     * Tries to trade if Ai doesn't have enough resources to build a street
     *
     * @return
     */
    public MaritimeTrade tradeCardsWithBankForStreet() {
        // Ai does not trade if it has enough resources to build a street or has built all of its streets
        if (hasResourcesToBuildStreet() && player.getStreets().size() >= 15)
            return null;
        Resources offerResource;
        Resources requestResource;
        // check if ai has to give only 3 of the same resources to the bank for a trade
        int xToOne = 4;
        if (hasAPort(PortTypes.THREE_FOR_ONE))
            xToOne = 3;
        // Ai tries to request loam if it does not have enough loam
        if (player.getResources().get(ResourceType.LOAM) == 0) {
            requestResource = new Resources(0, 1, 0, 0, 0, 0);
            offerResource = swapXResourcesToBuildStreet(xToOne);
        }
        // Ai tries to request wood if it does not have enough wood
        else {
            requestResource = new Resources(1, 0, 0, 0, 0, 0);
            offerResource = swapXResourcesToBuildStreet(xToOne);
        }
        // return null if ai does not have the resources to trade with the bank
		if(offerResource == null) return null;
		// otherwise return actual trade request
		GameStart.mainLogger.getLOGGER().fine("AI: "+player.getName()+" trades resources for STREET");
        GameStart.mainLogger.getLOGGER().fine(player.toString());
		GameStart.mainLogger.getLOGGER().fine("offerResource: " +offerResource.toString());
		GameStart.mainLogger.getLOGGER().fine("requestResource: " +requestResource.toString());
		return new MaritimeTrade(offerResource, requestResource);
	}

    /**
     * Checks if AI has a settlement in a port.
     *
     * @param portType The port type the AI is searching for.
     * @return True if there is such a port.
     */
    private boolean hasAPort(PortTypes portType) {
        for (WayPoint wp : settlementWayPoints) {
            if (wp.getPortType().equals(portType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds out which resource type the ai can trade away to build a street.
     *
     * @param x Simulates X:1 trade with bank. X stands for the number of cards
     *          the ai has to give to the bank. 4 is the default number of cards
     *          the ai gives away. 3 is the number of cards the ai can give away
     *          if it has a settlement in a 3:1 port.
     * @return
     */
    private Resources swapXResourcesToBuildStreet(int x) {
        // try to do a two for one trade if there is such a port and if there is enough
        // resources
        if (tryATwoForOneTrade(PortTypes.TWO_STONE_FOR_ONE) != null)
            return tryATwoForOneTrade(PortTypes.TWO_STONE_FOR_ONE);
        else if (tryATwoForOneTrade(PortTypes.TWO_GRAIN_FOR_ONE) != null)
            return tryATwoForOneTrade(PortTypes.TWO_GRAIN_FOR_ONE);
        else if (tryATwoForOneTrade(PortTypes.TWO_WOOL_FOR_ONE) != null)
            return tryATwoForOneTrade(PortTypes.TWO_WOOL_FOR_ONE);
        else if (tryATwoForOneTrade(PortTypes.TWO_LOAM_FOR_ONE) != null
                && player.getResources().get(ResourceType.LOAM) > 2)
            return tryATwoForOneTrade(PortTypes.TWO_LOAM_FOR_ONE);
        else if (tryATwoForOneTrade(PortTypes.TWO_WOOD_FOR_ONE) != null
                && player.getResources().get(ResourceType.WOOD) > 2)
            return tryATwoForOneTrade(PortTypes.TWO_WOOD_FOR_ONE);
        else if (player.getResources().get(ResourceType.STONE) >= x) {
            // AI trades STONE for LOAM if it has enough stone
            return new Resources(0, 0, 0, 0, x, 0);
        } else if (player.getResources().get(ResourceType.GRAIN) >= x) {
            // AI trades GRAIN for LOAM if it has enough grain
            return new Resources(0, 0, 0, x, 0, 0);
        } else if (player.getResources().get(ResourceType.WOOL) >= x) {
            // AI trades WOOL for LOAM if it has enough wool
            return new Resources(0, 0, x, 0, 0, 0);
        } else if (player.getResources().get(ResourceType.LOAM) > x) {
            // AI trades LOAM for WOOD if it has enough loam
            return new Resources(0, x, 0, 0, 0, 0);
        } else if (player.getResources().get(ResourceType.WOOD) > x) {
            // AI trades WOOD for LOAM if it has enough wood
            return new Resources(x, 0, 0, 0, 0, 0);
        } else
            // return null if AI doesnt have enough resources to trade
            return null;
    }

    /**
     * AI tries a two for one trade if it has enough resources and the specified
     * port.
     *
     * @param port Port AI wants to trade with.
     * @return Resources AI wants to give to the bank.
     */
    private Resources tryATwoForOneTrade(PortTypes port) {
        switch (port) {
            case TWO_WOOD_FOR_ONE:
                if (hasAPort(PortTypes.TWO_WOOD_FOR_ONE) && player.getResources().get(ResourceType.WOOD) >= 2)
                    return new Resources(2, 0, 0, 0, 0, 0);
                else
                    return null;
            case TWO_LOAM_FOR_ONE:
                if (hasAPort(PortTypes.TWO_LOAM_FOR_ONE) && player.getResources().get(ResourceType.LOAM) >= 2)
                    return new Resources(0, 2, 0, 0, 0, 0);
                else
                    return null;
            case TWO_WOOL_FOR_ONE:
                if (hasAPort(PortTypes.TWO_WOOL_FOR_ONE) && player.getResources().get(ResourceType.WOOL) >= 2)
                    return new Resources(0, 0, 2, 0, 0, 0);
                else
                    return null;
            case TWO_GRAIN_FOR_ONE:
                if (hasAPort(PortTypes.TWO_GRAIN_FOR_ONE) && player.getResources().get(ResourceType.GRAIN) >= 2)
                    return new Resources(0, 0, 0, 2, 0, 0);
                else
                    return null;
            case TWO_STONE_FOR_ONE:
                if (hasAPort(PortTypes.TWO_STONE_FOR_ONE) && player.getResources().get(ResourceType.STONE) >= 2)
                    return new Resources(0, 0, 0, 0, 2, 0);
                else
                    return null;
            default:
                return null;
        }
    }

    /**
     * Tries to trade if AI doesn't have enough resources to build a village.
     *
     * @return
     */
    public MaritimeTrade tradeCardsWithBankForVillage() {
        // Ai does not trade if it has enough resources to build a village
        if (hasResourcesToBuildVillage())
            return null;
        Resources offerResource;
        Resources requestResource;
        // check if ai has to give only 3 of the same resources to the bank for a trade
        int xToOne = 4;
        if (hasAPort(PortTypes.THREE_FOR_ONE))
            xToOne = 3;
        // Ai tries to request loam if it does not have enough loam
        if (player.getResources().get(ResourceType.LOAM) == 0) {
            requestResource = new Resources(0, 1, 0, 0, 0, 0);
            offerResource = swapXResourcesToBuildVillage(xToOne);
        }
        // Ai tries to request wood if it does not have enough wood
        else if (player.getResources().get(ResourceType.WOOD) == 0) {
            requestResource = new Resources(1, 0, 0, 0, 0, 0);
            offerResource = swapXResourcesToBuildVillage(xToOne);
        }
        // Ai tries to request wool if it does not have enough wool
        else if (player.getResources().get(ResourceType.WOOL) == 0) {
            requestResource = new Resources(0, 0, 1, 0, 0, 0);
            offerResource = swapXResourcesToBuildVillage(xToOne);
        }
        // Ai tries to request grain if it does not have enough grain
        else {
            requestResource = new Resources(0, 0, 0, 1, 0, 0);
            offerResource = swapXResourcesToBuildVillage(xToOne);
        }
        // return null if ai does not have the resources to trade with the bank
        if (offerResource == null) return null;
        // otherwise return actual trade request
        GameStart.mainLogger.getLOGGER().fine("AI: "+player.getName()+" trades resources for VILLAGE");
        GameStart.mainLogger.getLOGGER().fine(player.toString());
		GameStart.mainLogger.getLOGGER().fine("offerResource: " +offerResource.toString());
		GameStart.mainLogger.getLOGGER().fine("requestResource: " +requestResource.toString());
		return new MaritimeTrade(offerResource, requestResource);
	}

    /**
     * Finds out which resource type the AI can trade away to build a village.
     *
     * @param x Simulates X:1 trade with bank. X stands for the number of cards
     *          the AI has to give to the bank. 4 is the default number of cards
     *          the AI gives away. 3 is the number of cards the AI can give away
     *          if it has a settlement in a 3:1 port.
     * @return
     */
    private Resources swapXResourcesToBuildVillage(int x) {
        // try to do a two for one trade if there is such a port and if there is enough
        // resources
        if (tryATwoForOneTrade(PortTypes.TWO_STONE_FOR_ONE) != null)
            return tryATwoForOneTrade(PortTypes.TWO_STONE_FOR_ONE);
        else if (tryATwoForOneTrade(PortTypes.TWO_GRAIN_FOR_ONE) != null
                && player.getResources().get(ResourceType.GRAIN) > 2)
            return tryATwoForOneTrade(PortTypes.TWO_GRAIN_FOR_ONE);
        else if (tryATwoForOneTrade(PortTypes.TWO_WOOL_FOR_ONE) != null
                && player.getResources().get(ResourceType.WOOL) > 2)
            return tryATwoForOneTrade(PortTypes.TWO_WOOL_FOR_ONE);
        else if (tryATwoForOneTrade(PortTypes.TWO_LOAM_FOR_ONE) != null
                && player.getResources().get(ResourceType.LOAM) > 2)
            return tryATwoForOneTrade(PortTypes.TWO_LOAM_FOR_ONE);
        else if (tryATwoForOneTrade(PortTypes.TWO_WOOD_FOR_ONE) != null
                && player.getResources().get(ResourceType.WOOD) > 2)
            return tryATwoForOneTrade(PortTypes.TWO_WOOD_FOR_ONE);
        else if (player.getResources().get(ResourceType.STONE) >= x) {
            // AI trades STONE for LOAM if it has enough stone
            return new Resources(0, 0, 0, 0, x, 0);
        } else if (player.getResources().get(ResourceType.GRAIN) > x) {
            // AI trades GRAIN for LOAM if it has enough grain
            return new Resources(0, 0, 0, x, 0, 0);
        } else if (player.getResources().get(ResourceType.WOOL) > x) {
            // AI trades WOOL for LOAM if it has enough wool
            return new Resources(0, 0, x, 0, 0, 0);
        } else if (player.getResources().get(ResourceType.LOAM) > x) {
            // AI trades LOAM for WOOD if it has enough loam
            return new Resources(0, x, 0, 0, 0, 0);
        } else if (player.getResources().get(ResourceType.WOOD) > x) {
            // AI trades WOOD for LOAM if it has enough wood
            return new Resources(x, 0, 0, 0, 0, 0);
        } // return null if ai does not have enough resources
        else
            return null;
    }

    /**
     * Tries to trade if AI doesn't have enough resources to build a castle
     *
     * @return
     */
    public MaritimeTrade tradeCardsWithBankForCastle() {
        if (hasResourcesToBuildCastle())
            return null;
        Resources offerResource;
        Resources requestResource;
        // check if ai has to give only 3 of the same resources to the bank for a trade
        int xToOne = 4;
        if (hasAPort(PortTypes.THREE_FOR_ONE))
            xToOne = 3;
        // Ai tries to request stone if it only needs one stone to build a castle
        if (player.getResources().get(ResourceType.STONE) < 3) {
            requestResource = new Resources(0, 0, 0, 0, 1, 0);
            offerResource = swapXResourcesToBuildCastle(xToOne);
        }
        // Ai tries to request grain if it only needs one grain to build a castle
        else {
            requestResource = new Resources(0, 0, 0, 1, 0, 0);
            offerResource = swapXResourcesToBuildCastle(xToOne);
        }
        // return null if ai does not have the resources to trade with the bank
        if (offerResource == null) return null;
        // otherwise return actual trade request
        GameStart.mainLogger.getLOGGER().fine("AI: "+player.getName()+" trades resources for a castle");
        GameStart.mainLogger.getLOGGER().fine(player.toString());
		GameStart.mainLogger.getLOGGER().fine("offerResource: " +offerResource.toString());
		GameStart.mainLogger.getLOGGER().fine("requestResource: " +requestResource.toString());
		return new MaritimeTrade(offerResource, requestResource);
	}

    /**
     * Finds out which resource type the AI can trade away to build a castle.
     *
     * @param x Simulates X:1 trade with bank. X stands for the number of cards
     *          the AI has to give to the bank. 4 is the default number of cards
     *          the AI gives away. 3 is the number of cards the AI can give away
     *          if it has a settlement in a 3:1 port.
     * @return
     */
    private Resources swapXResourcesToBuildCastle(int x) {
        // try to do a two for one trade if there is such a port and if there is enough
        // resources
        if (tryATwoForOneTrade(PortTypes.TWO_STONE_FOR_ONE) != null
                && player.getResources().get(ResourceType.STONE) > 2 + 3)
            return tryATwoForOneTrade(PortTypes.TWO_STONE_FOR_ONE);
        else if (tryATwoForOneTrade(PortTypes.TWO_GRAIN_FOR_ONE) != null
                && player.getResources().get(ResourceType.GRAIN) > 2 + 2)
            return tryATwoForOneTrade(PortTypes.TWO_GRAIN_FOR_ONE);
        else if (tryATwoForOneTrade(PortTypes.TWO_WOOL_FOR_ONE) != null
                && player.getResources().get(ResourceType.WOOL) > 2)
            return tryATwoForOneTrade(PortTypes.TWO_WOOL_FOR_ONE);
        else if (tryATwoForOneTrade(PortTypes.TWO_LOAM_FOR_ONE) != null
                && player.getResources().get(ResourceType.LOAM) > 2)
            return tryATwoForOneTrade(PortTypes.TWO_LOAM_FOR_ONE);
        else if (tryATwoForOneTrade(PortTypes.TWO_WOOD_FOR_ONE) != null
                && player.getResources().get(ResourceType.WOOD) > 2)
            return tryATwoForOneTrade(PortTypes.TWO_WOOD_FOR_ONE);
        if (player.getResources().get(ResourceType.STONE) > x + 3) {
            // AI trades STONE if it has enough stone
            return new Resources(0, 0, 0, 0, x, 0);
        } else if (player.getResources().get(ResourceType.GRAIN) > x + 2) {
            // AI trades GRAIN if it has enough grain
            return new Resources(0, 0, 0, x, 0, 0);
        } else if (player.getResources().get(ResourceType.WOOL) > x) {
            // AI trades WOOL if it has enough wool
            return new Resources(0, 0, x, 0, 0, 0);
        } else if (player.getResources().get(ResourceType.LOAM) > x) {
            // AI trades LOAM if it has enough loam
            return new Resources(0, x, 0, 0, 0, 0);
        } else if (player.getResources().get(ResourceType.WOOD) > x) {
            // AI trades WOOD if it has enough wood
            return new Resources(x, 0, 0, 0, 0, 0);
        } // return null if ai does not have enough resources
        else
            return null;
    }

    /**
     * Tries to play a development card
     */
    public void tryPlayingDevelopmentCard(ClientProtocol protocol) {
        if (player.getEvolutionCards().containsKey(EvolutionType.ROAD_BUILDING)
                && player.getEvolutionCards().get(EvolutionType.ROAD_BUILDING) > 0) {
            GameStart.mainLogger.getLOGGER().fine("AI playing road building card...");
            protocol.requestToPlayRoadBuildingCard(getTwoStreetsToBuid());
        } else if (player.getEvolutionCards().containsKey(EvolutionType.KNIGHT)
                && player.getEvolutionCards().get(EvolutionType.KNIGHT) > 0 && thiefAtOwnField()) {
            GameStart.mainLogger.getLOGGER().fine("AI playing knight card...");
            Object[] objects = protocol.getConnectionHandler().getAiLogic().handleThiefAction(false);
            HexagonField hexagonField = (HexagonField) objects[0];
            Player playerTarget = (Player) objects[1];
        	GameStart.siedlerVonCatan.findPlayerByID(getPlayer().getPlayerID() )
			.setMoveThiefDueToKnightCard(true);
            protocol.requestMoveThief(hexagonField.getPosition().castTo2D(), playerTarget.getPlayerID());

        } else if (player.getEvolutionCards().containsKey(EvolutionType.MONOPOLY)
                && player.getEvolutionCards().get(EvolutionType.MONOPOLY) > 0) {
            GameStart.mainLogger.getLOGGER().fine("AI playing monopoly card...");
            protocol.requestToPlayMonopolyCard(getLowestResource(null));

        } else if (player.getEvolutionCards().containsKey(EvolutionType.YEAR_OF_PLENTY)
                && player.getEvolutionCards().get(EvolutionType.YEAR_OF_PLENTY) > 0) {
            GameStart.mainLogger.getLOGGER().fine("AI playing year of plenty card...");
            ResourceType type1 = getLowestResource(null);
            protocol.requestToPlayYearOfPlentyCard(type1, getLowestResource(type1));
        }
    }

    /**
     * Builds two streets
     *
     * @return Two streets to build (for free)
     */
    private Street[] getTwoStreetsToBuid() {
        Street[] twoStreets = new Street[2];
        for (int i = 0; i < 2; i++) {
            WayPoint[] bestWayPoints = null;
            HashSet<WayPoint> streetStartingPoints = new HashSet<>();
            streetStartingPoints.addAll(settlementWayPoints);

            ArrayList<Street> streetsTemp = (ArrayList<Street>) streets.clone();
            if (i == 1)
                streetsTemp.add(twoStreets[0]);

            for (Street street : streetsTemp) {
                streetStartingPoints.add(street.getConnectionPoints()[0]);
                streetStartingPoints.add(street.getConnectionPoints()[1]);
            }
            for (WayPoint startingPoint : streetStartingPoints) {
                WayPoint[] wayPoints = new WayPoint[2];
                wayPoints[0] = startingPoint;
                ArrayList<WayPoint> candidatesForStreet = new ArrayList<>();
                for (WayPoint wayPointCandidate : getNonOccupiedByEnemySettlementWaypoints(
                        startingPoint.getWayPointNeighbours())) {
                    Street streetToAdd = GameStart.siedlerVonCatan.getGameWorld().findStreetWithWayPoints(startingPoint,
                            wayPointCandidate);
                    if (streetToAdd.getOwnerID() == null)
                        candidatesForStreet.add(wayPointCandidate);
                }
                // In case the list is empty, try to include wayPoints with settlements, as we
                // might still be able to get the longest road
                if (candidatesForStreet.size() == 0) {
                    for (WayPoint wayPointCandidate : startingPoint.getWayPointNeighbours()) {
                        Street streetToAdd = GameStart.siedlerVonCatan.getGameWorld()
                                .findStreetWithWayPoints(startingPoint, wayPointCandidate);
                        if (streetToAdd.getOwnerID() == null)
                            candidatesForStreet.add(wayPointCandidate);
                    }
                }
                wayPoints[1] = getBestWayPointForStreet(candidatesForStreet);
                if (wayPoints[1] != null) {
                    if (bestWayPoints == null) {
                        bestWayPoints = wayPoints;
                    } else {
                        //
                        ArrayList<WayPoint> candidates = new ArrayList<>();
                        candidates.add(bestWayPoints[1]);
                        candidates.add(wayPoints[1]);
                        // Replace if better
                        if (getBestWayPointForStreet(candidates) == wayPoints[1]
                                && !wayPoints[1].getStreetConnectedWaypoints().contains(wayPoints[0])
                                && !streetStartingPoints.contains(wayPoints[1])) {
                            bestWayPoints = wayPoints;
                        }
                    }
                }
            }
            if (bestWayPoints == null || bestWayPoints[1] == null)
                return null;
            twoStreets[i] = GameStart.siedlerVonCatan.getGameWorld().findStreetWithWayPoints(bestWayPoints[0],
                    bestWayPoints[1]);
        }
        return twoStreets;
    }

    /**
     * Returns the lowest resource type the AI has.
     *
     * @return A type with lowest count on resources the player has.
     */
    private ResourceType getLowestResource(ResourceType notIncludingType) {
        if (notIncludingType == null)
            notIncludingType = ResourceType.HIDDEN;
        ResourceType type = ResourceType.LOAM;
        for (ResourceType typeR : player.getResources().keySet()) {
            if (typeR != notIncludingType && typeR != ResourceType.HIDDEN
                    && player.getResources().get(typeR) < player.getResources().get(type))
                type = typeR;
        }
        return type;
    }

    /**
     * Returns if thief is at field the AI owns.
     *
     * @return True if the thief is at one of our (hexa) fields.
     */
    private boolean thiefAtOwnField() {
        for (WayPoint wp : settlementWayPoints) {
            for (HexagonField field : wp.getFieldNeighbours())
                if (GameStart.siedlerVonCatan.getThief().getThiefPosition().isEqualTo(field.getPosition()))
                    return true;
        }
        return false;
    }

    /**
     * Tries to buy a development card
     *
     * @return True if we can buy a card
     */
    public boolean tryBuyingDevelopmentCard() {
        return hasResourcesToBuyDevelopmentCard();
    }

    /**
     * Returns if AI has enough resources to buy development card.
     *
     * @return True, if we have enough resources to a development card.
     */
    private boolean hasResourcesToBuyDevelopmentCard() {
        GameStart.mainLogger.getLOGGER().fine(player.getResources().toString());
        return player.getResources().get(ResourceType.WOOL) > 0 && player.getResources().get(ResourceType.GRAIN) > 0
                && player.getResources().get(ResourceType.STONE) > 0;
    }

    /**
     * Returns a way point which can be upgraded to a castle.
     *
     * @return A way point to upgrade to a castle
     */
    public WayPoint getWayPointToBuildCastle() {
        GameStart.mainLogger.getLOGGER().fine("CONSTRUCTION: " + player.getResources());
        if (!hasResourcesToBuildCastle())
            return null;
        ArrayList<WayPoint> wpToUpgrate = new ArrayList<>();
        for (WayPoint wp : settlementWayPoints) {
            if (wp.getSettlement().getBuildingType() == BuildingType.VILLAGE)
                wpToUpgrate.add(wp);
        }
        return getBestWaypointConsideringPorbabilities(wpToUpgrate);
    }

    /**
     * AI chooses a greeting.
     *
     * @return A random (funny) greeting as a string
     * @throws FileNotFoundException
     * @throws URISyntaxException
     */
    public String getGreeting() throws FileNotFoundException, URISyntaxException {
        String result = null;
        //File file = new File(ResourcePointer.class.getResource("Greetings.txt").toURI());

        //BufferedReader reader = new BufferedReader(new InputStreamReader(ResourcePointer.class.getResourceAsStream("Greetings.txt")));

        Random rand = new Random();
        int n = 0;
        for (Scanner sc = new Scanner(ResourcePointer.class.getResourceAsStream("Greetings.txt")); sc.hasNext(); ) {
            ++n;
            String line = sc.nextLine();
            if (rand.nextInt(n) == 0)
                result = line;
        }
        GameStart.mainLogger.getLOGGER().fine("RESULT: " + result);
        return result;
    }

    /**
     * Gets array of settlement way points
     *
     * @return settlementWayPoints
     */
    public ArrayList<WayPoint> getSettlementWayPoints() {
        return settlementWayPoints;
    }

    /**
     * gets array of streets
     *
     * @return streets
     */
    public ArrayList<Street> getStreets() {
        return streets;
    }
}