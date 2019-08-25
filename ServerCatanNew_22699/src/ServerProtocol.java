import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import networking.MessageObjects.*;

/**
 * Protocol for server messages
 *
 * @author Marcelina , Felip
 */
public class ServerProtocol {

	private ClientConnectionHandler connectionHandler;

	/**
	 * Shows if the player of this ServerProtocol played a development card this
	 * turn.
	 */
	private boolean playedDevelopmentCard;

	/**
	 * Constructor
	 */
	public ServerProtocol(ClientConnectionHandler clientConnectionHandler) {
		this.connectionHandler = clientConnectionHandler;
		this.playedDevelopmentCard = false;
	}

	// 4.1
	/**
	 * Connectionhandler sends hello object
	 * 
	 * @param version
	 * @param protocolInfo
	 */
	public void sendHello(String version, String protocolInfo) {
		InitialMessage initialMessage = new InitialMessage(version, protocolInfo);
		connectionHandler.sendObject(initialMessage);
	}

	// 4.2
	/**
	 * handles incoming hello message
	 * 
	 * @param message
	 */
	public void receiveHello(InitialMessage message) {
		if(connectionHandler.getServer().isGameActive()){
			sendServerError(new ServerError("Game was already started"));
			return;
		}
		// send welcome message
		sendWelcome();
		//send other players
		ArrayList<PlayerForProtocol> playerList = (ArrayList<PlayerForProtocol>) ServerMemoryLogic.playerList.clone();
		for(PlayerForProtocol player : playerList) {
			////GameStart.mainLogger.getLOGGER()()().fine("SENDING INFO " + player.getStatus() + " " + player.getId() + " " + connectionHandler.getPlayerId());
			connectionHandler.sendObject(new PlayerStatusUpdate(player));
		}
	}

	// 4.3
	/**
	 * Defines an (unique) id for the client.
	 */
	public void sendWelcome() {
		Integer id = ServerMemoryLogic.getIncrementedUniqueIdIncrementer();
		////GameStart.mainLogger.getLOGGER()()().fine(id);
		connectionHandler.setPlayerId(id);
		connectionHandler.sendObject(new WelcomeMessage(id));
	}

	// 6.1
	/**
	 * HAndles Server response
	 * 
	 * @param response
	 */
	public void sendServerResponse(String response) {
		ServerResponse serverResponse = new ServerResponse(response);
		connectionHandler.sendObject(serverResponse);
	}

	// 6.2
	/**
	 * Broadcasts a message to all clients
	 *
	 * @param chat
	 *            The chat message to send to the clients.
	 */
	public void receiveSendChatRequest(SendChat chat) {
		// Create object
		ReceiveChat receiveChat = new ReceiveChat(connectionHandler.getPlayerId(), chat.getMessage());
		// Broadcast message
		connectionHandler.getServer().broadcast(receiveChat);
		// ------------------------------------------------------------------------------------
		// for testing
		if (chat.getMessage().equals("cheat")) {
			for (PlayerForProtocol p : ServerMemoryLogic.playerList) {
				if (connectionHandler.getPlayerId() == p.getId()) {
					Resources r = new Resources(5, 5, 5, 5, 5, 0);
					p.addToPlayersResources(r);
					Earnings e = new Earnings(connectionHandler.getPlayerId(), r);
					sendEarnings(e);
				}
			}
		}
		// for testing
		if (chat.getMessage().equals("drop")) {
			for (PlayerForProtocol p : ServerMemoryLogic.playerList) {
				if (connectionHandler.getPlayerId() == p.getId()) {
					Resources r = new Resources(p.getResources().getWood(), p.getResources().getLoam(),
							p.getResources().getWool(), p.getResources().getGrain(), p.getResources().getStone(), 0);
					p.setResources(new Resources(0, 0, 0, 0, 0, 0));
					Costs cost = new Costs(p.getId(), r);
					sendCosts(cost);
				}
			}
		}
		if (chat.getMessage().equals("dropall")) {
			for (PlayerForProtocol p : ServerMemoryLogic.playerList) {
				Resources r = new Resources(p.getResources().getWood(), p.getResources().getLoam(),
						p.getResources().getWool(), p.getResources().getGrain(), p.getResources().getStone(), 0);
				p.setResources(new Resources(0, 0, 0, 0, 0, 0));
				Costs cost = new Costs(p.getId(), r);
				sendCosts(cost);
			}
		}
		if (chat.getMessage().equals("village")) {
			for (PlayerForProtocol p : ServerMemoryLogic.playerList) {
					Resources r = new Resources(1, 1, 1, 1, 0, 0);
					p.addToPlayersResources(r);
					Earnings e = new Earnings(p.getId(), r);
					sendEarnings(e);
			}
		}
		if (chat.getMessage().equals("dev")) {
			for (PlayerForProtocol p : ServerMemoryLogic.playerList) {
					Resources r = new Resources(0, 0, 1, 1, 1, 0);
					p.addToPlayersResources(r);
					Earnings e = new Earnings(p.getId(), r);
					sendEarnings(e);
			}
		}
		if (chat.getMessage().equals("city")) {
			for (PlayerForProtocol p : ServerMemoryLogic.playerList) {
					Resources r = new Resources(0, 0, 0, 2, 3, 0);
					p.addToPlayersResources(r);
					Earnings e = new Earnings(p.getId(), r);
					sendEarnings(e);
			}
		}
		if (chat.getMessage().equals("streets")) {
			for (PlayerForProtocol p : ServerMemoryLogic.playerList) {
					Resources r = new Resources(2, 2, 0, 0, 0, 0);
					p.addToPlayersResources(r);
					Earnings e = new Earnings(p.getId(), r);
					sendEarnings(e);
			}
		}
		if(chat.getMessage().equals("lose")){
			connectionHandler.getServer().broadcast(new GameOver("noone has won the game!", -1));
		}
		// --------------------------------------------------------------------------------------
	}

	// 7.1
	/**
	 * After receiving a player we want to add him to the list if he matches all
	 * the conditions (no double colors,...).
	 *
	 * @param player
	 *            The new connected player we potentially want to add to the
	 *            list.
	 */
	public void receivePlayer(PlayerForProtocol player) {
		ArrayList<PlayerForProtocol> playerList = (ArrayList<PlayerForProtocol>) ServerMemoryLogic.playerList.clone();
		// Check if color is available
		boolean checkPassed = true;
		for (PlayerForProtocol p : playerList) {
			if (p.getColor().equals(player.getColor())) {
				checkPassed = false;
				break;
			}
		}
		if (checkPassed) {
			PlayerForProtocol newPlayer = new PlayerForProtocol(connectionHandler.getPlayerId(),player.getColor(),player.getName());
			ServerMemoryLogic.playerList.add(newPlayer);
			// Add to hash map
			ServerMemoryLogic.clientToPlayer.put(connectionHandler, newPlayer);
			newPlayer.setStatus("Spiel starten");
			broadcastPlayerStatusUpdate(newPlayer);
		}
		// If check NOT passed, then send an error message to the client
		else
			connectionHandler.sendObject(new ServerError("Farbe bereits vergeben"));
	}

	// 7.2
	/**
	 * After receiving a game-start the server checks whether all clients are
	 * ready or not. If so, he must then send a notification to all clients that
	 * the game has started. A game can only start if a minimum of 3 players
	 * have joined. !!!!WARNING!!! Disconnecting a player during the lobby after
	 * marking himself as ready will result in bugs and issues!
	 */
	public void receiveStartGameRequest() {
		ServerMemoryLogic.numberOfReadyPlayers += 1;
		ServerMemoryLogic.playerIdToAvailableBuildings.put(connectionHandler.getPlayerId(), new int[] { 15, 5, 4 });
		if (ServerMemoryLogic.numberOfReadyPlayers == ServerMemoryLogic.playerList.size()
				&& ServerMemoryLogic.numberOfReadyPlayers >= 3) {
			try {
				sendGameStarted();
				Thread.sleep(5000);
			} catch (Exception e) {
				e.printStackTrace();
				ServerMemoryLogic.initialPhaseRemainingTurns = 0;
			}
		}
	}

