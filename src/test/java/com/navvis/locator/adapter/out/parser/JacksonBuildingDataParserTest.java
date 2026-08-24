package com.navvis.locator.adapter.out.parser;

import com.navvis.locator.domain.model.geometry.Building;
import com.navvis.locator.domain.model.geometry.Floor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonBuildingDataParserTest {

    private final JacksonBuildingDataParser parser =
            new JacksonBuildingDataParser(new ObjectMapper());

    @Test
    @DisplayName("parses a single building with one floor")
    void parseSingleBuilding() {
        byte[] json = """
                [{
                    "name": "Office",
                    "outline": [[0,0],[10,0],[10,10],[0,10],[0,0]],
                    "height": [0, 15],
                    "floors": [{
                        "name": "Ground",
                        "outline": [[0,0],[10,0],[10,10],[0,10],[0,0]],
                        "height": [0, 3]
                    }]
                }]
                """.getBytes();

        List<Building> buildings = parser.parse(json);

        assertEquals(1, buildings.size());
        Building building = buildings.getFirst();
        assertEquals("Office", building.name());
        assertEquals(0, building.height().min());
        assertEquals(15, building.height().max());
        assertEquals(5, building.outline().vertices().size());

        assertEquals(1, building.floors().size());
        Floor floor = building.floors().getFirst();
        assertEquals("Ground", floor.name());
        assertEquals(0, floor.height().min());
        assertEquals(3, floor.height().max());
    }

    @Test
    @DisplayName("parses multiple buildings")
    void parseMultipleBuildings() {
        byte[] json = """
                [
                    {
                        "name": "Building A",
                        "outline": [[0,0],[5,0],[5,5],[0,5],[0,0]],
                        "height": [0, 10],
                        "floors": []
                    },
                    {
                        "name": "Building B",
                        "outline": [[20,20],[30,20],[30,30],[20,30],[20,20]],
                        "height": [0, 20],
                        "floors": []
                    }
                ]
                """.getBytes();

        List<Building> buildings = parser.parse(json);

        assertEquals(2, buildings.size());
        assertEquals("Building A", buildings.get(0).name());
        assertEquals("Building B", buildings.get(1).name());
    }

    @Test
    @DisplayName("parses floor with different outline than building (setback)")
    void parseSetbackFloor() {
        byte[] json = """
                [{
                    "name": "Tower",
                    "outline": [[0,0],[20,0],[20,20],[0,20],[0,0]],
                    "height": [0, 30],
                    "floors": [{
                        "name": "Penthouse",
                        "outline": [[5,5],[15,5],[15,15],[5,15],[5,5]],
                        "height": [25, 30]
                    }]
                }]
                """.getBytes();

        List<Building> buildings = parser.parse(json);
        Floor penthouse = buildings.getFirst().floors().getFirst();

        assertEquals("Penthouse", penthouse.name());
        assertEquals(5, penthouse.outline().vertices().size());
        // Setback outline starts at (5,5), not (0,0)
        assertEquals(5, penthouse.outline().vertices().getFirst().x());
        assertEquals(5, penthouse.outline().vertices().getFirst().y());
    }

    @Test
    @DisplayName("parsed building's containment check works correctly")
    void parsedBuildingContainmentWorks() {
        byte[] json = """
                [{
                    "name": "Office",
                    "outline": [[0,0],[10,0],[10,10],[0,10],[0,0]],
                    "height": [0, 10],
                    "floors": [{
                        "name": "Ground",
                        "outline": [[0,0],[10,0],[10,10],[0,10],[0,0]],
                        "height": [0, 3]
                    }]
                }]
                """.getBytes();

        Building building = parser.parse(json).getFirst();

        assertTrue(building.contains(5, 5, 1));
        assertTrue(building.findFloor(5, 5, 1).isPresent());
        assertEquals("Ground", building.findFloor(5, 5, 1).get().name());
    }

    @Test
    @DisplayName("throws on invalid JSON")
    void invalidJson() {
        byte[] json = "not json".getBytes();

        assertThrows(Exception.class, () -> parser.parse(json));
    }
}