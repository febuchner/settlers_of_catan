package tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import application.GameStart;

/**
 * Translates world coordinates into coordinates within the date structure and
 * the other way round.
 *
 * @author Minh, Felip
 */
public class WorldTranslation {

	/**
	 * The resource translation in a hash map.
	 */
	public static final HashMap<String, FieldType> SERVERTYPE_TO_CLIENTTYPE;

	static {
		SERVERTYPE_TO_CLIENTTYPE = new HashMap<String, FieldType>();
		SERVERTYPE_TO_CLIENTTYPE.put("Ackerland", FieldType.GRAIN);
		SERVERTYPE_TO_CLIENTTYPE.put("Huegelland", FieldType.LOAM);
		SERVERTYPE_TO_CLIENTTYPE.put("Weideland", FieldType.WOOL);
		SERVERTYPE_TO_CLIENTTYPE.put("Wald", FieldType.WOOD);
		SERVERTYPE_TO_CLIENTTYPE.put("Gebirge", FieldType.STONE);
		SERVERTYPE_TO_CLIENTTYPE.put("Wueste", FieldType.DESERT);
		SERVERTYPE_TO_CLIENTTYPE.put("Meer", FieldType.WATER);
	}

	/**
	 * The coordinate translation in a hash map.
	 */
	public static final HashMap<String, Vector2D<Integer>> LETTER_TO_VECTOR;

	static {
		LETTER_TO_VECTOR = new HashMap<String, Vector2D<Integer>>();

		LETTER_TO_VECTOR.put("a", new Vector2D<Integer>(0, 3));
		LETTER_TO_VECTOR.put("b", new Vector2D<Integer>(0, 4));
		LETTER_TO_VECTOR.put("c", new Vector2D<Integer>(0, 5));
		LETTER_TO_VECTOR.put("d", new Vector2D<Integer>(0, 6));

		LETTER_TO_VECTOR.put("e", new Vector2D<Integer>(1, 2));
		LETTER_TO_VECTOR.put("f", new Vector2D<Integer>(1, 6));

		LETTER_TO_VECTOR.put("g", new Vector2D<Integer>(2, 1));
		LETTER_TO_VECTOR.put("h", new Vector2D<Integer>(2, 6));

		LETTER_TO_VECTOR.put("i", new Vector2D<Integer>(3, 0));
		LETTER_TO_VECTOR.put("j", new Vector2D<Integer>(3, 6));

		LETTER_TO_VECTOR.put("k", new Vector2D<Integer>(4, 0));
		LETTER_TO_VECTOR.put("l", new Vector2D<Integer>(4, 5));

		LETTER_TO_VECTOR.put("m", new Vector2D<Integer>(5, 0));
		LETTER_TO_VECTOR.put("n", new Vector2D<Integer>(5, 4));

		LETTER_TO_VECTOR.put("o", new Vector2D<Integer>(6, 0));
		LETTER_TO_VECTOR.put("p", new Vector2D<Integer>(6, 1));
		LETTER_TO_VECTOR.put("q", new Vector2D<Integer>(6, 2));
		LETTER_TO_VECTOR.put("r", new Vector2D<Integer>(6, 3));

		LETTER_TO_VECTOR.put("A", new Vector2D<Integer>(1, 3));
		LETTER_TO_VECTOR.put("B", new Vector2D<Integer>(1, 4));
		LETTER_TO_VECTOR.put("C", new Vector2D<Integer>(1, 5));

		LETTER_TO_VECTOR.put("L", new Vector2D<Integer>(2, 2));
		LETTER_TO_VECTOR.put("M", new Vector2D<Integer>(2, 3));
		LETTER_TO_VECTOR.put("N", new Vector2D<Integer>(2, 4));
		LETTER_TO_VECTOR.put("D", new Vector2D<Integer>(2, 5));

		LETTER_TO_VECTOR.put("K", new Vector2D<Integer>(3, 1));
		LETTER_TO_VECTOR.put("R", new Vector2D<Integer>(3, 2));
		LETTER_TO_VECTOR.put("S", new Vector2D<Integer>(3, 3));
		LETTER_TO_VECTOR.put("O", new Vector2D<Integer>(3, 4));
		LETTER_TO_VECTOR.put("E", new Vector2D<Integer>(3, 5));

		LETTER_TO_VECTOR.put("J", new Vector2D<Integer>(4, 1));
		LETTER_TO_VECTOR.put("Q", new Vector2D<Integer>(4, 2));
		LETTER_TO_VECTOR.put("P", new Vector2D<Integer>(4, 3));
		LETTER_TO_VECTOR.put("F", new Vector2D<Integer>(4, 4));

		LETTER_TO_VECTOR.put("I", new Vector2D<Integer>(5, 1));
		LETTER_TO_VECTOR.put("H", new Vector2D<Integer>(5, 2));
		LETTER_TO_VECTOR.put("G", new Vector2D<Integer>(5, 3));


	}