	// 7.3
	/**
	 * Sends server error
	 * 
	 * @param serverError
	 */
	public void sendServerError(ServerError serverError) {
		connectionHandler.sendObject(serverError);
	}

	// 7.4
	/**
	 * Broadcasts a "gameStarted" message to all clients.
	 */
	private void sendGameStarted() throws Exception {
		connectionHandler.getServer().setGameActive(true);
		ServerMemoryLogic.initialPhaseRemainingTurns = ServerMemoryLogic.playerList.size() * 2;
		// ////GameStart.mainLogger.getLOGGER()()().fine("initialPhaseRemainingTurns: (initialization) " +
		// ServerMemoryLogic.initialPhaseRemainingTurns);
		if (ServerMemoryLogic.initialPhaseRemainingTurns < 6)
			throw new Exception("initialPhaseRemainingTurns (" + ServerMemoryLogic.initialPhaseRemainingTurns
					+ ") must not be < 6 at game start");

		// Send player status from all players to all players
		ServerMemoryLogic.playerList.forEach(player -> {
			player.setStatus("Wartet auf Spielbeginn");
			broadcastPlayerStatusUpdate(player);
			// ////GameStart.mainLogger.getLOGGER()()().fine(player);
		});
		// Send Field
		// Map(Field[] felder,Building[] gebaeude,Port[] haefen,String raeuber)
		Building[] buildings = new Building[0];
		Field[] worldField = ServerMemoryLogic.generateRandomFields();
		Port[] ports = ServerMemoryLogic.generateRandomPorts();
		String raeuberStartPosition = getRaeuberPosition(worldField);
		ServerMemoryLogic.serverMap = new Map(worldField, buildings, ports, raeuberStartPosition);

		GameStarted gameStarted = new GameStarted(ServerMemoryLogic.serverMap);
		connectionHandler.getServer().broadcast(gameStarted);
		// Thread.sleep(1000);
		// Tell "Player1" to start
		Collections.shuffle(ServerMemoryLogic.playerList);
		PlayerForProtocol playerToStart = ServerMemoryLogic.playerList.get(0);
		playerToStart.setStatus("Dorf bauen");
		broadcastPlayerStatusUpdate(playerToStart);
	}

	/**
	 * Gets the desert fields location and sets the thief in that location.
	 *
	 * @param worldField
	 *            This is needed for searching the desert fields location.
	 * @return The desert fields location
	 */
	private String getRaeuberPosition(Field[] worldField) {
		for (Field field : worldField) {
			if (field.getType().equals("Wueste")) {
				return field.getLocation();
			}
		}
		// this should never be reached
		return "S";
	}

	// 8.1
	/**
	 * Sends a player's status update
	 * 
	 * @param player
	 */
	public void sendPlayerStatusUpdate(PlayerForProtocol player) {
		if (!player.getStatus().equals("Warten"))
			ServerMemoryLogic.setCurrentPlayer(player);
		connectionHandler.sendObject(new PlayerStatusUpdate(player));
	}

	// 8.1
	/**
	 * Broadcasts a player's status update
	 * 
	 * @param player
	 */
	private void broadcastPlayerStatusUpdate(PlayerForProtocol player) {
		connectionHandler.getServer().sendObjectTo(player.getId(), new PlayerStatusUpdate(player));
		// We must send a modified status update to everyone except the
		// associated
		// player to hide the resources
		// since resources are stored inside the player
		Resources modifiedResources = new Resources(null, null, null, null, null, player.getResources().getTotalResources());
		DevelopmentCards modifiedDevelopmentCards = new DevelopmentCards(null, null, null, null, null,
				player.getDevelopmentCards().getTotalCards());
		// PlayerForProtocol(Integer id, String color, String name, String
		// status,
		// Integer victoryPoints, Resources resources)
		PlayerForProtocol clonedPlayer = new PlayerForProtocol(player.getId(), player.getColor(), player.getName(),
				player.getStatus(), player.getVictoryPoints(), modifiedResources, player.getKnightPoints(),
				modifiedDevelopmentCards, player.hasLargestArmy(),player.hasLongestRoad());
		connectionHandler.getServer().broadcastToAllOthers(player.getId(), new PlayerStatusUpdate(clonedPlayer));
	}

	// 8.2
	/**
	 * Broadcasts the diceThrow
	 *
	 * @param diceThrow
	 */
	public void sendDiceThrowResult(DiceThrow diceThrow) {
		connectionHandler.getServer().broadcast(diceThrow);
		if (diceThrow.getDiceSum() != 7) {
			calculateEarnings(diceThrow.getDiceSum());
			PlayerForProtocol player = null;
			for (PlayerForProtocol p : ServerMemoryLogic.playerList) {
				if (diceThrow.getPlayer() == p.getId())
					player = p;
			}
			player.setStatus("Handeln oder Bauen");
			broadcastPlayerStatusUpdate(player);
		}
	}

	// 8.3
	/**
	 * Sends the earnings to their owner and everyone else receives notice about
	 * this with "hidden" earnings
	 *
	 * @param earnings
	 */
	public void sendEarnings(Earnings earnings) {
		////GameStart.mainLogger.getLOGGER()()().fine("SERVER RECEIVED EARNING: " + earnings.toString());
		boolean notEnoughResources=false;
		Resources resources = earnings.getResources();
		if (ServerMemoryLogic.availableResourceCards[0] - resources.getWood() < 0) {
			////GameStart.mainLogger.getLOGGER()()().fine("No sufficient wood in the server!");
			resources.setWood(ServerMemoryLogic.availableResourceCards[0]);
			notEnoughResources=true;
		}
		if (ServerMemoryLogic.availableResourceCards[1] - resources.getLoam() < 0) {
			////GameStart.mainLogger.getLOGGER()()().fine("No sufficient loam in the server!");
			resources.setLoam(ServerMemoryLogic.availableResourceCards[1]);
			notEnoughResources=true;
		}
		if (ServerMemoryLogic.availableResourceCards[2] - resources.getWool() < 0) {
			////GameStart.mainLogger.getLOGGER()()().fine("No sufficient wool in the server!");
			resources.setWool(ServerMemoryLogic.availableResourceCards[2]);
			notEnoughResources=true;
		}
		if (ServerMemoryLogic.availableResourceCards[3] - resources.getGrain() < 0) {
			////GameStart.mainLogger.getLOGGER()()().fine("No sufficient grain in the server!");
			resources.setGrain(ServerMemoryLogic.availableResourceCards[3]);
			notEnoughResources=true;
		}
		if (ServerMemoryLogic.availableResourceCards[4] - resources.getStone() < 0) {
			////GameStart.mainLogger.getLOGGER()()().fine("No sufficient stone in the server!");
			resources.setStone(ServerMemoryLogic.availableResourceCards[4]);
			notEnoughResources=true;
		}
		earnings.setResources(resources);
		////GameStart.mainLogger.getLOGGER()()().fine("SERVER SENT EARNING: " + earnings.toString());
		int totalResources = earnings.getResources().getTotalResources();
		Resources hidden = new Resources(null, null, null, null, null, totalResources);
		Earnings hiddenEarnings = new Earnings(earnings.getPlayer(), hidden);
		ServerMemoryLogic.reduceAvailableResourceCardsNumber(resources);
		if(notEnoughResources){
			sendServerError(new ServerError("There were not enough resources"));
		}
		connectionHandler.getServer().broadcastToAllOthers(earnings.getPlayer(), hiddenEarnings);
		connectionHandler.getServer().sendObjectTo(earnings.getPlayer(), earnings);
	}


