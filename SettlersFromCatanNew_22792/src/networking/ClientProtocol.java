package networking;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.HashMap;

import ai.AiLogic;
import application.GameStart;
import controller.InGameController;
import gameobjects.Elements.BuildingPrototype;
import gameobjects.Elements.Settlement;
import gameobjects.Elements.Street;
import gameworld.HexagonField;
import gameworld.WayPoint;
import javafx.application.Platform;
import menu.GameSetUp;
import networking.MessageObjects.*;
import player.Player;
import tools.*;
import view.ErrorPopUp;

public class ClientProtocol {

	private ServerConnectionHandler connectionHandler;

	public ClientProtocol(ServerConnectionHandler connectionHandler) {
		this.connectionHandler = connectionHandler;
	}

	// 3.1
	public void receiveHelloMessage(InitialMessage hello) {
		boolean versionSupported = false;
		for (String str : getConnectionHandler().getSupporedServerVersions()) {
			if (str.trim().equals(hello.getProtocol()))
				versionSupported = true;
		}
		if (versionSupported) {
			GameStart.mainLogger.getLOGGER().fine("supp");
			sendHelloMessage();
		} else
			getConnectionHandler().disconnectFromServer();
	}

	// 3.2
	public void sendHelloMessage() {
		InitialMessage initialMessage = new InitialMessage(getConnectionHandler().getClientVersion());
		getConnectionHandler().sendObject(initialMessage);
	}

	// 3.3

	/**
	 * Assigns the id (given from the server) to this client protocol.
	 *
	 * @param welcome
	 *            The welcome message containing the (unique) id.
	 */
	public void receiveWelcomeMessage(WelcomeMessage welcome) {
		Integer playerID = welcome.getId();
		getConnectionHandler().setPlayerId(playerID);
	}

	// 5.1
	public void receiveServerResponse(ServerResponse response) {
		if (response.getServerResponse().equals("Farbe bereits vergeben")) {
			GameStart.gameView.showGameSetUp(true);
			// Display an error Pop-Up
			Platform.runLater(() -> new ErrorPopUp("Color already occupied!",
					"Please select a different color " + "in order to be able to join the game session.", false));
		}
	}

	// 5.2

	/**
	 * Sends a chat message to the server.
	 *
	 * @param message
	 *            The chat message to send.
	 */
	public void requestToSendChat(String message) {
		// TODO: check if valid message
		if (message != null && message.length() > 0 && message != " ") {
			SendChat sendChat = new SendChat(message);
			getConnectionHandler().sendObject(sendChat);
		}
	}

	// 5.3

	/**
	 * Handles consequences (actions) after receiving a chat message from the
	 * server.
	 *
	 * @param chat
	 *            The chatMessage we received.
	 */
	public void receiveChat(ReceiveChat chat) {
		if (GameStart.gameView == null)
			return;
		if (chat.getSender() == null) {
			// Update chat window
			final String teamLabel = "[Server]";
			Platform.runLater(() -> {
				GameStart.gameView.updateInGameClientChatWindow(teamLabel + " " + chat.getMessage());
			});
			return;
		}
		// Do not draw world if singleplayer ai
		if (getConnectionHandler().isSinglePlayerAI())
			return;

		// Update chat window
		final String teamLabel = checkSameIdAsConnectionHandler(chat.getSender()) ? "[You]"
				: ("[" + GameStart.siedlerVonCatan.findPlayerByID(chat.getSender()).getTeam().toString() + "]");
		Platform.runLater(() -> {
			GameStart.gameView.updateInGameClientChatWindow(teamLabel + " " + chat.getMessage());
		});
	}

	// 6.1

	/**
	 * the player sends a request with a desired name and come, which have to be
	 * accepted by the server
	 *
	 * @param mainPlayer
	 *            player who wants to connect (KI or Human)
	 */
	public void sendPlayerRequest(Player mainPlayer) {
		// Convert to match protocol information
		// id set to null, as you dont inform the server about your id (it's the
		// other
		// way round)
		// PlayerForProtocol(int id, String farbe, String name, String status,
		// int
		// siegPunkte, Resources resource)

		// Set a color
		String color;
		switch (mainPlayer.getTeam()) {
		case TEAM_BLUE:
			color = "Blau";
			break;
		case TEAM_ORANGE:
			color = "Orange";
			break;
		case TEAM_RED:
			color = "Rot";
			break;
		case TEAM_WHITE:
			color = "Weiss";
			break;
		// Not really necessary, but just in case...
		default:
			color = "Orange";
			break;

		}
		// Create object (to be sent)
		PlayerForProtocol playerForProtocol = new PlayerForProtocol(getConnectionHandler().getPlayerId(), color,
				mainPlayer.getName(), null, 0, null, 0, null);
		// Test
		// new TestNetworkWindow(playerForProtocol);
		// Send object
		getConnectionHandler().sendObject(playerForProtocol);
	}

	// 6.2

	/**
	 * Request game start action. Player informs the server they are ready to
	 * start the game
	 */
	public void requestGameStart() {
		getConnectionHandler().sendObject(new StartGame());
	}

	// 6.3Only one street could be built
	public void receiveError(ServerError error) {
		GameStart.mainLogger.getLOGGER().fine("Error received: " + error.getErrorMessage());
		// Check different kind of error messages
		if (error.getErrorMessage().equals("Farbe bereits vergeben")) {
			GameStart.gameView.showGameSetUp(true);
			// Display an error Pop-Up
			Platform.runLater(() -> new ErrorPopUp("Color already occupied!",
					"Please select a different color " + "in order to be able to join the game session.", false));
		} else if (error.getErrorMessage().equals("Your trade request is not legal or invalid.")) {
			GameStart.gameView.updateInGameServerWindow("Trade not possible");
		} else if (error.getErrorMessage().equals("Your build request is not legal or invalid.")) {
			GameStart.gameView.updateInGameServerWindow("Building request denied");
		} else if (error.getErrorMessage().equals("Road building card declined, please choose different streets")) {
			Platform.runLater(() -> GameStart.gameView.dehighlightStreetButton());
			GameStart.gameView.updateInGameServerWindow("Please choose different streets");
			Platform.runLater(() -> GameStart.gameView.displayLargeErrorMessage("Please choose different streets"));
		} else if (error.getErrorMessage().equals("Only one street could be built")) {
			GameStart.gameView.updateInGameServerWindow("Only one street could be built");
		} else if (error.getErrorMessage().equals("You have no streets left")) {
			Platform.runLater(() -> GameStart.gameView.dehighlightStreetButton());
			GameStart.gameView.updateInGameServerWindow("You have no streets left");
			Platform.runLater(() -> GameStart.gameView.displayLargeErrorMessage("You have no streets left"));
		} else if (error.getErrorMessage().equals("Game was already started")) {
			Platform.runLater(() -> new ErrorPopUp("Game already started", "You cannot join this game anymore.", true));
		} else if (error.getErrorMessage().contains("location is invalid.")&&connectionHandler.isAI()) {
			Object[] objects = getConnectionHandler().getAiLogic().handleThiefAction(true);
			HexagonField hexagonField = (HexagonField) objects[0];
			requestMoveThief(hexagonField.getPosition().castTo2D(), null);
		} else {
			GameStart.gameView.updateInGameServerWindow(error.getErrorMessage());
			if (!getConnectionHandler().isSinglePlayerAI())
				Platform.runLater(() -> GameStart.gameView.displayLargeErrorMessage(error.getErrorMessage()));
		}

		if (getConnectionHandler().isAI() && GameStart.siedlerVonCatan.findPlayerByID(connectionHandler.getPlayerId())
				.getStatus().equals("Handeln oder Bauen")) {
			waitAndRequestEndTurn();
		}
	}

