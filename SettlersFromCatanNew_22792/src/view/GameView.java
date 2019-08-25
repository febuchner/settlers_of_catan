package view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import application.GameStart;
import controller.ChatWindowController;
import controller.ContinueMainMenuController;
import controller.InGameController;
import controller.MainMenuController;
import gameobjects.Cards.EvolutionCard;
import gameobjects.Cards.ResourceCard;
import gameobjects.Elements.Street;
import gameworld.GameMatrixEntry;
import gameworld.HexagonField;
import gameworld.WayPoint;
import gameworld.World;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import menu.ContinueMainMenu;
import menu.GameOverScreen;
import menu.GameSetUp;
import menu.MainMenu;
import menu.PauseScreen;
import menu.SplashScreen;
import menu.Tutorial;
import menu.VictoryScreen;
import networking.MessageObjects.Field;
import networking.MessageObjects.Map;
import networking.MessageObjects.Port;
import networking.MessageObjects.ReceivedTradeOffer;
import player.Player;
import resources.ResourcePointer;
import resources.hexagons.HexagonTexturePointer;
import tools.BuildingType;
import tools.EvolutionType;
import tools.FieldType;
import tools.PlayerTeam;
import tools.PortTypes;
import tools.ResourceType;
import tools.Styling;
import tools.Vector2D;
import tools.Vector3D;
import tools.WorldTranslation;

/**
 * The view class for the entire game.
 */
public class GameView {
    /**
     * A button simulating a rather large notification message.
     */
    private Button largeNotificationMessage;
    /**
     * A reference to make sure we only have one (but constantly changing)
     * accept button
     */
    private VBox acceptButton;
    /**
     * A reference to our in game controller.
     */
    public InGameController inGameController;
    /**
     * The main layout containing all graphical hexagon elements.
     */
    private Pane layout;
    /**
     * The radius of our hexagons
     */
    private int hexagonRadius;
    /**
     * A reference to our main game scene.
     */
    private Scene mainGameScene;
    /**
     * The primary stage of the game
     */
    private Stage primaryStage;
    /**
     * A list of all streets (but only in one direction)
     */
    private ArrayList<Street> streets;
    /**
     * A list of all wayPoints
     */
    private ArrayList<WayPoint> wayPointsList;
    /**
     * A hash map, mapping a tupel (Vector2D) of WayPoints to their
     * (corresponding) street.
     */
    public HashMap<Vector2D<WayPoint>, Street> wayPointsToStreet;
    /**
     * A hash map, mapping buttons to the waypoints and hexagons
     */
    private HashMap<GameMatrixEntry, Button> buttonsOfTheWorldMatrix;
    /**
     * A hash map, mapping a button to every street
     */
    private HashMap<Street, Button> buttonsOfTheStreets = new HashMap<Street, Button>();
    /**
     * hash map mapping a player object to his corresponding avatar button
     */
    private HashMap<Player, Button> playerToAvatar= new HashMap<Player,Button>();
    /**
     * saves the default avatar position (used to relocate the avatars)
     */
    private double defaultAvatarYPos;
    /**
     * signalisies wheter the avatars have already been created
     */
    private boolean avatarsCreated;
    /**
     * A representation of our gameMatrix
     */
    private GameMatrixEntry[][][] worldMatrix;
    private DropShadow dropShadowHexagon;
    /**
     * A reference to our in-game server chat window
     */
    private TextArea serverChat;
    /**
     * A reference to our in-game client chat window
     */
    private TextArea clientChat;
    /**
     * The drop down menu for an avatar.
     */
    private VBox dropDownMenu;
    /**
     * The bankTradingWindow
     */
    private VBox tradingWindow;
    /**
     * The player trading window displayed when receiving a trade request.
     */
    private VBox playerTradingWindow;
    /**
     * The window displayed when playing a monopoly card.
     */
    private VBox monopolyWindow;
    /**
     * The window displayed when playing a year of plenty card.
     */
    private VBox yearOfPleantyWindow;
    /**
     * A HBox for displaying resource cards
     */
    private HBox resourceCardHbox;
    /**
     * A HBox for displaying development cards
     */
    private HBox developmentCardHbox;
    /**
     * A button to display all players who accepted your prior trade request.
     */
    private Button acceptedTradeRequestButton;
    /**
     * A HBox to display all players who accepted your prior trade request.
     */
    private HBox acceptedTradeRequestHBox;
    /**
     * A reference to our thief button
     */
    private Button thiefAvatar;
    // private Button roadBuildingButton;
    /**
     * we need to save the highlighted street (road building card) to
     * de-highlight it if the request was denied by server
     */
    private Street highlightedStreet;
    /**
     * used to save the streets activated for the road building card (should it be rejected)
     */
    private ArrayList<Street> roadBuildingStreetsActivated=new ArrayList<Street>();
    private Boolean worldIsDrawn = false;

    public Button abortTradeButton;
    /**
     * A panel displaying information about (building) costs
     */
    private ImageView imageViewBuildInfo;
    /**
     * boolean which shows whether buildingCostsCard is drawn or not
     */
    public boolean isBuildingCostsDrawn = false;

    public void startDrawing(Stage primaryStage) throws Exception {
        this.setPrimaryStage(primaryStage);
        // Setting up stage
        primaryStage.setTitle("Settlers of Catan");
        primaryStage.setFullScreenExitHint("");
        primaryStage.fullScreenExitHintProperty();
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.setFullScreen(true);
        primaryStage.getIcons().add(new Image(ResourcePointer.class.getResourceAsStream("SettlersIcon.png")));
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        primaryStage.setWidth(bounds.getWidth() / 1.35);
        primaryStage.setHeight(bounds.getHeight() / 1.35);
        // Create splash screen and set up the scene
        Scene splashScreen = createSplashScreen();
        // Add scene to stage
        primaryStage.setScene(splashScreen);
        primaryStage.show();
        FadeTransition ft = new FadeTransition(Duration.millis(1000), splashScreen.getRoot());
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();

        // Terminate application when window closes
        primaryStage.setOnCloseRequest(closeEvent -> {
            // Inform server is connected
            if (GameStart.network != null) {
                GameStart.network.getConnectionHandler().disconnectFromServer();
            }
            Platform.exit();
        });
        // Set up magnifying glass
        //magnifyingGlass = new MagnifyingGlass();
    }

    /**
     * Resets all needed variables to start a new game. Use with caution!
     */
    public void resetVariables() {
    	avatarsCreated=false;
        if (streets != null)
            streets.clear();
        if (wayPointsList != null)
            wayPointsList.clear();
        if (wayPointsToStreet != null)
            wayPointsToStreet.clear();
        if (buttonsOfTheWorldMatrix != null)
            buttonsOfTheWorldMatrix.clear();
        if (buttonsOfTheStreets != null)
            buttonsOfTheStreets.clear();
    }

    /**
     * Creates a main menu scene (and its controller) and returns it.
     *
     * @return A newly created main menu scene.
     */
    public void createMainMenuScene() {
        MainMenu mainMenu = new MainMenu(this);
        new MainMenuController(mainMenu);
        swapScenes(mainMenu.getRoot());
    }

    public void createContinueMainMenuScene() {
        ContinueMainMenu continueMainMenu = new ContinueMainMenu(this);
        new ContinueMainMenuController(continueMainMenu);
        swapScenes(continueMainMenu.getRoot());
    }

    /**
     * Creates splash screen object and assigns the scene to the main game
     * scene.
     *
     * @return The scene from the splash screen.
     */
    private Scene createSplashScreen() {
        SplashScreen splashScreen = new SplashScreen();
        mainGameScene = splashScreen.createSplashScene(this);
        return mainGameScene;
    }

    /**
     * Creates and returns a background (for a scene).
     *
     * @param fileName       the name of the background image to load
     * @param backgroundSize The size of the background
     * @return background
     */
    public Background getBackground(String fileName, BackgroundSize backgroundSize, boolean repeatX) {
        // Background Image
        Image bgImage = new Image(ResourcePointer.class.getResourceAsStream(fileName));
        BackgroundImage backgroundImage = new BackgroundImage(bgImage,
                repeatX ? BackgroundRepeat.REPEAT : BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER, backgroundSize);
        // new Background(images...)
        Background background = new Background(backgroundImage);
        return background;
    }