	// 8.4
	/**
	 * Sends Costs
	 * 
	 * @param costs
	 */
	public void sendCosts(Costs costs) {
		////GameStart.mainLogger.getLOGGER()()().fine("SERVER COST: " + costs.toString());
		int totalResources = costs.getResources().getTotalResources();
		Resources hidden = new Resources(null, null, null, null, null, totalResources);
		Costs hiddenCosts = new Costs(costs.getPlayer(), hidden);
		ServerMemoryLogic.increaseAvailableResourceCardsNumber(costs.getResources());
		////GameStart.mainLogger.getLOGGER()()().fine("im sending costs");
		connectionHandler.getServer().sendObjectTo(costs.getPlayer(), costs);
		connectionHandler.getServer().broadcastToAllOthers(costs.getPlayer(), hiddenCosts);
	}


	// 8.5
	/**
	 * Sends thief moved to all clients
	 * 
	 * @param thiefMoved
	 */
	public void sendThiefMoved(ThiefMoved thiefMoved) {
		connectionHandler.getServer().broadcast(thiefMoved);
	}

	// 8.3
	/**
	 * Gives the diceNumber to ServerMemoryLogic to calculate which player gets
	 * which resources and then gives the results to sendEarnings() to send them
	 * to the clients. Also adds the earnings to the the players'
	 * (PlayerForProtocol) resources
	 *
	 * @param diceNumber
	 */
	public void calculateEarnings(int diceNumber) {
		ArrayList<Earnings> allEarnings = ServerMemoryLogic.updatePlayerResoucesAfterDiceThrow(diceNumber);

		for (Earnings earnings : allEarnings) {
			sendEarnings(earnings);
			ServerMemoryLogic.addEarnings(earnings);
		}
	}

	// 8.4
	// public void sendBuildingEvent() {
	// connectionHandler.sendObject(buildingEvent);
	// }

	// 9.1

	/**
	 * Rolls the dice and sends it back to the client.
	 */
	public void receiveDiceThrowRequest() {
		if (!ServerRequestCheck.checkStatusMatch(connectionHandler.getPlayerId(), "Wuerfeln")) {
			sendServerError(new ServerError("It is not your turn to roll the  ."));
			return;
		}
		int[] diceNumber = new int[2];
		ServerMemoryLogic.dice.rollDice();
		diceNumber[0] = ServerMemoryLogic.dice.getDie1();
		diceNumber[1] = ServerMemoryLogic.dice.getDie2();
		int playerID = ServerMemoryLogic.clientToPlayer.get(connectionHandler).getId();
		DiceThrow diceThrow = new DiceThrow(playerID, diceNumber);
		sendDiceThrowResult(diceThrow);
		if ((diceThrow.getDiceSum()) == 7) {
			// "sendMoveThief()" is executed within the method below...
			for (PlayerForProtocol p : ServerMemoryLogic.playerList) {
				if (connectionHandler.getPlayerId() == p.getId()) {
					p.setStatus("Warten");
				}
			}
			sendResourceCardsReturned();
		}
	}

	/**
	 * Returns whether building request is legal or not
	 * 
	 * @param building
	 *            requested building (including the ID of the player who
	 *            requested)
	 * @return True, if build request is legal
	 */
	private boolean isLegelBuildRequest(Building building) {
		return ServerRequestCheck.buildingCheck(building, ServerMemoryLogic.initialPhaseRemainingTurns > 0);

	}

	// 9.2 (&& 8.4?)
	/**
	 * Handles incoming building request
	 * 
	 * @param building
	 */
	public void receiveBuildRequest(Building building) {

		building.setOwner(connectionHandler.getPlayerId());

		if (!isLegelBuildRequest(building)) {
			sendServerError(new ServerError("Your build request is not legal or invalid."));
			return;
		}

		// If we are here we are fine :)
		int[] availableBuildings = ServerMemoryLogic.playerIdToAvailableBuildings.get(connectionHandler.getPlayerId());
		if (building.getType().equals("Strasse"))
			availableBuildings[0]--;
		else if (building.getType().equals("Dorf"))
			availableBuildings[1]--;
		else if (building.getType().equals("Stadt")) {
			availableBuildings[1]++;
			availableBuildings[2]--;
		}
		ServerMemoryLogic.playerIdToAvailableBuildings.put(connectionHandler.getPlayerId(), availableBuildings);
		ServerMemoryLogic.addBuilding(building);
		BuildingEvent buildingEvent = new BuildingEvent(building);
		// Broadcast building event
		connectionHandler.getServer().broadcast(buildingEvent);
		// Is initial phase?
		PlayerForProtocol player = ServerMemoryLogic.clientToPlayer.get(connectionHandler);
		if (ServerMemoryLogic.initialPhaseRemainingTurns > 0) {
			if (player.getStatus().equals("Strasse bauen")) {
				player.setStatus("Handeln oder Bauen");
				receiveTurnEndRequest();
			} else if (player.getStatus().equals("Dorf bauen")) {
				addVictoryPointForPlayer(player, 1);
				// add resources for second village build
				if (ServerMemoryLogic.initialPhaseRemainingTurns <= ServerMemoryLogic.playerList.size()) {
					Earnings earningsToUpdate = new Earnings(player.getId(), new Resources(0, 0, 0, 0, 0, 0));
					for (Field field : ServerMemoryLogic.serverMap.getFields()) {
						if (building.getLocation().contains(field.getLocation())) {
							String type = field.getType();
							switch (type) {
							case "Ackerland":
								earningsToUpdate.getResources()
										.setGrain(earningsToUpdate.getResources().getGrain() + 1);
								break;
							case "Huegelland":
								earningsToUpdate.getResources().setLoam(earningsToUpdate.getResources().getLoam() + 1);
								break;
							case "Weideland":
								earningsToUpdate.getResources().setWool(earningsToUpdate.getResources().getWool() + 1);
								break;
							case "Wald":
								earningsToUpdate.getResources().setWood(earningsToUpdate.getResources().getWood() + 1);
								break;
							case "Gebirge":
								earningsToUpdate.getResources()
										.setStone(earningsToUpdate.getResources().getStone() + 1);
								break;
							default:
								break;
							}
						}
					}
					player.addToPlayersResources(earningsToUpdate.getResources());
					sendEarnings(earningsToUpdate);
				}
				player.setStatus("Strasse bauen");
				broadcastPlayerStatusUpdate(player);
			}
		} // remove resources if not initial phase
		else {
			// Resources(Integer wood, Integer loam, Integer wool, Integer
			// grain, Integer stone, Integer hidden)
			PlayerForProtocol playerWithLongestRoute = null;
			switch (building.getType()) {
			case "Strasse":
				player.getResources().substractFromResources(new Resources(1, 1, 0, 0, 0, 0));
				ServerMemoryLogic.increaseAvailableResourceCardsNumber(new Resources(1, 1, 0, 0, 0, 0));
				connectionHandler.getServer()
						.broadcast(new Costs(building.getOwner(), new Resources(1, 1, 0, 0, 0, 0)));
				// Check if longest rode
				break;
			case "Dorf":
				addVictoryPointForPlayer(player, 1);
				player.getResources().substractFromResources(new Resources(1, 1, 1, 1, 0, 0));
				ServerMemoryLogic.increaseAvailableResourceCardsNumber(new Resources(1, 1, 1, 1, 0, 0));
				connectionHandler.getServer()
						.broadcast(new Costs(building.getOwner(), new Resources(1, 1, 1, 1, 0, 0)));
				break;
			case "Stadt":
				addVictoryPointForPlayer(player, 1);
				player.getResources().substractFromResources(new Resources(0, 0, 0, 2, 3, 0));
				ServerMemoryLogic.increaseAvailableResourceCardsNumber(new Resources(0, 0, 0, 2, 3, 0));
				connectionHandler.getServer()
						.broadcast(new Costs(building.getOwner(), new Resources(0, 0, 0, 2, 3, 0)));
				break;
			}
			playerWithLongestRoute = ServerMemoryLogic.getPlayerWithLongestRoute();
			// Broadcast only if necessary
			if (ServerMemoryLogic.currentPlayerWithLongestRoad != playerWithLongestRoute) {
				if (playerWithLongestRoute != null)
					sendLongestRoadInfo(playerWithLongestRoute);
				else
					sendLongestRoadInfo();
			}
		}
	}