	// 6.4 hier wird die Karte gesetzt - gamestarted hat map als attribut
	public void receiveGameStarted(GameStarted gameStarted) {
		// Do not draw world if singleplayer ai
		if (!getConnectionHandler().isSinglePlayerAI())
			Platform.runLater(() -> {
				// GameStart.mainLogger.getLOGGER().fine("Drawing World");
				// Initialize a basic game world
				GameStart.siedlerVonCatan.drawGameWorld();
				// Set specific features (for example the world from the server)
				GameStart.gameView.drawWorldOnline(gameStarted.getMap());
			});

		// Display a chat greeting-message if ai
		if (getConnectionHandler().isAI())
			try {
				requestToSendChat(getConnectionHandler().getAiLogic().getGreeting());
			} catch (FileNotFoundException | URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	// 7.1 playerStatusUpdate hat ein player objekt als attribut!
	public void receivePlayerStatusUpdate(PlayerStatusUpdate update) {
		if (GameStart.siedlerVonCatan.gameFinished)
			return;

		try {
			PlayerForProtocol playerForProtocol = update.getPlayer();
			if (playerForProtocol.getStatus().equals("Spiel starten")
					&& !checkSameIdAsConnectionHandler(update.getPlayer().getId())
					&& !GameStart.siedlerVonCatan.isSinglePlayer())
				Platform.runLater(() -> GameSetUp.playerConnected(update.getPlayer().getColor()));
			else if (playerForProtocol.getStatus().equals("Wartet auf Spielbeginn")) {
				if (GameStart.siedlerVonCatan == null)
					return;
				PlayerTeam translatedColor = tools.WorldTranslation.getClientColorType(playerForProtocol.getColor());
				GameStart.mainLogger.getLOGGER().fine(translatedColor.toString());
				getConnectionHandler().extendIdColorMap(playerForProtocol.getId(), translatedColor);
				if (!GameStart.siedlerVonCatan.isSinglePlayer()) {
					GameStart.mainLogger.getLOGGER().fine("Multiplayer");
					Player newPlayer = new Player(translatedColor, playerForProtocol.getName());
					if (GameStart.siedlerVonCatan.findPlayerByID(playerForProtocol.getId()) == null) {
						newPlayer.setPlayerID(playerForProtocol.getId());
						newPlayer.setStatus(playerForProtocol.getStatus());
						GameStart.siedlerVonCatan.addPlayer(newPlayer);
					}
					// If ai, then add Ai logic
					if (getConnectionHandler().isAI() && checkSameIdAsConnectionHandler(playerForProtocol.getId())) {
						// GameStart.mainLogger.getLOGGER().fine("SinglePlayer
						// AI");
						getConnectionHandler().addAILogic(new AiLogic(newPlayer));
					}
				} else { // Singleplayer
					GameStart.mainLogger.getLOGGER().fine("Singleplayer");
					Player newPlayer = new Player(translatedColor, playerForProtocol.getName());
					if (checkSameIdAsConnectionHandler(playerForProtocol.getId())) {
						newPlayer.setPlayerID(playerForProtocol.getId());
						newPlayer.setStatus(playerForProtocol.getStatus());
						GameStart.siedlerVonCatan.addPlayer(newPlayer);
					}
					if (playerForProtocol.getId() == getConnectionHandler().getPlayerId()) {

						// If ai, then add Ai logic
						if (getConnectionHandler().isAI()) {
							// GameStart.mainLogger.getLOGGER().fine("SinglePlayer
							// AI");
							getConnectionHandler().addAILogic(new AiLogic(newPlayer));
						}
					}
				}
				Platform.runLater(() -> {
					GameStart.gameView.relocatePlayerAvatars();
				});
			} else {
				// Update player status in the list as well
				if (playerForProtocol.getStatus().equals("Verbindung verloren")) {
					connectionHandler.disconnectFromServer();
					if (getConnectionHandler().isSinglePlayerAI())
						return;
					int playerId = playerForProtocol.getId();
					Platform.runLater(() -> {
						new ErrorPopUp("Game aborted!",
								"Player " + playerId
										+ " has disconnected from the server. You will now return to the main menu.",
								true);
					});
				}
				if (!GameStart.siedlerVonCatan.isSinglePlayer()
						|| (update.getPlayer().getId() == connectionHandler.getPlayerId())) {
					Player player = GameStart.siedlerVonCatan.findPlayerByID(update.getPlayer().getId());
					if (player == null)
						return;
					player.setStatus(playerForProtocol.getStatus());
					player.setVictoryPoints(playerForProtocol.getVictoryPoints());
					Platform.runLater(() -> {
						GameStart.gameView.relocatePlayerAvatars();
					});
					if (playerForProtocol.getStatus().equals("Wuerfeln")) {
						if (GameStart.siedlerVonCatan.isInitialStartPhase()) {
							GameStart.siedlerVonCatan.setInitialStartPhase(false);
							// Deactivate wayPoints
							Platform.runLater(() -> GameStart.gameView.deactivateAllUnoccupiedWayPointButtons());
						}
						if (!checkSameIdAsConnectionHandler(playerForProtocol.getId()))
							return;
						if (!getConnectionHandler().isSinglePlayerAI()) {
							Platform.runLater(() -> GameStart.gameView.onTurnStart());
						}
						//////////////////// < AI >///////////////////////
						if (getConnectionHandler().isAI())
							requestDiceThrow();
						/////////////////////////////////////////////////
					} else if (playerForProtocol.getStatus().equals("Dorf bauen")) {
						// Wait until we can start (until we finished drawing
						// the
						// world map, ...)
						if (checkSameIdAsConnectionHandler(playerForProtocol.getId())) {
							while (!GameStart.siedlerVonCatan.isReadyToStart()) {
								Thread.sleep(250);
								GameStart.mainLogger.getLOGGER().fine("LOADING GAME...");
							}
						}
						Thread.sleep(100);
						// Update server chat if not singleplayer Ai
						if (!getConnectionHandler().isSinglePlayerAI()) {
							final String color = getConnectionHandler().getPlayerColor(playerForProtocol.getId())
									.toString();
							Platform.runLater(() -> GameStart.gameView
									.updateInGameServerWindow(color + " to build settlement(s)..."));
							if (checkSameIdAsConnectionHandler(playerForProtocol.getId())) {
								// Display notification
								Platform.runLater(() -> GameStart.gameView.displayNotificationMessage("Build village"));
							}
						}
						//////////////////// < AI >///////////////////////
						if (getConnectionHandler().isAI()
								&& checkSameIdAsConnectionHandler(playerForProtocol.getId())) {
							GameStart.mainLogger.getLOGGER().fine(playerForProtocol.getColor());
							WayPoint waypointToBuild = getConnectionHandler().getAiLogic()
									.initialSettlementPlacementAI();
							Settlement settlement = new Settlement(waypointToBuild);
							requestBuilding(settlement, BuildingType.VILLAGE);
						}
						/////////////////////////////////////////////////
					} else if (playerForProtocol.getStatus().equals("Strasse bauen"))

					{
						GameStart.mainLogger.getLOGGER().fine("Build a street");
						// Update server chat if not singleplayer Ai
						if (!getConnectionHandler().isSinglePlayerAI()) {
							final String color = getConnectionHandler().getPlayerColor(playerForProtocol.getId())
									.toString();
							Platform.runLater(() -> GameStart.gameView
									.updateInGameServerWindow(color + " to build street(s)..."));
							if (checkSameIdAsConnectionHandler(playerForProtocol.getId())
									&& !getConnectionHandler().isSinglePlayerAI()) {
								// Display notification
								Platform.runLater(() -> GameStart.gameView.displayNotificationMessage("Build street"));
							}
						}
						//////////////////// < AI >///////////////////////
						if (getConnectionHandler().isAI()
								&& checkSameIdAsConnectionHandler(playerForProtocol.getId())) {
							try {
								WayPoint[] wps = getConnectionHandler().getAiLogic().initialStreetPlacementAI();
								if (wps == null)
									return;
								// GameStart.mainLogger.getLOGGER().fine(wps);
								Street street = GameStart.siedlerVonCatan.getGameWorld().findStreetWithWayPoints(wps[0],
										wps[1]);
								requestBuilding(street, BuildingType.STREET);
							} catch (Exception e1) {
								GameStart.mainLogger.getLOGGER().fine("[ERROR?!]");
								e1.printStackTrace();
							}
						}
						/////////////////////////////////////////////////
					} else if (playerForProtocol.getStatus().equals("Karten wegen Raeuber abgeben")) {
						GameStart.mainLogger.getLOGGER().fine("Server expects id: " + player.getPlayerID() + " color: "
								+ player.getTeam().toString() + " to have: " + playerForProtocol.getResources());
						GameStart.mainLogger.getLOGGER().fine("But in reality id: " + player.getPlayerID() + " color: "
								+ player.getTeam().toString() + " to have: " + player.getResources());
						// Update server chat if not singleplayer AI
						if (!getConnectionHandler().isSinglePlayerAI()) {
							final String id = getConnectionHandler().getPlayerColor(playerForProtocol.getId())
									.toString();
							Platform.runLater(() -> GameStart.gameView.updateInGameServerWindow(id + " loses 50% ..."));
							if (checkSameIdAsConnectionHandler(playerForProtocol.getId())) {
								// If we are NOT an AI...
								if (!getConnectionHandler().isAI()) {
									// Display notification
									Platform.runLater(() -> {
										GameStart.gameView.displayNotificationMessage("Drop 50%");
										GameStart.gameView.chooseCardsToDrop();
									});
								}
								// If we are AI (NOT singleplayer)...
								else {
									Platform.runLater(() -> {
										GameStart.gameView.displayLargePersistentMessage(
												"Waiting for other players to drop 50% of their cards...");
									});
								}
							}
						}
						//////////////////// < AI >///////////////////////
						if (getConnectionHandler().isAI()
								&& checkSameIdAsConnectionHandler(playerForProtocol.getId())) {
							Resources resources = getConnectionHandler().getAiLogic().dropHalfCards();
							requestResourceCardsReturned(new ResourcesReturned(resources));
							GameStart.mainLogger.getLOGGER().fine("AI DROPPED 50%: " + resources);
						}
						/////////////////////////////////////////////////
					} else if (playerForProtocol.getStatus().equals("Raeuber versetzen")
							&& checkSameIdAsConnectionHandler(playerForProtocol.getId())) {
						moveThief(playerForProtocol.getId());
					} else if (playerForProtocol.getStatus().equals("Warten") &&

							checkSameIdAsConnectionHandler(playerForProtocol.getId())) {
						// Display notification
						if (!getConnectionHandler().isSinglePlayerAI())
							Platform.runLater(() -> GameStart.gameView.displayNotificationMessage("Waiting"));

						// // Synchronize with resources with server
						// for (Player playerInSiedler :
						// GameStart.siedlerVonCatan.getPlayers()) {
						// if (playerInSiedler.getPlayerID().intValue() ==
						// playerForProtocol.getId().intValue()) {
						// playerInSiedler.overwriteResources(playerForProtocol.getResources());
						// break;
						// }
						// }
					} else if (playerForProtocol.getStatus().equals("Handeln oder Bauen")
							&& checkSameIdAsConnectionHandler(playerForProtocol.getId())) {
						if (!getConnectionHandler().isSinglePlayerAI())
							Platform.runLater(() -> {
								InGameController.endTheTurnButton.setDisable(false);
								GameStart.gameView.displayNotificationMessage("Trade & Build");
							});
						/////////////// < AI >////////////////////////
						if (getConnectionHandler().isAI()) {
							///////////////////////////// PLAY DEVELOPMENT
							///////////////////////////// CARD////////////////////////////
							getConnectionHandler().getAiLogic().tryPlayingDevelopmentCard(this);
							tryToBuildOrRequestEndTurn(playerForProtocol.getId());
						}
						/////////////// < AI >////////////////////////
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			GameStart.network.getConnectionHandler().disconnectFromServer();
			if (!getConnectionHandler().isSinglePlayerAI()) {
				Platform.runLater(() -> {
					new ErrorPopUp("Game aborted!", "Connection to server corrputed. Please try again.", true);
				});
			}
		}
	}

	/**
	 * (Allowes to) move the thief
	 */
	private void moveThief(Integer id) {
		// Update server chat if not singleplayer Ai
		if (!getConnectionHandler().isSinglePlayerAI()) {
			final String stringId = getConnectionHandler().getPlayerColor(id).toString();
			Platform.runLater(() -> {
				GameStart.gameView.updateInGameServerWindow(stringId + " to move thief");
				// remove removeLargePersistentMessage
				GameStart.gameView.removeLargePersistentMessage();
			});

			if (checkSameIdAsConnectionHandler(id)) {
				Platform.runLater(() -> {
					// Display notification
					GameStart.gameView.displayNotificationMessage("Move thief");
					// Change background of the game
					if (!GameStart.siedlerVonCatan.findPlayerByID(getConnectionHandler().getPlayerId())
							.isMoveThiefDueToKnightCard())
						GameStart.gameView.changeBackgroundToThief();
					else
						GameStart.gameView.changeBackgroundToKnight();
				});
			}
		}
		//////////////////// < AI >///////////////////////
		if (getConnectionHandler().isAI() && checkSameIdAsConnectionHandler(id)) {
			GameStart.mainLogger.getLOGGER().fine("Change Thief");
			Object[] objects = getConnectionHandler().getAiLogic().handleThiefAction(false);
			HexagonField hexagonField = (HexagonField) objects[0];
			Player playerTarget = (Player) objects[1];
			requestMoveThief(hexagonField.getPosition().castTo2D(), playerTarget.getPlayerID());
		}
		/////////////////////////////////////////////////
	}

	/**
	 * Waits and send a request to end the turn (useful for AIs). SERVER IS NOW
	 * DOING THIS JOB TO PREVENT LATENCY IN GUI
	 */
	private void waitAndRequestEndTurn() {
		try {
			// Wait some time before ending a turn
			if (connectionHandler.isAI())
				Thread.sleep(1500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		requestTurnEnd();
	}

	/**
	 * @param playerID
	 *            The id of the player
	 * @return True, if ID match
	 */
	private boolean checkSameIdAsConnectionHandler(Integer playerID) {
		boolean equal = playerID != null && getConnectionHandler().getPlayerId() != null
				&& playerID.intValue() == getConnectionHandler().getPlayerId().intValue();
		return equal;
	}
	// 7.2

	/**
	 * Shows the result of the dice roll in the server chat-window and
	 * visualizes it in the gameview
	 *
	 * @param diceThrow
	 */
	public void receiveDiceThrow(DiceThrow diceThrow) {
		PlayerTeam teamColor = getConnectionHandler().getPlayerColor(diceThrow.getPlayer());
		int[] diceThrows = diceThrow.getDiceThrows();
		Platform.runLater(() -> {
			if (!getConnectionHandler().isSinglePlayerAI())
				GameStart.gameView.showDiceThrow(diceThrows);
		});
		String messageUpdate = "\n" + teamColor.toString() + " has rolled a " + diceThrows[0] + "," + diceThrows[1];
		// Display if not singleplayer
		if (!getConnectionHandler().isSinglePlayerAI())
			GameStart.gameView.updateInGameServerWindow(messageUpdate);
	}

	// 7.3 earnings hat playerid und rohstoffe als attribute
	public synchronized void receiveEarnings(Earnings earnings) {
		Platform.runLater(() -> GameStart.gameView.removeMenus());
		// Do logic...
		if (GameStart.siedlerVonCatan.isSinglePlayer() && checkSameIdAsConnectionHandler(earnings.getPlayer())
				&& earnings.getResources().getTotalResources() > 0) {
			addResources(earnings);
		} else if (!GameStart.siedlerVonCatan.isSinglePlayer() && (!checkSameIdAsConnectionHandler(earnings.getPlayer())
				|| earnings.getResources().getTotalResources() > 0)) {
			addResources(earnings);
		}
	}

	/**
	 * Adds resources to ad player.
	 */
	private void addResources(Earnings earnings) {
		try {
			HashMap<ResourceType, Integer> newResources = new HashMap<>();
			Resources resources = earnings.getResources();

			newResources.put(ResourceType.WOOL, resources.getWool());
			newResources.put(ResourceType.GRAIN, resources.getGrain());
			newResources.put(ResourceType.WOOD, resources.getWood());
			newResources.put(ResourceType.STONE, resources.getStone());
			newResources.put(ResourceType.LOAM, resources.getLoam());

			newResources.put(ResourceType.HIDDEN,
					resources.getTotalResources() > 0 ? resources.getTotalResources() : resources.getHidden());

			// Adds the Resources to the player
			for (Player player : GameStart.siedlerVonCatan.getPlayers()) {
				if (player.getPlayerID() == earnings.getPlayer()) {
					player.addResources(newResources);
					GameStart.mainLogger.getLOGGER().fine("Added resources to " + player.getPlayerID());
				}
			}

			// Display earning on screen
			if (checkSameIdAsConnectionHandler(earnings.getPlayer())) {
				// Show it on screen
				Platform.runLater(() -> GameStart.gameView.updateResourceCardView());
			}
			GameStart.mainLogger.getLOGGER()
					.fine(GameStart.siedlerVonCatan.findPlayerByID(earnings.getPlayer()).toString());
			GameStart.mainLogger.getLOGGER()
					.fine(GameStart.siedlerVonCatan.findPlayerByID(earnings.getPlayer()).getTeam().toString());

			final String color = GameStart.siedlerVonCatan.findPlayerByID(earnings.getPlayer()).getTeam().toString();

			final String count = !checkSameIdAsConnectionHandler(earnings.getPlayer()) ? resources.getHidden() + ""
					: resources.getTotalResources() + "";
			if (count.equals("0"))
				return;
			GameStart.mainLogger.getLOGGER().fine(color + " " + count);

			Platform.runLater(
					() -> GameStart.gameView.updateInGameServerWindow(color + " received " + count + " card(s)."));

			Platform.runLater(() -> GameStart.gameView.fadeAwayAvatarDropDownMenu());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 8.4
	public synchronized void receiveCosts(Costs costs) {
		Platform.runLater(() -> GameStart.gameView.removeMenus());
		// Display earning on screen...
		Resources resources = costs.getResources();
		if (!connectionHandler.isSinglePlayerAI()) {
			final String playerTeam = GameStart.siedlerVonCatan.findPlayerByID(costs.getPlayer()).getTeam().toString();
			final String cards = (Math.max(resources.getHidden(), resources.getTotalResources())) + "";
			if (checkSameIdAsConnectionHandler(costs.getPlayer())) {
				// Show it on screen
				Platform.runLater(() -> GameStart.gameView.updateResourceCardView());
			}
			if (!cards.equals("0")) {
				Platform.runLater(() -> GameStart.gameView
						.updateInGameServerWindow(playerTeam + " dropped " + cards + " card(s)."));
			}
			Platform.runLater(() -> GameStart.gameView.fadeAwayAvatarDropDownMenu());
		}
		// Do logic...
		if (GameStart.siedlerVonCatan.isSinglePlayer() && checkSameIdAsConnectionHandler(costs.getPlayer())
				&& costs.getResources().getTotalResources() > 0) {
			subtractCosts(costs);
		} else if (!GameStart.siedlerVonCatan.isSinglePlayer() && (!checkSameIdAsConnectionHandler(costs.getPlayer())
				|| costs.getResources().getTotalResources() > 0)) {
			subtractCosts(costs);
		}
		if (getConnectionHandler().isAI() && GameStart.siedlerVonCatan
				.findPlayerByID(getConnectionHandler().getPlayerId()).getStatus().equals("Handeln oder Bauen")) {
			tryToBuildOrRequestEndTurn(costs.getPlayer());
		}
	}

	/**
	 * Subtracts costs from a player's resources
	 */
	private void subtractCosts(Costs costs) {
		HashMap<ResourceType, Integer> newResources = new HashMap<>();
		Resources resources = costs.getResources();

		newResources.put(ResourceType.WOOL, resources.getWool());
		newResources.put(ResourceType.GRAIN, resources.getGrain());
		newResources.put(ResourceType.WOOD, resources.getWood());
		newResources.put(ResourceType.STONE, resources.getStone());
		newResources.put(ResourceType.LOAM, resources.getLoam());

		newResources.put(ResourceType.HIDDEN,
				resources.getTotalResources() > 0 ? resources.getTotalResources() : resources.getHidden());

		// Subtracts the Resources to the player
		GameStart.siedlerVonCatan.findPlayerByID(costs.getPlayer()).subtractResources(newResources);
		GameStart.mainLogger.getLOGGER().fine("Subtracted resources from " + costs.getPlayer());

		// Show it on screen
		Platform.runLater(() -> GameStart.gameView.updateResourceCardView());
	}

	// 8.5
	public void receiveThiefMoved(ThiefMoved thiefMoved) {
		GameStart.mainLogger.getLOGGER().fine("CHANGE THIEF POSITION!");
		// convert letter location to vector
		final Vector2D<Integer> v = WorldTranslation.LETTER_TO_VECTOR.get(thiefMoved.getLocation());
		// Check if thief not already there
		if (GameStart.siedlerVonCatan.getThief().getThiefPosition() != v) {
			// Set thiefs location in package gameobjects.Elements
			GameStart.siedlerVonCatan.getThief().setThiefLocation(v);
		}
		if (!getConnectionHandler().isSinglePlayerAI()) {
			// Here the window shall display thiefs new location on the screen
			Platform.runLater(() -> {
				// Remove notification message
				GameStart.gameView.removeLargePersistentMessage();
				// Change background of the game
				GameStart.gameView.changeBackgroundToDefault();
				GameStart.gameView.drawThief(v);
			});

			// The player who placed the thief is shown in the server chat
			PlayerTeam teamColor = null;
			PlayerTeam teamColorTarget = null;
			teamColor = getConnectionHandler().getPlayerColor(thiefMoved.getPlayer());
			// Display information on the server chat if not singleplayer ai
			GameStart.gameView
					.updateInGameServerWindow(teamColor.toString() + " has moved the Thief to a new location.");
			// Show in the server chat if a card was stolen.
			if (thiefMoved.getTarget() != null && thiefMoved.getTarget() != -1 && GameStart.siedlerVonCatan
					.findPlayerByID(thiefMoved.getTarget()).getResources().get(ResourceType.HIDDEN) != 0) {
				teamColorTarget = getConnectionHandler().getPlayerColor(thiefMoved.getTarget());
				GameStart.gameView.updateInGameServerWindow(
						teamColor.toString() + " has stolen a card from " + teamColorTarget.toString() + ".");
			}
		}
		// TODO: Check if AI
		// ..
	}

	// 7.4 building als attribut
	public void receiveBuildingEvent(BuildingEvent event) {
		Platform.runLater(() -> GameStart.gameView.removeMenus());
		try {
			Building building = event.getBuilding();
			BuildingType buildingType = tools.WorldTranslation
					.getServerBuildingTypeToClientBuildingType(building.getType());
			String serverFieldCoordinates = building.getLocation();
			// Get (set) player color
			// Change teamColor to the corresponding team color
			final PlayerTeam teamColor = getConnectionHandler().getPlayerColor(building.getOwner());
			// Player player =
			// GameStart.siedlerVonCatan.findPlayerByID(getConnectionHandler().getPlayerId());
			switch (buildingType) {
			case CASTLE:
				Vector3D<Integer> vecPosCastle = tools.WorldTranslation
						.getWayPointFromSurroundingFields(serverFieldCoordinates);
				WayPoint wpCastle = (WayPoint) GameStart.gameView
						.getActivGameMatrixEntries()[vecPosCastle.x][vecPosCastle.y][vecPosCastle.z];
				wpCastle.getSettlement().upgradeToCastle();
				if (!connectionHandler.isSinglePlayerAI())
					Platform.runLater(() -> {
						GameStart.gameView.changeButtonImage(
								GameStart.gameView.getButtonsOfTheWorldMatrix().get(wpCastle), buildingType, teamColor);
						// Display information on the server chat if not
						// singleplayer ai
						GameStart.gameView.updateInGameServerWindow(teamColor.toString() + " has built a castle.");
					});

				break;
			case STREET:
				Vector3D<Integer>[] streetWaypoints = tools.WorldTranslation
						.getWaypointsOfStreet(serverFieldCoordinates);
				// Get street wayPoints
				WayPoint wpStreetA = (WayPoint) GameStart.gameView
						.getActivGameMatrixEntries()[streetWaypoints[0].x][streetWaypoints[0].y][streetWaypoints[0].z];
				WayPoint wpStreetB = (WayPoint) GameStart.gameView
						.getActivGameMatrixEntries()[streetWaypoints[1].x][streetWaypoints[1].y][streetWaypoints[1].z];
				// Find street
				Street street = GameStart.gameView.wayPointsToStreet.get(new Vector2D<WayPoint>(wpStreetA, wpStreetB));
				// Build street
				street.buildStreet(building.getOwner());
				if (!connectionHandler.isSinglePlayerAI())
					Platform.runLater(() -> {
						// Set last selected street to null
						GameStart.gameView.resetHighlightedStreet();
						GameStart.gameView.changeButtonImage(GameStart.gameView.getButtonsOfTheStreets().get(street),
								buildingType, teamColor);
						// Display information on the server chat
						GameStart.gameView.updateInGameServerWindow(teamColor.toString() + " has built a street.");
					});

				boolean activateWayPoints = checkSameIdAsConnectionHandler(building.getOwner())
						&& !getConnectionHandler().isSinglePlayerAI();

				street.getConnectionPoints()[0].getStreetConnectedWaypoints().add(street.getConnectionPoints()[1]);
				street.getConnectionPoints()[1].getStreetConnectedWaypoints().add(street.getConnectionPoints()[0]);
				markSurroundingWaypointsAsOccupied(street.getConnectionPoints()[0], activateWayPoints, null);
				markSurroundingWaypointsAsOccupied(street.getConnectionPoints()[1], activateWayPoints, street);
				if (!connectionHandler.isSinglePlayerAI()) {
					if (!street.getConnectionPoints()[0].getSettlement().isOccupied())
						Platform.runLater(() -> GameStart.gameView
								.activateOrDeactivateWayPointButton(street.getConnectionPoints()[0], true));
					if (!street.getConnectionPoints()[1].getSettlement().isOccupied())
						Platform.runLater(() -> GameStart.gameView
								.activateOrDeactivateWayPointButton(street.getConnectionPoints()[1], true));
				}

				if (checkSameIdAsConnectionHandler(event.getBuilding().getOwner())) {
					///////////////// <<< AI >>> ///////////////
					if (getConnectionHandler().isAI()) {
						// Add waypoint to the ai list (to improve performance,
						// but increases
						// redundancy)
						getConnectionHandler().getAiLogic().getStreets().add(street);
						// in the initial phase the ai should not try to build
						if (GameStart.siedlerVonCatan.isInitialStartPhase())
							;
						// waitAndRequestEndTurn();

					} // else // End turn if not AI and in initial phase
						// if (GameStart.siedlerVonCatan.isInitialStartPhase())
						// End turn
						// requestTurnEnd();
						////////////////////////////////////////////
				}
				break;
			case VILLAGE:
				Vector3D<Integer> vecPosVillage = tools.WorldTranslation
						.getWayPointFromSurroundingFields(serverFieldCoordinates);
				WayPoint wpVillage = (WayPoint) GameStart.gameView
						.getActivGameMatrixEntries()[vecPosVillage.x][vecPosVillage.y][vecPosVillage.z];
				wpVillage.getSettlement().buildVillage(building.getOwner());
				if (!getConnectionHandler().isSinglePlayerAI()) {
					Platform.runLater(() -> {
						GameStart.gameView.changeButtonImage(
								GameStart.gameView.getButtonsOfTheWorldMatrix().get(wpVillage), buildingType,
								teamColor);
						// Display nformation on the server chat if not
						// singleplayer ai
						GameStart.gameView.updateInGameServerWindow(teamColor.toString() + " has build a village.");
					});
				}
				boolean activateStreets = checkSameIdAsConnectionHandler(building.getOwner())
						&& !getConnectionHandler().isSinglePlayerAI();
				markSurroundingWaypointsAsOccupied(wpVillage, activateStreets, null);

				if (checkSameIdAsConnectionHandler(event.getBuilding().getOwner())) {
					///////////////// <<< AI >>> ///////////////
					if (getConnectionHandler().isAI()) {
						// Add waypoint to the ai list (to improve performance,
						// but increases
						// redundancy)
						getConnectionHandler().getAiLogic().getSettlementWayPoints().add(wpVillage);
					}
					////////////////////////////////////////////
					break;
				}
			default:
				break;
			}
			if (!connectionHandler.isSinglePlayerAI())
				Platform.runLater(() -> GameStart.gameView.fadeAwayAvatarDropDownMenu());

		} catch (Exception e) {
			GameStart.mainLogger.getLOGGER().fine("ERROR IN CLIENT PROTOCOL: #001");
			e.printStackTrace();
		}
	}

	/**
	 * Marks immediate surround neighour waypoints as occupied (<=> non
	 * buildable). Activates or deactivtes buttons accordingly.
	 *
	 * @param wp
	 *            The WayPoint we base on.
	 * @param activateStreets
	 *            Do we want to activate nearby streets?
	 */
	private void markSurroundingWaypointsAsOccupied(WayPoint wp, boolean activateStreets, Street streetToActivate) {
		for (WayPoint neighbour : wp.getWayPointNeighbours()) {
			if (activateStreets) {
				Street street = GameStart.gameView.wayPointsToStreet.get(new Vector2D<>(wp, neighbour));
				if (street != null)
					Platform.runLater(() -> GameStart.gameView.activateStreetButton(street));
			}
			if (wp.getSettlement().getBuildingType() == BuildingType.VILLAGE) {
				wp.getSettlement().setOccupied(true);
				neighbour.getSettlement().setOccupied(true);
				Platform.runLater(() -> GameStart.gameView.activateOrDeactivateWayPointButton(neighbour, false));
			}
			if (streetToActivate != null)
				Platform.runLater(() -> GameStart.gameView.activateStreetButton(streetToActivate));
		}
	}

	// 8.1
	public void requestDiceThrow() {
		getConnectionHandler().sendObject(new ThrowDiceRequest());
	}

	// 9.2
	public void requestResourceCardsReturned(ResourcesReturned resourcesReturned) {
		GameStart.mainLogger.getLOGGER().fine("Client sent request to give back cards...");
		getConnectionHandler().sendObject(resourcesReturned);
		// subtractCosts(new Costs(connectionHandler.getPlayerId(),
		// resourcesReturned.getResources()));
		Platform.runLater(() -> GameStart.gameView.updateResourceCardView());
	}

	// 9.3
	public void requestMoveThief(Vector2D<Integer> newThiefPosition, Integer playerTargetID) {
		if (!GameStart.siedlerVonCatan.findPlayerByID(getConnectionHandler().getPlayerId())
				.isMoveThiefDueToKnightCard()) {
			GameStart.mainLogger.getLOGGER().fine("Requesting Thief Move");
			MoveThief moveThief = new MoveThief(WorldTranslation.getPositionToLetter(newThiefPosition), playerTargetID);
			getConnectionHandler().sendObject(moveThief);
		} else {
			GameStart.mainLogger.getLOGGER().fine("Requesting Knight card thief Move");
			GameStart.siedlerVonCatan.findPlayerByID(getConnectionHandler().getPlayerId())
					.setMoveThiefDueToKnightCard(false);
			PlayKnightCard playKnightCard = new PlayKnightCard(WorldTranslation.getPositionToLetter(newThiefPosition),
					playerTargetID);
			getConnectionHandler().sendObject(playKnightCard);
		}
	}

	// 8.2
	// We already have a BuildingType inside buildingPrototype, but we can not
	// modify it until the server accepts our request.
	// That's why we must explicitly mention the requestedType.
	public void requestBuilding(BuildingPrototype buildingPrototype, BuildingType requestedType) {

		String typeString = "";
		String location = "";

		switch (requestedType) {

		case NONE:
			break;
		case STREET:
			typeString = "Strasse";
			location = buildingPrototype.getTranslatedCoordinates();
			break;
		case VILLAGE:
			typeString = "Dorf";
			location = buildingPrototype.getTranslatedCoordinates();
			break;
		case CASTLE:
			typeString = "Stadt";
			location = buildingPrototype.getTranslatedCoordinates();
			break;
		}

		Building building = new Building(null, typeString, location);
		getConnectionHandler().sendObject(building); // hier aber id nicht
		// setzen
		// (wird nicht mitgesendet)
	}

	// 8.3
	public void requestTurnEnd() {
		Player player = GameStart.siedlerVonCatan.findPlayerByID(getConnectionHandler().getPlayerId());

		if (getConnectionHandler() == null || player == null)
			return;
		player.setPlayedDevelopmentCard(false);
		// Reset development cards bought this round
		player.resetDevelopmentCardsBoughtThisRound();
		// Inform server
		getConnectionHandler().sendObject(new TurnEnd());
		if (!getConnectionHandler().isSinglePlayerAI())
			Platform.runLater(() -> GameStart.gameView.onTurnEnd());
	}

	// 9.5
	public void requestMaritimeTrade(MaritimeTrade maritimeTrade) {
		GameStart.mainLogger.getLOGGER().fine("Request: " + maritimeTrade.toString());
		getConnectionHandler().sendObject(maritimeTrade);
	}

	// 9.7
	public void receiveBoughtDevelopmentCard(DevelopmentCardBought developmentCardBought) {
		String cardType = developmentCardBought.getDevelopmentCard();
		Player player = GameStart.siedlerVonCatan.findPlayerByID(developmentCardBought.getPlayerId());
		final String color = player.getTeam().toString();
		final EvolutionType evolutiontype;
		if (developmentCardBought.getPlayerId() == getConnectionHandler().getPlayerId()) {
			if (cardType.equals("Ritter"))
				player.addSingleEvolutionCard(evolutiontype = EvolutionType.KNIGHT);
			else if (cardType.equals("Monopol"))
				player.addSingleEvolutionCard(evolutiontype = EvolutionType.MONOPOLY);
			else if (cardType.equals("Strassenbau"))
				player.addSingleEvolutionCard(evolutiontype = EvolutionType.ROAD_BUILDING);
			else if (cardType.equals("Siegpunkt"))
				player.addSingleEvolutionCard(evolutiontype = EvolutionType.VICTORY_POINT);
			else if (cardType.equals("Erfindung"))
				player.addSingleEvolutionCard(evolutiontype = EvolutionType.YEAR_OF_PLENTY);
			else
				evolutiontype = null;
		} else
			evolutiontype = null;

		// For multiplayer
		if (!(developmentCardBought.getPlayerId() == getConnectionHandler().getPlayerId())
				&& !GameStart.siedlerVonCatan.isSinglePlayer()) {
			player.addSingleEvolutionCard(EvolutionType.HIDDEN);
		}
		if (getConnectionHandler().isSinglePlayerAI())
			return;

		if (evolutiontype != null) {
			// Display earning on screen
			if (checkSameIdAsConnectionHandler(developmentCardBought.getPlayerId())) {
				// Show it on screen
				Platform.runLater(() -> {
					GameStart.gameView.drawReceivedEvolutionCard(evolutiontype.getFilepath() + ".png");
					GameStart.gameView.updateDevelopmentCardView();
				});
			}
		}
		Platform.runLater(() -> {
			GameStart.gameView.updateInGameServerWindow(color + " received a development card.");
			GameStart.gameView.displayLargeMessage(color + " bought a development card.");
		});
		Platform.runLater(() -> GameStart.gameView.fadeAwayAvatarDropDownMenu());
	}

	// 9.10
	public void receiveLongestRoadInfo(LongestRoad longestRoad) {
		if (getConnectionHandler().isSinglePlayerAI())
			return;
		for (Player previousPlayer : GameStart.siedlerVonCatan.getPlayers()) {
			if (previousPlayer.hasLongestRoad()) {
				previousPlayer.setHasLongestRoad(false);
			}
		}
		if (longestRoad.getPlayerId() == -1)
			Platform.runLater(() -> GameStart.gameView.displayLargeMessage("No one has the longest road anymore."));

		Player player = GameStart.siedlerVonCatan.findPlayerByID(longestRoad.getPlayerId());
		if (player == null)
			return;

		player.setHasLongestRoad(true);
		player.setVictoryPoints(player.getVictoryPoints() + 2);
		final String color = longestRoad.getClass() == null ? "Noone"
				: GameStart.siedlerVonCatan.findPlayerByID(longestRoad.getPlayerId()).getTeam().toString();
		// Display notification
		Platform.runLater(() -> GameStart.gameView.displayLargeMessage(color + " has the longest road now!"));
	}

	// 9.10
	public void receiveLargestArmyInfo(LargestArmy largestArmy) {
		if (getConnectionHandler().isSinglePlayerAI())
			return;
		for (Player previousPlayer : GameStart.siedlerVonCatan.getPlayers()) {
			if (previousPlayer.hasLargestArmy()) {
				previousPlayer.setHasLargestArmy(false);
				previousPlayer.setVictoryPoints(previousPlayer.getVictoryPoints() - 2);
			}
		}
		Player player = GameStart.siedlerVonCatan.findPlayerByID(largestArmy.getPlayerId());
		if (player == null)
			return;
		player.setHasLargestArmy(true);
		player.setVictoryPoints(player.getVictoryPoints() + 2);
		final String color = player.getTeam().toString();
		// Display notification
		Platform.runLater(() -> GameStart.gameView.displayLargeMessage(color + " has the largest army now!"));
	}

	// 10.1
	public void requestDomesticTrade(DomesticTradeOffer domesticTradeOffer) {
		GameStart.mainLogger.getLOGGER().fine("Request: " + domesticTradeOffer.toString());
		getConnectionHandler().sendObject(domesticTradeOffer);
	}

	// 10.1
	public void receiveTradeOffer(ReceivedTradeOffer receivedTradeOffer) {
		if (checkSameIdAsConnectionHandler(receivedTradeOffer.getPlayer()))
			return;

		if (getConnectionHandler().isAI()) {
			// Always decline trades
			// abandonDomesticTrade(receivedTradeOffer.getTradeId());
			getConnectionHandler().sendObject(new PlayerReadyForDomesticTrade(receivedTradeOffer.getTradeId(), false));
			Platform.runLater(() -> GameStart.gameView.displayLargeMessage("Trade received and AI declined..."));
		} else {
			Platform.runLater(() -> GameStart.gameView.drawTradeNotification(receivedTradeOffer));
		}
	}

	// 10.2
	public void acceptDomesticTrade(int tradeID) {
		getConnectionHandler().sendObject(new PlayerReadyForDomesticTrade(tradeID));
		Platform.runLater(() -> GameStart.gameView.displayRejectTradeOption(tradeID));
	}

	// 10.2

	/**
	 * When someone accepts your trade (mutliple accepts possible)
	 *
	 * @param playersReadyForTrade
	 */
	public void receiveAcceptedTrade(PlayerWhoAcceptedTrade playersReadyForTrade) {
		if (!playersReadyForTrade.isAccepted()) {
			if (!GameStart.siedlerVonCatan.playersWhoRejectedTradeRightAway
					.contains((Integer) playersReadyForTrade.getPlayerID())
					&& !GameStart.siedlerVonCatan.findPlayerByID(connectionHandler.getPlayerId()).getStatus()
							.equals("Warten")) {
				GameStart.siedlerVonCatan.playersWhoRejectedTradeRightAway.add(playersReadyForTrade.getPlayerID());
			}
			if (GameStart.siedlerVonCatan.getPlayers().size()
					- 1 == GameStart.siedlerVonCatan.playersWhoRejectedTradeRightAway.size()
					&& !GameStart.siedlerVonCatan.isSinglePlayer()) {
				Platform.runLater(() -> GameStart.gameView.displayLargeMessage("The trade was not accepted by anyone"));
				GameStart.siedlerVonCatan.playersWhoRejectedTradeRightAway.clear();
			}
			return;
		}
		GameStart.mainLogger.getLOGGER().fine("Received trade ACCEPT!");
		// Find yourself
		Player player = GameStart.siedlerVonCatan.findPlayerByID(getConnectionHandler().getPlayerId());
		Integer tradeID = playersReadyForTrade.getTradeId();
		Integer playerID = playersReadyForTrade.getPlayerID();
		// Are we waiting for trade acceptors?
		if (!player.isWaitingForRequests() || !player.getStatus().equals("Handeln oder Bauen"))
			return;
		// If we are here we are fine...

		// Check if not singleplayer AI
		if (!getConnectionHandler().isSinglePlayerAI()) {
			// Display small pop up notification
			Platform.runLater(() -> GameStart.gameView.displayTradeCandidateMessage(tradeID, playerID));
		}
	}

	// 10.3

	/**
	 * Used to send an execute trade to the server.
	 *
	 * @param tradeID
	 * @param playerID
	 */
	public void completeDomesticTrade(Integer tradeID, Integer playerID) {
		getConnectionHandler().sendObject(new CompleteDomesticTrade(tradeID, playerID));
	}

	// 10.3
	public void receiveTradeFinished(TradeFinished tradeFinished) {
		// Display it in the server chat (if not singleplayerAI)
		if (!getConnectionHandler().isSinglePlayerAI()) {
			String playerA = GameStart.siedlerVonCatan.findPlayerByID(tradeFinished.getPlayer()).getTeam().toString();
			String playerB = GameStart.siedlerVonCatan.findPlayerByID(tradeFinished.getOtherPlayer()).getTeam()
					.toString();
			Platform.runLater(() -> {
				GameStart.gameView.updateInGameServerWindow(playerA + " has traded with " + playerB + ".");
				// Remove option to decline trade
				if (GameStart.gameView.abortTradeButton != null
						&& GameStart.gameView.getLayout().getChildren().contains(GameStart.gameView.abortTradeButton))
					Platform.runLater(() -> GameStart.gameView.getLayout().getChildren()
							.remove(GameStart.gameView.abortTradeButton));
			});

			if (tradeFinished.getOtherPlayer() == getConnectionHandler().getPlayerId()
					|| tradeFinished.getPlayer() == getConnectionHandler().getPlayerId())
				Platform.runLater(() -> GameStart.gameView.displayLargeMessage("Trade successful"));
		}
		GameStart.siedlerVonCatan.playersWhoRejectedTrade.clear();
		GameStart.siedlerVonCatan.playersWhoRejectedTradeRightAway.clear();
	}

	// 10.4
	public void abandonDomesticTrade(int tradeID) {
		getConnectionHandler().sendObject(new AbandonDomesticTrade(tradeID));
	}

	// 10.4
	public void declineDomesticTrade(int tradeID) {
		getConnectionHandler().sendObject(new PlayerReadyForDomesticTrade(tradeID, false));
		Platform.runLater(() -> GameStart.gameView.displayLargeErrorMessage("Trade declined"));
	}

	// 10.4
	public void receiveAbandonedTrade(AbandonedTrade abandonedTrade) {
		if (GameStart.siedlerVonCatan.findPlayerByID(abandonedTrade.getPlayer()).getStatus()
				.equals("Handeln oder Bauen")) {
			// Remove option to decline trade
			if (GameStart.gameView.abortTradeButton != null
					&& GameStart.gameView.getLayout().getChildren().contains(GameStart.gameView.abortTradeButton))
				Platform.runLater(
						() -> GameStart.gameView.getLayout().getChildren().remove(GameStart.gameView.abortTradeButton));
		} else if (checkSameIdAsConnectionHandler(abandonedTrade.getPlayer())) {
			Platform.runLater(() -> GameStart.gameView.displayLargeErrorMessage("Trade request canceled"));
			return;
		}
		if (GameStart.siedlerVonCatan.playersWhoRejectedTradeRightAway.contains((Integer) abandonedTrade.getPlayer())) {
			GameStart.siedlerVonCatan.playersWhoRejectedTradeRightAway.remove((Integer) abandonedTrade.getPlayer());
		}
		if (!GameStart.siedlerVonCatan.playersWhoRejectedTrade.contains((Integer) abandonedTrade.getPlayer())) {
			GameStart.siedlerVonCatan.playersWhoRejectedTrade.add(abandonedTrade.getPlayer());
			Platform.runLater(() -> GameStart.gameView.removeCandidate(abandonedTrade.getPlayer()));
		}
		if (GameStart.siedlerVonCatan.getPlayers().size() - 1 == GameStart.siedlerVonCatan.playersWhoRejectedTrade
				.size() && !GameStart.siedlerVonCatan.isSinglePlayer()) {
			Platform.runLater(() -> GameStart.gameView.displayLargeMessage("Trade has been canceled..."));
			GameStart.siedlerVonCatan.playersWhoRejectedTrade.clear();
		}
	}

	// 10.5
	public void sendBuyDevelopmentCardRequest() {
		getConnectionHandler().sendObject(new BuyDevelopmentCard());
	}

	// 7.5
	public void receiveGameOver(GameOver gameOver) {
		// Check if game is over (<=> somebody won <=> reaches 10 points or
		// more)
		GameStart.siedlerVonCatan.gameFinished = true;
		if (connectionHandler.isSinglePlayerAI())
			return;
		// Display victory screen if your id
		if (checkSameIdAsConnectionHandler(gameOver.getWinner()))
			Platform.runLater(() -> GameStart.gameView.showVictoryScreen());
		else
			Platform.runLater(() -> GameStart.gameView.showGameOverScreen());
	}

	// 12.1
	// https://www.catan.de/faq/716-entwicklungskarten-ritter-muessen-auch-beim-ausspielen-einer-ritterkarte-alle-spieler-mit

	/**
	 * Actual "send request" implemented in the controller (following
	 * moveThief())
	 */
	public void requestToPlayKnightCard() {
		GameStart.mainLogger.getLOGGER().fine("play knight card");
		Player player = GameStart.siedlerVonCatan.findPlayerByID(getConnectionHandler().getPlayerId());
		player.setMoveThiefDueToKnightCard(true);
		moveThief(player.getPlayerID());
	}

	// 12.1
	public void receiveKnightCardPlayed(PlayKnightCard playedKnightCard) {
		// update evolution card quantity of the player
		Player player = GameStart.siedlerVonCatan.findPlayerByID(playedKnightCard.getPlayer());
		if (player.getPlayerID() == getConnectionHandler().getPlayerId())
			player.removeSingleEvolutionCard(EvolutionType.KNIGHT);
		// For multiplayer
		if ((playedKnightCard.getPlayer() != getConnectionHandler().getPlayerId())
				&& !GameStart.siedlerVonCatan.isSinglePlayer()) {
			player.removeSingleEvolutionCard(EvolutionType.HIDDEN);
		}
		// Display in game notification
		if (!getConnectionHandler().isSinglePlayerAI()) {
			if (getConnectionHandler().getPlayerId() == playedKnightCard.getPlayer())
				player.setPlayedDevelopmentCard(true);
			player.setNumberOfKnights(player.getNumberOfKnights() + 1);
			// Create a virtual thief
			ThiefMoved thiefMoved = new ThiefMoved(playedKnightCard.getPlayer(), playedKnightCard.getLocation(),
					playedKnightCard.getTarget());
			receiveThiefMoved(thiefMoved);
			final String color = player.getTeam().toString();
			Platform.runLater(() -> {
				GameStart.gameView.displayLargeMessage(color + " has used a knight card!");
				// Update
				GameStart.gameView.updateDevelopmentCardView();
			});
		}
		// If AI, end Turn
		////////////////////////// AI ///////////////////////////
		if (connectionHandler.isAI() && checkSameIdAsConnectionHandler(player.getPlayerID())) {
			tryToBuildOrRequestEndTurn(player.getPlayerID());
		}
		////////////////////////////////////////////////////////
	}

	// 12.2
	public void requestToPlayRoadBuildingCard(Street[] streetsChosenForRoadBuildingCard) {
		if (streetsChosenForRoadBuildingCard == null)
			return;

		BuildingPrototype buildingPrototype1 = streetsChosenForRoadBuildingCard[0];
		String location1 = buildingPrototype1.getTranslatedCoordinates();
		PlayRoadBuildingCard playRoadBuildingCard;
		if (streetsChosenForRoadBuildingCard[1] != null) {
			BuildingPrototype buildingPrototype2 = streetsChosenForRoadBuildingCard[1];
			String location2 = buildingPrototype2.getTranslatedCoordinates();
			playRoadBuildingCard = new PlayRoadBuildingCard(location1, location2);
		} else
			playRoadBuildingCard = new PlayRoadBuildingCard(location1);
		getConnectionHandler().sendObject(playRoadBuildingCard);
	}

	// 12.2
	public void receiveRoadBuildingCardPlayed(PlayRoadBuildingCard playedRoadBuildingCard) {
		// update evolution card quantity of the player
		Player player = GameStart.siedlerVonCatan.findPlayerByID(playedRoadBuildingCard.getPlayerId());
		// For multiplayer
		if ((playedRoadBuildingCard.getPlayerId() != getConnectionHandler().getPlayerId())
				&& !GameStart.siedlerVonCatan.isSinglePlayer()) {
			player.removeSingleEvolutionCard(EvolutionType.HIDDEN);
		}
		if (!connectionHandler.isSinglePlayerAI()) {
			Platform.runLater(
					() -> GameStart.gameView.displayLargeMessage(player.getTeam() + " played a road building card!"));
			if (checkSameIdAsConnectionHandler(player.getPlayerID())) {
				player.setPlayedDevelopmentCard(true);
				Platform.runLater(() -> GameStart.gameView.updateDevelopmentCardView());
			}
		}
		if (player.getPlayerID() == getConnectionHandler().getPlayerId())
			player.removeSingleEvolutionCard(EvolutionType.ROAD_BUILDING);
	}

	// 12.3
	public void requestToPlayMonopolyCard(ResourceType type) {
		GameStart.mainLogger.getLOGGER().fine("play monopoly card");
		String resource = "";
		switch (type) {
		case GRAIN:
			resource = "Getreide";
			break;
		case LOAM:
			resource = "Lehm";
			break;
		case STONE:
			resource = "Erz";
			break;
		case WOOD:
			resource = "Holz";
			break;
		case WOOL:
			resource = "Wolle";
			break;
		default:
			break;
		}
		Monopoly monopolyCard = new Monopoly(resource);

		getConnectionHandler().sendObject(monopolyCard);
	}

	// 12.3
	public void receiveMonopolyCardPlayed(Monopoly playedMonopolyCard) {
		// update evolution card quantity of the player
		Player player = GameStart.siedlerVonCatan.findPlayerByID(playedMonopolyCard.getPlayerId());
		if (checkSameIdAsConnectionHandler(player.getPlayerID()))
			player.removeSingleEvolutionCard(EvolutionType.MONOPOLY);
		// For multiplayer
		if ((playedMonopolyCard.getPlayerId() != getConnectionHandler().getPlayerId())
				&& !GameStart.siedlerVonCatan.isSinglePlayer()) {
			player.removeSingleEvolutionCard(EvolutionType.HIDDEN);
		}
		if (!getConnectionHandler().isSinglePlayerAI()) {
			Platform.runLater(() -> GameStart.gameView.updateDevelopmentCardView());
			Platform.runLater(
					() -> GameStart.gameView.displayLargeMessage(player.getTeam() + " played a monopoly card!"));
			if (checkSameIdAsConnectionHandler(player.getPlayerID())) {
				player.setPlayedDevelopmentCard(true);
				Platform.runLater(() -> GameStart.gameView.updateDevelopmentCardView());
			}
		}
	}

	// 12.4

	/**
	 * Requesting to play a year of plenty card with the resources firstResource
	 * and secondResource
	 *
	 * @param firstResource
	 * @param secondResource
	 */
	public void requestToPlayYearOfPlentyCard(ResourceType firstResource, ResourceType secondResource) {
		Resources resources = new Resources(0, 0, 0, 0, 0, 0);

		for (int i = 0; i < 2; i++) {
			ResourceType resourceType = (i == 0) ? firstResource : secondResource;
			switch (resourceType) {
			case GRAIN:
				resources.setGrain(resources.getGrain() + 1);
				break;
			case LOAM:
				resources.setLoam(resources.getLoam() + 1);
				break;
			case STONE:
				resources.setStone(resources.getStone() + 1);
				break;
			case WOOD:
				resources.setWood(resources.getWood() + 1);
				break;
			case WOOL:
				resources.setWool(resources.getWool() + 1);
				break;
			default:
				break;
			}
		}
		YearOfPlenty yearOfPlentyCard = new YearOfPlenty(resources);
		getConnectionHandler().sendObject(yearOfPlentyCard);
	}

	// 12.4

	/**
	 * Updates the view and the players' developmentcards
	 *
	 * @param playedYearOfPlentyCard
	 */
	public void receiveYearOfPlentyCardPlayed(YearOfPlenty playedYearOfPlentyCard) {
		// update evolution card quantity of the player
		Player player = GameStart.siedlerVonCatan.findPlayerByID(playedYearOfPlentyCard.getPlayerId());
		if (checkSameIdAsConnectionHandler(player.getPlayerID()))
			player.removeSingleEvolutionCard(EvolutionType.YEAR_OF_PLENTY);
		// For multiplayer
		if ((playedYearOfPlentyCard.getPlayerId() != getConnectionHandler().getPlayerId())
				&& !GameStart.siedlerVonCatan.isSinglePlayer()) {
			player.removeSingleEvolutionCard(EvolutionType.HIDDEN);
		}
		if (!getConnectionHandler().isSinglePlayerAI()) {
			Platform.runLater(() -> GameStart.gameView.updateDevelopmentCardView());
			Platform.runLater(
					() -> GameStart.gameView.displayLargeMessage(player.getTeam() + " played a year of plenty card!"));
			if (checkSameIdAsConnectionHandler(player.getPlayerID())) {
				player.setPlayedDevelopmentCard(true);
				Platform.runLater(() -> GameStart.gameView.updateDevelopmentCardView());
			}
		}
	}

	/**
	 * Tries to build, trade,... or ends the turn if nothing is possible (useful
	 * for AIs)
	 */
	private void tryToBuildOrRequestEndTurn(Integer id) {

		if (!checkSameIdAsConnectionHandler(id))
			return;

		if (!GameStart.siedlerVonCatan.findPlayerByID(getConnectionHandler().getPlayerId()).getStatus()
				.equals("Handeln oder Bauen")) {
			waitAndRequestEndTurn();
		} else {
			///////////////////////////// CASTLE
			///////////////////////////// /////////////////////////////////////
			// Try to trade if there is not enough resources available
			MaritimeTrade tradeRequestForCastle = getConnectionHandler().getAiLogic().tradeCardsWithBankForCastle();
			if (tradeRequestForCastle != null) {
				requestMaritimeTrade(tradeRequestForCastle);
			}
			// Try to build another settlement first
			WayPoint wayPointToBuildCastle = getConnectionHandler().getAiLogic().getWayPointToBuildCastle();
			if (wayPointToBuildCastle != null) {
				GameStart.mainLogger.getLOGGER()
						.fine("AI wants to build a castle at: " + wayPointToBuildCastle.toString());
				// Request to build a village
				Settlement settlement = new Settlement(wayPointToBuildCastle);
				requestBuilding(settlement, BuildingType.CASTLE);
			} else {
				///////////////////////////// VILLAGE
				///////////////////////////// /////////////////////////////////////
				// Try to trade if there is not enough resources available
				MaritimeTrade tradeRequestForVillage = getConnectionHandler().getAiLogic()
						.tradeCardsWithBankForVillage();
				if (tradeRequestForVillage != null) {
					requestMaritimeTrade(tradeRequestForVillage);
				}
				// Try to build another settlement first
				WayPoint wayPointToBuildVillage = getConnectionHandler().getAiLogic().getWayPointToBuildSettlement();

				if (wayPointToBuildVillage != null) {
					GameStart.mainLogger.getLOGGER()
							.fine("AI wants to build a village at: " + wayPointToBuildVillage.toString());

					// Request to build a village
					Settlement settlement = new Settlement(wayPointToBuildVillage);
					requestBuilding(settlement, BuildingType.VILLAGE);
				}
				// Else try to build a road
				else {
					///////////////////////////// ROAD
					///////////////////////////// /////////////////////////////////////
					// Try to trade if there is not enough resources available
					MaritimeTrade tradeRequestForStreet = getConnectionHandler().getAiLogic()
							.tradeCardsWithBankForStreet();
					if (tradeRequestForStreet != null) {
						requestMaritimeTrade(tradeRequestForStreet);
					}
					// Try to build a road
					WayPoint[] waypointsForStreet = getConnectionHandler().getAiLogic().getStreetToBuild();
					if (waypointsForStreet != null) {
						GameStart.mainLogger.getLOGGER().fine("AI wants to build a street at: "
								+ waypointsForStreet[0].toString() + " " + waypointsForStreet[1].toString());
						// Build street
						Street street = GameStart.siedlerVonCatan.getGameWorld()
								.findStreetWithWayPoints(waypointsForStreet[0], waypointsForStreet[1]);
						requestBuilding(street, BuildingType.STREET);

					} else {
						///////////////////////////// BUY DEVELOPMENT
						///////////////////////////// CARD////////////////////////////
						// TODO: Trade if necessary

						if (getConnectionHandler().getAiLogic().tryBuyingDevelopmentCard())
							sendBuyDevelopmentCardRequest();
						else
							waitAndRequestEndTurn();
					}
				}
			}
		}
	}

	/**
	 * Getter for the connection handler
	 *
	 * @return connectionHandler
	 */
	public ServerConnectionHandler getConnectionHandler() {
		return connectionHandler;
	}

}