    /**
     * Draws the online world from the server and features for the online game.
     * A chat for instance should be added here. (drawWorld() should have been
     * executed already)
     *
     * @param map The map to be drawn
     */
    public void drawWorldOnline(Map map) {
        // iterate through all field objects
        for (Field field : map.getFields()) {
            // Get the world translated hexagon field
            Vector2D<Integer> pos2D = WorldTranslation.getLetterToPosition(field.getLocation());
            // Get the corresponding hexa field
            HexagonField hexagonField = (HexagonField) worldMatrix[pos2D.x][pos2D.y][0];
            Button hexagonbutton = buttonsOfTheWorldMatrix.get(hexagonField);
            FieldType newType = WorldTranslation.getServerFieldTypeToClientFieldType(field.getType());
            // GameStart.mainLogger.getLOGGER().fine(newType + " " +field.getTyp());
            Integer chipNumber = field.getNumber();
            // Define the new properties for the hexagon field
            hexagonField.defineHexagonProperties(newType, chipNumber);
            // Adjust texture
            Polygon polygon = (Polygon) hexagonbutton.getGraphic();
            String fileNameToRead = ".png";
            switch (newType) {
                case DESERT:
                    fileNameToRead = "Desert" + fileNameToRead;
                    break;
                case GRAIN:
                    fileNameToRead = "Wheat" + fileNameToRead;
                    break;
                case LOAM:
                    fileNameToRead = "Loam" + fileNameToRead;
                    break;
                case STONE:
                    fileNameToRead = "Mountain" + fileNameToRead;
                    break;
                case WOOD:
                    fileNameToRead = "Forest" + fileNameToRead;
                    break;
                case WOOL:
                    fileNameToRead = "Heather" + fileNameToRead;
                    break;
                case PORT:
                    fileNameToRead = "Water" + fileNameToRead;
                    break;
                case WATER:
                    fileNameToRead = "Water" + fileNameToRead;
                    break;
                default:
                    break;
            }
            Image hexagonTexture = new Image(HexagonTexturePointer.class.getResourceAsStream(fileNameToRead),
                    hexagonRadius * 2, hexagonRadius * 2, true, true);
            polygon.setFill((new ImagePattern(hexagonTexture, 0, 0, 1, 1, true)));
            // Display chip number if not 7 (Thief)
            if (chipNumber == 7 || chipNumber == 0) {
                if (chipNumber == 7 || newType == FieldType.DESERT) {
                    GameStart.siedlerVonCatan.getThief()
                            .setThiefLocation(WorldTranslation.LETTER_TO_VECTOR.get(map.getThief()));
                    drawThief(hexagonField.getPosition().castTo2D());
                }
                continue;
            }
            Button chipNumberDisplayer = new Button("" + chipNumber);
            // Size of the chip number button
            double size = hexagonRadius / 2;
            chipNumberDisplayer.setMinSize(size, size);
            chipNumberDisplayer.setMaxSize(size, size);
            chipNumberDisplayer.setMouseTransparent(true);
            chipNumberDisplayer.setId("chipNumber");
            Font fontChipNumber = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                    size / 2.5);
            chipNumberDisplayer.setFont(fontChipNumber);
            chipNumberDisplayer.relocate(hexagonField.getScenePosition().x + hexagonRadius - size / 2,
                    hexagonField.getScenePosition().y + hexagonRadius - size / 2);
            // Add texture to the chipNumberCircle
            layout.getChildren().add(chipNumberDisplayer);
            if (chipNumber == 8 || chipNumber == 6)
                chipNumberDisplayer.setTextFill(Color.DARKGOLDENROD);
            else if (chipNumber == 12 || chipNumber == 2)
                chipNumberDisplayer.setTextFill(Color.DARKRED);
        }
        // Add and display ports
        String[] portLabels = {"3:1", "Wood 2:1", "Wool 2:1", "Stone 2:1", "Grain 2:1", "Loam 2:1"};
        for (Port port : map.getPorts()) {
            // Create button
            String serverCoordinates = port.getLocation();
            Vector3D<Integer>[] wayPoints = WorldTranslation.getWaypointsOfStreet(serverCoordinates);
            WayPoint wpA = (WayPoint) worldMatrix[wayPoints[0].x][wayPoints[0].y][wayPoints[0].z];
            WayPoint wpB = (WayPoint) worldMatrix[wayPoints[1].x][wayPoints[1].y][wayPoints[1].z];
            // Mark waypoints as port
            Street street = wayPointsToStreet.get(new Vector2D<WayPoint>(wpA, wpB));
            Button mainStreetButton = buttonsOfTheStreets.get(street);
            Button portStreetButton = new Button();
            portStreetButton.setMinSize(mainStreetButton.getMinWidth(), 4 * mainStreetButton.getMinHeight());
            portStreetButton.setMaxSize(mainStreetButton.getMaxWidth(), 4 * mainStreetButton.getMaxHeight());
            Font portFont = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                    portStreetButton.getMinHeight() / 2.5);
            portStreetButton.setFont(portFont);
            // Assign label (or texture...)
            switch (port.getType()) {
                case "Holz Hafen":
                    portStreetButton.setText(portLabels[1]);
                    wpA.setPortType(PortTypes.TWO_WOOD_FOR_ONE);
                    wpB.setPortType(wpA.getPortType());
                    break;
                case "Lehm Hafen":
                    portStreetButton.setText(portLabels[5]);
                    wpA.setPortType(PortTypes.TWO_LOAM_FOR_ONE);
                    wpB.setPortType(wpA.getPortType());
                    break;
                case "Wolle Hafen":
                    portStreetButton.setText(portLabels[2]);
                    wpA.setPortType(PortTypes.TWO_WOOL_FOR_ONE);
                    wpB.setPortType(wpA.getPortType());
                    break;
                case "Erz Hafen":
                    portStreetButton.setText(portLabels[3]);
                    wpA.setPortType(PortTypes.TWO_STONE_FOR_ONE);
                    wpB.setPortType(wpA.getPortType());
                    break;
                case "Getreide Hafen":
                    portStreetButton.setText(portLabels[4]);
                    wpA.setPortType(PortTypes.TWO_GRAIN_FOR_ONE);
                    wpB.setPortType(wpA.getPortType());
                    break;
                default:
                    portStreetButton.setText(portLabels[0]);
                    wpA.setPortType(PortTypes.THREE_FOR_ONE);
                    wpB.setPortType(wpA.getPortType());
                    break;
            }
            // Change texture of the hexagon
            changeHexagonToPortTexture(port.getLocation(), portStreetButton, mainStreetButton);
            // Add port button to layout
            layout.getChildren().add(portStreetButton);
        }
        // Display start notification
        displayNotificationMessage("Round 0");
        // Mark as readyToStart
        GameStart.siedlerVonCatan.readyToStart();
        // Swap scene
        swapScenes(layout);
    }

    /**
     * Creates a new "game" scene displaying our world matrix.
     *
     * @param world The world's matrix.
     * @author Felip
     */
    public void drawWorld(World world) {
        worldMatrix = world.getGameMatrix();
        wayPointsToStreet = world.getWayPointsToStreet();
        buttonsOfTheWorldMatrix = new HashMap<GameMatrixEntry, Button>();
        streets = world.getStreets();
        wayPointsList = world.getWayPoints();
        layout = new Pane();
        // Load styles
        String style = ResourcePointer.class.getResource("StyleWorld.css").toExternalForm();
        // Add a background image to the scene
        // Screen screen = Screen.getPrimary();
        double sceneWidth = mainGameScene.getWidth();
        double sceneHeight = mainGameScene.getHeight();
        changeBackgroundToDefault();
        // Helper values for calculations
        int points = 6;
        hexagonRadius = (int) (sceneHeight / (2.25 * worldMatrix.length));
        // Set a distance for the hexagons
        final double distanceOffset = hexagonRadius / 4.5;
        double distance = hexagonRadius * 2 - distanceOffset;
        // We need an offset, since we do not want to have all everything at the
        // top
        // left corner.
        double offsetX = sceneWidth / 2 - distance * ((double) worldMatrix.length / 2d);
        double offsetY = hexagonRadius;
        // Create buttons at wayPoint "location"
        // Set up the values for the polygon
        double angle = ((2 * Math.PI) / points);
        double[] hexagonValues = new double[points * 2];
        for (int i = 0; i < points; i++) {
            int ii = 2 * i;
            hexagonValues[ii] = hexagonRadius * Math.sin(angle * i);
            hexagonValues[ii + 1] = hexagonRadius * Math.cos(angle * i);
        }
        // Set up offsets for the coordinates
        double extraOffsetX = 0;
        double extraOffsetY = 0;
        boolean firstWaterTile = false;
        ArrayList<Button> streetButtons = new ArrayList<>();
        ArrayList<Button> activeHexagons = new ArrayList<>();
        ArrayList<Button> activeWayPoints = new ArrayList<>();
        // Create a shadow
        dropShadowHexagon = new DropShadow();
        dropShadowHexagon.setRadius(5.0);
        dropShadowHexagon.setOffsetX(-hexagonRadius / 6);
        dropShadowHexagon.setOffsetY(hexagonRadius / 4);
        dropShadowHexagon.setColor(Color.color(0, 0, 0, 0.775));
        // Define size and angle of the street buttons
        Vector2D<Double> streetSize = new Vector2D<Double>((double) hexagonRadius + hexagonRadius / 10,
                (double) (hexagonRadius / 10));
        // Iterate through matrix
        for (int row = worldMatrix.length - 1; row >= 0; row--) {
            firstWaterTile = true;
            // Set an extra OffSet
            extraOffsetX = -((worldMatrix.length - 1) / 2 - row) * (hexagonRadius - distanceOffset / 2);
            extraOffsetY = ((worldMatrix.length - 1) / 2 - row) * distanceOffset;
            for (int column = 0; column < worldMatrix.length; column++) {
                // If entry null, continue....
                if (worldMatrix[row][column][0] != null) {
                    // Draw hexagon
                    HexagonField hexagonField = (HexagonField) worldMatrix[row][column][0];
                    Button hexagonFieldButton = new Button();
                    buttonsOfTheWorldMatrix.put(hexagonField, hexagonFieldButton);
                    // Create Polynom for the hexagon
                    // Adding coordinates to the hexagon
                    Polygon hexagonPolygon = new Polygon(hexagonValues);
                    hexagonPolygon.setEffect(dropShadowHexagon);
                    // Add graphic to button
                    hexagonFieldButton.setGraphic(hexagonPolygon);
                    hexagonFieldButton.setId("hexagon");
                    // Add a default texture (Water):
                    Image hexagonTexture = new Image(HexagonTexturePointer.class.getResourceAsStream("Water.png"),
                            hexagonRadius * 2, hexagonRadius * 2, true, true);
                    hexagonPolygon.setFill(new ImagePattern(hexagonTexture, 0, 0, 1, 1, true));
                    // Load and set the style sheet
                    hexagonFieldButton.getStylesheets().add(style);
                    // Set position
                    hexagonField.storeScenePosition(new Vector2D<Double>(offsetX + extraOffsetX + column * distance,
                            offsetY + extraOffsetY + row * distance));
                    hexagonFieldButton.relocate(hexagonField.getScenePosition().x, hexagonField.getScenePosition().y);
                    activeHexagons.add(0, hexagonFieldButton);
                    // Style the wayPoint "button"
                    // Each hexagon "creates" 2 (left) wayPoints
                    // Only add if not left wayPoints from water tiles! (Left
                    // Side)
                    if (firstWaterTile) {
                        firstWaterTile = false;
                        continue;
                    }
                    for (int i = 1; i <= 2; i++) {
                        // Only add if not left wayPoints from water tiles! (Top
                        // and bottom)
                        if ((i == 1 && row == 0) || (i == 2 && row == worldMatrix.length - 1))
                            continue;
                        Button wayPointButton = new Button();
                        buttonsOfTheWorldMatrix.put(worldMatrix[row][column][i], wayPointButton);
                        // Load and set the style sheet
                        activeWayPoints.add(wayPointButton);
                        wayPointButton.setId("wayPoint");
                        wayPointButton.getStylesheets().add(style);
                        double buttonSize = hexagonRadius / 3;
                        // set size of wayPoint buttons
                        wayPointButton.setStyle("-fx-min-width: " + buttonSize + "px;" + " -fx-min-height: "
                                + buttonSize + "px;" + " -fx-max-width: " + buttonSize + "px;" + " -fx-max-height: "
                                + buttonSize + "px");
                        // Set Layout position
                        wayPointButton.setLayoutX(offsetX + extraOffsetX + column * distance);
                        wayPointButton.setLayoutY(offsetY + extraOffsetY + row * distance);
                        // Draw button at the right position
                        wayPointButton.setTranslateX(0);
                        // 3/8 is a number i calculated that should work
                        wayPointButton.setTranslateY(hexagonRadius * 3 / 8 + (i == 1 ? 0 : hexagonRadius * 9 / 8));
                        // Set position vector
                        double xPos = wayPointButton.getTranslateX() + wayPointButton.getLayoutX() - streetSize.x / 2
                                + buttonSize / 2;
                        double yPos = wayPointButton.getTranslateY() + wayPointButton.getLayoutY() - streetSize.y / 2
                                + buttonSize / 2;
                        // Create streets (only once, therefor i==1)
                        if (i == 1) {
                            // Add streets to hashMap. Each hexagon adds 3
                            // streets.
                            if (row < worldMatrix.length - 1) {
                                Vector2D<WayPoint> vec = new Vector2D<WayPoint>((WayPoint) worldMatrix[row][column][1],
                                        (WayPoint) worldMatrix[row][column][2]);
                                for (Street street : streets) {
                                    if (street.vectorEqualsStreet(vec)) {
                                        Button streetButton = new Button();
                                        // Set button size, position and
                                        // rotation
                                        setStreetSize(streetButton,
                                                new Vector2D<Double>(xPos, yPos + hexagonRadius / 2), streetSize, 90,
                                                style);
                                        // Add street to hashMap/list
                                        buttonsOfTheStreets.put(street, streetButton);
                                        streetButtons.add(streetButton);
                                        // Deactivate street button
                                        streetButton.setDisable(true);
                                        // Set default image
                                        changeButtonImage(streetButton, BuildingType.STREET, PlayerTeam.NONE);
                                    }
                                }
                            }
                            // Check if we have a left street
                            // Left connection point is at [row-1][column][2]
                            if (row - 1 >= 0 && column - 1 >= 0 && worldMatrix[row - 1][column - 1][0] != null) {
                                Vector2D<WayPoint> vecLeft = new Vector2D<WayPoint>(
                                        (WayPoint) worldMatrix[row][column][1],
                                        (WayPoint) worldMatrix[row - 1][column][2]);
                                for (Street street : streets) {
                                    if (street.vectorEqualsStreet(vecLeft)) {
                                        Button streetButtonLeft = new Button();
                                        // Set button size, position and
                                        // rotation
                                        setStreetSize(streetButtonLeft, new Vector2D<Double>(xPos - hexagonRadius / 2,
                                                yPos - hexagonRadius / 4), streetSize, 30, style);
                                        // Add street to hashMap/list
                                        buttonsOfTheStreets.put(street, streetButtonLeft);
                                        streetButtons.add(streetButtonLeft);
                                        // Deactivate street button
                                        streetButtonLeft.setDisable(true);
                                        // Set default image
                                        changeButtonImage(streetButtonLeft, BuildingType.STREET, PlayerTeam.NONE);
                                    }
                                }
                            }
                            // Check if we have a right street
                            // Right connection point is at [row-1][column-1][2]
                            if (row - 1 >= 0 && column + 1 < worldMatrix.length
                                    && worldMatrix[row - 1][column + 1][0] != null) {
                                Vector2D<WayPoint> vecRight = new Vector2D<WayPoint>(
                                        (WayPoint) worldMatrix[row][column][1],
                                        (WayPoint) worldMatrix[row - 1][column + 1][2]);
                                for (Street street : streets) {
                                    if (street.vectorEqualsStreet(vecRight)) {
                                        Button streetButtonRight = new Button();
                                        // Set button size, position and
                                        // rotation
                                        setStreetSize(streetButtonRight, new Vector2D<Double>(xPos + hexagonRadius / 2,
                                                yPos - hexagonRadius / 4), streetSize, -30, style);
                                        // Add street to hashMap/list
                                        buttonsOfTheStreets.put(street, streetButtonRight);
                                        streetButtons.add(streetButtonRight);
                                        // Deactivate street button
                                        streetButtonRight.setDisable(true);
                                        // Set default image
                                        changeButtonImage(streetButtonRight, BuildingType.STREET, PlayerTeam.NONE);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Create a header (label)
        // Set up a font
        Font fontBasic = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                sceneHeight / 20);
        Label header = new Label("Game Board:");
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(5.0);
        dropShadow.setOffsetX(-5.0);
        dropShadow.setOffsetY(6.0);
        dropShadow.setColor(Color.color(0, 0, 0, 0.5));
        header.setEffect(dropShadow);
        header.relocate(sceneWidth / 2 - distance * (worldMatrix.length / 5.5), offsetY / 4);
        header.setFont(fontBasic);
        // Add all the new nodes to the layout to display it on the screen.
        layout.getChildren().add(header);
        // Add a roll-a-dice button
        ArrayList<Button> inGameButtons = createInGameButtons(sceneWidth, sceneHeight);
        // Preserve rendering priorities.
        layout.getChildren().addAll(activeHexagons);
        layout.getChildren().addAll(streetButtons);
        layout.getChildren().addAll(activeWayPoints);
        // Add other stuff
        layout.getChildren().addAll(inGameButtons);
        // Create a server chat window
        double serverWindowHeight = sceneHeight / 4;
        createServerChatMessageWindow(sceneWidth / 7, serverWindowHeight);
        // Add online chat window
        createClientOnlineChatMessageWindow(sceneWidth / 5, sceneHeight / 2.5, serverWindowHeight);
        // If you click ESCAPE during the game you get to the PauseScreen
        layout.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                showPauseScreen();
            }
            else if (e.getCode() == KeyCode.CONTROL) {
        		if (isBuildingCostsDrawn == false)
        			drawBuildingCostCard();
        		else hideBuildingCostCard();
        	}
        });


        // Draw avatars
        createAvatars(sceneWidth - 50, 50, hexagonRadius * 2 / 3);
        // Add neighbour waypoints
        addWayPointNeighbourBidirectional();
        worldIsDrawn = true;
    }

    /**
     * Implements neighbour waypoints.
     */
    private void addWayPointNeighbourBidirectional() {
        buttonsOfTheStreets.keySet().forEach(street -> {
            WayPoint wpA = street.getConnectionPoints()[0];
            WayPoint wpB = street.getConnectionPoints()[1];
            // Add bidirectional
            wpA.addWayPointToNeighbourList(wpB);
            wpB.addWayPointToNeighbourList(wpA);
        });
    }

    /**
     * Creates all in game buttons.
     *
     * @return A list of all in game buttons.
     */
    private ArrayList<Button> createInGameButtons(double sceneWidth, double sceneHeight) {
        ArrayList<Button> buttonArrayList = new ArrayList<>();
        // Create Dice Button
        Button diceButton = new Button("Roll dice");
        diceButton.setEffect(dropShadowHexagon);
        diceButton.setId("MenuButton");
        Font fontDice = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                sceneHeight / 55);
        Font fontLabel = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                sceneHeight / 83);
        double length = sceneHeight / 7;
        double height = sceneHeight / 20;
        diceButton.setMaxSize(length, height);
        diceButton.setMinSize(length, height);
        diceButton.setFont(fontDice);
        diceButton.relocate(sceneWidth / 2 - length / 2, sceneHeight - height * 3);
        diceButton.setDisable(true);
        buttonArrayList.add(diceButton);
        // Set action when clicking
        inGameController = new InGameController();
        // Create End Turn button
        Button endTurnButton = new Button("End Turn");
        // Deactivate button
        endTurnButton.setDisable(true);
        // TODO: Style Endturn button in css
        endTurnButton.setId("Endturn");
        endTurnButton.setEffect(dropShadowHexagon);
        endTurnButton.setFont(fontDice);
        endTurnButton.setMaxSize(length, height);
        endTurnButton.setMinSize(length, height);
        endTurnButton.setFont(fontDice);
        endTurnButton.relocate(sceneWidth / 2 - length / 2, sceneHeight - height * 1.5);
        buttonArrayList.add(endTurnButton);
        VBox developmentVBox = new VBox(-10);
        Rectangle developmentCardRectangle = new Rectangle(length * 0.665, length);
        Button developmentCardButtonLabel = new Button("Development Card");
        developmentCardButtonLabel.setMouseTransparent(true);
        developmentVBox.getChildren().addAll(developmentCardRectangle, developmentCardButtonLabel);
        developmentVBox.setAlignment(Pos.CENTER);
        // getDevelopmentCardButton.setDisable(true);
        developmentCardRectangle.setEffect(dropShadowHexagon);
        developmentCardButtonLabel.setEffect(dropShadowHexagon);
        developmentCardButtonLabel.setFont(fontLabel);
        developmentCardButtonLabel.setId("MenuButton");
        developmentVBox.relocate(sceneWidth * 2.9 / 4, sceneHeight * 2.15 / 5);
        // Load avatar image
        Image cardImage = new Image(ResourcePointer.class.getResourceAsStream("HiddenCard" + ".png"));
        // Set image
        developmentCardRectangle.setFill((new ImagePattern(cardImage, 0, 0, 1, 1, true)));
        layout.getChildren().add(developmentVBox);
        // Define actions
        inGameController.defineEndTurnActions(endTurnButton);
        inGameController.defineDiceActions(diceButton);
        inGameController.defineGetDevelopmentCardActions(developmentCardRectangle);
        // inGameController.defineRoadBuildingButtonActions(roadBuildingButton);
        return buttonArrayList;
    }

    /**
     * Defines rotation and shape and position of a button
     *
     * @param button     The button we are working with
     * @param streetSize The new size with x:length, y:height
     * @param angle      The angle to rotate
     * @param position   The (new) Position of the button
     */
    private void setStreetSize(Button button, Vector2D<Double> position, Vector2D<Double> streetSize, double angle,
                               String style) {
        button.setMinSize(streetSize.x, streetSize.y);
        button.setMaxSize(streetSize.x, streetSize.y);
        button.setRotate(angle);
        button.relocate(position.x, position.y);
        button.setId("street");
        button.getStylesheets().add(style);
    }

    /**
     * Creates and returns a TextArea, simulatnig a server chat window for
     * server messages.
     */
    private void createServerChatMessageWindow(double length, double height) {
        // Create message window
        double xCord = 100;
        double yCord = 60;
        this.serverChat = createTextArea(length, height, xCord, yCord, "Server Chat", "Game started...", 17);
    }

    /**
     * Creates an online chat window for chat messages (Do not confuse with the
     * server chat window)
     */
    private void createClientOnlineChatMessageWindow(double length, double height, double serverChatHeight) {
        // Create chat message window
        double xCord = 100;
        double yCord = 120 + serverChatHeight;
        this.clientChat = createTextArea(length, height, xCord, yCord, "Online Chat", "[Server] Welcome settlers!", 25);
        // Create TextInput field
        TextField textField = new TextField();
        // Set default setPromptText
        textField.setPromptText("Enter chat message!");
        textField.relocate(xCord, yCord + height);
        double inputFieldHeight = height / 10;
        textField.setMinSize(length, inputFieldHeight);
        textField.setMaxSize(length, inputFieldHeight);
        layout.getStylesheets().add(ResourcePointer.class.getResource("Tutorial.css").toExternalForm());
        Font chatFont = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                inputFieldHeight / 2.5);
        textField.setFont(chatFont);
        textField.setEffect(dropShadowHexagon);
        textField.setStyle("-fx-text-fill: black;");
        textField.setEditable(true);
        layout.getChildren().add(textField);
        // Create Controller
        new ChatWindowController(textField);
    }

    /**
     * Updates the server chat window in the game scene. The server chat window
     * is NOT meant to display chat messages. (There is a separate chat window
     * for chat messages)
     */
    public synchronized void updateInGameServerWindow(String message) {
        // Log information
        GameStart.mainLogger.getLOGGER().finest(message);
        // Display it in the game
        Platform.runLater(() -> {
            GameStart.mainLogger.getLOGGER().fine(message);
            if (serverChat == null || message == null)
                return;
            serverChat.appendText(message + "\n");
        });
    }

    /**
     * Updates the client chat window in the game scene. The server chat window
     * is NOT meant to display any server messages. (There is a separate window
     * for server messagess)
     */
    public void updateInGameClientChatWindow(String message) {
        if (clientChat == null)
            return;
        clientChat.appendText(message + "\n");
    }

    /**
     * Creates a text-chat-area window and a label.
     *
     * @param length The length of the chat window.
     * @param height The height of the chat window.
     * @param xCord  The start x-position.
     * @param yCord  The start y-position.
     * @return text area
     */
    private TextArea createTextArea(double length, double height, double xCord, double yCord, String labelName,
                                    String startMessage, int fontSize) {
        Label label = new Label(labelName);
        TextArea chatWindow = new TextArea();
        chatWindow.appendText(startMessage + "\n");
        chatWindow.setMinSize(length, height);
        chatWindow.setMaxSize(length, height);
        // Set position
        chatWindow.relocate(xCord, yCord);
        label.relocate(xCord + length / 4, yCord - 50);
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(2.0);
        dropShadow.setOffsetX(-3.0);
        dropShadow.setOffsetY(4.0);
        dropShadow.setColor(Color.color(0, 0, 0, 0.5));
        chatWindow.setWrapText(true);
        chatWindow.setEditable(false);
        layout.getStylesheets().add(ResourcePointer.class.getResource("Tutorial.css").toExternalForm());
        Font chatFont = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                length / fontSize);
        Font chatLabel = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                length / 12);
        chatWindow.setFont(chatFont);
        label.setFont(chatLabel);
        label.setEffect(dropShadow);
        chatWindow.setEffect(dropShadowHexagon);
        // Add to the scene
        layout.getChildren().addAll(chatWindow, label);
        return chatWindow;
    }

    /**
     * Changes the texture of a hexagon to a port texture and relocates the
     * portStreetButton;
     */
    private void changeHexagonToPortTexture(String coordinate, Button portStreetButton, Button streetButton) {
        coordinate = coordinate.charAt(1) + "";
        Vector2D<Integer> vec = WorldTranslation.getLetterToPosition(coordinate);
        HexagonField hexagonField = (HexagonField) worldMatrix[vec.x][vec.y][0];
        Button hexagonFieldButton = buttonsOfTheWorldMatrix.get(hexagonField);
        Polygon polygon = (Polygon) hexagonFieldButton.getGraphic();
        String fileNameToRead = "Port.png";
        Image hexagonTexture = new Image(HexagonTexturePointer.class.getResourceAsStream(fileNameToRead),
                hexagonRadius * 2, hexagonRadius * 2, true, true);
        polygon.setFill((new ImagePattern(hexagonTexture, 0, 0, 1, 1, true)));
        // Set offset for portButton
        // Define offset and position for the button
        double basePosX = hexagonFieldButton.getLayoutX() + hexagonRadius - portStreetButton.getMinWidth() / 2;
        double basePosY = hexagonFieldButton.getLayoutY() + hexagonRadius - portStreetButton.getMinHeight() / 2;
        double offsetX = (basePosX - streetButton.getLayoutX()) * 6 / 10;
        double offsetY = (basePosY - streetButton.getLayoutY()) * 6 / 10;
        portStreetButton.relocate(basePosX - offsetX, basePosY - offsetY);
        // Set rotation of the button
        portStreetButton.setRotate(streetButton.getRotate());
        portStreetButton.setId("port");
    }

    /**
     * Creates avatar buttons for each team/fraction
     *
     * @param startX The X-Coordinate of the start-to-draw-left point
     * @param startY The Y-Coordinate of the start-to-draw-left point
     * @param size   The size of the button
     */
    private void createAvatars(double startX, double startY, double size) {
        int i = 1;
        defaultAvatarYPos=startY;
        for (Player player : GameStart.siedlerVonCatan.getPlayers()) {
            // Load avatar image
            Image avatarImage = new Image(
                    ResourcePointer.class.getResourceAsStream("Avatar" + player.getTeam().toString() + ".png"));
            Circle imageHolder = new Circle(size);
            Button avatar = new Button();
            playerToAvatar.put(player, avatar);
            // Set image
            avatar.setGraphic(imageHolder);
            imageHolder.setFill((new ImagePattern(avatarImage, 0, 0, 1, 1, true)));
            avatar.setEffect(dropShadowHexagon);
            avatar.setId("avatar");
            // Position avatar
            double xCord = startX - i * 2 * size + size / 4;
            avatar.relocate(xCord, startY);
            if(!player.getStatus().equals("Warten")&&!player.getStatus().equals("Wartet auf Spielbeginn")) avatar.relocate(xCord, startY-10);
            PlayerTeam teamColor = null;
            // Add team label below the avatar
            String name = "";
            name = player.getTeam().toString();
            teamColor = player.getTeam();
            Button teamIndicator = new Button(name);
            Font labelFont = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                    size / 4);
            teamIndicator.setFont(labelFont);
            teamIndicator.setMinWidth(size * 1.5);
            teamIndicator.setMaxWidth(size * 1.5);
            // Disable mouse on label
            teamIndicator.setMouseTransparent(true);
            teamIndicator.relocate(xCord + size / 2, startY + 2 * size + 20);
            if (GameStart.network.getConnectionHandler().getPlayerColor(GameStart.network
            		.getConnectionHandler().getPlayerId()).equals(teamColor)){
            	teamIndicator.setText("You");
            	teamIndicator.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
                teamIndicator.setTextFill(Color.WHITE);
            }
            // Add to layout
            layout.getChildren().addAll(avatar, teamIndicator);
            inGameController.defineAvatarMouseActions(avatar, teamColor);
            ++i;
        }
        avatarsCreated=true;
    }

    /**
     * Creates a drop down menu for an avatar / player.
     *
     * @param teamColor
     */
    public void createAvatarDropDownMenu(PlayerTeam teamColor) {
        // Set up font
        Font basicFont = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                hexagonRadius / 4);
        // Remove old menus if existing
        removeMenus();
        // Create new menu
        dropDownMenu = new VBox(5);
        dropDownMenu.setAlignment(Pos.CENTER);
        dropDownMenu.setBackground(new Background(new BackgroundFill(Color.GRAY, null, null)));
        dropDownMenu.setEffect(dropShadowHexagon);
        dropDownMenu.setOpacity(0.7);
        Player player = GameStart.siedlerVonCatan.findPlayerByColor(teamColor);
        Button nameButton = new Button(player.getName());
        nameButton.setId("MenuButton2");
        nameButton.setMouseTransparent(true);
        nameButton.setFont(basicFont);
        Label pointsLabel = new Label("Victory points: " + player.getVictoryPoints());
        pointsLabel.setFont(basicFont);
        pointsLabel.setTextFill(Color.WHITE);
        nameButton.setTextFill(Color.GREEN);
        Color color = getColorFromTeamColor(teamColor);
        nameButton.setTextFill(color);
        Label developmentCards = new Label("Development cards: " + player.getTotalNumberOfDevelopmentCards());
        developmentCards.setFont(basicFont);
        developmentCards.setTextFill(Color.WHITE);

        Label knights = new Label("Knights: " + player.getNumberOfKnights());
        knights.setFont(basicFont);
        knights.setTextFill(Color.WHITE);
        Label labelStatus = new Label(player.getStatus());
        labelStatus.setFont(basicFont);
        labelStatus.setTextFill(Color.WHITE);
        // Add all nodes but "resourceLabel" & "tradePlayerButton" (already
        // added above)
        dropDownMenu.getChildren().addAll(nameButton, labelStatus,pointsLabel);
        GameStart.mainLogger.getLOGGER().fine("----------------------");
        Label resourceLabel = new Label("Resources: " + player.getResources().get(ResourceType.HIDDEN));
        GameStart.mainLogger.getLOGGER().fine(teamColor.toString() + "_" + player.getResources().get(ResourceType.HIDDEN));
        GameStart.mainLogger.getLOGGER().fine(player.getResources().toString());
        Button tradePlayerButton = new Button("Player trade");
        tradePlayerButton.setId("MenuButton");
        tradePlayerButton.setFont(basicFont);
        resourceLabel.setFont(basicFont);
        resourceLabel.setTextFill(Color.WHITE);
        tradePlayerButton.setTextFill(Color.WHITE);
        inGameController.defineTradeButtonActions(tradePlayerButton, teamColor, player);
        // Define button action
        inGameController.defineActionToShowPlayerTradingWindow(tradePlayerButton, teamColor);


        dropDownMenu.getChildren().addAll(resourceLabel, developmentCards, knights);

        if(player.hasLargestArmy()) {
          Label labelArmy = new Label("<Largest Army>");
          labelArmy.setFont(basicFont);
          dropDownMenu.getChildren().add(labelArmy);
            labelArmy.setTextFill(Color.WHITE);
        }
        if(player.hasLongestRoad()) {
            Label labelRoad = new Label("<Longest Road>");
            labelRoad.setFont(basicFont);
            dropDownMenu.getChildren().add(labelRoad);
            labelRoad.setTextFill(Color.WHITE);
        }
        dropDownMenu.getChildren().addAll(tradePlayerButton);

        if (GameStart.network.getConnectionHandler()
                .getPlayerColor(GameStart.network.getConnectionHandler().getPlayerId()).equals(teamColor)) {
            Button tradeBank = new Button("Trade 4 : 1");
            tradeBank.setId("MenuButton");
            tradeBank.setFont(basicFont);
            tradeBank.setTextFill(Color.WHITE);
            dropDownMenu.getChildren().add(tradeBank);
            // Define button action
            inGameController.defineActionToShowBasicBankTradingWindow(tradeBank);
        }
        // Height and Width
        double sceneWidth = mainGameScene.getWidth();
        double sceneHeight = mainGameScene.getHeight();
        dropDownMenu.setMinWidth(sceneWidth / 10);
        dropDownMenu.setMaxWidth(sceneWidth / 10);
        dropDownMenu.relocate(sceneWidth - hexagonRadius * 4.9, sceneHeight / 4.75);
        layout.getChildren().add(dropDownMenu);
    }

    /**
     * Draws interface for bank trading
     */
    public void drawBankTrading(PortTypes portType) {
        // Set up font
        Font basicFont = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                hexagonRadius / 4);
        // Remove old menu if existing
        if (tradingWindow != null && layout.getChildren().contains(tradingWindow))
            layout.getChildren().remove(tradingWindow);
        // Remove monopoly window
        if (monopolyWindow != null && layout.getChildren().contains(monopolyWindow))
            layout.getChildren().remove(monopolyWindow);
        if (yearOfPleantyWindow != null && layout.getChildren().contains(yearOfPleantyWindow))
            layout.getChildren().remove(yearOfPleantyWindow);
        // Create new menu
        tradingWindow = new VBox(10);
        tradingWindow.setAlignment(Pos.CENTER);
        tradingWindow.setBackground(new Background(new BackgroundFill(Color.GAINSBORO, null, null)));
        tradingWindow.setEffect(dropShadowHexagon);
        tradingWindow.setOpacity(0.7);
        Button nameButton = new Button(portType.toString());
        nameButton.setId("MenuButton");
        nameButton.setMouseTransparent(true);
        nameButton.setFont(basicFont);
        Button sendTradeButton = new Button("Send trade");
        sendTradeButton.setId("MenuButton");
        sendTradeButton.setFont(basicFont);
        // Define send trade button action
        Button cancelTradeButton = new Button("Cancel");
        cancelTradeButton.setId("MenuButton");
        cancelTradeButton.setFont(basicFont);
        Label offerLabel = new Label("You offer:");
        offerLabel.setFont(basicFont);
        tradingWindow.getChildren().addAll(nameButton, offerLabel);
        RadioButton grainButtonOffer = new RadioButton("Grain");
        RadioButton woolButtonOffer = new RadioButton("Wool");
        RadioButton woodButtonOffer = new RadioButton("Wood");
        RadioButton stoneButtonOffer = new RadioButton("Stone");
        RadioButton loamButtonOffer = new RadioButton("Loam");
        loamButtonOffer.setFont(basicFont);
        grainButtonOffer.setFont(basicFont);
        woolButtonOffer.setFont(basicFont);
        stoneButtonOffer.setFont(basicFont);
        woodButtonOffer.setFont(basicFont);
        // Add to group
        ToggleGroup groupOffer = new ToggleGroup();
        grainButtonOffer.setToggleGroup(groupOffer);
        woolButtonOffer.setToggleGroup(groupOffer);
        woodButtonOffer.setToggleGroup(groupOffer);
        stoneButtonOffer.setToggleGroup(groupOffer);
        loamButtonOffer.setToggleGroup(groupOffer);
        switch (portType) {
            case NONE:
                tradingWindow.getChildren().addAll(grainButtonOffer, woolButtonOffer, woodButtonOffer, stoneButtonOffer,
                        loamButtonOffer);
                break;
            case THREE_FOR_ONE:
                tradingWindow.getChildren().addAll(grainButtonOffer, woolButtonOffer, woodButtonOffer, stoneButtonOffer,
                        loamButtonOffer);
                break;
            case TWO_GRAIN_FOR_ONE:
                tradingWindow.getChildren().add(grainButtonOffer);
                grainButtonOffer.setSelected(true);
                grainButtonOffer.setDisable(true);
                break;
            case TWO_LOAM_FOR_ONE:
                tradingWindow.getChildren().add(loamButtonOffer);
                loamButtonOffer.setSelected(true);
                loamButtonOffer.setDisable(true);
                break;
            case TWO_STONE_FOR_ONE:
                tradingWindow.getChildren().add(stoneButtonOffer);
                stoneButtonOffer.setSelected(true);
                stoneButtonOffer.setDisable(true);
                break;
            case TWO_WOOD_FOR_ONE:
                tradingWindow.getChildren().add(woodButtonOffer);
                woodButtonOffer.setSelected(true);
                woodButtonOffer.setDisable(true);
                break;
            case TWO_WOOL_FOR_ONE:
                tradingWindow.getChildren().add(woolButtonOffer);
                woolButtonOffer.setSelected(true);
                woolButtonOffer.setDisable(true);
                break;
            default:
                break;
        }
        Label requestingLabel = new Label("You request:");
        requestingLabel.setFont(basicFont);
        RadioButton grainButton = new RadioButton("Grain");
        RadioButton woolButton = new RadioButton("Wool");
        RadioButton woodButton = new RadioButton("Wood");
        RadioButton stoneButton = new RadioButton("Stone");
        RadioButton loamButton = new RadioButton("Loam");
        loamButton.setFont(basicFont);
        grainButton.setFont(basicFont);
        woolButton.setFont(basicFont);
        stoneButton.setFont(basicFont);
        woodButton.setFont(basicFont);
        // Add to group
        ToggleGroup group = new ToggleGroup();
        grainButton.setToggleGroup(group);
        woolButton.setToggleGroup(group);
        woodButton.setToggleGroup(group);
        stoneButton.setToggleGroup(group);
        loamButton.setToggleGroup(group);
        // Height and Width
        double sceneWidth = mainGameScene.getWidth();
        double sceneHeight = mainGameScene.getHeight();
        // Size
        tradingWindow.setMinWidth(sceneWidth / 10);
        tradingWindow.setMaxWidth(sceneWidth / 10);
        // Relocate
        tradingWindow.relocate(sceneWidth - hexagonRadius * 4.9,
                sceneHeight / 4.75);