	/**
	 * Adds a victory point for the player and saves it in the player list
	 *
	 * @param player
	 *            Player who gets to add the victory point
	 */
	private void addVictoryPointForPlayer(PlayerForProtocol player, int amount) {
		player.setVictoryPoints(player.getVictoryPoints() + amount);
		addVictoryCardPointsIfWinning(player);
	}


	// 9.2
	/**
	 * handles what happens when resource cards need to be dropped
	 */
	public void sendResourceCardsReturned() {
		ServerMemoryLogic.playersWhoNeedToGiveBackCards.clear();
		// Save the player who needs to move the thief
		ServerMemoryLogic.playerToMoveThiefNext = ServerMemoryLogic.clientToPlayer.get(connectionHandler);
		for (PlayerForProtocol player : ServerMemoryLogic.playerList) {
			if (player.getResources().getTotalResources() > 7) {
				////GameStart.mainLogger.getLOGGER()()().fine(player.getId() + " needs to drop 50%");
				// Add to list
				ServerMemoryLogic.playersWhoNeedToGiveBackCards.add(player.getId());
			}
		}

		if (ServerMemoryLogic.playersWhoNeedToGiveBackCards.size() == 0) {
			sendMoveThief();
			return;
		}
		// Due to latency in network messaging, we need an separate for loop for
		// broadcasting
		HashSet<Integer> playersWhoNeedToGiveBackCardsCloned = (HashSet<Integer>) ServerMemoryLogic.playersWhoNeedToGiveBackCards
				.clone();
		for (Integer id : playersWhoNeedToGiveBackCardsCloned) {
			// sends message to client if player has more than 7 resource
			// cards
			PlayerForProtocol playerForProtocol = ServerMemoryLogic.findPlayerByID(id);
			playerForProtocol.setStatus("Karten wegen Raeuber abgeben");
			broadcastPlayerStatusUpdate(playerForProtocol);
		}

	}


	// 9.2
	/**
	 * Receives a resource cards returned request
	 * 
	 * @param resourcesReturned
	 */
	public synchronized void receiveResourceCardsReturnedRequest(ResourcesReturned resourcesReturned) {
		if (!ServerMemoryLogic.playersWhoNeedToGiveBackCards.contains(connectionHandler.getPlayerId())
				|| ServerMemoryLogic.playerToMoveThiefNext == null)
			return;
		// player is the one whose resources gets returned
		PlayerForProtocol player = ServerMemoryLogic.clientToPlayer.get(connectionHandler);
		// Check if enough resources are dropped 
		if(player.getResources().getTotalResources()/2 > resourcesReturned.getResources().getTotalResources()){
			sendServerError(new ServerError("You need to drop more resources"));
			return;
		}
		// Check if player has the resources he wants to drop 
		if(!ServerRequestCheck.checkIfEnoughResources(player, resourcesReturned.getResources())){
			sendServerError(new ServerError("You don't have this amount of resources"));
			return;
		}
			
		// then set the resources of that player to his new resources
		////GameStart.mainLogger.getLOGGER()()().fine(player.getId() + " requests to give back cards...");
		player.substractFromPlayersResources(resourcesReturned.getResources());
		ServerMemoryLogic.increaseAvailableResourceCardsNumber(resourcesReturned.getResources());

		// Check if we received all requests, if so, then send out a message to
		// move the
		// thief
		ServerMemoryLogic.playersWhoNeedToGiveBackCards.remove(player.getId());

		////GameStart.mainLogger.getLOGGER()()().fine("playersWhoNeedToGiveBackCards still to go: " + ServerMemoryLogic.playersWhoNeedToGiveBackCards);
		//send costs
		sendCosts(new Costs(player.getId(),resourcesReturned.getResources()));
		
		if (player.getId() != ServerMemoryLogic.playerToMoveThiefNext.getId()) {
			// Change to waiting and tell everyone
			player.setStatus("Warten");
			broadcastPlayerStatusUpdate(player);
		}
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (ServerMemoryLogic.playersWhoNeedToGiveBackCards.size() == 0) {
			// Just in case, make sure "numberOfDropHalfCardsPending" is set to
			// 0.
			sendMoveThief();
		}
	}


	// 9.3
	/**
	 * sends move thief to player who has to move thief and informs others
	 */
	public void sendMoveThief() {
		if (ServerMemoryLogic.playerToMoveThiefNext == null)
			return;
		if (connectionHandler.getPlayerId() != ServerMemoryLogic.playerToMoveThiefNext.getId()) {
			// Change to waiting and tell everyone
			ServerMemoryLogic.getPlayerRespondingToHandler(connectionHandler).setStatus("Warten");
			broadcastPlayerStatusUpdate(ServerMemoryLogic.getPlayerRespondingToHandler(connectionHandler));
		}
		// Load the player who needs to move the thief
		PlayerForProtocol player = ServerMemoryLogic.playerToMoveThiefNext;
		// Reset "ServerMemoryLogic.playerToMoveThiefNext"
		ServerMemoryLogic.playerToMoveThiefNext = null;
		if (player == null) {
			////GameStart.mainLogger.getLOGGER()()().fine("[#123] sendMoveThief aborted, since player is null");
			return;
		}
		player.setStatus("Raeuber versetzen");
		broadcastPlayerStatusUpdate(player);

	}