	/**
	 * The coordinate translation in a hash map.
	 */
	public static final HashMap<Vector2D<Integer>, String> COORDINATE_TO_LETTER;

	static {
		COORDINATE_TO_LETTER = new HashMap<Vector2D<Integer>, String>();

		COORDINATE_TO_LETTER.put(new Vector2D<Integer>(0, 3), "a");
		COORDINATE_TO_LETTER.put(new Vector2D<Integer>(1, 3), "b");
		COORDINATE_TO_LETTER.put(new Vector2D<Integer>(2, 3), "c");
		COORDINATE_TO_LETTER.put(new Vector2D<Integer>(3, 3),"d");

		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-1, 2),"e");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(3, 2), "f");

		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-2, 1),"g");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(3, 1), "h");

		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-3, 0),"i");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(3, 0), "j");

		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-3, -1),"k");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(2, -1), "l");

		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-3, -2), "m");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(1, -2),"n");

		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-3, -3),"o");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-2, -3),"p");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-1, -3),"q");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(0, -3), "r");

		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(0, 2),"A");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(1, 2),"B");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(2, 2),"C");

		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-1, 1), "L");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(0, 1),"M");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(1, 1), "N");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(2, 1), "D");

		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-2, 0), "K");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-1, 0), "R");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(0, 0),"S");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(1, 0), "O");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(2, 0),"E");

		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-2, -1),"J");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-1, -1),"Q");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(0, -1), "P");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(1, -1),"F");

		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-2, -2),"I");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(-1, -2),"H");
		COORDINATE_TO_LETTER.put( new Vector2D<Integer>(0, -2),"G");
	}

	/**
	 * Takes the position of a waypoint represented with a String and three
	 * characters and returns the coordinates of the waypoints as a
	 * Vector3D<Integer>
	 *
	 * @param position
	 * @return waypoint (coordinates)
	 */
	public static Vector3D<Integer> getWayPointFromSurroundingFields(String position) {
		ArrayList<Vector2D<Integer>> fieldsAround = new ArrayList<Vector2D<Integer>>();
		for (int i = 0; i < 3; i++) {
			fieldsAround.add(getLetterToPosition("" + position.charAt(i)));
		}
		int x = fieldsAround.get(0).x == fieldsAround.get(1).x ? fieldsAround.get(0).x : fieldsAround.get(2).x;
		int y = fieldsAround.get(0).y > fieldsAround.get(1).y ? fieldsAround.get(0).y
				: (fieldsAround.get(1).y > fieldsAround.get(2).y) ? fieldsAround.get(1).y : fieldsAround.get(2).y;
		int z = (fieldsAround.get(0).x < x) | (fieldsAround.get(1).x < x) | (fieldsAround.get(2).x < x) ? 1 : 2;
		Vector3D<Integer> waypoint = new Vector3D<Integer>(x, y, z);
		return waypoint;
	}

	/**
	 * Takes the position of a street represented with a String with two characters
	 * and returns an array with the coordinates of the waypoints in the worldmatrix
	 * for this street
	 *
	 * @param position
	 *            (String with two characters)
	 * @return wayPoints
	 */
	public static Vector3D<Integer>[] getWaypointsOfStreet(String position) {
		Vector3D<Integer>[] wayPoints = new Vector3D[2];

		ArrayList<Vector2D<Integer>> fieldsAroundStreet = new ArrayList<Vector2D<Integer>>();
		for (int i = 0; i < 2; i++) {
			fieldsAroundStreet.add(getLetterToPosition("" + position.charAt(i)));
		}
		int x1 = fieldsAroundStreet.get(0).x;
		int x2 = fieldsAroundStreet.get(1).x;
		int y1 = fieldsAroundStreet.get(0).y;
		int y2 = fieldsAroundStreet.get(1).y;

		String neighbour1 = "";
		String neighbour2 = "";

		if (y1 == y2 && x1 < x2) {
			// GameStart.mainLogger.getLOGGER().fine("C");

			neighbour1 = getPositionToLetter(new Vector2D<Integer>(x2, y2 - 1));
			neighbour2 = getPositionToLetter(new Vector2D<Integer>(x1, y1 + 1));
		} else if (y1 == y2 && x1 > x2) {
			// GameStart.mainLogger.getLOGGER().fine("D");

			neighbour1 = getPositionToLetter(new Vector2D<Integer>(x1, y1 - 1));
			neighbour2 = getPositionToLetter(new Vector2D<Integer>(x2, y2 + 1));
		} else if (y1 > y2 && x1 == x2) {
			// GameStart.mainLogger.getLOGGER().fine("E");

			neighbour1 = getPositionToLetter(new Vector2D<Integer>(x2 + 1, y2));
			neighbour2 = getPositionToLetter(new Vector2D<Integer>(x1 - 1, y1));
		} else if (y1 < y2 && x1 == x2) {
			// GameStart.mainLogger.getLOGGER().fine("F");

			neighbour1 = getPositionToLetter(new Vector2D<Integer>(x2 - 1, y2));
			neighbour2 = getPositionToLetter(new Vector2D<Integer>(x1 + 1, y1));
		} else if (y1 > y2 && x1 < x2) {
			// GameStart.mainLogger.getLOGGER().fine("A");

			neighbour1 = getPositionToLetter(new Vector2D<Integer>(x2, y2 + 1));
			neighbour2 = getPositionToLetter(new Vector2D<Integer>(x1, y1 - 1));
		} else if (y1 < y2 && x1 > x2) {
			// GameStart.mainLogger.getLOGGER().fine("B");

			neighbour1 = getPositionToLetter(new Vector2D<Integer>(x2 + 1, y2));
			neighbour2 = getPositionToLetter(new Vector2D<Integer>(x1 - 1, y1));
		}

		wayPoints[0] = getWayPointFromSurroundingFields(position + neighbour1);
		wayPoints[1] = getWayPointFromSurroundingFields(position + neighbour2);
		return wayPoints;
	}

	/**
	 * Returns the letter matching the data structures coordinate
	 *
	 * @param value
	 *            The data structures coordinate
	 * @return The corresponding letter
	 */
	public static String getPositionToLetter(Vector2D<Integer> value) {
		for (String o : LETTER_TO_VECTOR.keySet()) {
			if (LETTER_TO_VECTOR.get(o).equals(value)) {
				return o;
			}
		}
		GameStart.mainLogger.getLOGGER().fine("REQUESTED FAILURE IN: " + value);
		return "";
	}

	/**
	 * Returns the coordinate within data structure matching the server's
	 * coordinate.
	 *
	 * @param letter
	 *            The server's coordinate.
	 * @return The coordinate within the data structure.
	 */
	public static Vector2D<Integer> getLetterToPosition(String letter) {
		return LETTER_TO_VECTOR.get(letter);
	}

	/**
	 * Returns the servers data type matching a clients (internal datastructure's)
	 * field type.
	 *
	 * @param type
	 *            The internal (client's) field type
	 * @return The corresponding server's type.
	 */
	public static String getClientFieldTypeToServerFieldType(FieldType type) {
		for (String o : SERVERTYPE_TO_CLIENTTYPE.keySet()) {
			if (SERVERTYPE_TO_CLIENTTYPE.get(o).equals(type)) {
				return o;
			}
		}
		return null;
	}

	/**
	 * Returns the coordinate within data structure matching the server's
	 * coordinate.
	 *
	 * @param fieldType
	 *            The server's type.
	 * @return The corresponding data structure type.
	 */
	public static FieldType getServerFieldTypeToClientFieldType(String fieldType) {
		return SERVERTYPE_TO_CLIENTTYPE.get(fieldType);
	}

	/**
	 * Translates server building type into client building type.
	 *
	 * @param typeString
	 *            The server type.
	 * @return The translated corresponding client type.
	 */
	public static BuildingType getServerBuildingTypeToClientBuildingType(String typeString) {
		if (typeString.equals("Strasse"))
			return BuildingType.STREET;
		else if (typeString.equals("Dorf"))
			return BuildingType.VILLAGE;
		else if (typeString.equals("Stadt"))
			return BuildingType.CASTLE;
		else
			return BuildingType.NONE;

	}
	public static PlayerTeam getClientColorType(String colorString){
		switch (colorString) {
			case "Rot": return PlayerTeam.TEAM_RED;
			case "Orange": return PlayerTeam.TEAM_ORANGE;
			case "Blau": return PlayerTeam.TEAM_BLUE;
			case "Weiss": return PlayerTeam.TEAM_WHITE;
		}
		return null;
	}

	public static PortTypes getServerPortTypeToClientPortType(String type) {
		if(type.equals("Holz Hafen")) return PortTypes.TWO_WOOD_FOR_ONE;
		else if(type.equals("Wolle Hafen")) return PortTypes.TWO_WOOL_FOR_ONE;
		else if(type.equals("Lehm Hafen")) return PortTypes.TWO_LOAM_FOR_ONE;
		else if(type.equals("Hafen")) return PortTypes.THREE_FOR_ONE;
		else if(type.equals("Erz Hafen")) return PortTypes.TWO_STONE_FOR_ONE;
		else if(type.equals("Getreide Hafen")) return PortTypes.TWO_GRAIN_FOR_ONE;
		return null;
	}
	/**
	 * Sorts a string by its characters
	 * @param stringToSort
	 * @return The sorted string
	 */
	public static String sortString(String stringToSort) {
		char[] ar = stringToSort.toCharArray();
		Arrays.sort(ar);
		String sorted = String.valueOf(ar);
		GameStart.mainLogger.getLOGGER().fine("BEFORE SORTING:" + stringToSort);
		GameStart.mainLogger.getLOGGER().fine("AFTER SORTING:" + sorted);
		return sorted;
	}
}