//				sceneHeight / 5 + (dropDownMenu != null ? dropDownMenu.getHeight() : 0) + 10);
        // Add to layout
        tradingWindow.getChildren().addAll(requestingLabel, grainButton, woolButton, woodButton, stoneButton,
                loamButton, sendTradeButton, cancelTradeButton);
        layout.getChildren().add(tradingWindow);
        // Define send button action
        inGameController.defineBankTradingButton(sendTradeButton, portType, grainButton, woolButton, woodButton,
                stoneButton, loamButton, grainButtonOffer, woolButtonOffer, woodButtonOffer, stoneButtonOffer,
                loamButtonOffer, cancelTradeButton);
    }

    /**
     * Draws a trading window for trading with players.
     *
     * @param color The color of the player we want to trade with.
     */
    public void drawPlayerTrading(PlayerTeam color) {
        // Set up font
        Font basicFont = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                hexagonRadius / 4);
        // Remove old menu if existing
        if (tradingWindow != null && layout.getChildren().contains(tradingWindow))
            layout.getChildren().remove(tradingWindow);
        // Create new menu
        tradingWindow = new VBox(10);
        tradingWindow.setAlignment(Pos.CENTER);
        tradingWindow.setBackground(new Background(new BackgroundFill(Color.GAINSBORO, null, null)));
        tradingWindow.setEffect(dropShadowHexagon);
        tradingWindow.setOpacity(0.7);
        Button nameButton = new Button("Player Trade");
        nameButton.setId("MenuButton2");
        nameButton.setMouseTransparent(true);
        nameButton.setFont(basicFont);
        nameButton.setTextFill(Color.WHITE);
        Button sendTradeButton = new Button("Send trade");
        sendTradeButton.setId("MenuButton");
        sendTradeButton.setFont(basicFont);
        // Define send trade button action
        Button cancelTradeButton = new Button("Cancel");
        cancelTradeButton.setId("MenuButton");
        cancelTradeButton.setFont(basicFont);
        Label offerLabel = new Label("You offer:");
        offerLabel.setFont(basicFont);
        tradingWindow.getChildren().addAll(nameButton, offerLabel);
        Slider grainSliderOffer = defineTradingSliderPropperties(ResourceType.GRAIN, false);
        Slider woodSliderOffer = defineTradingSliderPropperties(ResourceType.WOOD, false);
        Slider woolSliderOffer = defineTradingSliderPropperties(ResourceType.WOOL, false);
        Slider stoneSliderOffer = defineTradingSliderPropperties(ResourceType.STONE, false);
        Slider loamSliderOffer = defineTradingSliderPropperties(ResourceType.LOAM, false);
        //
        Label requestingLabel = new Label("You request:");
        requestingLabel.setFont(basicFont);
        tradingWindow.getChildren().add(requestingLabel);
        Slider grainSliderRequest = defineTradingSliderPropperties(ResourceType.GRAIN, true);
        Slider woodSliderRequest = defineTradingSliderPropperties(ResourceType.WOOD, true);
        Slider woolSliderRequest = defineTradingSliderPropperties(ResourceType.WOOL, true);
        Slider stoneSliderRequest = defineTradingSliderPropperties(ResourceType.STONE, true);
        Slider loamSliderRequest = defineTradingSliderPropperties(ResourceType.LOAM, true);
        // Height and Width
        double sceneWidth = mainGameScene.getWidth();
        double sceneHeight = mainGameScene.getHeight();
        // Size
        tradingWindow.setMinWidth(sceneWidth * 1.5 / 10);
        tradingWindow.setMaxWidth(sceneWidth * 1.5 / 10);
        // Relocate
        tradingWindow.relocate(sceneWidth - hexagonRadius * 4.9,
                sceneHeight / 4.75);
        // Add to layout
        tradingWindow.getChildren().addAll(sendTradeButton, cancelTradeButton);
        layout.getChildren().add(tradingWindow);
        // Define send button action
        inGameController.definePlayerTradingButtonAction(grainSliderOffer, woodSliderOffer, woolSliderOffer,
                stoneSliderOffer, loamSliderOffer, grainSliderRequest, woodSliderRequest, woolSliderRequest,
                stoneSliderRequest, loamSliderRequest, sendTradeButton, cancelTradeButton);
    }

    private Slider defineTradingSliderPropperties(ResourceType type, boolean request) {
        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER);
        Font smallFont = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                hexagonRadius / 6);
        // Get player
        Player player = GameStart.siedlerVonCatan
                .findPlayerByID(GameStart.network.getConnectionHandler().getPlayerId());
        Label label = new Label(type.toString());
        label.setFont(smallFont);
        Label valueLabel = new Label("0");
        valueLabel.setFont(smallFont);
        Slider slider = new Slider(0, request ? 15 : player.getNumberOfResource(type), 0);
        slider.setMaxWidth(mainGameScene.getWidth() / 15);
        slider.setMaxWidth(mainGameScene.getWidth() / 15);
        slider.setBlockIncrement(1f);
        slider.valueProperty().addListener((obs, oldval, newVal) -> {
            slider.setValue(Math.round(newVal.doubleValue()));
            int value = (int) slider.getValue();
            valueLabel.setText("" + value);
        });
        hbox.getChildren().addAll(label, slider, valueLabel);
        tradingWindow.getChildren().add(hbox);
        return slider;
    }

    /**
     * Fades the trading request window away (if open)
     */
    public void fadeTradingRequestAway() {
        if (tradingWindow != null && layout.getChildren().contains(tradingWindow)) {
            FadeTransition ftFadeIn = new FadeTransition(Duration.millis(200), tradingWindow);
            ftFadeIn.setFromValue(1.0);
            ftFadeIn.setToValue(0.0);
            ftFadeIn.play();
            ftFadeIn.setOnFinished(e -> {
                layout.getChildren().remove(tradingWindow);
                tradingWindow = null;
            });
        }
    }

    /**
     * Fades away the player trading window.
     */
    public void fadeAwayPlayerTradingWindow() {
        if (playerTradingWindow != null && layout.getChildren().contains(playerTradingWindow)) {
            FadeTransition ftFadeIn = new FadeTransition(Duration.millis(200), playerTradingWindow);
            ftFadeIn.setFromValue(1.0);
            ftFadeIn.setToValue(0.0);
            ftFadeIn.play();
            ftFadeIn.setOnFinished(e -> {
                layout.getChildren().remove(playerTradingWindow);
                playerTradingWindow = null;
            });
        }
    }

    public void fadeAwayAvatarDropDownMenu() {
        fadeTradingRequestAway();
        if (dropDownMenu != null && layout.getChildren().contains(dropDownMenu)) {
            FadeTransition ftFadeIn = new FadeTransition(Duration.millis(200), dropDownMenu);
            ftFadeIn.setFromValue(1.0);
            ftFadeIn.setToValue(0.0);
            ftFadeIn.play();
            ftFadeIn.setOnFinished(e -> {
                layout.getChildren().remove(dropDownMenu);
                dropDownMenu = null;
            });
        }
    }

    /**
     * Fades the monopoly request window away (if open)
     */
    public void fadeMonopolyAway() {
        if (monopolyWindow != null && layout.getChildren().contains(monopolyWindow)) {
            FadeTransition ftFadeIn = new FadeTransition(Duration.millis(650), monopolyWindow);
            ftFadeIn.setFromValue(1.0);
            ftFadeIn.setToValue(0.0);
            ftFadeIn.play();
            ftFadeIn.setOnFinished(e -> {
                layout.getChildren().remove(monopolyWindow);
                monopolyWindow = null;
            });
        }
    }

    /**
     * Fades the monopoly request window away (if open)
     */
    public void fadeYearOfPleantyAway() {
        if (yearOfPleantyWindow != null && layout.getChildren().contains(yearOfPleantyWindow)) {
            FadeTransition ftFadeIn = new FadeTransition(Duration.millis(650), yearOfPleantyWindow);
            ftFadeIn.setFromValue(1.0);
            ftFadeIn.setToValue(0.0);
            ftFadeIn.play();
            ftFadeIn.setOnFinished(e -> {
                layout.getChildren().remove(yearOfPleantyWindow);
                yearOfPleantyWindow = null;
            });
        }
    }

    /**
     * Creates a game set up scene to configure different in-game options (Like
     * color, ...)
     */
    public void showGameSetUp(boolean isOnline) {
        GameSetUp setUp = new GameSetUp();
        swapScenes(setUp.getGameSetUpLayout(isOnline));
    }

    /**
     * Creates a tutorial scene and displays the game rules
     */
    public void showGameRules() {
        Tutorial tutorial = new Tutorial();
        swapScenes(tutorial.getTutorialLayout(this));
    }

    /**
     * Creates a gameover screen
     */
    public void showGameOverScreen() {
        GameOverScreen gameOver = new GameOverScreen();
        swapScenes(gameOver.getGameOverLayout(this));
    }

    /**
     * Creates a victory screen
     */
    public void showVictoryScreen() {
        VictoryScreen victoryScreen = new VictoryScreen();
        swapScenes(victoryScreen.getVictoryLayout(this));
    }

    /**
     * Creates a pause scene
     */
    public void showPauseScreen() {
        PauseScreen pauseScreen = new PauseScreen();
        swapScenes(pauseScreen.getPauseLayout(this));
        pauseScreen.getResume().setOnMouseClicked(e -> {
            swapScenes(layout);
        });
    }

    /**
     * Creates a magnifyingGlass CHANGE HERE !!!!
     */
    public void showMagnifyingGlass() {
        GameStart.mainLogger.getLOGGER().fine("showMagnifyingGlass...?");
    }

    /**
     * Changes the primary's scene content.
     *
     * @param newContent The new content to be added to the scene.
     */
    public void swapScenes(Parent newContent) {
        FadeTransition ft = new FadeTransition(Duration.millis(400), newContent);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
        primaryStage.getScene().setRoot(newContent);
    }

    /**
     * Displays a notification message on the screen/scene.
     *
     * @param notificationMessage The message we want to display.
     */
    public void displayNotificationMessage(String notificationMessage) {
        // Screen screen = Screen.getPrimary();
        double sceneWidth = mainGameScene.getWidth();
        double sceneHeight = mainGameScene.getHeight();
        double length = sceneWidth / 6;
        double height = sceneHeight / 8;
        InGameNotification notification = new InGameNotification(notificationMessage, length, height, sceneWidth / 2,
                sceneHeight - height * 5 / 4);
        Button notificationButton = notification.getNotificationButton();
        layout.getChildren().add(notificationButton);
        // Add fade transition
        addFadeAnimation(notificationButton, 2000);
    }

    /**
     * Displays an "Accept" button.
     *
     * @param o        Must be a street or a waypoint.
     * @param freeCost Can we build for free?
     * @throws Exception Fail, if not street and not waypoint.
     */
    public void displayAcceptButton(Object o, boolean freeCost) throws Exception {
        // Remove old accept button
        if (acceptButton != null) {
            layout.getChildren().remove(acceptButton);
            acceptButton = null;
        }
        acceptButton = new VBox(-40);
        acceptButton.setAlignment(Pos.CENTER);
        // Play a sound
        GameStart.soundManager.playSoundAccept();
        // Screen screen = Screen.getPrimary();
        double sceneWidth = mainGameScene.getWidth();
        double sceneHeight = mainGameScene.getHeight();
        double size = sceneHeight / 6;
        InGameNotification notification = new InGameNotification(null, size, size, 0, 0);
        Button acceptButtonMainIcon = notification.getAcceptButton();
        // Add image
        Image acceptImage = new Image(ResourcePointer.class.getResourceAsStream("Accept.png"));
        Circle imageHolder = new Circle(size / 2.2);
        // Set image
        acceptButtonMainIcon.setGraphic(imageHolder);
        imageHolder.setFill((new ImagePattern(acceptImage, 0, 0, 1, 1, true)));
        acceptButton.relocate(sceneWidth - size * 1.25, sceneHeight - size * 1.25);
        // Create cost label (displaying the cost to build the building)
        Button costButton = new Button("Free");
        Styling.styleButton(costButton, mainGameScene.getHeight() / 65);
        costButton.setTextFill(Color.WHITESMOKE);
        costButton.setMouseTransparent(true);
        // Add to layout
        acceptButton.getChildren().addAll(acceptButtonMainIcon, costButton);
        layout.getChildren().add(acceptButton);
        // Add fade transition
        addFadeAnimation(acceptButton, 6000);
        acceptButton.getChildren().forEach(button -> addFadeAnimation((Button) button, 6000));
        // Dehighlight (if existing)
        dehighlightStreetButton();
        // Assign button action/consequences
        if (o instanceof Street) {
            if (!freeCost && !GameStart.siedlerVonCatan.isRoadBuildingCardPlayed())
                costButton.setText("Wood, Loam");
            inGameController.defineAcceptStreetAction((Street) o, acceptButtonMainIcon);
            // Mark street
            highlightStreetButton((Street) o);
        } else if (o instanceof WayPoint) {
            if (!freeCost) {
                if (((WayPoint) o).getSettlement().getBuildingType() == BuildingType.NONE)
                    costButton.setText("Wood, Loam\nGrain, Wool");
                else
                    costButton.setText("Grain(2), Stone(3)");
            }
            inGameController.defineAcceptWayPointAction((WayPoint) o, acceptButtonMainIcon);
        } else if (o instanceof HexagonField) {
            inGameController.defineAcceptHexagonFieldButton((HexagonField) o, acceptButtonMainIcon);
            costButton.setText("Set Thief");
        } else {
            throw new Exception("Type must be Hexagon, Street or WayPoint");
        }
    }

    /**
     * Adds an fadeIn,Idle,and FadeOut animation to a button.
     *
     * @param nodeToChange The button we want to fade.
     * @param idleTime     The time we want it to stay idle.
     */
    private void addFadeAnimation(Node nodeToChange, int idleTime) {
        // Add FadeIn, Idle & FadeOut animations
        FadeTransition ftFadeIn = new FadeTransition(Duration.millis(650), nodeToChange);
        ftFadeIn.setFromValue(0.0);
        ftFadeIn.setToValue(1.0);
        ftFadeIn.play();
        ftFadeIn.setOnFinished(e -> {
            FadeTransition ftIdle = new FadeTransition(Duration.millis(idleTime), nodeToChange);
            ftIdle.setFromValue(1.0);
            ftIdle.setToValue(1.0);
            ftIdle.play();
            ftIdle.setOnFinished(e2 -> {
                FadeTransition ftFadeOut = new FadeTransition(Duration.millis(400), nodeToChange);
                ftFadeOut.setFromValue(1.0);
                ftFadeOut.setToValue(0.0);
                ftFadeOut.play();
                ftFadeOut.setOnFinished(e3 -> {
                    if (layout.getChildren().contains(nodeToChange))
                        layout.getChildren().remove(nodeToChange);
                });
            });
        });
    }

    /**
     * Draws the thief on the screen.
     *
     * @param thiefPosition This is the thiefs new position
     */
    public void drawThief(Vector2D<Integer> thiefPosition) {
        if (thiefAvatar != null && layout.getChildren().contains(thiefAvatar))
            layout.getChildren().remove(thiefAvatar);
        // Load thief image
        Image image = new Image(ResourcePointer.class.getResourceAsStream("Thief.png"));
        // Get the corresponding hexagon field
        HexagonField hexagonField = (HexagonField) worldMatrix[thiefPosition.x][thiefPosition.y][0];
        // Size of the thief avatar button
        double size = hexagonRadius / 3;
        // First get the corresponding button of the hexagon field
        thiefAvatar = new Button();
        thiefAvatar.setMouseTransparent(true);
        Circle imageHolder = new Circle(size);
        // Then set the graphic image
        thiefAvatar.setGraphic(imageHolder);
        imageHolder.setFill((new ImagePattern(image, 0, 0, 1, 1, true)));
        thiefAvatar.setId("avatar");
        // Relocate image
        thiefAvatar.relocate(hexagonField.getScenePosition().x + hexagonRadius * 6 / 7 - size,
                hexagonField.getScenePosition().y + hexagonRadius - size);
        // Add to the scene view
        layout.getChildren().add(thiefAvatar);
    }

    // Getters

    /**
     * Returns our main game scene
     *
     * @return mainGameScene
     */
    public Scene getMainGameScene() {
        return mainGameScene;
    }

    /**
     * Getter for the primary stage.
     *
     * @return primaryStage
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Getter for the matrix of all GameMatrixEntries from the gameView. Meaning
     * all waypoints and hexagonfields
     *
     * @return worldMatrix
     */
    public GameMatrixEntry[][][] getActivGameMatrixEntries() {
        return worldMatrix;
    }

    /**
     * Getter for the list of all streets
     *
     * @return streets
     */
    public ArrayList<Street> getStreets() {
        return streets;
    }

    /**
     * @return the wayPointsList
     */
    public ArrayList<WayPoint> getWayPointsList() {
        return wayPointsList;
    }

    /**
     * Getter for the hash map, which contains all waypoints and hexagons with
     * their button
     *
     * @return buttonsOfTheWorldMatrix
     */
    public HashMap<GameMatrixEntry, Button> getButtonsOfTheWorldMatrix() {
        return buttonsOfTheWorldMatrix;
    }

    /**
     * Getter for the hash map, which contains every street with their button
     *
     * @return buttonsOfTheStreets
     */
    public HashMap<Street, Button> getButtonsOfTheStreets() {
        return buttonsOfTheStreets;
    }

    /**
     * Setter for the primary stage
     *
     * @param primaryStage The new stage to assign as primary stage.
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Removes the accept button from the layout
     */
    public void removeAcceptButton() {
        if (acceptButton != null && layout.getChildren().contains(acceptButton))
            layout.getChildren().remove(acceptButton);
    }

    /**
     * Changes the texture of the associated button.
     *
     * @param button The associated button.
     * @param color
     */
    public void changeButtonImage(Button button, BuildingType type, PlayerTeam color) {
        try {
            if (color == null || type == null) {
                GameStart.mainLogger.getLOGGER().fine(color + "_" + type);
                return;
            }
            String fileName;
            String colorName;
            Image image;
            // Select color
            switch (color) {
                case TEAM_BLUE:
                    colorName = "Blue";
                    break;
                case TEAM_ORANGE:
                    colorName = "Orange";
                    break;
                case TEAM_RED:
                    colorName = "Red";
                    break;
                case TEAM_WHITE:
                    colorName = "White";
                    break;
                default:
                    colorName = "Default";
                    break;
            }
            if (PlayerTeam.NONE != color && BuildingType.STREET == type) {
                button.setMouseTransparent(true);
                button.setOpacity(0.9);
            }
            // Select building type
            switch (type) {
                case CASTLE:
                    fileName = "Castle" + colorName + ".png";
                    Circle imageHolder = new Circle(hexagonRadius / 2.5);
                    // Load image
                    image = new Image(ResourcePointer.class.getResourceAsStream(fileName));
                    // Set image
                    button.setGraphic(imageHolder);
                    imageHolder.setFill((new ImagePattern(image, 0, 0, 1, 1, true)));
                    button.toFront();
                    break;
                case STREET:
                    fileName = "Cobblestone" + colorName + ".png";
                    Rectangle imageHolder1 = new Rectangle(hexagonRadius, hexagonRadius / 5);
                    // Load image
                    image = new Image(ResourcePointer.class.getResourceAsStream(fileName));
                    // Set image
                    button.setGraphic(imageHolder1);
                    ((Rectangle) imageHolder1).setFill((new ImagePattern(image, 0, 0, 1, 1, true)));
                    break;
                case VILLAGE:
                    fileName = "Village" + colorName + ".png";
                    Circle imageHolder2 = new Circle(hexagonRadius / 3.35);
                    // Load image
                    image = new Image(ResourcePointer.class.getResourceAsStream(fileName));
                    // Set image
                    button.setGraphic(imageHolder2);
                    imageHolder2.setFill((new ImagePattern(image, 0, 0, 1, 1, true)));
                    // define action on mouse hovering and entering
                    inGameController.defineButtonBuildingMouseAction(button);
                    button.toFront();
                    break;
                default:
                    GameStart.mainLogger.getLOGGER().fine(color + "_C_" + type);
                    return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // button.setEffect(dropShadowHexagon);
        // button.setId("avatar");
    }

    /**
     * Visualizes the results of the diceThrow
     *
     * @param diceThrows The two results of the dice
     */
    public void showDiceThrow(int[] diceThrows) {
        if (layout == null)
            return;
        // Play a random sound
        GameStart.soundManager.playRandomDiceRollingSound();
        // Show dice throw
        for (int i = 0; i < diceThrows.length; i++) {
            int diceThrow = diceThrows[i];
            String number = null;
            switch (diceThrow) {
                case 1:
                    number = "One";
                    break;
                case 2:
                    number = "Two";
                    break;
                case 3:
                    number = "Three";
                    break;
                case 4:
                    number = "Four";
                    break;
                case 5:
                    number = "Five";
                    break;
                case 6:
                    number = "Six";
                    break;
                default:
                    break;
            }
            String fileName = "Dice" + number + ".png";
            // Load matching image from resources
            Image image = new Image(ResourcePointer.class.getResourceAsStream(fileName));
            Rectangle imageHolder = new Rectangle(hexagonRadius, hexagonRadius);
            imageHolder.setFill((new ImagePattern(image, 0, 0, hexagonRadius, hexagonRadius, false)));
            imageHolder.relocate(mainGameScene.getWidth() / 8 + i * hexagonRadius * 3 / 2,
                    mainGameScene.getHeight() - 2 * hexagonRadius);
            imageHolder.setEffect(dropShadowHexagon);
            layout.getChildren().add(imageHolder);
            addFadeAnimation(imageHolder, 3500);
        }
    }

    /**
     * Draws all the (available) resource cards to screen.
     */
    public void updateResourceCardView() {
        if (resourceCardHbox != null && layout.getChildren().contains(resourceCardHbox))
            layout.getChildren().remove(resourceCardHbox);
        // Display cards
        resourceCardHbox = new HBox(10);
        resourceCardHbox.setAlignment(Pos.CENTER_RIGHT);
        layout.getChildren().add(resourceCardHbox);
        resourceCardHbox.relocate(mainGameScene.getWidth() * 2.7 / 4, mainGameScene.getHeight() * 4 / 5);
        Player player = GameStart.siedlerVonCatan
                .findPlayerByID(GameStart.network.getConnectionHandler().getPlayerId());
        for (ResourceType type : ResourceType.values()) {
            int count = player.getNumberOfResource(type);
            if (count <= 0 || type == ResourceType.HIDDEN)
                continue;
            ResourceCard card = new ResourceCard(type);
            card.getCardView().updateCountLabel(count);
            resourceCardHbox.getChildren().add(card.getCardView().getLayout());
        }
    }

    public void updateDevelopmentCardView() {
        if (developmentCardHbox != null && layout.getChildren().contains(developmentCardHbox))
            layout.getChildren().remove(developmentCardHbox);
        developmentCardHbox = new HBox(10);
        developmentCardHbox.setAlignment(Pos.CENTER_RIGHT);
        // Button but1 = new Button();
        // Button but2 = new Button();
        // Button but3 = new Button();
        // Button but4 = new Button();
        // Button but5 = new Button();
        // developmentCardHbox.getChildren().addAll(but1,but2,but3,but4,but5);
        layout.getChildren().add(developmentCardHbox);
        developmentCardHbox.relocate(mainGameScene.getWidth() * 2.7 / 4, mainGameScene.getHeight() * 3 / 5);
        Player player = GameStart.siedlerVonCatan
                .findPlayerByID(GameStart.network.getConnectionHandler().getPlayerId());
        HashMap<EvolutionType,Integer> cardTypes = (HashMap<EvolutionType, Integer>) player.getEvolutionCards().clone();

        for (EvolutionType type : cardTypes.keySet()) {
            EvolutionCard card = new EvolutionCard(type);
            card.getCardView().updateCountLabel(player.getEvolutionCards().get(type));
            developmentCardHbox.getChildren().add(card.getCardView().getLayout());
            // Deactivate button if we already played a development card
            card.getCardView().getCardButton().setDisable(GameStart.siedlerVonCatan
                    .findPlayerByID(GameStart.network.getConnectionHandler().getPlayerId()).hasPlayedDevelopmentCard());
        }
    }

    /**
     * Creates a menu to for the player to chose which resourcetype to request
     */
    public void drawOptionsForMonopoly() {
        // Set up font
        Font basicFont = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                hexagonRadius / 4);
        // Remove old (and other) menu(s) if existing
        removeMenus();
        // Create new menu
        monopolyWindow = new VBox(10);
        monopolyWindow.setAlignment(Pos.CENTER);
        monopolyWindow.setBackground(new Background(new BackgroundFill(Color.GAINSBORO, null, null)));
        monopolyWindow.setEffect(dropShadowHexagon);
        monopolyWindow.setOpacity(0.7);
        Button sendMonopolyButton = new Button("Send monopoly");
        sendMonopolyButton.setId("MenuButton");
        sendMonopolyButton.setFont(basicFont);
        Button cancelTradeButton = new Button("Cancel");
        cancelTradeButton.setId("MenuButton");
        cancelTradeButton.setFont(basicFont);
        Label requestingLabel = new Label("You request:");
        requestingLabel.setFont(basicFont);
        RadioButton grainButton = new RadioButton("Grain");
        RadioButton woolButton = new RadioButton("Wool");
        RadioButton woodButton = new RadioButton("Wood");
        RadioButton stoneButton = new RadioButton("Stone");
        RadioButton loamButton = new RadioButton("Loam");
        loamButton.setFont(basicFont);
        grainButton.setFont(basicFont);
        woolButton.setFont(basicFont);
        stoneButton.setFont(basicFont);
        woodButton.setFont(basicFont);
        // Add to group
        ToggleGroup group = new ToggleGroup();
        grainButton.setToggleGroup(group);
        woolButton.setToggleGroup(group);
        woodButton.setToggleGroup(group);
        stoneButton.setToggleGroup(group);
        loamButton.setToggleGroup(group);
        // Height and Width
        double sceneWidth = mainGameScene.getWidth();
        double sceneHeight = mainGameScene.getHeight();
        // Size
        monopolyWindow.setMinWidth(sceneWidth / 10);
        monopolyWindow.setMaxWidth(sceneWidth / 10);
        // Relocate
        monopolyWindow.relocate(sceneWidth - hexagonRadius * 4.2567837, sceneHeight / 5);
        // Add to layout
        monopolyWindow.getChildren().addAll(requestingLabel, grainButton, woolButton, woodButton, stoneButton,
                loamButton, sendMonopolyButton, cancelTradeButton);
        layout.getChildren().add(monopolyWindow);
        // Define send button action
        inGameController.defineMonopolyButtons(sendMonopolyButton, grainButton, woolButton, woodButton, stoneButton,
                loamButton, cancelTradeButton);
    }

    /**
     * Creates a menu to for the player to chose which resourcetype to request.
     * The first input for firstResource is null, and then when we've chosen the
     * first resource this method will be called again, creating a menu again,
     * but now with the resource we chose before as firstResource
     *
     * @param firstResource
     */
    public void drawOptionsForYearOfPleanty(ResourceType firstResource) {
        // Set up font
        Font basicFont = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                hexagonRadius / 4);
        // Remove old (and other) menu(s) if existing
        removeMenus();
        // Create new menu
        yearOfPleantyWindow = new VBox(10);
        yearOfPleantyWindow.setAlignment(Pos.CENTER);
        yearOfPleantyWindow.setBackground(new Background(new BackgroundFill(Color.GAINSBORO, null, null)));
        yearOfPleantyWindow.setEffect(dropShadowHexagon);
        yearOfPleantyWindow.setOpacity(0.7);
        Button sendYearOfPleantyButton;
        if (firstResource != null)
            sendYearOfPleantyButton = new Button("Send year of pleanty");
        else
            sendYearOfPleantyButton = new Button("Second resource");
        sendYearOfPleantyButton.setId("MenuButton");
        sendYearOfPleantyButton.setFont(basicFont);
        Button cancelTradeButton = new Button("Cancel");
        cancelTradeButton.setId("MenuButton");
        cancelTradeButton.setFont(basicFont);
        Label requestingLabel;
        if (firstResource != null)
            requestingLabel = new Label("Second resource");
        else
            requestingLabel = new Label("First resource");
        requestingLabel.setFont(basicFont);
        RadioButton grainButton = new RadioButton("Grain");
        RadioButton woolButton = new RadioButton("Wool");
        RadioButton woodButton = new RadioButton("Wood");
        RadioButton stoneButton = new RadioButton("Stone");
        RadioButton loamButton = new RadioButton("Loam");
        loamButton.setFont(basicFont);
        grainButton.setFont(basicFont);
        woolButton.setFont(basicFont);
        stoneButton.setFont(basicFont);
        woodButton.setFont(basicFont);
        // Add to group
        ToggleGroup group = new ToggleGroup();
        grainButton.setToggleGroup(group);
        woolButton.setToggleGroup(group);
        woodButton.setToggleGroup(group);
        stoneButton.setToggleGroup(group);
        loamButton.setToggleGroup(group);
        // Height and Width
        double sceneWidth = mainGameScene.getWidth();
        double sceneHeight = mainGameScene.getHeight();
        // Size
        yearOfPleantyWindow.setMinWidth(sceneWidth / 8);
        yearOfPleantyWindow.setMaxWidth(sceneWidth / 8);
        // Relocate
        yearOfPleantyWindow.relocate(sceneWidth - hexagonRadius * 4.5, sceneHeight / 5);
        // Add to layout
        yearOfPleantyWindow.getChildren().addAll(requestingLabel, grainButton, woolButton, woodButton, stoneButton,
                loamButton, sendYearOfPleantyButton, cancelTradeButton);
        layout.getChildren().add(yearOfPleantyWindow);
        // Define send button action
        inGameController.defineYearOfPleantyButtons(firstResource, sendYearOfPleantyButton, grainButton, woolButton,
                woodButton, stoneButton, loamButton, cancelTradeButton);
    }

    /**
     * Draw a trade notification to the screen.
     */
    public void drawTradeNotification(ReceivedTradeOffer receivedTradeOffer) {
        // Remove if already existing
        if (playerTradingWindow != null && layout.getChildren().contains(playerTradingWindow))
            layout.getChildren().remove(playerTradingWindow);
        TradeNotification tradeNotification = new TradeNotification(receivedTradeOffer);
        playerTradingWindow = tradeNotification.getLayout();
        layout.getChildren().add(playerTradingWindow);
    }

    /**
     * Draws a notification if another player accepted (your) trade request.
     *
     * @param playerID
     * @param tradeID
     */
    public void displayTradeCandidateMessage(Integer tradeID, Integer playerID) {
        // Small pop up notification
        GameStart.gameView.displayNotificationMessage("Trade accepted");
        // Display larger information (add it to accepted trade view list)
        // Screen screen = Screen.getPrimary();
        double sceneWidth = mainGameScene.getWidth();
        double sceneHeight = mainGameScene.getHeight();
        Font font = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                sceneHeight / 55);
        if (acceptedTradeRequestHBox == null) {
            acceptedTradeRequestHBox = new HBox(10);
            acceptedTradeRequestButton = new Button("Traders:");
            acceptedTradeRequestButton.setEffect(dropShadowHexagon);
            acceptedTradeRequestButton.setId("MenuButton");
            acceptedTradeRequestButton.setFont(font);
            acceptedTradeRequestHBox.relocate(sceneWidth * 1 / 20, sceneHeight * 15 / 17);
            acceptedTradeRequestHBox.setAlignment(Pos.CENTER);
            acceptedTradeRequestHBox.getChildren().add(acceptedTradeRequestButton);
            layout.getChildren().add(acceptedTradeRequestHBox);
            inGameController.defineTradersButton(acceptedTradeRequestButton, tradeID);
        }
        // Add player to list
        // TODO: Check if not already there
        // ...
        Player tradePlayer = GameStart.siedlerVonCatan.findPlayerByID(playerID);
        Button playerButton = new Button(tradePlayer.getTeam().toString());
        playerButton.setId("MenuButton2");
        Color color = getColorFromTeamColor(tradePlayer.getTeam());
        playerButton.setTextFill(color);
        playerButton.setEffect(dropShadowHexagon);
        playerButton.setFont(font);
        acceptedTradeRequestHBox.getChildren().add(playerButton);
        inGameController.defineTradePartnerButton(playerButton, tradeID, playerID);
        // Add actions to the button
    }

    // Getters

    /**
     * Getter for the hexagonRadius
     *
     * @return hexagonRadius
     */
    public int getHexagonRadius() {
        return hexagonRadius;
    }

    /**
     * assign the correct color to a team color that was given
     *
     * @param teamColor
     * @return color
     */
    public Color getColorFromTeamColor(PlayerTeam teamColor) {
        Color color = null;
        switch (teamColor) {
            case TEAM_RED:
                color = Color.rgb(206, 14, 20); // red like
                break;
            case TEAM_BLUE:
                color = Color.CORNFLOWERBLUE;
                break;
            case TEAM_WHITE:
                color = Color.WHITESMOKE;
                break;
            case TEAM_ORANGE:
                color = Color.ORANGE;
                break;
		default:
			break;
        }
        return color;
    }

    /**
     * returns the dropShadow Effect
     *
     * @return dropShadowHexagon
     */
    public DropShadow getDropShadowHexagon() {
        return dropShadowHexagon;
    }

    public void disableTradersWindow() {
        layout.getChildren().remove(acceptedTradeRequestHBox);
        acceptedTradeRequestHBox = null;
    }

    /**
     * Creates a window to pick a color to steal from
     */
    public void pickPlayerToStealResources(HashSet<Player> playerSet) {
        VBox vbox = new VBox(10);
        // vbox.setBackground(new Background(new
        // BackgroundFill(Color.gray(0.2902,
        // 0.85), null, null)));
        Font fontBasic = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                mainGameScene.getHeight() / 45);
        Font fontHeader = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                mainGameScene.getHeight() / 30);
        Button header = new Button("Pick target:\n");
        vbox.setId("avatar");
        header.setMouseTransparent(true);
        header.setEffect(dropShadowHexagon);
        vbox.setEffect(dropShadowHexagon);
        header.setFont(fontHeader);
        header.setId("MenuButton");
        vbox.setMinWidth(mainGameScene.getWidth() / 5);
        vbox.setMaxWidth(mainGameScene.getWidth() / 5);
        vbox.getChildren().add(header);
        // Create players
        for (Player player : playerSet) {
            Button button = new Button(player.getTeam().toString());
            button.setId("MenuButton2");
            button.setTextFill(getColorFromTeamColor(player.getTeam()));
            vbox.getChildren().add(button);
            button.setFont(fontBasic);
            button.setEffect(dropShadowHexagon);
            inGameController.definePickThiefTarget(button, player, vbox);
        }
        // Relocate & set up
        vbox.relocate(mainGameScene.getWidth() / 2 - vbox.getMinWidth() / 2, mainGameScene.getHeight() / 3);
        vbox.setAlignment(Pos.CENTER);
        // Add to layout
        layout.getChildren().add(vbox);
    }

    /**
     * Changes the main game background to a thief-wallpaper
     */
    public void changeBackgroundToThief() {
        layout.setBackground(getBackground("ThiefWallpaper.jpg",
                new BackgroundSize(mainGameScene.getWidth(), mainGameScene.getHeight(), false, false, true, false),
                false));
    }

    /**
     * Changes the main game background to a knight-wallpaper
     */
    public void changeBackgroundToKnight() {
        layout.setBackground(getBackground("KnightWallpaper2.jpg",
                new BackgroundSize(mainGameScene.getWidth(), mainGameScene.getHeight(), false, false, true, false),
                false));
    }

    /**
     * Changes the main game background to a streets-wallpaper
     */
    public void changeBackgroundToStreets() {
        layout.setBackground(getBackground("FreeStreetBackground.jpg",
                new BackgroundSize(mainGameScene.getWidth(), mainGameScene.getHeight(), false, false, true, false),
                false));
    }

    /**
     * Changes the main game background to the default background
     */
    public void changeBackgroundToDefault() {
        layout.setBackground(getBackground("InGameBackground.jpg",
                new BackgroundSize(mainGameScene.getWidth(), mainGameScene.getHeight(), false, false, true, false),
                false));
    }

    /**
     * gets layout of this gameView
     *
     * @return layout
     */
    public Pane getLayout() {
        return layout;
    }

    /**
     * Lets the user choose cards to drop (after a 7 has been rolled).
     */
    public void chooseCardsToDrop() {
        DropCardsView dropCardsView = new DropCardsView();
        layout.getChildren().add(dropCardsView.getLayout());
    }

    /**
     * Creates a large persistant message. Must be removed manually somewhere
     * (else) in the code.
     *
     * @param message
     */
    public void displayLargePersistentMessage(String message) {
        // Remove and replace if already existing
        removeLargePersistentMessage();
        // Display notification message
        largeNotificationMessage = new Button(message);
        Font fontBasic = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                mainGameScene.getHeight() / 40);
        largeNotificationMessage.setFont(fontBasic);
        largeNotificationMessage.setEffect(this.getDropShadowHexagon());
        largeNotificationMessage.setMouseTransparent(true);
        layout.getChildren().add(largeNotificationMessage);
        largeNotificationMessage.setMinWidth(mainGameScene.getWidth() / 2);
        largeNotificationMessage.setMaxWidth(mainGameScene.getWidth() / 2);
        largeNotificationMessage.relocate(mainGameScene.getWidth() / 2 - largeNotificationMessage.getMinWidth() / 2,
                mainGameScene.getHeight() / 3.8);
    }

    /**
     * Remove the persistent notification message (Assuming it exists already).
     */
    public void removeLargePersistentMessage() {
        if (largeNotificationMessage != null && layout.getChildren().contains(largeNotificationMessage)) {
            layout.getChildren().remove(largeNotificationMessage);
        }
    }

    /**
     * Displays a (rather large) error message and fades it out automatically
     *
     * @param message
     */
    public void displayLargeErrorMessage(String message) {
        displayLargePersistentMessage(message);
        // largeNotificationMessage is our new message now (but non Persistent
        // this
        // time)
        largeNotificationMessage.setBackground(new Background(new BackgroundFill(Color.PALEVIOLETRED, null, null)));
        // Deactivate after some time
        addFadeAnimation(largeNotificationMessage, 3000);
    }

    /**
     * Deactivates a waypoint's associated button.
     */
    public void activateOrDeactivateWayPointButton(WayPoint wayPoint, boolean activate) {
        getButtonsOfTheWorldMatrix().get(wayPoint).setDisable(!activate);
    }

    /**
     * Activates a street's associated button.
     */
    public void activateStreetButton(Street street) {
        buttonsOfTheStreets.get(street).setDisable(false);
        buttonsOfTheStreets.get(street).setOpacity(0.9);
    }
    /**
     * activates the streets button because of a road building card
     */
    public void activateStreetButtonRoadBuilding(Street street) {
    	Button b=buttonsOfTheStreets.get(street);
    	if(b.isDisabled()) roadBuildingStreetsActivated.add(street);
        b.setDisable(false);
        b.setOpacity(0.9);
    }
    /**
     * deactivates the streets that were activated because of the road building card
     */
    public void deactivateRoadBuildingStreets() {
    	for(Street street : roadBuildingStreetsActivated){
            buttonsOfTheStreets.get(street).setDisable(true);
            buttonsOfTheStreets.get(street).setOpacity(0.2);
    	}
    }
    /**
     * Getter for the dropDownMenu
     *
     * @return dropDownMenu
     */
    public VBox getDropDownMenu() {
        return dropDownMenu;
    }

    /**
     * Draws the received evolution card temporarily and very large centered on
     * the screen.
     */
    public void drawReceivedEvolutionCard(String path) {
        Image image = new Image(ResourcePointer.class.getResourceAsStream(path), mainGameScene.getHeight() / 3,
                mainGameScene.getWidth() / 3, true, true);
        ImageView imageView = new ImageView(image);
        layout.getChildren().add(imageView);
        imageView.relocate(mainGameScene.getWidth() / 2 - mainGameScene.getHeight() / 6,
                mainGameScene.getHeight() / 2 - mainGameScene.getHeight() / 6);
        addFadeAnimation(imageView, 2000);
        GameStart.soundManager.playReceivingCardSound();
    }

    /**
     * Draws the building costs card very large centered on
     * the screen.
     */
    public void drawBuildingCostCard() {
    	Image image = new Image(ResourcePointer.class.getResourceAsStream("BuildingCosts.png"), mainGameScene.getHeight() / 2.75,
              mainGameScene.getWidth() / 2.75, true, true);
      imageViewBuildInfo = new ImageView(image);
      imageViewBuildInfo.relocate(mainGameScene.getWidth() / 2 - mainGameScene.getHeight() / 5.5,
              mainGameScene.getHeight() / 2 - mainGameScene.getHeight() / 4);
    	if(imageViewBuildInfo == null || layout.getChildren().contains(imageViewBuildInfo))
    		return;
    	GameStart.soundManager.playReceivingCardSound();
    	FadeTransition fadeIn = new FadeTransition(Duration.millis(650), imageViewBuildInfo);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    	layout.getChildren().add(imageViewBuildInfo);
    	isBuildingCostsDrawn = true;
    }
    /**
     * Hides the building cost panel
     */
    public void hideBuildingCostCard() {
    	if(imageViewBuildInfo == null || !layout.getChildren().contains(imageViewBuildInfo))
    		return;
    	GameStart.soundManager.playSoundAccept();
    	FadeTransition fadeOut = new FadeTransition(Duration.millis(650), imageViewBuildInfo);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.play();
        fadeOut.setOnFinished(e -> {
            if (layout.getChildren().contains(imageViewBuildInfo))
                layout.getChildren().remove(imageViewBuildInfo);
        });
        isBuildingCostsDrawn = false;

    }

    /**
     * Displays a view for "two road building"
     */
    public void showTwoRoadBuildingView() {
        // Change background
        changeBackgroundToStreets();
        displayLargeMessage("Select a street");
        updateDevelopmentCardView();
    }

    public Boolean getWorldIsDrawn() {
        return worldIsDrawn;
    }

    /**
     * Displays a larger notification message
     *
     * @param message The message to display.
     */
    public void displayLargeMessage(String message) {
        displayLargePersistentMessage(message);
        // largeNotificationMessage is our new message now (but non Persistent
        // this
        // time)
        largeNotificationMessage.setBackground(new Background(new BackgroundFill(Color.BISQUE, null, null)));
        // Deactivate after some time
        addFadeAnimation(largeNotificationMessage, 3000);
    }

    /**
     * Highlights a (street) button
     *
     * @param street
     */
    public void highlightStreetButton(Street street) {
        GameStart.mainLogger.getLOGGER().fine("Highlight street");
        // Before de-highlight last highlighted street
        dehighlightStreetButton();
        highlightStreetButton(street, "CobblestoneUnderConstruction.png");
    }

    /**
     * Highlights a (street) button with a given texture file path
     *
     * @param street
     */
    private void highlightStreetButton(Street street, String fileName) {
        highlightedStreet = street;
        Rectangle imageHolder1 = new Rectangle(hexagonRadius, hexagonRadius / 5);
        // Load image
        Image image = new Image(ResourcePointer.class.getResourceAsStream(fileName));
        // Set image
        getButtonsOfTheStreets().get(street).setGraphic(imageHolder1);
        ((Rectangle) imageHolder1).setFill((new ImagePattern(image, 0, 0, 1, 1, true)));
    }

    /**
     * @param worldIsDrawn the worldIsDrawn to set
     */
    public void setWorldIsDrawn(Boolean worldIsDrawn) {
        this.worldIsDrawn = worldIsDrawn;
    }

    /**
     * De-highlights a (street) button(if building card request rejected by
     * server)
     *
     * @param street
     */
    public void dehighlightStreetButton() {
        if (highlightedStreet == null)
            return;
        Rectangle imageHolder1 = new Rectangle(hexagonRadius, hexagonRadius / 5);
        // Load image
        Image image = new Image(ResourcePointer.class.getResourceAsStream("CobblestoneDefault.png"));
        // Set image
        getButtonsOfTheStreets().get(highlightedStreet).setGraphic(imageHolder1);
        ((Rectangle) imageHolder1).setFill((new ImagePattern(image, 0, 0, 1, 1, true)));
        highlightedStreet = null;
    }

    /**
     * Deactivates all unoccupied wayPoint buttons.
     */
    public void deactivateAllUnoccupiedWayPointButtons() {
        GameStart.mainLogger.getLOGGER().fine("deactivateAllUnoccupiedWayPointButtons");
        GameStart.siedlerVonCatan.getGameWorld().getWayPoints().forEach(wp -> {
            if (!wp.getSettlement().isOccupied() && buttonsOfTheWorldMatrix.get(wp) != null)
                buttonsOfTheWorldMatrix.get(wp).setDisable(true);
        });
    }

    /**
     * Method which gets executed every time the turn starts
     */
    public void onTurnStart() {
        InGameController.diceButton.setDisable(false);
        displayNotificationMessage("Your turn!");
    }

    /**
     * Method which gets executed every time the turn ends
     */
    public void onTurnEnd() {
        // De-highlight last marked street (if existing)
        dehighlightStreetButton();
        // Update development cards number
        updateDevelopmentCardView();
        // Cancels the activation of a knight/ roadbuilding card
        resetKnightCardChosen();
        resetStreetsCardChosen();
    }

    /**
     * Resets the highlightedStreet (to null)
     */
    public void resetHighlightedStreet() {
        highlightedStreet = null;
    }

    /**
     * Cancels the activation of a knightcard
     */
    public void resetKnightCardChosen() {
        for (Player player : GameStart.siedlerVonCatan.getPlayers()) {
            player.setMoveThiefDueToKnightCard(false);
        }
        GameStart.gameView.changeBackgroundToDefault();
    }

    /**
     * Cancels the activation of a roadbuildingcard
     */
    public void resetStreetsCardChosen() {
        GameStart.siedlerVonCatan.setRoadBuildingCardPlayed(false);
        deactivateRoadBuildingStreets();
        InGameController.resetStreetsChosenForRoadBuildingCard();
        GameStart.gameView.changeBackgroundToDefault();
        dehighlightStreetButton();
        roadBuildingStreetsActivated.clear();
    }

    /**
     * Removes trading/development card menu(s) if existing
     */
    public void removeMenus() {
        if (yearOfPleantyWindow != null && layout.getChildren().contains(yearOfPleantyWindow))
            layout.getChildren().remove(yearOfPleantyWindow);
        if (monopolyWindow != null && layout.getChildren().contains(monopolyWindow))
            layout.getChildren().remove(monopolyWindow);
        if (dropDownMenu != null && layout.getChildren().contains(dropDownMenu))
            layout.getChildren().remove(dropDownMenu);
        if (tradingWindow != null && layout.getChildren().contains(tradingWindow))
            layout.getChildren().remove(tradingWindow);
        if (playerTradingWindow != null && layout.getChildren().contains(playerTradingWindow))
            layout.getChildren().remove(playerTradingWindow);
    }

    /**
     * Displays a menu to cancel a trade (that was accepted prior)
     *
     * @param tradeID
     */
    public void displayRejectTradeOption(Integer tradeID) {
        // Display larger information (add it to accepted trade view list)
        // Screen screen = Screen.getPrimary();
        double sceneWidth = mainGameScene.getWidth();
        double sceneHeight = mainGameScene.getHeight();
        Font font = Font.loadFont(ResourcePointer.class.getResource("PaladinFLF.ttf").toExternalForm(),
                sceneHeight / 55);
        if (abortTradeButton != null && getLayout().getChildren().contains(abortTradeButton))
            getLayout().getChildren().remove(abortTradeButton);

        abortTradeButton = new Button("Abort Trade");
        abortTradeButton.setEffect(dropShadowHexagon);
        abortTradeButton.setId("MenuButton");
        abortTradeButton.setFont(font);
        abortTradeButton.relocate(sceneWidth * 1 / 20, sceneHeight * 15 / 17);
        layout.getChildren().add(abortTradeButton);
        inGameController.defineTraderCancelButton(abortTradeButton,tradeID);
    }

    /**
     * Removes player from candiodate list
     * @param id
     */
    public void removeCandidate(Integer id) {
        if(acceptedTradeRequestHBox == null)
            return;
        for (Node node : acceptedTradeRequestHBox.getChildren()) {
            Button button = (Button) node;
            if (GameStart.siedlerVonCatan.findPlayerByID(id).getTeam().toString().equals(button.getText())) {
                acceptedTradeRequestHBox.getChildren().remove(node);
                return;
            }
        }
    }
    /**
     * relocates the avatar of the player whose turn it is
     * sets the position of the other players back to the original one
     * @param player player whose status got updated - we need to check whether their avatar should be relocated, and if so- do it
     */
    public void relocatePlayerAvatars(){
    	if(!avatarsCreated) return;
    	for(Player player: GameStart.siedlerVonCatan.getPlayers()){
            if(player.getStatus().equals("Warten")||player.getStatus().equals("Wartet auf Spielbeginn"))
            	playerToAvatar.get(player).setLayoutY(defaultAvatarYPos);
            else playerToAvatar.get(player).setLayoutY(defaultAvatarYPos-10);
    	}
    }
}