	// 9.3
	/**
	 * CHecks if future location is available, sets thief to new location,
	 * handles target to steal from if possible and player who set the thief's
	 * new location gets to perform his turn
	 * 
	 * @param moveThief
	 */
	public void receiveMoveThiefRequest(MoveThief moveThief) {
		////GameStart.mainLogger.getLOGGER()()().fine("Receive move thief!");
		try {
			// Set id of the player who gets to move thief
			Integer playerId = connectionHandler.getPlayerId();
			if(!ServerMemoryLogic.findPlayerByID(playerId).getStatus().equals("Raeuber versetzen")){
				sendServerError(new ServerError("You are not allowed to move the thief"));
				return;
			}
			// return if the requested location is also the thiefs current location
			// or if player has chosen a location a water/port location
			if (checkValidThiefLocation(moveThief.getLocation())) {
				sendServerError(new ServerError("Your chosen " + moveThief.getLocation() + " location is invalid."));
				return;
			}
			// otherwise set the thief's current location to requested location
			ServerMemoryLogic.serverMap.setThief(moveThief.getLocation());
			// create thiefMoved object to be sent to confirm that the thief has
			// been moved
			// to new location
			ThiefMoved thiefMoved = new ThiefMoved(playerId, moveThief.getLocation(), moveThief.getTarget());
			// just send ThiefMoved if there is no target to steal from
			if (moveThief.getTarget() == null) {
				sendThiefMoved(thiefMoved);
			} else {// otherwise player gets to steal a resource card from
				// target
				// stolen resource card is determined randomly
				int i = (int) (Math.random() * 5);
				for (PlayerForProtocol p : ServerMemoryLogic.playerList) {
					if (p.getId() == moveThief.getTarget()) {
						PlayerForProtocol targetPlayer = p;
						stealCard(i, playerId, moveThief.getTarget(), targetPlayer);
						break;
					}
				}
				sendThiefMoved(thiefMoved);
			}
			// the player who set the thiefs new location gets to perform his
			// turn
			PlayerForProtocol player = ServerMemoryLogic.clientToPlayer.get(connectionHandler);
			if(player == null)
				return;
			player.setStatus("Handeln oder Bauen");
			sendPlayerStatusUpdate(player);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * handles stealing from target player if thief is moved and adding
	 * resources to player who moved thief
	 * 
	 * @param i
	 *            continuous variable
	 * @param playerId
	 * @param targetId
	 * @param targetPlayer
	 */
	private void stealCard(int i, Integer playerId, Integer targetId, PlayerForProtocol targetPlayer) {
		try {
			Resources stolenCard;
			PlayerForProtocol player = ServerMemoryLogic.findPlayerByID(playerId);
			if (targetPlayer.getResources().getTotalResources() == 0) {
				return;
			}
			switch (i) {
			// TODO update the cards amount for player on the server side
			// Resources(Integer wood, Integer loam, Integer wool, Integer
			// grain, Integer
			// stone, Integer hidden)
			case 0:
				// a wood card gets stolen
				if (targetPlayer.getResources().getWood() == 0) {
					stealCard(1, playerId, targetId, targetPlayer);
					return;
				}
				stolenCard = new Resources(1, 0, 0, 0, 0, 1);
				break;
			case 1:
				if (targetPlayer.getResources().getLoam() == 0) {
					stealCard(2, playerId, targetId, targetPlayer);
					return;
				}
				// a loam card gets stolen
				stolenCard = new Resources(0, 1, 0, 0, 0, 1);
				break;
			case 2:
				if (targetPlayer.getResources().getWool() == 0) {
					stealCard(3, playerId, targetId, targetPlayer);
					return;
				}
				// a wool card gets stolen
				stolenCard = new Resources(0, 0, 1, 0, 0, 1);
				break;
			case 3:
				if (targetPlayer.getResources().getGrain() == 0) {
					stealCard(4, playerId, targetId, targetPlayer);
					return;
				}
				// a grain card gets stolen
				stolenCard = new Resources(0, 0, 0, 1, 0, 1);
				break;
			case 4:
				if (targetPlayer.getResources().getStone() == 0) {
					stealCard(0, playerId, targetId, targetPlayer);
					return;
				}
				// a stone card gets stolen
				stolenCard = new Resources(0, 0, 0, 0, 1, 1);
				break;
			default:
				// to avoid problems with switch case
				stolenCard = new Resources(0, 0, 0, 0, 0, 0);
				break;
			}
			sendCosts(new Costs(targetId, stolenCard));
			sendEarnings(new Earnings(playerId, stolenCard));
			targetPlayer.substractFromPlayersResources(stolenCard);
			player.addToPlayersResources(stolenCard);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * CHecks if requested location is valid location
	 * 
	 * @param newThiefLocation
	 */
	private boolean checkValidThiefLocation(String newThiefLocation) {
		return (ServerMemoryLogic.serverMap.getThief().equals(newThiefLocation) || newThiefLocation.equals("a")
				|| newThiefLocation.equals("b") || newThiefLocation.equals("c") || newThiefLocation.equals("d")
				|| newThiefLocation.equals("e") || newThiefLocation.equals("f") || newThiefLocation.equals("g")
				|| newThiefLocation.equals("h") || newThiefLocation.equals("i") || newThiefLocation.equals("j")
				|| newThiefLocation.equals("k") || newThiefLocation.equals("l") || newThiefLocation.equals("m")
				|| newThiefLocation.equals("n") || newThiefLocation.equals("o") || newThiefLocation.equals("p")
				|| newThiefLocation.equals("q") || newThiefLocation.equals("r"));
	}

	// 9.3
	/**
	 * Handles server action on receiving end-turn-request.
	 */
	public void receiveTurnEndRequest() {
		if (!ServerRequestCheck.checkStatusMatch(connectionHandler.getPlayerId(), "Handeln oder Bauen")) {
			sendServerError(new ServerError("It is not your turn."));
			return;
		}
		//Wait some time
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		////GameStart.mainLogger.getLOGGER()()().fine			"ServerMemoryLogic.initialPhaseRemainingTurns " + ServerMemoryLogic.initialPhaseRemainingTurns);
		ServerMemoryLogic.initialPhaseRemainingTurns--;

		PlayerForProtocol playerWhoEndedTurn = ServerMemoryLogic.clientToPlayer.get(connectionHandler);
		ServerMemoryLogic.playerIdToDevCardsBoughtThisRound.put(playerWhoEndedTurn.getId(),
				new DevelopmentCards(0, 0, 0, 0, 0, 0));
		int index = ServerMemoryLogic.playerList.indexOf(playerWhoEndedTurn);

		int indexNextTurn;
		if (ServerMemoryLogic.initialPhaseRemainingTurns == 0)
			indexNextTurn = 0;
		else {
			indexNextTurn = (ServerMemoryLogic.initialPhaseRemainingTurns <= ServerMemoryLogic.playerList.size()
					&& ServerMemoryLogic.initialPhaseRemainingTurns > 0)
							? index
									- (ServerMemoryLogic.playerList
											.size() == ServerMemoryLogic.initialPhaseRemainingTurns ? 0
													: 1)
							: (index + 1) % ServerMemoryLogic.playerList.size();
		}

		PlayerForProtocol playerForNextTurn = ServerMemoryLogic.playerList.get(indexNextTurn);
		////GameStart.mainLogger.getLOGGER()()().fine("Next player: " + playerForNextTurn.getColor() + "(total players: 			+ ServerMemoryLogic.playerList.size());
		playerWhoEndedTurn.setStatus("Warten");
		playerForNextTurn.setStatus(ServerMemoryLogic.initialPhaseRemainingTurns > 0 ? "Dorf bauen" : "Wuerfeln");

		if (playerWhoEndedTurn != playerForNextTurn)
			broadcastPlayerStatusUpdate(playerWhoEndedTurn);
		broadcastPlayerStatusUpdate(playerForNextTurn);
		playedDevelopmentCard = false;
	}

	// 9.7 MUSS BROADCASTET WERDEN MIT HIDDEN FUER ANDERE
	/**
	 * SEnds bought development card to player
	 * 
	 * @param developmentCard
	 */
	public void sendBoughtDevelopmentCard(DevelopmentCards developmentCard) {
		connectionHandler.getServer()
				.broadcast(new DevelopmentCardBought(connectionHandler.getPlayerId(), "Unbekannt"));
		String devCardType = "";
		if (developmentCard.getKnight() != 0)
			devCardType = "Ritter";
		else if (developmentCard.getRoadBuilding() != 0)
			devCardType = "Strassenbau";
		else if (developmentCard.getMonopoly() != 0)
			devCardType = "Monopol";
		else if (developmentCard.getYearOfPlenty() != 0)
			devCardType = "Erfindung";
		else if (developmentCard.getVictoryPoint() != 0)
			devCardType = "Siegpunkt";
		connectionHandler.sendObject(new DevelopmentCardBought(connectionHandler.getPlayerId(), devCardType));
		sendCosts(new Costs(connectionHandler.getPlayerId(), ServerMemoryLogic.costsOfADevelopmentCard));
		ServerMemoryLogic.findPlayerByID(connectionHandler.getPlayerId())
				.substractFromPlayersResources(ServerMemoryLogic.costsOfADevelopmentCard);
	}

	// 9.10
	/**
	 * Notifies clients that a player has the achieved longest road.
	 *
	 * @param player
	 *            Player with the longest road.
	 */
	public void sendLongestRoadInfo(PlayerForProtocol player) {
		// Remove 2 victory points from the player who had the longest road
		PlayerForProtocol previousPlayer = ServerMemoryLogic.currentPlayerWithLongestRoad;
		if (previousPlayer != null) {
			addVictoryPointForPlayer(previousPlayer, -2); // <--- New
			previousPlayer.setLongestRoad(false);
		}

		ServerMemoryLogic.currentPlayerWithLongestRoad = player;
		LongestRoad longestRoad = new LongestRoad(player.getId());
		connectionHandler.getServer().broadcast(longestRoad);
		// Add +2 victory points to the player who obtained the longest road
		addVictoryPointForPlayer(player, 2); // <--- New
		player.setLongestRoad(true);
		// Broadcast information
		broadcastPlayerStatusUpdate(player);
	}

	/**
	 * Notifies clients that no one has the longest round anymore.
	 */
	public void sendLongestRoadInfo() {
		connectionHandler.getServer().broadcast(new LongestRoad());
		// Remove 2 victory points from the player who had the longest road
		// at last
		if (ServerMemoryLogic.currentPlayerWithLongestRoad != null) {
			addVictoryPointForPlayer(ServerMemoryLogic.currentPlayerWithLongestRoad, -2);
			ServerMemoryLogic.currentPlayerWithLongestRoad.setLongestRoad(false);
			// Broadcast information
			broadcastPlayerStatusUpdate(ServerMemoryLogic.currentPlayerWithLongestRoad);
		}
		// Reset player with longest road
		ServerMemoryLogic.currentPlayerWithLongestRoad = null;

	}

	// 9.10 das soll man glaub ich broadcasten?
	/**
	 * Sends largest army info to all clients via broadcast
	 * 
	 * @param playerId
	 */
	public void sendLargestArmyInfo(Integer playerId) {
		connectionHandler.getServer().broadcast(new LargestArmy(playerId));
	}

	/**
	 * HAndles maritime trade at port
	 * 
	 * @param maritimeTrade
	 */
	public void receiveMaritimeTrade(MaritimeTrade maritimeTrade) {
		// player who sent the trade request
		PlayerForProtocol player = ServerMemoryLogic.findPlayerByID(connectionHandler.getPlayerId());
        ////GameStart.mainLogger.getLOGGER()()().fine("player's resources before trade " + player.getResources());
		// TODO : Check if trade legal and if we have enough resources in the
		// server
		// ...check if able to trade this kind of trade (settlement at port?)
		// ..do we have enough resources?
		if (!ServerRequestCheck.mariTimeTradeCheck(maritimeTrade, player)) {
            ////GameStart.mainLogger.getLOGGER()()().fine("Your trade request is not legal or invalid.");
		    ////GameStart.mainLogger.getLOGGER()()().fine(player.toString());
			sendServerError(new ServerError("Your trade request is not legal or invalid."));
			return;
		}
		Resources offeredResources = maritimeTrade.getResourcesSupply();
		Resources requestedResources = maritimeTrade.getResourcesDemand();
		// Send cost
		sendCosts(new Costs(connectionHandler.getPlayerId(), offeredResources));
		player.substractFromPlayersResources(offeredResources);
		// Send earning
		sendEarnings(new Earnings(connectionHandler.getPlayerId(), requestedResources));
		player.addToPlayersResources(requestedResources);
		////GameStart.mainLogger.getLOGGER()()().fine("player's current resources " + player.getResources());
	}

	// 10.1
	/**
	 * HAndles domestic trade via trade id
	 * 
	 * @param domesticTradeOffer
	 */
	public void receiveDomesticTrade(DomesticTradeOffer domesticTradeOffer) {
		// check if the player offering the trade has enough resources
		if (!ServerRequestCheck.checkIfEnoughResources(ServerMemoryLogic.findPlayerByID(connectionHandler.getPlayerId()),
				domesticTradeOffer.getResourcesSupply())
				||domesticTradeOffer.getResourcesSupply().getTotalResources()==0
				||domesticTradeOffer.getResourcesDemand().getTotalResources()==0
				||domesticTradeOffer.containsSameResourceType()) {
			sendServerError(new ServerError("Your trade request is not legal or invalid."));
			return;
		}
		// check if it's the player's turn
		if (!(ServerMemoryLogic.findPlayerByID(connectionHandler.getPlayerId()).getStatus()
				.equals("Handeln oder Bauen"))) {
			sendServerError(new ServerError("It's not your turn to trade."));
			return;
		}
		// Set a trade id
		Integer tradeID = ServerMemoryLogic.getIncrementedUniqueIdIncrementer();
		// Resource array
		Resources[] resources = new Resources[2];
		resources[0] = domesticTradeOffer.getResourcesDemand();
		resources[1] = domesticTradeOffer.getResourcesSupply();
		// Add trade to hashmap
		ServerMemoryLogic.tradesResourceHashMap.put(tradeID, resources);
		// Add requester to hashmap
		ServerMemoryLogic.tradesIDHashMap.put(tradeID, connectionHandler.getPlayerId());

		ReceivedTradeOffer receivedTradeOffer = new ReceivedTradeOffer(connectionHandler.getPlayerId(), tradeID,
				domesticTradeOffer.getResourcesSupply(), domesticTradeOffer.getResourcesDemand());
		// TODO: Make sure this works, otherwise uncomment line below
		// connectionHandler.getServer().broadcast(receivedTradeOffer); // <---
		connectionHandler.getServer().broadcastToAllOthers(connectionHandler.getPlayerId(), receivedTradeOffer);
	}

	// 10.2
	/**
	 * HAndles accept trade
	 * 
	 * @param playerReadyForDomesticTrade
	 */
	public void receiveAcceptTrade(PlayerReadyForDomesticTrade playerReadyForDomesticTrade) {
		//Quit if player declined trade
		if(!playerReadyForDomesticTrade.isAccepted()){
			connectionHandler.getServer().broadcastToAllOthers(connectionHandler.getPlayerId(), new PlayerWhoAcceptedTrade(connectionHandler.getPlayerId(),playerReadyForDomesticTrade.getTradeId(), false));
			return;
		}
		// check if the player accepting the trade has enough resources
		Resources requiredResources = ServerMemoryLogic
				.getRequestedResourcesFromTradeId(playerReadyForDomesticTrade.getTradeId());
		if (!ServerRequestCheck.checkIfEnoughResources(
				ServerMemoryLogic.findPlayerByID(connectionHandler.getPlayerId()), requiredResources)) {
			sendServerError(new ServerError("Your trade request is not legal or invalid."));
			connectionHandler.getServer().broadcast(new AbandonedTrade(connectionHandler.getPlayerId(), playerReadyForDomesticTrade.getTradeId()));
			return;
		}
		////GameStart.mainLogger.getLOGGER()()().fine("Player ID: " + (connectionHandler.getPlayerId()) + " joined the trade");
		PlayerWhoAcceptedTrade playerWhoAcceptedTrade = new PlayerWhoAcceptedTrade(connectionHandler.getPlayerId(),
				playerReadyForDomesticTrade.getTradeId());
		connectionHandler.getServer().broadcast(playerWhoAcceptedTrade);
	}

	// 10.3
	/**
	 * Executes domestic trade via trade id
	 * 
	 * @param completeDomesticTrade
	 */
	public void receiveExecuteDomesticTrade(CompleteDomesticTrade completeDomesticTrade) {
		////GameStart.mainLogger.getLOGGER()()().fine("Execute domestic trade with ID: " + completeDomesticTrade.getTradeId());
		Resources[] resources = ServerMemoryLogic.tradesResourceHashMap.get(completeDomesticTrade.getTradeId());

		PlayerForProtocol requester = ServerMemoryLogic
				.findPlayerByID(ServerMemoryLogic.tradesIDHashMap.get(completeDomesticTrade.getTradeId()));
		PlayerForProtocol acceptor = ServerMemoryLogic.findPlayerByID(completeDomesticTrade.getPlayerId());

		if (resources == null || acceptor == null || requester == null)
			return;
		// TODO: Check if trade is legal
		// ..
		Resources demandResources = resources[0];
		Resources supplyResources = resources[1];

		// Handle trade requester...

		// Send cost
		sendCosts(new Costs(requester.getId(), supplyResources));
		requester.substractFromPlayersResources(supplyResources);
		// Send earning
		sendEarnings(new Earnings(requester.getId(), demandResources));
		requester.addToPlayersResources(demandResources);

		// handle trade accepter...

		// Send cost
		sendCosts(new Costs(acceptor.getId(), demandResources));
		acceptor.substractFromPlayersResources(demandResources);
		// Send earning
		sendEarnings(new Earnings(acceptor.getId(), supplyResources));
		acceptor.addToPlayersResources(supplyResources);

		// Inform everyone that a trade has been executed...
		connectionHandler.getServer().broadcast(new TradeFinished(requester.getId(), acceptor.getId()));

	}

	// 10.4
	/**
	 * Canceled trade
	 * 
	 * @param abandonDomesticTrade
	 */
	public void receiveAbandonDomesticTrade(AbandonDomesticTrade abandonDomesticTrade) {
		////GameStart.mainLogger.getLOGGER()()().fine("Player ID: " + (connectionHandler.getPlayerId()) + " canceled the trade");
		connectionHandler.getServer().broadcast(new AbandonedTrade(connectionHandler.getPlayerId(), abandonDomesticTrade.getTradeId()));
	}

	// 10.5
	/**
	 * handles buy development card request and checks several options
	 */
	public void receiveBuyDevelopmentCardRequest() {
		if (!ServerRequestCheck
				.checkIfAbleToBuyDevelopmentCard(ServerMemoryLogic.findPlayerByID(connectionHandler.getPlayerId()))) {
			sendServerError(new ServerError("You cannot buy a development card right now"));
			return;
		}
		DevelopmentCards developmentCards = ServerMemoryLogic.getRandomDevelopmentCard();
		if (developmentCards == null) {
			sendServerError(new ServerError("There are no development cards left"));
			return;
		}
		PlayerForProtocol player = ServerMemoryLogic.findPlayerByID(connectionHandler.getPlayerId());
		ServerMemoryLogic.playerIdToDevCardsBoughtThisRound.get(player.getId()).addCards(developmentCards);
		player.getDevelopmentCards().addCards(developmentCards);
		sendBoughtDevelopmentCard(developmentCards);
		addVictoryCardPointsIfWinning(player);
	}

	// 12.1
	// hier bekommt man nur die zwei ersten parameter!
	/**
	 * HAndles playing of knight card
	 * 
	 * @param playKnightCard
	 */
	public void receivePlayKnightCardRequest(PlayKnightCard playKnightCard) {
		PlayerForProtocol player = ServerMemoryLogic.findPlayerByID(connectionHandler.getPlayerId());
		// Check if player is allowed to play this card
		if (!(player.getStatus().equals("Handeln oder Bauen") || player.getStatus().equals("Wuerfeln")) 
				|| player.getDevelopmentCards().getKnight() < 1 || playedDevelopmentCard 
				|| ServerMemoryLogic.playerIdToDevCardsBoughtThisRound.get(player.getId())
						.getKnight() == player.getDevelopmentCards().getKnight()) {
			sendServerError(new ServerError("You can't play that now"));
			return;
		}
		if (checkValidThiefLocation(playKnightCard.getLocation())) {
			sendServerError(new ServerError("Invalid location!"));
			return;
		}
		if (player.getKnightPoints() != null)
			player.setKnightPoints(player.getKnightPoints() + 1);
		else
			player.setKnightPoints(1);
		player.getDevelopmentCards().removeCard(new DevelopmentCards(1, 0, 0, 0, 0, 0));
		PlayKnightCard fullPlayKnightCard = new PlayKnightCard(playKnightCard.getLocation(), playKnightCard.getTarget(),
				connectionHandler.getPlayerId());
		ServerMemoryLogic.serverMap.setThief(fullPlayKnightCard.getLocation());
		// steal a card if there is a target to steal from
		if (playKnightCard.getTarget() != null) {
			int i = (int) (Math.random() * 5);
			for (PlayerForProtocol p : ServerMemoryLogic.playerList) {
				if (p.getId() == playKnightCard.getTarget()) {
					PlayerForProtocol targetPlayer = p;
					stealCard(i, fullPlayKnightCard.getPlayer(), fullPlayKnightCard.getTarget(), targetPlayer);
					break;
				}
			}
		}
		connectionHandler.getServer().broadcast(fullPlayKnightCard);
		playedDevelopmentCard = true;
		if (player.getKnightPoints() > ServerMemoryLogic.largestArmy && !player.hasLargestArmy()) {
			// Change the player who had the most knights before this card was
			// played
			PlayerForProtocol previousPlayer = ServerMemoryLogic.currentPlayerWithLargestArmy;
			if (previousPlayer != null) {
				addVictoryPointForPlayer(previousPlayer, -2);
				previousPlayer.setLargestArmy(false);
			}
			ServerMemoryLogic.largestArmy = player.getKnightPoints();
			ServerMemoryLogic.currentPlayerWithLargestArmy = player;
			// Change the player who now has the most knights
			player.setLargestArmy(true);
			addVictoryPointForPlayer(player, 2);
			sendLargestArmyInfo(player.getId());
//			//Broadcast broadcast
//			sendPlayerStatusUpdate(player);
		}		
	}

	// 12.2
	// hier bekommt man nur die zwei ersten parameter!
	/**
	 * this method needs to check if the chosen streets are legal and handle
	 * accordingly - however, it is possible that the streets are legal only if
	 * checked in the reverted order - the second street is required for the
	 * first to be legal that's why this method needs to be called twice, once
	 * with street1, street2 and then with street2, street1
	 * 
	 * @param playRoadBuildingCard
	 * @param firstCheckPerformed
	 *            0 by the first check, 1 by the second one (reverted order)
	 */
	public void receivePlayRoadBuildingCardRequest(PlayRoadBuildingCard playRoadBuildingCard,
			boolean firstCheckPerformed) {
		PlayerForProtocol player = ServerMemoryLogic.findPlayerByID(connectionHandler.getPlayerId());
		// Check if player is allowed to play this card
		if (!(player.getStatus().equals("Handeln oder Bauen") || player.getStatus().equals("Wuerfeln")) 
				|| player.getDevelopmentCards().getRoadBuilding() < 1 || playedDevelopmentCard 
				|| ServerMemoryLogic.playerIdToDevCardsBoughtThisRound.get(player.getId())
						.getRoadBuilding() == player.getDevelopmentCards().getRoadBuilding()) {
			sendServerError(new ServerError("You can't play that now"));
			return;
		}
		if ((ServerMemoryLogic.playerIdToAvailableBuildings.get(player.getId())[0] == 0)) {
			////GameStart.mainLogger.getLOGGER()()().fine("all streets used");
			sendServerError(new ServerError("You have no streets left"));
			return;
		}
		if(playRoadBuildingCard.getStreet1().equals(playRoadBuildingCard.getStreet2())){
			sendServerError(new ServerError("You chose the same street twice"));
			return;
		}
		Building street1 = new Building(connectionHandler.getPlayerId(), "Strasse", playRoadBuildingCard.getStreet1());
		Building street2;
		if (playRoadBuildingCard.getStreet2() != null
				&& ServerMemoryLogic.playerIdToAvailableBuildings.get(player.getId())[0] >= 2) {
			street2 = new Building(connectionHandler.getPlayerId(), "Strasse", playRoadBuildingCard.getStreet2());
		} else if (playRoadBuildingCard.getStreet2() != null
				&& ServerMemoryLogic.playerIdToAvailableBuildings.get(player.getId())[0] == 1) {
			sendServerError(new ServerError("Only one street could be built"));
			street2 = null;
		} else
			street2 = null;
		if (ServerRequestCheck.checkStreetRequest(street1, false, true)) {
			ServerMemoryLogic.addBuilding(street1);
			if (street2 == null) {
				broadcastRoadBuildingCardPlayed(
						new PlayRoadBuildingCard(playRoadBuildingCard.getStreet1(), connectionHandler.getPlayerId()));
				BuildingEvent buildingEvent1 = new BuildingEvent(street1);
				int[] availableBuildings = ServerMemoryLogic.playerIdToAvailableBuildings
						.get(connectionHandler.getPlayerId());
				availableBuildings[0]--;
				ServerMemoryLogic.playerIdToAvailableBuildings.put(connectionHandler.getPlayerId(), availableBuildings);
				connectionHandler.getServer().broadcast(buildingEvent1);

			} else if (!ServerRequestCheck.checkStreetRequest(street2, false, true)) {
				ServerMemoryLogic.removeBuilding(street1);
				sendServerError(new ServerError("Road building card declined, please choose different streets"));
				return;
			}
			// if we reach this "else" it means both streets were accepted
			else {
				ServerMemoryLogic.addBuilding(street2);
				broadcastRoadBuildingCardPlayed(new PlayRoadBuildingCard(playRoadBuildingCard.getStreet1(),
						playRoadBuildingCard.getStreet2(), connectionHandler.getPlayerId()));
				BuildingEvent buildingEvent1 = new BuildingEvent(street1);
				BuildingEvent buildingEvent2 = new BuildingEvent(street2);
				int[] availableBuildings = ServerMemoryLogic.playerIdToAvailableBuildings
						.get(connectionHandler.getPlayerId());
				availableBuildings[0] -= 2;
				ServerMemoryLogic.playerIdToAvailableBuildings.put(connectionHandler.getPlayerId(), availableBuildings);
				// Broadcast building event
				connectionHandler.getServer().broadcast(buildingEvent1);
				connectionHandler.getServer().broadcast(buildingEvent2);
			}
			PlayerForProtocol playerWithLongestRoute = ServerMemoryLogic.getPlayerWithLongestRoute();
			// Broadcast only if necessary
			if (ServerMemoryLogic.currentPlayerWithLongestRoad != playerWithLongestRoute) {
				if (playerWithLongestRoute != null)
					sendLongestRoadInfo(playerWithLongestRoute);
				else
					sendLongestRoadInfo();
			}
		} else if (!ServerRequestCheck.checkStreetRequest(street1, false, true) && !firstCheckPerformed) {
			receivePlayRoadBuildingCardRequest(
					new PlayRoadBuildingCard(playRoadBuildingCard.getStreet2(), playRoadBuildingCard.getStreet1()),
					true);
		} else
			sendServerError(new ServerError("Road building card declined, please choose different streets"));
	}

	// 12.2
	// hier den konstruktor mit 3 parametern verwenden!
	/**
	 * broadcast that road building card was played to all clients
	 * 
	 * @param playRoadBuildingCard
	 */
	public void broadcastRoadBuildingCardPlayed(PlayRoadBuildingCard playRoadBuildingCard) {
		connectionHandler.getServer().broadcast(playRoadBuildingCard);
		playedDevelopmentCard = true;
	}

	// 12.3
	// hier bekommt man nur den ersten parameter!
	/**
	 * Handles monopoly card is played
	 * 
	 * @param monopolyCard
	 */
	public void receivePlayMonopolyCardRequest(Monopoly monopolyCard) {
		PlayerForProtocol player = ServerMemoryLogic.findPlayerByID(connectionHandler.getPlayerId());
		// Check if player is allowed to play this card
		if (!(player.getStatus().equals("Handeln oder Bauen") || player.getStatus().equals("Wuerfeln")) 
				|| player.getDevelopmentCards().getMonopoly() < 1 || playedDevelopmentCard 
				|| ServerMemoryLogic.playerIdToDevCardsBoughtThisRound.get(player.getId())
						.getMonopoly() == player.getDevelopmentCards().getMonopoly()) {
			sendServerError(new ServerError("You can't play that now"));
			return;
		}
		monopolyCard.setPlayerId(connectionHandler.getPlayerId());
		player.getDevelopmentCards().setMonopoly(player.getDevelopmentCards().getMonopoly() - 1);
		broadcastMonopolyCardPlayed(monopolyCard);

	}

	// 12.3
	// hier den konstruktor mit 2 parametern verwenden!
	/**
	 * Broadcasts that monopoly card was played to all clients
	 * 
	 * @param monopoly
	 */
	public void broadcastMonopolyCardPlayed(Monopoly monopoly) {
		// tell everyone the monopoly card was played
		connectionHandler.getServer().broadcast(monopoly);
		playedDevelopmentCard = true;
		Earnings earnings = new Earnings(monopoly.getPlayerId(), new Resources(0, 0, 0, 0, 0, 0));
		PlayerForProtocol receivingPlayer = null;
		// Send cost of the resource to every player
		for (PlayerForProtocol player : ServerMemoryLogic.playerList) {
			// except the one who played the card
			if (player.getId() == monopoly.getPlayerId()) {
				receivingPlayer = player;
				continue;
			}
			int amount = player.getResources().getResource(monopoly.getResource());
			Resources resources = new Resources(0, 0, 0, 0, 0, 0);
			resources.addResource(monopoly.getResource(), amount);
			Costs cost = new Costs(player.getId(), resources);
			sendCosts(cost);
			player.getResources().substractFromResources(resources);
			earnings.getResources().addToResources(resources);
		}
		// Set hidden 0, because we send earnings always without hidden
		earnings.getResources().setHidden(0);
		// add the earnings of the monopoly to the players' resources
		sendEarnings(earnings);
		receivingPlayer.getResources().addToResources(earnings.getResources());
	}

	// 12.4
	// hier bekommt man nur den ersten parameter!
	/**
	 * handles year of plenty card is played
	 * 
	 * @param yearOfPlentyCard
	 */
	public void receivePlayYearOfPlentyCardRequest(YearOfPlenty yearOfPlentyCard) {
		PlayerForProtocol player = ServerMemoryLogic.findPlayerByID(connectionHandler.getPlayerId());
		// Check if player is allowed to play this card
		if (!(player.getStatus().equals("Handeln oder Bauen") || player.getStatus().equals("Wuerfeln")) 
				|| player.getDevelopmentCards().getYearOfPlenty() < 1 || playedDevelopmentCard 
				|| ServerMemoryLogic.playerIdToDevCardsBoughtThisRound.get(player.getId())
						.getYearOfPlenty() == player.getDevelopmentCards().getYearOfPlenty()) {
			sendServerError(new ServerError("You can't play that now"));
			return;
		}
		if (yearOfPlentyCard.getResources().getTotalResources() != 2) {
			sendServerError(new ServerError("Wrong amount of resources"));
			return;
		}
		yearOfPlentyCard.setPlayerId(player.getId());
		broadcastYearOfPlentyCardPlayed(yearOfPlentyCard);
		player.getDevelopmentCards().setYearOfPlenty(player.getDevelopmentCards().getYearOfPlenty() - 1);
		player.getResources().addToResources(yearOfPlentyCard.getResources());
		sendEarnings(new Earnings(player.getId(), yearOfPlentyCard.getResources()));
	}

	// 12.4
	// hier den konstruktor mit 2 parametern verwenden!
	/**
	 * Broadcasts that year of plenty card was played to all clients
	 * 
	 * @param yearOfPlenty
	 */
	public void broadcastYearOfPlentyCardPlayed(YearOfPlenty yearOfPlenty) {
		connectionHandler.getServer().broadcast(yearOfPlenty);
		playedDevelopmentCard = true;
	}

	/**
	 * Checks if a player could win the game by playing victory cards
	 * 
	 * @param player
	 */
	private void addVictoryCardPointsIfWinning(PlayerForProtocol player) {
		int maximalPoints = player.getVictoryPoints() + player.getDevelopmentCards().getVictoryPoint();
		if (maximalPoints >= 10) {
			player.setVictoryPoints(maximalPoints);
			connectionHandler.getServer().broadcast(new GameOver(player.getName() + " has won the game!", player.getId()));
            connectionHandler.getServer().clearMemory();
        }
	}
}