package networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

import networking.MessageObjects.*;
import tools.PlayerTeam;
import view.ErrorPopUp;
import ai.AiLogic;
import application.GameStart;

import com.google.gson.Gson;

import javafx.application.Platform;

/**
 * Handles commands from server and client
 */
public class ServerConnectionHandler extends Thread {
	private ClientProtocol protocol;
	private Network network;
	private String serverIP;
	private int serverPort;
	private BufferedReader reader;
	private PrintWriter writer;
	private Boolean connected;
	private Socket socket;
	private Integer playerId;
	private String[] supportedServerVersions = {"1.0"};
	private static final String clientVersion = "1.0";
	private HashMap<Integer, PlayerTeam> idToColor = new HashMap<Integer, PlayerTeam>();
	private AiLogic ai;

	/**
	 * Added to "clientVersion" in method "getClientVersion"
	 */
	private static final String gruppenName = "(PikanteQuizshows)";
	private static final String aiLabel = "(KI)";
	private static final String clientType = "JavaFXClient";

	private boolean isAI;
	private boolean isSinglePlayerAI;

	public ServerConnectionHandler(Network network, String serverIP, int serverPort) {
		this.network = network;
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.connected = false;
		protocol = new ClientProtocol(this);
	}

	@Override
	public void run() {
		try {
			socket = new Socket(serverIP, serverPort);
			GameStart.mainLogger.getLOGGER().fine("Connection to server established");

			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream(), true);
			connected = true;
			String line;
			while ((line = reader.readLine()) != null)
				receiveMessage(line);

			reader.close();
			writer.close();
			disconnectFromServer();

		} catch (Exception e) {
			if(!protocol.getConnectionHandler().isSinglePlayerAI)
				Platform.runLater(() -> new ErrorPopUp(e.getMessage(), "Connection to server disrupted or missing", true));
			GameStart.mainLogger.getLOGGER().fine(e.getMessage());
			GameStart.mainLogger.getLOGGER().fine("Connection to server disrupted");
			// Terminate thread, since we don't need it any more.
			try {
				this.join(2000);
			} catch (InterruptedException e1) {
				this.stop();
			}

		}
	}

	public void sendMessage(String message) {
		if (connected) {
			Gson gson = new Gson();
			String mes = gson.toJson(message);
			writer.println(mes);
		}
	}

	public void sendObject(Object obj) {
		if (connected) {
			Gson gson = new Gson();
			// 3.2
			if (obj instanceof InitialMessage)
				writer.println("{\"Hallo\":" + gson.toJson(obj, InitialMessage.class) + "}");
			// 5.2
			else if (obj instanceof SendChat)
				writer.println("{\"Chatnachricht senden\":" + gson.toJson(obj, SendChat.class) + "}");
			// 6.1
			else if (obj instanceof PlayerForProtocol)
				writer.println("{\"Spieler\":" + gson.toJson(obj, PlayerForProtocol.class) + "}");
			// 6.2
			else if (obj instanceof StartGame)
				writer.println("{\"Spiel starten\":" + gson.toJson(obj, StartGame.class) + "}");
			// 8.2
			else if (obj instanceof Building)
				writer.println("{\"Bauen\":" + gson.toJson(obj, Building.class) + "}");
			// 8.3
			else if (obj instanceof TurnEnd)
				writer.println("{\"Zug beenden\":" + gson.toJson(obj, TurnEnd.class) + "}");
			// 9.2
			else if (obj instanceof ResourcesReturned)
				writer.println("{\"Karten abgeben\":" + gson.toJson(obj, ResourcesReturned.class) + "}");
			// 9.3
			else if (obj instanceof MoveThief)
				writer.println("{\"Raeuber versetzen\":" + gson.toJson(obj, MoveThief.class) + "}");
			// 9.5
			else if (obj instanceof MaritimeTrade)
				writer.println("{\"Seehandel\":" + gson.toJson(obj, MaritimeTrade.class) + "}");
			// 10.1
			else if (obj instanceof DomesticTradeOffer)
				writer.println("{\"Handel anbieten\":" + gson.toJson(obj, DomesticTradeOffer.class) + "}");
			// 10.2
			else if (obj instanceof PlayerReadyForDomesticTrade)
				writer.println("{\"Handel annehmen\":" + gson.toJson(obj, PlayerReadyForDomesticTrade.class) + "}");
			// 10.3
			else if (obj instanceof CompleteDomesticTrade)
				writer.println("{\"Handel abschliessen\":" + gson.toJson(obj, CompleteDomesticTrade.class) + "}");
			// 10.4
			else if (obj instanceof AbandonDomesticTrade)
				writer.println("{\"Handel abbrechen\":" + gson.toJson(obj, AbandonDomesticTrade.class) + "}");
			// 10.5
			else if (obj instanceof BuyDevelopmentCard)
				writer.println("{\"Entwicklungskarte kaufen\":" + gson.toJson(obj, BuyDevelopmentCard.class) + "}");
			else if (obj instanceof ThrowDiceRequest)
				writer.println("{\"Wuerfeln\":" + gson.toJson(obj, ThrowDiceRequest.class) + "}");
			//12.1
			else if (obj instanceof PlayKnightCard)
				writer.println("{\"Ritter ausspielen\":" + gson.toJson(obj, PlayKnightCard.class) + "}");
			//12.2
			else if (obj instanceof PlayRoadBuildingCard)
				writer.println("{\"Strassenbaukarte ausspielen\":" + gson.toJson(obj, PlayRoadBuildingCard.class) + "}");
			//12.3
			else if (obj instanceof Monopoly)
				writer.println("{\"Monopol\":" + gson.toJson(obj, Monopoly.class) + "}");
			//12.4
			else if (obj instanceof YearOfPlenty)
				writer.println("{\"Erfindung\":" + gson.toJson(obj, YearOfPlenty.class) + "}");
		}
	}

	public void receiveMessage(String message) {
		Gson gson = new Gson();
		GameStart.mainLogger.getLOGGER().fine(message);

		// 3.1
		if (message.contains("Chatnachricht")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Chatnachricht\":", "");
			ReceiveChat chat = gson.fromJson(objFromMessage, ReceiveChat.class);
			protocol.receiveChat(chat);
		}
		else if (message.contains("Hallo")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Hallo\":", "");
			InitialMessage hello = gson.fromJson(objFromMessage, InitialMessage.class);
			protocol.receiveHelloMessage(hello);
		}
		// 3.3
		else if (message.contains("Willkommen")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Willkommen\":", "");
			WelcomeMessage welcome = gson.fromJson(objFromMessage, WelcomeMessage.class);
			protocol.receiveWelcomeMessage(welcome);
		}
		// 3.1
		else if (message.contains("Serverantwort")) {
			ServerResponse response = gson.fromJson(message, ServerResponse.class);
			protocol.receiveServerResponse(response);
		}
		// 6.3
		else if (message.contains("Fehler")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Fehler\":", "");
			ServerError error = gson.fromJson(objFromMessage, ServerError.class);
			protocol.receiveError(error);
		}
		// 6.4
		else if (message.contains("Spiel gestartet")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Spiel gestartet\":", "");
			GameStarted gameStarted = gson.fromJson(objFromMessage, GameStarted.class);
			protocol.receiveGameStarted(gameStarted);
		}
		// 7.1
		else if (message.contains("Statusupdate")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Statusupdate\":", "");
			PlayerStatusUpdate update = gson.fromJson(objFromMessage, PlayerStatusUpdate.class);
			protocol.receivePlayerStatusUpdate(update);
		}
		// 7.2
		else if (message.contains("Wuerfelwurf")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Wuerfelwurf\":", "");
			DiceThrow diceThrow = gson.fromJson(objFromMessage, DiceThrow.class);
			protocol.receiveDiceThrow(diceThrow);
		}
		// 7.3
		else if (message.contains("Ertrag")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Ertrag\":", "");
			Earnings earnings = gson.fromJson(objFromMessage, Earnings.class);
			protocol.receiveEarnings(earnings);
		}
		// 8.5
		else if (message.contains("Raeuber versetzt")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Raeuber versetzt\":", "");
			ThiefMoved thiefMoved = gson.fromJson(objFromMessage, ThiefMoved.class);
			protocol.receiveThiefMoved(thiefMoved);
		}

		// 8.4 MADE BY FELIP. CHECK IF CORRECT?
		else if (message.contains("Kosten")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Kosten\":", "");
			Costs costs = gson.fromJson(objFromMessage, Costs.class);
			protocol.receiveCosts(costs);
		}

		// 7.4
		else if (message.contains("Bauvorgang")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Bauvorgang\":", "");
			BuildingEvent event = gson.fromJson(objFromMessage, BuildingEvent.class);
			protocol.receiveBuildingEvent(event);
		}
		//9.7
		else if (message.contains("Entwicklungskarte gekauft")) {
			String objFromMessage = message.substring(0, message.length() - 1)
					.replace("{\"Entwicklungskarte gekauft\":", "");
			DevelopmentCardBought developmentCardBought = gson.fromJson(objFromMessage, DevelopmentCardBought.class);
			protocol.receiveBoughtDevelopmentCard(developmentCardBought);
		}
		//9.10
		else if (message.contains("Laengste Handelsstrasse")) {
			String objFromMessage = message.substring(0, message.length() - 1)
					.replace("{\"Laengste Handelsstrasse\":", "");
			LongestRoad longestRoad = gson.fromJson(objFromMessage, LongestRoad.class);
			protocol.receiveLongestRoadInfo(longestRoad);
		}
		//9.10
		else if (message.contains("Groesste Rittermacht")) {
			String objFromMessage = message.substring(0, message.length() - 1)
					.replace("{\"Groesste Rittermacht\":", "");
			LargestArmy largestArmy = gson.fromJson(objFromMessage, LargestArmy.class);
			protocol.receiveLargestArmyInfo(largestArmy);
		}
		// 10.2
		else if (message.contains("Handelsangebot angenommen")) {
			String objFromMessage = message.substring(0, message.length() - 1)
					.replace("{\"Handelsangebot angenommen\":", "");
			PlayerWhoAcceptedTrade playerWhoAcceptedTrade = gson.fromJson(objFromMessage, PlayerWhoAcceptedTrade.class);
			protocol.receiveAcceptedTrade(playerWhoAcceptedTrade);
		}
		// 10.3
		else if (message.contains("Handel ausgefuehrt")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Handel ausgefuehrt\":", "");
			TradeFinished tradeFinished = gson.fromJson(objFromMessage, TradeFinished.class);
			protocol.receiveTradeFinished(tradeFinished);
		}
		// 10.4
		else if (message.contains("Handelsangebot abgebrochen")) {
			String objFromMessage = message.substring(0, message.length() - 1)
					.replace("{\"Handelsangebot abgebrochen\":", "");
			AbandonedTrade abandonedTrade = gson.fromJson(objFromMessage, AbandonedTrade.class);
			protocol.receiveAbandonedTrade(abandonedTrade);
		}
		// 10.1 // IMPORTANT: HAVE THIS BELOW ANY OTHER "HANDELANGEBOT" messages!
		else if (message.contains("Handelsangebot")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Handelsangebot\":", "");
			ReceivedTradeOffer receivedTradeOffer = gson.fromJson(objFromMessage, ReceivedTradeOffer.class);
			protocol.receiveTradeOffer(receivedTradeOffer);
		}
		// 7.5 (neues Blatt)
		else if (message.contains("Spiel beendet")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Spiel beendet\":", "");
			GameOver gameOver = gson.fromJson(objFromMessage, GameOver.class);
			protocol.receiveGameOver(gameOver);
		}
		//12.1
		else if (message.contains("Ritter ausspielen")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Ritter ausspielen\":", "");
			PlayKnightCard playedKnightCard = gson.fromJson(objFromMessage, PlayKnightCard.class);
			protocol.receiveKnightCardPlayed(playedKnightCard);
		}
		//12.2
		else if (message.contains("Strassenbaukarte ausspielen")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Strassenbaukarte ausspielen\":", "");
			PlayRoadBuildingCard playedRoadBuildingCard= gson.fromJson(objFromMessage, PlayRoadBuildingCard.class);
			protocol.receiveRoadBuildingCardPlayed(playedRoadBuildingCard);
		}
		//12.3
		else if (message.contains("Monopol")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Monopol\":", "");
			Monopoly monopolyCard= gson.fromJson(objFromMessage, Monopoly.class);
			protocol.receiveMonopolyCardPlayed(monopolyCard);
		}
		//12.4
		else if (message.contains("Erfindung")) {
			String objFromMessage = message.substring(0, message.length() - 1).replace("{\"Erfindung\":", "");
			YearOfPlenty yearOfPlentyCard= gson.fromJson(objFromMessage, YearOfPlenty.class);
			protocol.receiveYearOfPlentyCardPlayed(yearOfPlentyCard);
		}
	}

	public void disconnectFromServer() {
		if(socket == null)
			return;
		try {
			socket.close();
		} catch (IOException e) {
			GameStart.mainLogger.getLOGGER().fine("disconnected");
		}
	}

	public void setPlayerId(Integer id) {
		this.playerId = id;
	}

	public Integer getPlayerId() {
		return playerId;
	}

	public String[] getSupporedServerVersions() {
		return this.supportedServerVersions;
	}

	/**
	 * Returns the full information about the client's version. Adds a "(KI)" label
	 * if we join as an AI.
	 *
	 * @return
	 */
	public String getClientVersion() {
		String versionText = clientType + " " + clientVersion + " " + gruppenName + (isAI() ? " " + aiLabel : "");
		return versionText;
	}

	/**
	 * @return the isAI
	 */
	public boolean isAI() {
		return isAI;
	}

	/**
	 * @param isAI
	 *            the isAI to set. If True, then create an AI, if false, then set
	 *            its reference to null.
	 */
	public void setAI(boolean isAI) {
		if (!isAI)
			ai = null;
		this.isAI = isAI;
	}

	/**
	 * @return the isSinglePlayerAI
	 */
	public boolean isSinglePlayerAI() {
		return isSinglePlayerAI;
	}

	/**
	 * @param isSinglePlayerAI
	 *            the isSinglePlayerAI to set
	 */
	public void setSinglePlayerAI(boolean isSinglePlayerAI) {
		this.isSinglePlayerAI = isSinglePlayerAI;
		if (isSinglePlayerAI) {
			setAI(true);
		}
	}

	public PlayerTeam getPlayerColor(Integer id) {
		return idToColor.get(id);
	}

	public Integer getPlayerIdFromColor(PlayerTeam color) {
		for (Integer id : idToColor.keySet()) {
			if (idToColor.get(id).equals(color)) {
				return id;
			}
		}
		return null;
	}

	public void extendIdColorMap(Integer id, PlayerTeam color) {
		if (!idToColor.containsKey(id)) {
			idToColor.put(id, color);
		}
	}

	public AiLogic getAiLogic() {
		return ai;
	}

	public void addAILogic(AiLogic ai) {
		this.ai = ai;
		setAI(true);
	}
}
